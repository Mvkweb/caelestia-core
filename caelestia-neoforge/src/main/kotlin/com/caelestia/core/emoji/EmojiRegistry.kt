package com.caelestia.core.emoji

object EmojiRegistry {
    val discordToMcMap = mutableMapOf<String, String>() // :name: -> \uE000

    fun translateDiscordToMc(input: String): String {
        var output = input
        discordToMcMap.forEach { (discordName, unicode) ->
            output = output.replace(":$discordName:", unicode)
        }
        return output
    }
}
