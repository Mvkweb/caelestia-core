package com.caelestia.paper.config

import org.bukkit.configuration.file.FileConfiguration

class CaelestiaConfig(private val config: FileConfiguration) {
    val discordBotToken: String = config.getString("discord.bot-token") ?: ""
    val discordChannelId: String = config.getString("discord.channel-id") ?: ""
    val discordGuildId: String = config.getString("discord.guild-id") ?: ""

    val msgMcToDiscord: String = config.getString("messages.minecraft-to-discord") ?: "**%player%** » %message%"
    val msgDiscordToMc: String = config.getString("messages.discord-to-minecraft") ?: "<dark_blue>[Discord]</dark_blue> <aqua>%username%</aqua> » %message%"
    val msgNeoForgeToMc: String = config.getString("messages.neoforge-to-minecraft") ?: "<color:#E36139>⚙</color> <gray>%username%</gray> <dark_gray>•</dark_gray> <white>%message%</white>"
    
    val msgJoin: String = config.getString("messages.join") ?: "**%player%** joined the server"
    val msgQuit: String = config.getString("messages.quit") ?: "**%player%** left the server"
    val msgDeath: String = config.getString("messages.death") ?: "**%player%** %death_message%"
    
    val msgNeoForgeJoin: String = config.getString("messages.neoforge-join") ?: "⚙ **%player%** joined the modded server"
    val msgNeoForgeQuit: String = config.getString("messages.neoforge-quit") ?: "⚙ **%player%** left the modded server"
    val msgNeoForgeDeath: String = config.getString("messages.neoforge-death") ?: "⚙ **%player%** %death_message%"

    val msgServerStart: String = config.getString("messages.server-start") ?: "🟢 **Server has started!**"
    val msgServerStop: String = config.getString("messages.server-stop") ?: "🔴 **Server is shutting down...**"

    val avatarSkinApiUrl: String = config.getString("avatars.skin-api-url") ?: "https://mc-heads.net"
    val avatarRenderType: String = config.getString("avatars.render-type") ?: "head"
    val avatarScale: Int = config.getInt("avatars.scale", 100)

    val bridgeChannelName: String = config.getString("bridge.channel-name") ?: "caelestia:bridge"
    val bridgeSocketPort: Int = config.getInt("bridge.socket-port", 25570)
    val bridgeSocketPass: String = config.getString("bridge.socket-password", "YOUR_SECRET_PASSWORD") ?: "YOUR_SECRET_PASSWORD"

    val featXpClumps: Boolean = config.getBoolean("xp-clumps.enabled", true)
    val xpClumpsMaxValue: Int = config.getInt("xp-clumps.max-value", 500)
    val xpClumpsMergeRadius: Double = config.getDouble("xp-clumps.merge-radius", 4.0)

    val featRelayJoinQuit: Boolean = config.getBoolean("features.relay-join-quit", true)
    val featRelayDeath: Boolean = config.getBoolean("features.relay-death-messages", true)
    val featRelayAdvancement: Boolean = config.getBoolean("features.relay-advancement-messages", true)
    val featServerStartStop: Boolean = config.getBoolean("features.server-start-stop", true)
    val featUseWebhooks: Boolean = config.getBoolean("features.use-webhooks", true)

    fun isValidDiscordConfig(): Boolean {
        return discordBotToken.isNotBlank() && discordBotToken != "YOUR_BOT_TOKEN" &&
               discordChannelId.isNotBlank() && discordChannelId != "YOUR_CHANNEL_ID"
    }
}
