package com.caelestia.paper.listeners

import com.caelestia.paper.CaelestiaPlugin
import com.caelestia.paper.bridge.BridgeMessage
import com.caelestia.paper.bridge.Source
import com.caelestia.paper.bridge.Type
import com.caelestia.paper.util.ColorUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ServerEventListener(private val plugin: CaelestiaPlugin) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!plugin.caelestiaConfig.featRelayJoinQuit) return
        
        val player = event.player
        player.addCustomChatCompletions(plugin.memberCache.memberMap.keys)
        
        val text = plugin.caelestiaConfig.msgJoin.replace("%player%", player.name)
        
        plugin.discordBot.webhookManager.sendMessage(
            username = player.name,
            content = text
        )
        
        val bridgeMsg = BridgeMessage(
            source = Source.PAPER,
            type = Type.JOIN,
            playerName = player.name,
            playerUuid = player.uniqueId.toString(),
            message = text
        )
        plugin.bridgeManager.broadcast(bridgeMsg)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!plugin.caelestiaConfig.featRelayJoinQuit) return
        
        val player = event.player
        val text = plugin.caelestiaConfig.msgQuit.replace("%player%", player.name)
        
        plugin.discordBot.webhookManager.sendMessage(
            username = player.name,
            content = text
        )
        
        val bridgeMsg = BridgeMessage(
            source = Source.PAPER,
            type = Type.QUIT,
            playerName = player.name,
            playerUuid = player.uniqueId.toString(),
            message = text
        )
        plugin.bridgeManager.broadcast(bridgeMsg)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!plugin.caelestiaConfig.featRelayDeath) return
        
        val player = event.entity
        val deathMsgComponent = event.deathMessage()
        val plainDeathMsg = if (deathMsgComponent != null) ColorUtil.toPlainText(deathMsgComponent) else "died"
        
        val text = plugin.caelestiaConfig.msgDeath
            .replace("%player%", player.name)
            .replace("%death_message%", plainDeathMsg)
            
        plugin.discordBot.webhookManager.sendMessage(
            username = player.name,
            content = text
        )
        
        val bridgeMsg = BridgeMessage(
            source = Source.PAPER,
            type = Type.DEATH,
            playerName = player.name,
            playerUuid = player.uniqueId.toString(),
            message = text
        )
        plugin.bridgeManager.broadcast(bridgeMsg)
    }
    
    @EventHandler
    fun onAdvancement(event: PlayerAdvancementDoneEvent) {
        if (!plugin.caelestiaConfig.featRelayAdvancement) return
        
        // Exclude recipe advancements and internal advancements
        val key = event.advancement.key.key
        if (key.startsWith("recipes/") || event.advancement.display == null) return
        
        val title = event.advancement.display?.title()?.let { ColorUtil.toPlainText(it) } ?: return
        val player = event.player
        val text = "**${player.name}** has made the advancement **[$title]**"
        
        plugin.discordBot.webhookManager.sendMessage(
            username = player.name,
            content = "has made the advancement **[$title]**"
        )
        
        val bridgeMsg = BridgeMessage(
            source = Source.PAPER,
            type = Type.ADVANCEMENT,
            playerName = player.name,
            playerUuid = player.uniqueId.toString(),
            message = text
        )
        plugin.bridgeManager.broadcast(bridgeMsg)
    }
}
