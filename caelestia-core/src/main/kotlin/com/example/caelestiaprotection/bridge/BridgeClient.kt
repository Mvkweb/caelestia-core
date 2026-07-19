package com.example.caelestiaprotection.bridge

import com.example.caelestiaprotection.core.CaelestiaProtection
import com.google.gson.Gson
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.server.ServerLifecycleHooks
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.io.File
import java.nio.file.Paths

data class BridgeConfig(
    val host: String = "127.0.0.1",
    val port: Int = 25570,
    val password: String = "super_secret_password"
)

class BridgeClient {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var running = false
    private var thread: Thread? = null
    private val gson = Gson()
    private val processedMessages = mutableSetOf<String>()
    
    private var config = BridgeConfig()

    fun init() {
        // Load config
        try {
            val configFile = Paths.get("config", "caelestia-bridge.json").toFile()
            if (!configFile.exists()) {
                configFile.parentFile.mkdirs()
                configFile.writeText(gson.toJson(config))
            } else {
                config = gson.fromJson(configFile.readText(), BridgeConfig::class.java)
            }
        } catch (e: Exception) {
            CaelestiaProtection.LOGGER.error("Failed to load caelestia-bridge.json", e)
        }

        running = true
        thread = Thread {
            while (running) {
                try {
                    socket = Socket(config.host, config.port)
                    writer = PrintWriter(socket!!.outputStream, true)
                    reader = BufferedReader(InputStreamReader(socket!!.inputStream))
                    
                    // Send auth
                    writer?.println(config.password)
                    
                    CaelestiaProtection.LOGGER.info("Connected to Caelestia Paper Bridge Socket!")
                    
                    var line: String? = null
                    while (running && reader?.readLine().also { line = it } != null) {
                        processMessage(line!!)
                    }
                } catch (e: Exception) {
                    // Ignore, will retry
                } finally {
                    cleanup()
                    if (running) {
                        Thread.sleep(5000) // Reconnect delay
                    }
                }
            }
        }
        thread?.start()
    }

    private fun processMessage(json: String) {
        try {
            val msg = gson.fromJson(json, BridgeMessage::class.java)
            if (!processedMessages.add(msg.messageId)) return
            if (processedMessages.size > 1000) processedMessages.clear()
            
            // If it's a chat message from Paper or Discord, broadcast to our server!
            if (msg.source == Source.PAPER || msg.source == Source.DISCORD) {
                val server = ServerLifecycleHooks.getCurrentServer()
                if (server != null) {
                    if (msg.type == Type.CHAT) {
                        // Format prefix
                        val prefix = if (msg.source == Source.DISCORD) {
                            Component.literal("§9💬 §7${msg.playerName} §8• §f")
                        } else {
                            Component.literal("§b${msg.playerName} §8» §f")
                        }
                        val finalMsg = prefix.copy().append(Component.literal(msg.message.replace("<gold>", "§6").replace("</gold>", "§f")))
                        
                        server.playerList.broadcastSystemMessage(finalMsg, false)
                    } else if (msg.type == Type.JOIN) {
                        server.playerList.broadcastSystemMessage(Component.literal("§a🌱 §b${msg.playerName} §7joined the vanilla server"), false)
                    } else if (msg.type == Type.QUIT) {
                        server.playerList.broadcastSystemMessage(Component.literal("§c🌱 §b${msg.playerName} §7left the vanilla server"), false)
                    } else if (msg.type == Type.TAB_COMPLETIONS) {
                        // We received custom chat completions from Paper!
                        val completions = msg.message.split(",")
                        for (player in server.playerList.players) {
                            val packet = net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket(
                                net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket.Action.ADD,
                                completions
                            )
                            player.connection.send(packet)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            CaelestiaProtection.LOGGER.error("Failed to parse bridge message", e)
        }
    }

    fun broadcast(message: BridgeMessage) {
        if (!running || writer == null) return
        Thread {
            try {
                writer?.println(gson.toJson(message))
            } catch (e: Exception) {
                CaelestiaProtection.LOGGER.error("Failed to send bridge message", e)
            }
        }.start()
    }

    private fun cleanup() {
        try { writer?.close() } catch (e: Exception) {}
        try { reader?.close() } catch (e: Exception) {}
        try { socket?.close() } catch (e: Exception) {}
        writer = null
        reader = null
        socket = null
    }

    fun shutdown() {
        running = false
        cleanup()
    }
}
