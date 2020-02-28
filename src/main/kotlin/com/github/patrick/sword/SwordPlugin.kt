package com.github.patrick.sword

import com.github.noonmaru.math.Vector
import com.github.noonmaru.tap.Tap
import com.github.noonmaru.tap.entity.TapArmorStand
import com.github.noonmaru.tap.entity.TapPlayer
import com.github.noonmaru.tap.firework.FireworkEffect
import com.github.noonmaru.tap.packet.Packet
import org.bukkit.Bukkit.getOnlinePlayers
import org.bukkit.Bukkit.getPlayer
import org.bukkit.Bukkit.getPluginCommand
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.random.Random.Default.nextInt
import kotlin.streams.toList

@Suppress("UNUSED")
class SwordPlugin : JavaPlugin(), Listener {
    val swordPlayer = HashMap<Player, SwordPlayer>()
    var time: Long? = null
    private val command = getPluginCommand("sword")
    private var task: BukkitTask? = null

    override fun onEnable() {
        task = object: BukkitRunnable() {
            override fun run() {
                swordPlayer.values.forEach { player ->
                    val sword = player.swords
                    sword.forEachIndexed { i, e -> e.update(i / sword.count().toDouble()) }
                    sword.removeIf { !it.valid }
                }
            }
        }.runTaskTimer(this, 0, 1)
        time = System.currentTimeMillis()
        getPluginManager().registerEvents(this, this)
        command.executor = this
        command.tabCompleter = this
    }

    override fun onDisable() {
        resetSword(swordPlayer.values)
        swordPlayer.clear()
        HandlerList.unregisterAll()
        time = null
        task?.cancel()
    }

