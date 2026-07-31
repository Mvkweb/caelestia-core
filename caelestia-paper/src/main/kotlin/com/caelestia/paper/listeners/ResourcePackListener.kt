package com.caelestia.paper.listeners

import com.caelestia.paper.CaelestiaPlugin
import com.caelestia.paper.emoji.HttpPackServer
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class ResourcePackListener(private val plugin: CaelestiaPlugin) : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val ip = plugin.packConfig.rpServerIp
        val port = plugin.packConfig.rpServerPort
        val url = "http://$ip:$port/emojis.zip"
        
        val mm = MiniMessage.miniMessage()
        val prompt = mm.deserialize(plugin.packConfig.rpPromptMessage)
        
        if (HttpPackServer.packHash.isNotEmpty()) {
            player.setResourcePack(url, HttpPackServer.packHash, prompt, false)
        }
    }
}
