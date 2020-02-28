package com.github.patrick.sword

import com.github.noonmaru.math.Vector
import com.github.noonmaru.tap.Tap
import com.github.noonmaru.tap.entity.TapArmorStand
import com.github.noonmaru.tap.entity.TapPlayer
import com.github.noonmaru.tap.firework.FireworkEffect
import com.github.noonmaru.tap.packet.Packet
import com.github.patrick.sword.SwordProcess.getAllPlayers
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.random.Random

@Suppress("DEPRECATION")
class SwordEntity(private val owner: Player?, private val item: ItemStack?) {
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
        getAllPlayers()?.let { sendTo(it, true) }
    }

    fun update(radius: Double) {
        tapArmorStand?.let { stand ->
            ++ticks
            if (ticks == 2) getAllPlayers()?.let { updateEquipment(it) }
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
                            FireworkEffect.builder().color(Color.fromRGB(Random.nextInt(0xFFFFFF)).asRGB()).type(
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
                    it.add(location.direction.multiply(1.5))
                    stand.setPositionAndRotation(it.x , it.y + 0.5, it.z, it.yaw, it.pitch)
                }
            }
            getAllPlayers()?.let { sendTo(it, false) }
        }
    }

    private fun updateEquipment(players: Collection<Player>) = armorStand?.entityId?.let { Packet.ENTITY.equipment(it, EquipmentSlot.HEAD, item?.type?.id?.let { id -> Tap.ITEM.newItemStack(id, 1, 0 ) }).sendTo(players) }

    fun remove() {
        armorStand?.entityId?.let { Packet.ENTITY.destroy(it).sendAll() }
        this.valid = false
    }

    fun sendTo(players: Collection<Player>, new: Boolean) {
        updateEquipment(players)
        armorStand?.entityId?.let { Packet.ENTITY.equipment(it, EquipmentSlot.HEAD, item?.type?.id?.let { id -> Tap.ITEM.newItemStack(id, 1, 0 ) }).sendTo(players) }
        Packet.ENTITY.metadata(armorStand).sendTo(players)
        tapArmorStand?.let { Packet.ENTITY.teleport(armorStand, it.posX, it.posY, it.posZ, it.yaw, it.pitch, false).sendTo(players) }
        if (new) {
            Packet.ENTITY.spawnMob(armorStand).sendTo(players)
        }
    }
}