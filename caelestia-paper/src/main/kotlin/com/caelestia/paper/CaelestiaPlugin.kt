package com.caelestia.paper

import com.caelestia.paper.bridge.BridgeManager
import com.caelestia.paper.commands.CaelestiaCommand
import com.caelestia.paper.commands.DiscordReplyCommand
import com.caelestia.paper.commands.EmojisCommand
import com.caelestia.paper.config.CaelestiaConfig
import com.caelestia.paper.config.PackConfig
import com.caelestia.paper.discord.DiscordBotManager
import com.caelestia.paper.discord.DiscordMemberCache
import com.caelestia.paper.listeners.MinecraftChatListener
import com.caelestia.paper.listeners.ServerEventListener
import org.bukkit.plugin.java.JavaPlugin

class CaelestiaPlugin : JavaPlugin() {

    lateinit var caelestiaConfig: CaelestiaConfig
        private set
    lateinit var packConfig: PackConfig
        private set
        
    lateinit var discordBot: DiscordBotManager
        private set
        
    lateinit var bridgeManager: BridgeManager
        private set

    lateinit var memberCache: DiscordMemberCache
        private set

    override fun onEnable() {
        // Save default config
        saveDefaultConfig()
        
        // Save default custom emojis if they don't exist
        try {
            saveResource("custom-emojis/discord.png", false)
            saveResource("custom-emojis/modded.png", false)
            saveResource("custom-emojis/vanilla.png", false)
        } catch (e: Exception) {
            logger.warning("Could not save default custom emojis: ${e.message}")
        }
        
        caelestiaConfig = CaelestiaConfig(config)
        
        // Load pack config
        val packConfigFile = java.io.File(dataFolder, "pack.yml")
        if (!packConfigFile.exists()) {
            saveResource("pack.yml", false)
        }
        val packConfigYaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(packConfigFile)
        packConfig = PackConfig(packConfigYaml)

        // Initialize Discord Bot
        discordBot = DiscordBotManager(this)

        // Initialize Bridge Manager
        bridgeManager = BridgeManager(this, discordBot)
        
        // Start services
        discordBot.init()
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
        server.pluginManager.registerEvents(com.caelestia.paper.listeners.ResourcePackListener(this), this)

        // Register Commands
        server.commandMap.register(name.lowercase(), com.caelestia.paper.commands.CaelestiaCommand(this))
        server.commandMap.register(name.lowercase(), DiscordReplyCommand(this))
        server.commandMap.register(name.lowercase(), EmojisCommand(this))

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
