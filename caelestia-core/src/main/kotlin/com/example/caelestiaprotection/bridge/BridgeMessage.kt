package com.example.caelestiaprotection.bridge

import java.util.UUID

enum class Source {
    PAPER, NEOFORGE, DISCORD
}

enum class Type {
    CHAT, JOIN, QUIT, DEATH, ADVANCEMENT, TAB_COMPLETIONS
}

data class BridgeMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val source: Source,
    val type: Type,
    val playerName: String?,
    val playerUuid: String?,
    val message: String
)
