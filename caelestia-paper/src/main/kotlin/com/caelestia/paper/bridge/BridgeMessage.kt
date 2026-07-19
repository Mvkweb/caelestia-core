package com.caelestia.paper.bridge

enum class Source {
    PAPER,
    NEOFORGE,
    DISCORD
}

enum class Type {
    CHAT,
    JOIN,
    QUIT,
    DEATH,
    ADVANCEMENT,
    TAB_COMPLETIONS,
    SYSTEM
}

data class BridgeMessage(
    val source: Source,
    val type: Type,
    val playerName: String?,
    val playerUuid: String?,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageId: String = java.util.UUID.randomUUID().toString()
)
