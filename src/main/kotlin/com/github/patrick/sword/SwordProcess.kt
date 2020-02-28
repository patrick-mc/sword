package com.github.patrick.sword

import com.github.noonmaru.math.Vector
import org.bukkit.Bukkit.getOnlinePlayers
import org.bukkit.Bukkit.getPluginCommand
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.Bukkit.getScheduler
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask

object SwordProcess {
    val swordPlayer = HashMap<Player, SwordPlayer>()
    private var task: BukkitTask? = null
    private val command = getPluginCommand("sword")

    fun start(plugin: SwordPlugin) {
        task = getScheduler().runTaskTimer(plugin, SwordTask(), 0, 1)
        getPluginManager().registerEvents(SwordListener(), plugin)
        command.executor = SwordCommandExecutor()
        command.tabCompleter = SwordTabCompleter()
    }

    fun stop() {
        resetSword(swordPlayer.values)
        swordPlayer.clear()
        HandlerList.unregisterAll()
        task?.cancel()
    }

    fun getAllPlayers(): Collection<Player>? {
        val players = getOnlinePlayers()
        return if (players.isEmpty()) null else players
    }

    fun resetSword(array: Collection<SwordPlayer?>) = array.forEach {
        it?.swords?.forEach { entity -> entity.remove() }
        it?.swords?.clear()
    }

    fun clickAction(action: Action, player: Player, item: ItemStack, swordPlayer: SwordPlayer) {
        val swords = swordPlayer.swords
        val flyingSwords = swordPlayer.flyingSword
        if (setOf(Action.LEFT_CLICK_BLOCK, Action.LEFT_CLICK_AIR).contains(action)) {
            if(swords.isEmpty()) return

            val sword = swords[0]
            val speed = player.location.direction.multiply(getMultiplier(item.type))
            sword.flying = true
            sword.move = Vector(speed.x, speed.y, speed.z)

            swords.remove(sword)
            flyingSwords.add(sword)

            setCoolDown(player, null)
        }

        if (setOf(Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR).contains(action)) {
            if (swords.count() > 15) return

            swords += SwordEntity(player, item)
            setCoolDown(player, item)
        }
    }

    fun sendDeathMessage(event: PlayerDeathEvent, player: Player, killer: Player) = swordPlayer[killer]?.let { event.deathMessage = "Player " + ChatColor.RED + player.name + ChatColor.WHITE + " is killed by SwordMaster " + ChatColor.BLUE + killer.name + "." }

    fun sendPacketTo(player: Player) = swordPlayer.values.forEach { swordPlayer -> swordPlayer.swords.forEach { it.sendTo(setOf(player).toList(), true) } }

    private fun getMultiplier(type: Material): Double = when (type) {
        Material.WOOD_SWORD -> 2.0
        Material.STONE_SWORD -> 3.0
        Material.GOLD_SWORD -> 4.0
        Material.IRON_SWORD -> 3.6
        Material.DIAMOND_SWORD -> 5.0
        else -> 1.0
    }

    private fun setCoolDown(player: Player, item: ItemStack?) {
        var material = Material.AIR
        item?.let { material = it.type }
        arrayOf(Material.WOOD_SWORD, Material.STONE_SWORD, Material.GOLD_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD).forEach {
            player.setCooldown(it, when (material) {
                Material.WOOD_SWORD -> 25
                Material.STONE_SWORD -> 20
                Material.GOLD_SWORD -> 2
                Material.IRON_SWORD -> 15
                Material.DIAMOND_SWORD -> 10
                Material.AIR -> 5
                else -> 0
            })
        }
    }
}