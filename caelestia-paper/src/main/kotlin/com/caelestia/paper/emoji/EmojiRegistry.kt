package com.caelestia.paper.emoji

object EmojiRegistry {
    val discordToMcMap = mutableMapOf<String, String>() // :name: -> \uE000
    val mcToDiscordMap = mutableMapOf<String, String>() // \uE000 -> <:name:id>

    fun translateDiscordToMc(input: String): String {
        var output = input
        discordToMcMap.forEach { (discordName, unicode) ->
            // Replace both the formatted <:name:id> and the literal :name: with unicode
            output = output.replace(Regex("<a?:$discordName:\\d+>"), unicode)
            output = output.replace(":$discordName:", unicode)
        }
        return output
    }

    fun translateMcToDiscord(input: String): String {
        var output = input
        // Replace unicode with formatted <:name:id>
        mcToDiscordMap.forEach { (unicode, formatted) ->
            output = output.replace(unicode, formatted)
        }
        // Also allow players who type :name: to get it translated to formatted <:name:id>
        discordToMcMap.forEach { (discordName, _) ->
            val shortcode = ":$discordName:"
            if (output.contains(shortcode)) {
                val formatted = mcToDiscordMap.values.find { it.contains(shortcode) }
                if (formatted != null) {
                    // Only replace standalone shortcodes, not ones already inside <:name:id>
                    val alreadyFormatted = output.contains("<$shortcode") || output.contains("<a$shortcode")
                    if (!alreadyFormatted) {
                        output = output.replace(shortcode, formatted)
                    }
                }
            }
        }
        return output
    }
}
