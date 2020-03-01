package com.github.patrick.sword

import com.github.patrick.sword.SwordProcess.getAllPlayers
import com.github.patrick.sword.SwordProcess.swordPlayer
import org.bukkit.Bukkit.getPlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import kotlin.streams.toList

class SwordCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender?, command: Command?, label: String?, args: Array<out String>?): Boolean {
        if (args == null || args.isEmpty()) {
            sender?.sendMessage("Action: 'add', 'remove', 'on', or 'off'")
            return true
        }
        val action = args[0]
        when {
            setOf("add", "remove").contains(action) -> manageSwordPlayer(sender, args)
            setOf("on", "off").contains(action) -> toggleSwordPlayer(sender, args)
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender?, command: Command?, alias: String?, args: Array<out String>?): List<String>? {
        return when (args?.size) {
            1 -> listOf("add", "remove", "off", "on").filter { it.startsWith(args[0]) }
            2 -> if (getAllPlayers() != null && setOf("add", "remove").contains(args[0])) getAllPlayers()?.stream()?.map(Player::getName)?.toList()?.filter { it.startsWith(args[1]) } else listOf("")
            else -> listOf("")
        }
    }

    private fun manageSwordPlayer(sender: CommandSender?, args: Array<out String>) {
        if (args.size < 2) sender?.sendMessage("Player: " + getAllPlayers()?.toString())
        if (args.size == 2) {
            val action = args[0]
            val entry = args[1]
            if (getPlayer(entry) == null) {
                sender?.sendMessage("Not found player: $entry")
                return
            }
            val player = getPlayer(entry)
            if (action == "add") addPlayer(player)
            if (action == "remove") removePlayer(player)
        }
        sendMessage(sender, 2, args)
    }

    private fun toggleSwordPlayer(sender: CommandSender?, args: Array<out String>) {
        if (args.size == 1) {
            val action = args[0]
            if (action == "on") getAllPlayers()?.forEach { addPlayer(it) }
            if (action == "off") getAllPlayers()?.forEach { removePlayer(it) }
        }
        sendMessage(sender, 1, args)
    }

    private fun addPlayer(player: Player) = if (swordPlayer[player] == null) swordPlayer[player] = SwordPlayer() else null

    private fun removePlayer(player: Player) = swordPlayer[player]?.let { swordPlayer.remove(player) }

    private fun sendMessage(sender: CommandSender?, size: Int, args: Array<out String>) = if (args.size > size) sender?.sendMessage("Unrecognized arguments: " + args.drop(size)) else null
}