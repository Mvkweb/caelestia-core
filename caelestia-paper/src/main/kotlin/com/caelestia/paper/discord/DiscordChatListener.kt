package com.caelestia.paper.discord

import com.caelestia.paper.CaelestiaPlugin
import com.caelestia.paper.bridge.BridgeMessage
import com.caelestia.paper.bridge.Source
import com.caelestia.paper.bridge.Type
import com.caelestia.paper.util.ColorUtil
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.bukkit.Bukkit
import com.caelestia.paper.emoji.EmojiRegistry
import net.kyori.adventure.text.Component as AdventureComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor as AdventureTextColor
import net.kyori.adventure.text.event.ClickEvent as AdventureClickEvent
import net.kyori.adventure.text.event.HoverEvent as AdventureHoverEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import me.lucko.spark.api.SparkProvider
import me.lucko.spark.api.statistic.StatisticWindow
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color
import java.lang.management.ManagementFactory
import java.util.UUID

class DiscordChatListener(private val plugin: CaelestiaPlugin) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        // Ignore bots and webhooks
        if (event.author.isBot || event.isWebhookMessage) return

        // Ignore messages from other channels
        if (event.channel.id != plugin.caelestiaConfig.discordChannelId) return

        val member = event.member ?: return
        val username = member.effectiveName
        val contentRaw = event.message.contentDisplay
        var content = contentRaw.replace(Regex("```[\\s\\S]*?```"), "[Code snippet hidden]")
        content = EmojiRegistry.translateDiscordToMc(content)

        if (event.message.attachments.isNotEmpty()) {
            val count = event.message.attachments.size
            val attachmentText = if (count == 1) "[Attached 1 file]" else "[Attached $count files]"
            content = if (content.isBlank()) {
                attachmentText
            } else {
                "$content $attachmentText"
            }
        }

        // Check if this is a reply to another message
        val referencedMessage = event.message.referencedMessage
        var refAuthor: String? = null
        var refContent: String? = null
        if (referencedMessage != null) {
            refAuthor = if (referencedMessage.isWebhookMessage) {
                referencedMessage.author.name
            } else {
                referencedMessage.member?.effectiveName ?: referencedMessage.author.effectiveName
            }
            refContent = referencedMessage.contentDisplay.let {
                if (it.length > 40) it.substring(0, 40) + "..." else it
            }
        }

        val finalUsername = if (refAuthor != null) {
            "<dark_gray>[Replying to $refAuthor]</dark_gray> $username"
        } else {
            username
        }
        
        var finalMsg = plugin.caelestiaConfig.msgDiscordToMc
            .replace("%username%", finalUsername)
            .replace("%message%", content)

        finalMsg = EmojiRegistry.translateDiscordToMc(finalMsg)

        // Cache for replying
        plugin.discordBot.messageCache[event.messageId] = Pair(username, content)

        // Parse format colors/markdown to mini message
        val miniMessageStr = ColorUtil.formatDiscordToMinecraft(finalMsg)
        
        val component = ColorUtil.parseMiniMessage(miniMessageStr)
            .clickEvent(AdventureClickEvent.suggestCommand("/discordreply ${event.messageId} "))
            .hoverEvent(AdventureHoverEvent.showText(AdventureComponent.text("Click to reply to $username!")))

        // Broadcast to Minecraft main thread
        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.broadcast(component)
        })

        // Broadcast to Bridge for NeoForge - include reply context if present
        val bridgeContent = if (refAuthor != null && refContent != null) {
            "$refAuthor: $refContent\n$content"
        } else {
            content
        }
        val bridgeType = if (refAuthor != null && refContent != null) Type.DISCORD_REPLY else Type.CHAT
        val bridgeMessage = BridgeMessage(
            source = Source.DISCORD,
            type = bridgeType,
            playerName = username,
            playerUuid = null,
            messageId = event.messageId,
            message = bridgeContent
        )
        plugin.bridgeManager.broadcast(bridgeMessage)
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.channel.id != plugin.caelestiaConfig.discordChannelId) {
            event.reply("This command can only be used in the designated channel.").setEphemeral(true).queue()
            return
        }

        if (event.name == "status") {
            event.deferReply().queue()
            
            // Generate Paper spark stats
            val spark = SparkProvider.get()
            val tps = spark.tps()?.poll(StatisticWindow.TicksPerSecond.MINUTES_5) ?: 0.0
            val msptInfo = spark.mspt()?.poll(StatisticWindow.MillisPerTick.MINUTES_1)
            val mspt = msptInfo?.mean() ?: 0.0
            val cpu = spark.cpuSystem().poll(StatisticWindow.CpuUsage.MINUTES_1) * 100
            
            val usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
            val maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024
            val players = "${Bukkit.getOnlinePlayers().size} / ${Bukkit.getMaxPlayers()}"
            val chunks = Bukkit.getWorlds().sumOf { it.loadedChunks.size }.toString()
            
            val uptimeMs = ManagementFactory.getRuntimeMXBean().uptime
            val seconds = (uptimeMs / 1000) % 60
            val minutes = (uptimeMs / (1000 * 60)) % 60
            val hours = (uptimeMs / (1000 * 60 * 60)) % 24
            val days = (uptimeMs / (1000 * 60 * 60 * 24))
            val uptimeStr = if (days > 0) "${days}d ${hours}h ${minutes}m" else "${hours}h ${minutes}m ${seconds}s"
            
            val paperStats = mapOf(
                "tps" to String.format("%.2f", tps),
                "mspt" to String.format("%.2f", mspt),
                "cpu" to String.format("%.2f", cpu),
                "ram" to "${usedMem}MB / ${maxMem}MB",
                "players" to players,
                "chunks" to chunks,
                "uptime" to uptimeStr
            )
            
            val reqId = UUID.randomUUID().toString()
            plugin.bridgeManager.pendingSparkRequests[reqId] = Pair(event.hook, paperStats)
            
            plugin.bridgeManager.broadcast(BridgeMessage(
                source = Source.PAPER,
                type = Type.SPARK_REQUEST,
                playerName = null,
                playerUuid = null,
                messageId = reqId,
                message = ""
            ))
            
            // Timeout after 3 seconds if NeoForge doesn't respond
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                val pending = plugin.bridgeManager.pendingSparkRequests.remove(reqId)
                if (pending != null) {
                    val embed = EmbedBuilder()
                        .setTitle("Caelestia Server Status")
                        .setColor(Color.decode("#A259FF"))
                        .addField("Vanilla Server", "TPS: ${paperStats["tps"]}\nMSPT: ${paperStats["mspt"]}ms\nCPU: ${paperStats["cpu"]}%\nRAM: ${paperStats["ram"]}\nPlayers: ${paperStats["players"]}\nChunks: ${paperStats["chunks"]}\nUptime: ${paperStats["uptime"]}", false)
                        .addField("Modded Server", "Offline / Unreachable", false)
                        .build()
                    pending.first.editOriginalEmbeds(embed).queue()
                }
            }, 60L)
        }
    }
}
