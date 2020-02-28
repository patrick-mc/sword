package com.github.patrick.sword

import com.github.patrick.sword.SwordProcess.getAllPlayers
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import kotlin.streams.toList

class SwordTabCompleter : TabCompleter {
    override fun onTabComplete(sender: CommandSender?, command: Command?, alias: String?, args: Array<out String>?): List<String>? {
        return when (args?.size) {
            1 -> listOf("add", "remove", "off", "on")
            2 -> if (getAllPlayers() != null && setOf("add", "remove").contains(args[0])) getAllPlayers()?.stream()?.map(Player::getName)?.toList() else listOf("")
            else -> listOf("")
        }
    }
}