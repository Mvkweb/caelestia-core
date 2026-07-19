package com.caelestia.paper.discord

import com.caelestia.paper.CaelestiaPlugin
import net.dv8tion.jda.api.entities.Member
import org.bukkit.Bukkit

class DiscordMemberCache(private val plugin: CaelestiaPlugin) {

    // Map of "@username" to "DiscordUserID"
    val memberMap = mutableMapOf<String, String>()

    fun init() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            updateCache()
        }, 20L * 10, 20L * 60 * 5) // Run after 10s, then every 5 minutes
    }

    fun updateCache() {
        val guildId = plugin.caelestiaConfig.discordGuildId
        if (guildId.isEmpty()) return

        val jda = plugin.discordBot.jda ?: return
        val guild = jda.getGuildById(guildId) ?: return

        guild.loadMembers().onSuccess { members: List<net.dv8tion.jda.api.entities.Member> ->
            val newMap = mutableMapOf<String, String>()
            for (member in members) {
                if (member.user.isBot) continue
                
                val displayName = member.effectiveName.replace(" ", "_")
                val username = member.user.name.replace(" ", "_")
                
                newMap["@$displayName"] = member.id
                if (displayName != username) {
                    newMap["@$username"] = member.id
                }
            }
            
            memberMap.clear()
            memberMap.putAll(newMap)
            
            // Sync tab completions to all online Paper players
            syncToPaperPlayers()
            
            // Sync to NeoForge
            syncToNeoForge()
        }
    }

    private fun syncToPaperPlayers() {
        val completions = memberMap.keys
        for (player in Bukkit.getOnlinePlayers()) {
            player.addCustomChatCompletions(completions)
        }
    }

    private fun syncToNeoForge() {
        val msg = com.caelestia.paper.bridge.BridgeMessage(
            source = com.caelestia.paper.bridge.Source.PAPER,
            type = com.caelestia.paper.bridge.Type.TAB_COMPLETIONS,
            playerName = null,
            playerUuid = null,
            message = memberMap.keys.joinToString(",")
        )
        plugin.bridgeManager.broadcast(msg)
    }
}
