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

        // Parse format colors/markdown to mini message
        val miniMessageStr = ColorUtil.formatDiscordToMinecraft(configMsg)
        val component = ColorUtil.parseMiniMessage(miniMessageStr)

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
            message = content
        )
        plugin.bridgeManager.broadcast(bridgeMessage)
    }
}
