package com.caelestia.paper.commands

import com.caelestia.paper.CaelestiaPlugin
import com.caelestia.paper.emoji.EmojiRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class EmojisCommand(private val plugin: CaelestiaPlugin) : Command(
    "emojis",
    "List all available custom emojis",
    "/emojis",
    emptyList()
) {
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        val mm = MiniMessage.miniMessage()
        
        if (EmojiRegistry.discordToMcMap.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>No custom emojis have been synced yet!</red>"))
            return true
        }

        sender.sendMessage(mm.deserialize("<color:#A259FF>✨</color> <white><b>Available Custom Emojis:</b></white>"))
        
        // Group emojis nicely, e.g. 3 per line
        var currentLine = ""
        var count = 0
        
        EmojiRegistry.discordToMcMap.forEach { (name, unicode) ->
            val formatted = "<gray>:$name:</gray> <white>$unicode</white>"
            currentLine += if (currentLine.isEmpty()) formatted else "  <dark_gray>|</dark_gray>  $formatted"
            
            count++
            if (count % 3 == 0) {
                sender.sendMessage(mm.deserialize(currentLine))
                currentLine = ""
            }
        }
        
        if (currentLine.isNotEmpty()) {
            sender.sendMessage(mm.deserialize(currentLine))
        }

        return true
    }
}
