package com.caelestia.paper.listeners

import com.caelestia.paper.CaelestiaPlugin
import com.caelestia.paper.bridge.BridgeMessage
import com.caelestia.paper.bridge.Source
import com.caelestia.paper.bridge.Type
import com.caelestia.paper.util.ColorUtil
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class MinecraftChatListener(private val plugin: CaelestiaPlugin) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        // Convert the component to plain text for Discord
        val contentStr = (event.message() as net.kyori.adventure.text.TextComponent).content()
        var mcFormatted = contentStr
        var discordFormatted = contentStr
        
        // Scan for @Username
        val words = contentStr.split(" ")
        for (word in words) {
            if (word.startsWith("@")) {
                val discordId = plugin.memberCache.memberMap[word]
                if (discordId != null) {
                    mcFormatted = mcFormatted.replace(word, "<gold>$word</gold>")
                    discordFormatted = discordFormatted.replace(word, "<@$discordId>")
                }
            }
        }

        // Set the modified chat message for Minecraft
        event.message(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(mcFormatted))

        val config = plugin.caelestiaConfig
        val discordMsg = config.msgMcToDiscord
            .replace("%player%", player.name)
            .replace("%message%", discordFormatted)
        
        plugin.discordBot.sendWebhook(player.name, player.uniqueId.toString(), discordMsg)

        // Broadcast to bridge
        val bridgeMsg = BridgeMessage(
            source = Source.PAPER,
            type = Type.CHAT,
            playerName = player.name,
            playerUuid = player.uniqueId.toString(),
            message = mcFormatted
        )
        plugin.bridgeManager.broadcast(bridgeMsg)
    }
}
