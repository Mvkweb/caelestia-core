package com.caelestia.paper.discord

import com.caelestia.paper.CaelestiaPlugin
import com.caelestia.paper.bridge.BridgeMessage
import com.caelestia.paper.bridge.Source
import com.caelestia.paper.bridge.Type
import com.caelestia.paper.util.ColorUtil
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.bukkit.Bukkit

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

        if (event.message.attachments.isNotEmpty()) {
            val count = event.message.attachments.size
            val attachmentText = if (count == 1) "[Attached 1 file]" else "[Attached $count files]"
            content = if (content.isBlank()) {
                attachmentText
            } else {
                "$content $attachmentText"
            }
        }


        val configMsg = plugin.caelestiaConfig.msgDiscordToMc
            .replace("%username%", username)
            .replace("%message%", content)

        // Cache for replying
        plugin.discordBot.messageCache[event.messageId] = Pair(username, content)

        // Parse format colors/markdown to mini message
        val miniMessageStr = ColorUtil.formatDiscordToMinecraft(configMsg)
        val component = ColorUtil.parseMiniMessage(miniMessageStr)
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/discordreply ${event.messageId} "))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text("Click to reply to $username!")))

        // Broadcast to Minecraft main thread
        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.broadcast(component)
        })

        // Broadcast to Bridge for NeoForge
        val bridgeMessage = BridgeMessage(
            source = Source.DISCORD,
            type = Type.CHAT,
            playerName = username,
            playerUuid = null,
            messageId = event.messageId,
            message = content
        )
        plugin.bridgeManager.broadcast(bridgeMessage)
    }

    override fun onSlashCommandInteraction(event: net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent) {
        if (event.channel.id != plugin.caelestiaConfig.discordChannelId) {
            event.reply("This command can only be used in the designated channel.").setEphemeral(true).queue()
            return
        }

        if (event.name == "status") {
            event.deferReply().queue()
            
            // Generate Paper spark stats
            val spark = me.lucko.spark.api.SparkProvider.get()
            val tps = spark.tps()?.poll(me.lucko.spark.api.statistic.StatisticWindow.TicksPerSecond.MINUTES_5) ?: 0.0
            val msptInfo = spark.mspt()?.poll(me.lucko.spark.api.statistic.StatisticWindow.MillisPerTick.MINUTES_1)
            val mspt = msptInfo?.mean() ?: 0.0
            val cpu = spark.cpuSystem().poll(me.lucko.spark.api.statistic.StatisticWindow.CpuUsage.MINUTES_1) * 100
            
            val usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
            val maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024
            val players = "${org.bukkit.Bukkit.getOnlinePlayers().size} / ${org.bukkit.Bukkit.getMaxPlayers()}"
            val chunks = org.bukkit.Bukkit.getWorlds().sumOf { it.loadedChunks.size }.toString()
            
            val uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().uptime
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
            
            val reqId = java.util.UUID.randomUUID().toString()
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
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                val pending = plugin.bridgeManager.pendingSparkRequests.remove(reqId)
                if (pending != null) {
                    val embed = net.dv8tion.jda.api.EmbedBuilder()
                        .setTitle("Caelestia Server Status")
                        .setColor(java.awt.Color.decode("#A259FF"))
                        .addField("Vanilla Server", "TPS: ${paperStats["tps"]}\nMSPT: ${paperStats["mspt"]}ms\nCPU: ${paperStats["cpu"]}%\nRAM: ${paperStats["ram"]}\nPlayers: ${paperStats["players"]}\nChunks: ${paperStats["chunks"]}\nUptime: ${paperStats["uptime"]}", false)
                        .addField("Modded Server", "Offline / Unreachable", false)
                        .build()
                    pending.first.editOriginalEmbeds(embed).queue()
                }
            }, 60L)
        }
    }
}
