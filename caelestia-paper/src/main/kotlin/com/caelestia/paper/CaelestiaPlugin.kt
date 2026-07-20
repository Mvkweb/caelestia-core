package com.caelestia.paper

import com.caelestia.paper.bridge.BridgeManager
import com.caelestia.paper.config.CaelestiaConfig
import com.caelestia.paper.discord.DiscordBotManager
import com.caelestia.paper.discord.DiscordMemberCache
import com.caelestia.paper.listeners.MinecraftChatListener
import com.caelestia.paper.listeners.ServerEventListener
import com.caelestia.paper.commands.CaelestiaCommand
import org.bukkit.plugin.java.JavaPlugin

class CaelestiaPlugin : JavaPlugin() {

    lateinit var caelestiaConfig: CaelestiaConfig
        private set
        
    lateinit var discordBot: DiscordBotManager
        private set
        
    lateinit var bridgeManager: BridgeManager
        private set

    lateinit var memberCache: DiscordMemberCache
        private set

    override fun onEnable() {
        // Save default config if not exists
        saveDefaultConfig()
        caelestiaConfig = CaelestiaConfig(config)

        // Initialize Discord Bot
        discordBot = DiscordBotManager(this)
        discordBot.init()

        // Initialize Bridge Manager
        bridgeManager = BridgeManager(this, discordBot)
        bridgeManager.init()

        // Initialize Member Cache
        memberCache = DiscordMemberCache(this)
        memberCache.init()

        // Register Listeners
        server.pluginManager.registerEvents(MinecraftChatListener(this), this)
        server.pluginManager.registerEvents(ServerEventListener(this), this)
        if (caelestiaConfig.featXpClumps) {
            server.pluginManager.registerEvents(com.caelestia.paper.listeners.XpClumpsListener(this), this)
        }

        // Register Command
        server.commandMap.register(name.lowercase(), CaelestiaCommand(this))
        server.commandMap.register(name.lowercase(), com.caelestia.paper.commands.DiscordReplyCommand(this))

        // Send Server Start message
        if (caelestiaConfig.featServerStartStop) {
            discordBot.sendMessage(caelestiaConfig.msgServerStart)
        }

        logger.info("Caelestia Chat Bridge has been enabled!")
    }

    fun reloadPlugin() {
        reloadConfig()
        caelestiaConfig = CaelestiaConfig(config)
        
        discordBot.shutdown()
        discordBot.init()
        
        bridgeManager.shutdown()
        bridgeManager.init()
    }

    override fun onDisable() {
        // Send Server Stop message
        if (caelestiaConfig.featServerStartStop) {
            // Need to block briefly since shutdown is async and we are disabling
            val channel = discordBot.getChannel()
            if (channel != null) {
                try {
                    channel.sendMessage(caelestiaConfig.msgServerStop).complete()
                } catch (e: Exception) {
                    logger.warning("Could not send server stop message: ${e.message}")
                }
            }
        }

        // Shutdown bridge
        bridgeManager.shutdown()
        
        // Shutdown Discord bot
        discordBot.shutdown()

        logger.info("Caelestia Chat Bridge has been disabled!")
    }
}
