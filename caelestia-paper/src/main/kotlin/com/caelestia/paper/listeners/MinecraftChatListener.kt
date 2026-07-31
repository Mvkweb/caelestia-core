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
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import com.caelestia.paper.emoji.EmojiRegistry
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent

class MinecraftChatListener(private val plugin: CaelestiaPlugin) : Listener {

    private val miniMessage = MiniMessage.miniMessage()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerChat(event: AsyncChatEvent) {
        if (!plugin.caelestiaConfig.featureWebhooks) return
        
        val player = event.player
        // Convert the component to plain text for Discord
        val contentStr = (event.message() as TextComponent).content()
        var mcFormatted = EmojiRegistry.translateDiscordToMc(contentStr)
        var discordFormatted = EmojiRegistry.translateMcToDiscord(contentStr)
        
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
        event.message(miniMessage.deserialize(mcFormatted))

        val config = plugin.caelestiaConfig
        val discordMsg = config.msgMcToDiscord
            .replace("%player%", player.name)
            .replace("%message%", discordFormatted)
        
        plugin.discordBot.sendWebhook(player.name, discordMsg)

        // Broadcast to bridge
        val bridgeMessage = BridgeMessage(
            source = Source.PAPER,
            type = Type.CHAT,
            playerName = player.name,
            playerUuid = player.uniqueId.toString(),
            message = mcFormatted
        )
        plugin.bridgeManager.broadcast(bridgeMessage)
    }

    @EventHandler
    fun onTabComplete(event: AsyncTabCompleteEvent) {
        val buffer = event.buffer
        
        // We only want to autocomplete in chat, not commands (commands start with /)
        if (event.isCommand || buffer.startsWith("/")) return
        
        val words = buffer.split(" ")
        if (words.isEmpty()) return
        val lastWord = words.last()

        if (lastWord.startsWith(":")) {
            val search = lastWord.substring(1).lowercase()
            val matches = EmojiRegistry.discordToMcMap.keys
                .filter { it.lowercase().startsWith(search) }
                .map { ":$it:" }
                
            if (matches.isNotEmpty()) {
                event.completions.addAll(matches)
                event.isHandled = true
            }
        }
    }
}
