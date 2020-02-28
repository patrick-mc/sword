package com.github.patrick.sword

import com.github.patrick.sword.SwordProcess.getAllPlayers
import com.github.patrick.sword.SwordProcess.swordPlayer
import org.bukkit.Bukkit.getPlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SwordCommandExecutor : CommandExecutor {
    override fun onCommand(
        sender: CommandSender?,
        command: Command?,
        label: String?,
        args: Array<out String>?
    ): Boolean {
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

    private fun manageSwordPlayer(sender: CommandSender?, args: Array<out String>) {
        if (args.size < 2) sender?.sendMessage("Player: " + getAllPlayers()?.toString())
        if (args.size == 2) {
            val action = args[0]
            val entry = args[1]
            if (getPlayer(entry) != null) {
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

    private fun sendMessage(sender: CommandSender?, size: Int, args: Array<out String>) = if (size > 1) sender?.sendMessage("Unrecognized arguments: " + args.drop(size)) else null
}