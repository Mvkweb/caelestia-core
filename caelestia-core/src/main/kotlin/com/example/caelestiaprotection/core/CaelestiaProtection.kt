package com.example.caelestiaprotection.core

import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.ModLoadingContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.common.NeoForge

@Mod(CaelestiaProtection.MODID)
class CaelestiaProtection(modEventBus: IEventBus, modContainer: net.neoforged.fml.ModContainer) {
    companion object {
        const val MODID = "caelestiaprotection"
        val LOGGER: Logger = LogManager.getLogger()
        var bridgeClient: com.example.caelestiaprotection.bridge.BridgeClient? = null
    }

    init {
        // Register the common setup method
        modEventBus.addListener(this::commonSetup)

    }

    private fun commonSetup(event: FMLCommonSetupEvent) {
        bridgeClient = com.example.caelestiaprotection.bridge.BridgeClient()
        bridgeClient?.init()
        LOGGER.info("Caelestia Protection initialized!")
    }
}
