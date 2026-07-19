package com.caelestia.paper.bridge

import com.caelestia.paper.CaelestiaPlugin
import com.caelestia.paper.discord.DiscordBotManager
import com.caelestia.paper.util.ColorUtil
import org.bukkit.Bukkit
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

class BridgeManager(private val plugin: CaelestiaPlugin, private val discordBot: DiscordBotManager) {
    
    private val gson = Gson()
    private val processedMessages = mutableSetOf<String>()
    
    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null
    private var running = false
    
    private val clients = CopyOnWriteArrayList<Socket>()
    private val clientWriters = CopyOnWriteArrayList<PrintWriter>()

    fun init() {
        running = true
        serverThread = Thread {
            try {
                val port = plugin.caelestiaConfig.bridgeSocketPort
                serverSocket = ServerSocket(port)
                plugin.logger.info("TCP Socket Server listening on port $port")
                
                while (running) {
                    val client = serverSocket?.accept() ?: break
                    plugin.logger.info("Accepted connection from ${client.inetAddress.hostAddress}")
                    handleClient(client)
                }
            } catch (e: Exception) {
                if (running) {
                    plugin.logger.severe("TCP Server Error: ${e.message}")
                }
            }
        }
        serverThread?.start()
    }
    
    private fun handleClient(socket: Socket) {
        Thread {
            var writer: PrintWriter? = null
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                writer = PrintWriter(socket.outputStream, true)
                
                // Extremely simple auth: wait for password as first line
                val password = reader.readLine()
                if (password != plugin.caelestiaConfig.bridgeSocketPass) {
                    plugin.logger.warning("Client ${socket.inetAddress.hostAddress} failed authentication!")
                    socket.close()
                    return@Thread
                }
                
                plugin.logger.info("Client ${socket.inetAddress.hostAddress} authenticated successfully.")
                clients.add(socket)
                clientWriters.add(writer)
                
                var line: String? = null
                while (running && reader.readLine().also { line = it } != null) {
                    processMessage(line!!)
                }
            } catch (e: Exception) {
                // Ignore disconnect errors
            } finally {
                clients.remove(socket)
                if (writer != null) clientWriters.remove(writer)
                try { socket.close() } catch (e: Exception) {}
            }
        }.start()
    }

    fun shutdown() {
        running = false
        try { serverSocket?.close() } catch (e: Exception) {}
        clients.forEach { try { it.close() } catch (e: Exception) {} }
        clients.clear()
        clientWriters.clear()
    }

    fun broadcast(message: BridgeMessage) {
        // Send our local chat OR Discord messages over the socket to NeoForge clients
        if (message.source == Source.PAPER || message.source == Source.DISCORD) {
            val json = gson.toJson(message)
            clientWriters.forEach { writer ->
                try {
                    writer.println(json)
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }
    }

    private fun processMessage(json: String) {
        try {
            val msg = gson.fromJson(json, BridgeMessage::class.java)
            
            if (!processedMessages.add(msg.messageId)) return
            if (processedMessages.size > 1000) processedMessages.clear()
            
            when (msg.type) {
                Type.CHAT -> {
                    // It's from NeoForge, broadcast to Paper
                    if (msg.source == Source.NEOFORGE) {
                        var mcFormatted = msg.message
                        var discordFormatted = msg.message
                        
                        // Scan for @Username
                        val words = msg.message.split(" ")
                        for (word in words) {
                            if (word.startsWith("@")) {
                                val discordId = plugin.memberCache.memberMap[word]
                                if (discordId != null) {
                                    mcFormatted = mcFormatted.replace(word, "<gold>$word</gold>")
                                    discordFormatted = discordFormatted.replace(word, "<@$discordId>")
                                }
                            }
                        }

                        val mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        val paperFormat = plugin.caelestiaConfig.msgNeoForgeToMc
                            .replace("%username%", msg.playerName ?: "Unknown")
                            .replace("%message%", mcFormatted)
                        plugin.server.broadcast(mm.deserialize(paperFormat))
                        
                        // And send to Discord
                        val discordMsg = plugin.caelestiaConfig.msgMcToDiscord
                            .replace("%player%", msg.playerName ?: "Unknown")
                            .replace("%message%", discordFormatted)
                        plugin.discordBot.sendWebhook(
                            msg.playerName ?: "Unknown",
                            msg.playerUuid ?: "00000000-0000-0000-0000-000000000000",
                            discordMsg
                        )
                    }
                }
                Type.JOIN -> {
                    if (msg.source == Source.NEOFORGE) {
                        if (plugin.caelestiaConfig.featRelayJoinQuit) {
                            val discordMsg = plugin.caelestiaConfig.msgNeoForgeJoin.replace("%player%", msg.playerName ?: "Unknown")
                            plugin.discordBot.sendWebhook(msg.playerName ?: "Unknown", msg.playerUuid ?: "00000000-0000-0000-0000-000000000000", discordMsg)
                        }
                    }
                }
                Type.QUIT -> {
                    if (msg.source == Source.NEOFORGE) {
                        if (plugin.caelestiaConfig.featRelayJoinQuit) {
                            val discordMsg = plugin.caelestiaConfig.msgNeoForgeQuit.replace("%player%", msg.playerName ?: "Unknown")
                            plugin.discordBot.sendWebhook(msg.playerName ?: "Unknown", msg.playerUuid ?: "00000000-0000-0000-0000-000000000000", discordMsg)
                        }
                    }
                }
                Type.DEATH -> {
                    if (msg.source == Source.NEOFORGE) {
                        if (plugin.caelestiaConfig.featRelayDeath) {
                            val discordMsg = plugin.caelestiaConfig.msgNeoForgeDeath
                                .replace("%player%", msg.playerName ?: "Unknown")
                                .replace("%death_message%", msg.message)
                            plugin.discordBot.sendWebhook(msg.playerName ?: "Unknown", msg.playerUuid ?: "00000000-0000-0000-0000-000000000000", discordMsg)
                        }
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to decode bridge message: ${e.message}")
        }
    }
}
