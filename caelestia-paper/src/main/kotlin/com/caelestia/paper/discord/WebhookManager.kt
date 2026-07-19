package com.caelestia.paper.discord

import com.caelestia.paper.CaelestiaPlugin
import com.caelestia.paper.util.SkinUtil
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.JsonObject
import java.util.UUID

class WebhookManager(private val plugin: CaelestiaPlugin, private val discordBotManager: DiscordBotManager) {
    private val httpClient = OkHttpClient()
    private var webhookUrl: String? = null

    fun init() {
        val config = plugin.caelestiaConfig
        if (!config.featUseWebhooks) return

        val channel = discordBotManager.getChannel()
        if (channel == null) {
            plugin.logger.warning("Could not find Discord channel to setup webhook.")
            return
        }

        channel.retrieveWebhooks().queue({ webhooks ->
            val existing = webhooks.find { it.name == "Caelestia Bridge" }
            if (existing != null) {
                webhookUrl = existing.url
            } else {
                channel.createWebhook("Caelestia Bridge").queue({ webhook ->
                    webhookUrl = webhook.url
                }, { error ->
                    plugin.logger.warning("Failed to create webhook: ${error.message}")
                })
            }
        }, { error ->
            plugin.logger.warning("Failed to fetch webhooks: ${error.message}")
        })
    }

    fun shutdown() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }

    fun sendMessage(username: String, uuid: UUID?, content: String) {
        val config = plugin.caelestiaConfig
        val url = webhookUrl
        
        if (!config.featUseWebhooks || url == null) {
            val prefix = if (uuid == null) "" else "**$username** » "
            discordBotManager.sendMessage("$prefix$content")
            return
        }

        val avatarUrl = if (uuid != null) {
            SkinUtil.getAvatarUrl(config.avatarSkinApiUrl, uuid, config.avatarRenderType, config.avatarScale)
        } else {
            null
        }

        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            try {
                val json = JsonObject()
                json.addProperty("content", content)
                json.addProperty("username", username)
                if (avatarUrl != null) {
                    json.addProperty("avatar_url", avatarUrl)
                }

                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        plugin.logger.warning("Webhook failed with code: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to send webhook message: ${e.message}")
            }
        })
    }
}
