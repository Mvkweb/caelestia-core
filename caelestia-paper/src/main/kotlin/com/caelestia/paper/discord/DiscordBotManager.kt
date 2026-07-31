package com.caelestia.paper.discord

import com.caelestia.paper.CaelestiaPlugin
import com.caelestia.paper.bridge.BridgeMessage
import com.caelestia.paper.bridge.Source
import com.caelestia.paper.bridge.Type
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.util.UUID

class DiscordBotManager(private val plugin: CaelestiaPlugin) {
    var jda: JDA? = null
        private set
        
    val webhookManager = WebhookManager(plugin, this)
    
    val messageCache = object : java.util.LinkedHashMap<String, Pair<String, String>>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<String, String>>): Boolean {
            return size > 100
        }
    }

    fun init() {
        val config = plugin.caelestiaConfig
        if (!config.isValidDiscordConfig()) {
            plugin.logger.warning("Discord config is invalid. Please check config.yml.")
            return
        }


        plugin.logger.info("Starting JDA...")
        try {
            jda = JDABuilder.createLight(config.discordBotToken)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_EMOJIS_AND_STICKERS)
                .enableCache(CacheFlag.EMOJI)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(DiscordChatListener(plugin))
                .build()
                
            jda?.awaitReady()
            if (config.discordGuildId.isNotBlank()) {
                val guild = jda?.getGuildById(config.discordGuildId)
                guild?.updateCommands()?.addCommands(
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash("status", "View server status and performance")
                )?.queue()
                
                if (guild != null) {
                    syncEmojis(guild)
                }
            }
            plugin.logger.info("JDA started successfully!")
            
            webhookManager.init()
        } catch (e: Exception) {
            plugin.logger.severe("Failed to start JDA: ${e.message}")
            e.printStackTrace()
        }
    }

    fun shutdown() {
        plugin.logger.info("Shutting down JDA...")
        com.caelestia.paper.emoji.HttpPackServer.stop()
        webhookManager.shutdown()
        jda?.shutdown()
        jda = null
    }

    private fun syncEmojis(guild: net.dv8tion.jda.api.entities.Guild) {
        plugin.logger.info("Syncing Discord Emojis...")
        val packAssetsDir = java.io.File(plugin.dataFolder, "pack_assets")
        packAssetsDir.mkdirs()
        val customEmojisDir = java.io.File(plugin.dataFolder, "custom-emojis")
        customEmojisDir.mkdirs()
        
        val mappings = mutableMapOf<String, String>()
        var currentUnicode = 0xE000
        
        // 1. Process Local Custom Emojis
        val localFiles = customEmojisDir.listFiles { _, name -> name.endsWith(".png") }
        if (localFiles != null) {
            for (localFile in localFiles) {
                val emojiName = localFile.nameWithoutExtension
                val targetFile = java.io.File(packAssetsDir, "${emojiName}.png")
                val size = plugin.packConfig.emojiSizes[emojiName] ?: 32
                
                if (com.caelestia.paper.emoji.EmojiProcessor.resizeLocal(localFile, targetFile, size)) {
                    val unicodeStr = String(Character.toChars(currentUnicode))
                    mappings[emojiName] = unicodeStr
                    com.caelestia.paper.emoji.EmojiRegistry.discordToMcMap[emojiName] = unicodeStr
                    com.caelestia.paper.emoji.EmojiRegistry.mcToDiscordMap[unicodeStr] = ":$emojiName:"
                    currentUnicode++
                }
            }
        }
        
        // 2. Process Discord Emojis
        val emojis = guild.emojis
        for (emoji in emojis) {
            // Skip if a local emoji already took this name
            if (mappings.containsKey(emoji.name)) continue
            
            val targetFile = java.io.File(packAssetsDir, "${emoji.name}.png")
            val url = emoji.imageUrl.replace(".gif", ".png")
            val size = plugin.packConfig.emojiSizes[emoji.name] ?: 32
            
            if (com.caelestia.paper.emoji.EmojiProcessor.downloadAndResize(url, targetFile, size)) {
                val unicodeStr = String(Character.toChars(currentUnicode))
                mappings[emoji.name] = unicodeStr
                com.caelestia.paper.emoji.EmojiRegistry.discordToMcMap[emoji.name] = unicodeStr
                com.caelestia.paper.emoji.EmojiRegistry.mcToDiscordMap[unicodeStr] = emoji.asMention
                currentUnicode++
            }
        }
        
        com.caelestia.paper.emoji.EmojiRegistry.discordToMcMap.clear()
        com.caelestia.paper.emoji.EmojiRegistry.mcToDiscordMap.clear()
        com.caelestia.paper.emoji.EmojiRegistry.discordToMcMap.putAll(mappings)
        for (emoji in emojis) {
            val unicode = mappings[emoji.name]
            if (unicode != null) {
                com.caelestia.paper.emoji.EmojiRegistry.mcToDiscordMap[unicode] = emoji.asMention
            }
        }
        
        val outputZip = java.io.File(plugin.dataFolder, "emojis.zip")
        com.caelestia.paper.emoji.PackGenerator.generatePack(outputZip, mappings, packAssetsDir)
        
        com.caelestia.paper.emoji.HttpPackServer.start(plugin.packConfig.rpServerPort, outputZip)
        plugin.logger.info("Emoji sync complete! Synced ${mappings.size} emojis.")
        plugin.bridgeManager?.sendEmojiSync()
    }

    fun getChannel(): TextChannel? {
        val channelId = plugin.caelestiaConfig.discordChannelId
        return jda?.getTextChannelById(channelId)
    }

    fun relayBridgeMessage(msg: BridgeMessage) {
        // If it came from Discord originally, don't echo it back
        if (msg.source == Source.DISCORD) return
        
        val config = plugin.caelestiaConfig
        
        if (config.featUseWebhooks && msg.playerName != null && msg.playerUuid != null) {
            val uuid = try { UUID.fromString(msg.playerUuid) } catch (e: Exception) { null }
            if (uuid != null) {
                webhookManager.sendMessage(msg.playerName, msg.message)
                return
            }
        }
        
        // Use regular bot message
        val prefix = if (msg.source == Source.NEOFORGE) "**[NeoForge]** " else ""
        val text = if (msg.playerName != null) {
            "$prefix**${msg.playerName}** » ${msg.message}"
        } else {
            "$prefix${msg.message}"
        }
        
        sendMessage(text)
    }

    fun sendWebhook(username: String, content: String) {
        webhookManager.sendMessage(username, content)
    }

    fun replyToMessage(username: String, uuidStr: String, discordMessageId: String, replyContent: String) {
        val cached = messageCache[discordMessageId]
        val formattedContent = if (cached != null) {
            val author = cached.first
            var text = cached.second
            if (text.length > 40) text = text.take(37) + "..."
            "> **$author** _${text}_\n$replyContent"
        } else {
            replyContent
        }
        sendWebhook(username, formattedContent)
    }

    fun sendMessage(text: String) {
        getChannel()?.sendMessage(text)?.queue({}, { err ->
            plugin.logger.warning("Failed to send message to Discord: ${err.message}")
        })
    }
}
