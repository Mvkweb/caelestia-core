package com.caelestia.paper.discord

import com.caelestia.paper.CaelestiaPlugin
import com.caelestia.paper.bridge.BridgeMessage
import com.caelestia.paper.bridge.Source
import com.caelestia.paper.bridge.Type
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.GatewayIntent
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
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(DiscordChatListener(plugin))
                .build()
                
            jda?.awaitReady()
            if (config.discordGuildId.isNotBlank()) {
                jda?.getGuildById(config.discordGuildId)?.updateCommands()?.addCommands(
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash("status", "View server status and performance")
                )?.queue()
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
        webhookManager.shutdown()
        jda?.shutdown()
        jda = null
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
                webhookManager.sendMessage(msg.playerName, uuid, msg.message)
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

    fun sendWebhook(username: String, uuidStr: String, content: String) {
        val uuid = try { UUID.fromString(uuidStr) } catch (e: Exception) { null }
        webhookManager.sendMessage(username, uuid, content)
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
        sendWebhook(username, uuidStr, formattedContent)
    }

    fun sendMessage(text: String) {
        getChannel()?.sendMessage(text)?.queue({}, { err ->
            plugin.logger.warning("Failed to send message to Discord: ${err.message}")
        })
    }
}