    override fun onCommand(sender: CommandSender?, command: Command?, label: String?, args: Array<out String>?): Boolean {
        if (args?.size == 0) {
            sender?.sendMessage("Action: 'add', 'remove', 'on', or 'off'")
            return true
        }
        val action = args?.get(0)
        if ((action.equals("add", true) || action.equals("remove", true)) && args?.size == 1) {
            sender?.sendMessage("Player: " + getAllPlayers()?.toString())
            return true
        }
        when {
            action.equals("add", true) -> {
                val player = args?.get(1)
                getPlayer(player)?.let {
                    swordPlayer[it]?.let {
                        sender?.sendMessage("Already Sword Player: $player")
                        return true
                    }
                    swordPlayer[it] = SwordPlayer()
                    return true
                }
                sender?.sendMessage("Wrong player name: $player")
            }
            action.equals("remove", true) -> {
                val player = args?.get(1)
                getPlayer(player)?.let {
                    swordPlayer.remove(it)
                    return true
                }
                sender?.sendMessage("Wrong player name: $player")
            }
            action.equals("on", true) -> {
                getAllPlayers()?.forEach {
                    swordPlayer[it]?.let { player ->
                        sender?.sendMessage("Already Sword Player: $player")
                        return true
                    }
                    swordPlayer[it] = SwordPlayer()
                }
            }
            action.equals("off", true) -> {
                getAllPlayers()?.forEach { swordPlayer.remove(it) }
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender?, command: Command?, alias: String?, args: Array<out String>?): List<String>? {
        return when (args?.size) {
            1 -> listOf("add", "remove", "off", "on")
            2 -> {
                if (getAllPlayers() == null) listOf("") else getAllPlayers()?.stream()?.map(Player::getName)?.toList()
            }
            else -> listOf("")
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        event.item?.run {
            if (type == Material.WOOD_SWORD || type == Material.STONE_SWORD || type == Material.IRON_SWORD || type == Material.GOLD_SWORD || type == Material.DIAMOND_SWORD) {
                val action = event.action
                val player = event.player
                val swordPlayer = swordPlayer[player]?: return
                val swords = swordPlayer.swords

                if (player.hasCooldown(type)) {
                    event.isCancelled = true
                    return
                }
                if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                    if(swords.isEmpty()) {
                        event.isCancelled = true
                        return
                    }
                    val sword = swords[0]
                    sword.flying = true
                    val speed = player.location.direction.multiply(when (type) {
                        Material.WOOD_SWORD -> 2.0
                        Material.STONE_SWORD -> 3.0
                        Material.GOLD_SWORD -> 4.0
                        Material.IRON_SWORD -> 3.6
                        Material.DIAMOND_SWORD -> 5.0
                        else -> 1.0
                    })
                    sword.move = Vector(speed.x, speed.y, speed.z)
                    setCoolDown(player, null)
                } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    if (swords.count() < 16) {
                        swords += SwordEntity(player, this)
                        setCoolDown(player, this)
                    } else {
                        event.isCancelled = true
                        return
                    }
                } else {
                    event.isCancelled = true
                }
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
        val player = entity?.player
        val killer = entity?.killer
        swordPlayer[killer]?.let { event.deathMessage = player?.name + ChatColor.WHITE + " is killed by SwordMaster " + killer?.name + "." }
        resetSword(setOf(swordPlayer[player]))
    }

    private fun setCoolDown(player: Player, item: ItemStack?) {
        var material = Material.AIR
        item?.let { material = it.type }
        arrayListOf(Material.WOOD_SWORD, Material.STONE_SWORD, Material.GOLD_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD).forEach {
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

    private fun sendPacketTo(player: Player) = swordPlayer.values.forEach { swordPlayer -> swordPlayer.swords.forEach { it.sendTo(setOf(player).toList(), true) } }

    private fun resetSword(array: Collection<SwordPlayer?>) = array.forEach {
        it?.swords?.forEach { entity -> entity.remove() }
        it?.swords?.clear()
    }

    private fun getAllPlayers(): Collection<Player>? {
        val players = getOnlinePlayers()
        return if (players.isEmpty()) null else players
    }

    inner class SwordPlayer {
        val swords = ArrayList<SwordEntity>()
    }

    @Suppress("DEPRECATION")
    inner class SwordEntity(private val owner: Player?, private val item: ItemStack?) {
        private var ticks = 0
        private var flyingTicks = 0
        private var position: Vector? = null
        private var yaw = 0F
        private var pitch = 0F
        var flying = false
        var valid = true
        var move: Vector? = null
        private val armorStand: ArmorStand?
        private val tapArmorStand: TapArmorStand? = Tap.ENTITY.createEntity(ArmorStand::class.java)

        init {
            tapArmorStand?.apply {
                setGravity(false)
                setBasePlate(false)
                isInvisible = true
                isMarker = true
                owner?.location?.let { setPosition(it.x, it.y + 10.5, it.z) }
                setHeadPose(0F, 0F, -45F)
            }
            armorStand = tapArmorStand?.bukkitEntity
            sendTo(getOnlinePlayers(), true)
        }

        fun update(radius: Double) {
            tapArmorStand?.let { stand ->
                ++ticks
                if (ticks == 2) armorStand?.entityId?.let { Packet.ENTITY.equipment(it, EquipmentSlot.HEAD, item?.type?.id?.let { id -> Tap.ITEM.newItemStack(id, 1, 0 ) }).sendAll() }
                position = armorStand?.location?.let { Vector(it.x, it.y, it.z) }
                if(flying) {
                    move?.let {
                        yaw = (-Math.toDegrees(atan2(it.x, it.z))).toFloat()
                        pitch = (-Math.toDegrees(asin(it.y))).toFloat()
                    }
                    flyingTicks++
                    if (flyingTicks == 1) position?.let { stand.setPositionAndRotation(it.x, it.y - 0.25, it.z - 1, yaw, pitch) }
                    if (flyingTicks == 2) armorStand?.velocity = org.bukkit.util.Vector(0.0, 0.0, 0.0)
                    if (flyingTicks > 2) {
                        stand.setHeadPose(0F, 0F, pitch - 45F)
                        if(flyingTicks > 62)
                            remove()
                        position?.let { vec -> move?.let { stand.setPositionAndRotation(vec.x + it.x, vec.y + it.y, vec.z + it.z, yaw, pitch) } }

                        var foundPlayer: Player? = null
                        var distance = 0.0

                        getAllPlayers()?.forEach { player ->
                            if (player != this.owner && player.isValid) {
                                val rayTraceResult = Tap.ENTITY.wrapEntity<TapPlayer>(player)?.boundingBox?.expand(1.0, 2.0, 1.0)?.calculateRayTrace(position, position?.add(move))
                                rayTraceResult?.let {
                                    val location = player.location
                                    val currentDistance = position?.distance(Vector(location.x, location.y, location.z))
                                    currentDistance?.let { current ->
                                        if (current < distance || distance == 0.0) {
                                            distance = current
                                            foundPlayer = player
                                        }
                                    }
                                }
                            }
                        }
                        foundPlayer?.let { player ->
                            val location = player.location
                            Packet.EFFECT.firework(
                                FireworkEffect.builder().color(Color.fromRGB(nextInt(0xFFFFFF)).asRGB()).type(
                                    FireworkEffect.Type.STAR).build(), location.x, location.y + 0.9, location.z).sendAll()
                            player.noDamageTicks = 0
                            player.damage(when (item?.type) {
                                Material.WOOD_SWORD -> 4.0
                                Material.STONE_SWORD -> 5.0
                                Material.GOLD_SWORD -> 6.0
                                Material.IRON_SWORD -> 7.0
                                Material.DIAMOND_SWORD -> 8.0
                                else -> 0.0
                            }, owner)
                            player.velocity = move?.normalize()?.let { vector -> org.bukkit.util.Vector(vector.x, vector.y, vector.z) }
                            remove()
                        }
                    }
                } else {
                    val location = owner?.location?.clone()
                    location?.let {
                        if (ticks < 2) {
                            it.add(0.0, 10.5.minus(ticks.times(5)), 0.0)
                        }
                        it.yaw = (radius * 360).toFloat()
                        it.pitch = 0F
                        time?.let { sec -> yaw.plus((System.currentTimeMillis() - sec).div(25)) }
                        it.add(location.direction.multiply(1.5))
                        stand.setPositionAndRotation(it.x , it.y + 0.5, it.z, it.yaw, it.pitch)
                    }
                }
                getAllPlayers()?.let { sendTo(it, false) }
            }
        }

        fun remove() {
            armorStand?.entityId?.let { Packet.ENTITY.destroy(it).sendAll() }
            this.valid = false
        }

        fun sendTo(players: Collection<Player>, new: Boolean) {
            armorStand?.entityId?.let { Packet.ENTITY.equipment(it, EquipmentSlot.HEAD, item?.type?.id?.let { id -> Tap.ITEM.newItemStack(id, 1, 0 ) }).sendTo(players) }
            Packet.ENTITY.metadata(armorStand).sendTo(players)
            tapArmorStand?.let { Packet.ENTITY.teleport(armorStand, it.posX, it.posY, it.posZ, it.yaw, it.pitch, false).sendTo(players) }
            if (new) {
                Packet.ENTITY.spawnMob(armorStand).sendTo(players)
            }
        }
    }
}