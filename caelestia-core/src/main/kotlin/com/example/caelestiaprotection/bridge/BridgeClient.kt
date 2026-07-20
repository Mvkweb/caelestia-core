package com.example.caelestiaprotection.bridge

import com.example.caelestiaprotection.core.CaelestiaProtection
import com.google.gson.Gson
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent
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
                    CaelestiaProtection.LOGGER.error("Bridge connection lost or error occurred!", e)
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
                        var finalMsg = prefix.copy().append(Component.literal(msg.message.replace("<gold>", "§6").replace("</gold>", "§f")))
                        
                        if (msg.source == Source.DISCORD && msg.messageId.matches(Regex("\\d+"))) {
                            finalMsg = finalMsg.withStyle { style -> 
                                style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/discordreply ${msg.messageId} "))
                                     .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to reply to ${msg.playerName}!")))
                            }
                        }
                        
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
                    } else if (msg.type == Type.SPARK_REQUEST) {
                        CaelestiaProtection.LOGGER.info("Received SPARK_REQUEST from Paper!")
                        try {
                            val spark = me.lucko.spark.api.SparkProvider.get()
                            val tps = spark.tps()?.poll(me.lucko.spark.api.statistic.StatisticWindow.TicksPerSecond.MINUTES_5) ?: 0.0
                            val msptInfo = spark.mspt()?.poll(me.lucko.spark.api.statistic.StatisticWindow.MillisPerTick.MINUTES_1)
                            val mspt = msptInfo?.mean() ?: 0.0
                            val cpu = spark.cpuSystem().poll(me.lucko.spark.api.statistic.StatisticWindow.CpuUsage.MINUTES_1) * 100
                            
                            val usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
                            val maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024
                            val players = "${server.playerCount} / ${server.maxPlayers}"
                            val chunks = server.allLevels.sumOf { it.chunkSource.loadedChunksCount }.toString()
                            
                            val uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().uptime
                            val seconds = (uptimeMs / 1000) % 60
                            val minutes = (uptimeMs / (1000 * 60)) % 60
                            val hours = (uptimeMs / (1000 * 60 * 60)) % 24
                            val days = (uptimeMs / (1000 * 60 * 60 * 24))
                            val uptimeStr = if (days > 0) "${days}d ${hours}h ${minutes}m" else "${hours}h ${minutes}m ${seconds}s"
                            
                            val neoStats = mapOf(
                                "tps" to String.format("%.2f", tps),
                                "mspt" to String.format("%.2f", mspt),
                                "cpu" to String.format("%.2f", cpu),
                                "ram" to "${usedMem}MB / ${maxMem}MB",
                                "players" to players,
                                "chunks" to chunks,
                                "uptime" to uptimeStr
                            )
                            
                            CaelestiaProtection.LOGGER.info("Sending SPARK_RESPONSE back to Paper")
                            
                            broadcast(BridgeMessage(
                                source = Source.NEOFORGE,
                                type = Type.SPARK_RESPONSE,
                                playerName = null,
                                playerUuid = null,
                                messageId = msg.messageId,
                                message = gson.toJson(neoStats)
                            ))
                        } catch (e: Exception) {
                            CaelestiaProtection.LOGGER.error("Failed to fetch spark stats", e)
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
