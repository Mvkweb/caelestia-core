package com.caelestia.paper.commands

import com.caelestia.paper.CaelestiaPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class DiscordReplyCommand(private val plugin: CaelestiaPlugin) : Command(
    "discordreply",
    "Reply to a Discord message",
    "/discordreply <id> <message>",
    emptyList()
) {
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can use this command.")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(Component.text("Usage: /discordreply <messageId> <message>").color(NamedTextColor.RED))
            return true
        }

        val messageId = args[0]
        val messageContent = args.drop(1).joinToString(" ")
        val mcFormatted = com.caelestia.paper.emoji.EmojiRegistry.translateDiscordToMc(messageContent)
        val discordFormatted = com.caelestia.paper.emoji.EmojiRegistry.translateMcToDiscord(messageContent)

        plugin.discordBot.replyToMessage(
            username = sender.name,
            uuidStr = sender.uniqueId.toString(),
            discordMessageId = messageId,
            replyContent = discordFormatted
        )

        val mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
        val formattedReply = "<color:#5865F2>💬</color> <gray>${sender.name}</gray> <dark_gray>•</dark_gray> <white>$mcFormatted</white>"
        plugin.server.broadcast(mm.deserialize(formattedReply))

        val bridgeMessage = com.caelestia.paper.bridge.BridgeMessage(
            source = com.caelestia.paper.bridge.Source.PAPER,
            type = com.caelestia.paper.bridge.Type.CHAT,
            playerName = sender.name,
            playerUuid = sender.uniqueId.toString(),
            message = mcFormatted
        )
        plugin.bridgeManager.broadcast(bridgeMessage)

        return true
    }
}
