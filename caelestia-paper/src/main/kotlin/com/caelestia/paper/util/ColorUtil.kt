package com.caelestia.paper.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

object ColorUtil {
    private val miniMessage = MiniMessage.miniMessage()

    fun parseMiniMessage(input: String): Component {
        return miniMessage.deserialize(input)
    }

    fun toPlainText(component: Component): String {
        return PlainTextComponentSerializer.plainText().serialize(component)
    }
    
    // Very basic discord to minecraft formatting 
    // In a full implementation, you'd want a comprehensive parser for **bold**, *italics*, etc.
    fun formatDiscordToMinecraft(message: String): String {
        var formatted = message
        formatted = formatted.replace(Regex("\\*\\*(.*?)\\*\\*"), "<bold>$1</bold>")
        formatted = formatted.replace(Regex("\\*(.*?)\\*"), "<italic>$1</italic>")
        formatted = formatted.replace(Regex("__(.*?)__"), "<underlined>$1</underlined>")
        formatted = formatted.replace(Regex("~~(.*?)~~"), "<strikethrough>$1</strikethrough>")
        return formatted
    }
}
