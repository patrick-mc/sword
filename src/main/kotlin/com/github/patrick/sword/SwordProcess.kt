package com.github.patrick.sword

import com.github.noonmaru.math.Vector
import org.bukkit.Bukkit.getOnlinePlayers
import org.bukkit.Bukkit.getPluginCommand
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.Bukkit.getScheduler
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask

object SwordProcess {
    val swordPlayer = HashMap<Player, SwordPlayer>()
    val materials = arrayOf(Material.WOOD_SWORD, Material.STONE_SWORD, Material.GOLD_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD)
    private var task: BukkitTask? = null
    private val command = getPluginCommand("sword")
    private val leftAction = setOf(Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK)
    private val rightAction = setOf(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK)

    fun start(plugin: SwordPlugin) {
        task = getScheduler().runTaskTimer(plugin, SwordTask(), 0, 1)
        getPluginManager().registerEvents(SwordListener(), plugin)
        command.executor = SwordCommand()
        command.tabCompleter = SwordCommand()
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

    fun sendPacketTo(player: Player) = swordPlayer.values.forEach { swordPlayer -> swordPlayer.swords.forEach { it.sendTo(setOf(player).toList(), true) } }

    fun swordAction(action: Action, player: Player, item: ItemStack, swordPlayer: SwordPlayer) {
        if(leftAction.contains(action)) {
            val swords = swordPlayer.swords
            if(swords.isEmpty()) return

            val sword = swords[0]
            val speed = player.location.direction.multiply(getModifier(item.type))

            sword.setFlying()
            sword.setMove(Vector(speed.x, speed.y, speed.z))
            swords.remove(sword)
            swordPlayer.flyingSword.add(sword)

            setCoolDown(player, null)
        }
        if(rightAction.contains(action)) {
            val swords = swordPlayer.swords
            if (swords.count() > 15) return

            swords += SwordEntity(player, item)
            setCoolDown(player, item)
        }
    }

    fun getModifier(type: Material): Double = when (type) {
        Material.WOOD_SWORD -> 2.0
        Material.STONE_SWORD -> 2.5
        Material.GOLD_SWORD -> 3.0
        Material.IRON_SWORD -> 3.5
        Material.DIAMOND_SWORD -> 4.0
        else -> 0.0
    }

    private fun setCoolDown(player: Player, item: ItemStack?) {
        var material = Material.AIR
        item?.let { material = it.type }
        materials.forEach { player.setCooldown(it, 40.div(getModifier(material)).toInt()) }
    }
}