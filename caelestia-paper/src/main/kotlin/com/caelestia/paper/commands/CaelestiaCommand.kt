package com.caelestia.paper.commands

import com.caelestia.paper.CaelestiaPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class CaelestiaCommand(private val plugin: CaelestiaPlugin) : Command(
    "caelestia",
    "Main command for Caelestia Chat Bridge",
    "/caelestia",
    listOf("caele", "cael")
) {
    init {
        permission = "caelestia.admin"
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        if (args.isEmpty() || args[0].equals("help", ignoreCase = true)) {
            sender.sendMessage(Component.text("Caelestia Chat Bridge", NamedTextColor.AQUA))
            sender.sendMessage(Component.text("/caelestia reload - Reload the config and Discord bot", NamedTextColor.GRAY))
            return true
        }

        if (args[0].equals("reload", ignoreCase = true)) {
            sender.sendMessage(Component.text("Reloading Caelestia...", NamedTextColor.YELLOW))

            plugin.reloadPlugin()

            sender.sendMessage(Component.text("Caelestia reloaded successfully!", NamedTextColor.GREEN))
            return true
        }

        sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED))
        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val completions = listOf("help", "reload")
            return completions.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}
