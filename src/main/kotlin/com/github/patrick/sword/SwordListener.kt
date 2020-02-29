package com.github.patrick.sword

import com.github.patrick.sword.SwordProcess.materials
import com.github.patrick.sword.SwordProcess.resetSword
import com.github.patrick.sword.SwordProcess.swordAddAction
import com.github.patrick.sword.SwordProcess.swordThrowAction
import com.github.patrick.sword.SwordProcess.sendPacketTo
import com.github.patrick.sword.SwordProcess.swordPlayer
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent

internal class SwordListener : Listener {
    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        event.item?.run {
            if (materials.contains(type)) {
                val action = event.action
                val player = event.player
                val swordPlayer = swordPlayer[player]?: return

                if (player.hasCooldown(type)) return
                if (setOf(Action.LEFT_CLICK_BLOCK, Action.LEFT_CLICK_AIR).contains(action)) swordThrowAction(player, this, swordPlayer)
                if (setOf(Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR).contains(action)) swordAddAction(player, this, swordPlayer)

                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) = sendPacketTo(event.player)

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) = swordPlayer.remove(event.player)

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) = sendPacketTo(event.player)

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val entity = event.entity
        val player = entity?.player?: return

        entity.killer?.let { killer -> swordPlayer[killer]?.let { event.deathMessage = "Player " + ChatColor.RED + player.name + ChatColor.WHITE + " is killed by SwordMaster " + ChatColor.BLUE + killer.name + "." } }
        resetSword(setOf(swordPlayer[player]))
    }
}