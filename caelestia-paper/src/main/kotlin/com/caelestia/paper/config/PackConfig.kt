package com.caelestia.paper.config

import org.bukkit.configuration.file.FileConfiguration

class PackConfig(private val config: FileConfiguration) {
    val rpServerIp: String = config.getString("resource-pack.server-ip", "127.0.0.1") ?: "127.0.0.1"
    val rpServerPort: Int = config.getInt("resource-pack.server-port", 25625)
    val rpPromptMessage: String = config.getString("resource-pack.prompt-message", "<color:#A259FF>✨</color> <white>Please accept the custom resource pack to see Discord emojis in chat!</white>") ?: "<color:#A259FF>✨</color> <white>Please accept the custom resource pack to see Discord emojis in chat!</white>"

    val emojiSizes = mutableMapOf<String, Int>()

    init {
        val sizesSection = config.getConfigurationSection("emoji-sizes")
        if (sizesSection != null) {
            for (key in sizesSection.getKeys(false)) {
                emojiSizes[key] = sizesSection.getInt(key)
            }
        }
    }
}
