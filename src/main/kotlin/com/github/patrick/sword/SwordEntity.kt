package com.github.patrick.sword

import com.github.noonmaru.math.Vector
import com.github.noonmaru.tap.Tap
import com.github.noonmaru.tap.entity.TapArmorStand
import com.github.noonmaru.tap.entity.TapPlayer
import com.github.noonmaru.tap.firework.FireworkEffect
import com.github.noonmaru.tap.packet.Packet
import com.github.patrick.sword.SwordProcess.getAllPlayers
import com.github.patrick.sword.SwordProcess.getModifier
import org.bukkit.Color
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.random.Random

@Suppress("DEPRECATION")
class SwordEntity(private val owner: Player, private val item: ItemStack) {
    private var ticks = 0
    private var flyingTicks = 0
    private var flying = false
    private var position: Vector? = null
    private var move: Vector? = null
    private var valid = true
    private val tapArmorStand: TapArmorStand? = Tap.ENTITY.createEntity(ArmorStand::class.java)
    private val armorStand: ArmorStand?

    init {
        tapArmorStand?.apply { setUp(this) }
        armorStand = tapArmorStand?.bukkitEntity
        getAllPlayers()?.let { sendTo(it, true) }
    }

    fun update(radius: Double) = tapArmorStand?.let {
        ++ticks
        position = armorStand?.location?.let { pos -> Vector(pos.x, pos.y, pos.z) }

        if(flying) fly(it)
        else queue(it, radius)

        getAllPlayers()?.let { sendTo(it, false) }
    }

    fun remove() {
        armorStand?.entityId?.let { Packet.ENTITY.destroy(it).sendAll() }
        valid = false
    }

    fun sendTo(players: Collection<Player>, new: Boolean) {
        updateEquipment(players)
        Packet.ENTITY.metadata(armorStand).sendTo(players)
        tapArmorStand?.let { Packet.ENTITY.teleport(armorStand, it.posX, it.posY, it.posZ, it.yaw, it.pitch, false).sendTo(players) }
        if (new) Packet.ENTITY.spawnMob(armorStand).sendTo(players)
    }

    fun setMove(vector: Vector) {
        move = vector
    }

    fun setFlying() {
        flying = true
    }

    fun getValid(): Boolean = valid

    private fun setUp(stand: TapArmorStand) {
        stand.let {
            it.isInvisible = true
            it.isMarker = true
            it.setGravity(false)
            it.setBasePlate(false)
            it.setHeadPose(0F, 0F, -45F)
            owner.location.let { pos -> it.setPosition(pos.x, pos.y + 10.5, pos.z) }
        }
    }

    private fun fly(stand: TapArmorStand) {
        move?.let { vector -> tapArmorStand?.let { it.setPositionAndRotation(it.posX, it.posY, it.posZ.minus(1), (-Math.toDegrees(atan2(vector.x, vector.z))).toFloat(), (-Math.toDegrees(asin(vector.y))).toFloat()) } }
        flyingTicks++
        if (flyingTicks == 1) position?.let { stand.setPosition(it.x, it.y.minus(0.25), it.z.minus(1)) }
        if (flyingTicks == 2) armorStand?.velocity = org.bukkit.util.Vector(0.0, 0.0, 0.0)
        if (flyingTicks > 2) {
            if(flyingTicks > 62) remove()
            position?.let { vec -> move?.let { stand.setPosition(vec.x.plus(it.x), vec.y.plus(it.y), vec.z.plus(it.z)) } }
            rayTrace()?.let { hit(it) }
        }
    }

    private fun queue(stand: TapArmorStand, radius: Double) {
        if (ticks == 2) getAllPlayers()?.let { updateEquipment(it) }
        val location = owner.location.clone()
        location.let {
            if (ticks < 2) it.add(0.0, 10.5.minus(ticks.times(5)), 0.0)
            it.yaw = (radius.times(360)).toFloat()
            it.pitch = 0F
            it.add(location.direction.multiply(1.5))
            stand.setPositionAndRotation(it.x , it.y.plus(0.5), it.z, it.yaw, it.pitch)
        }
    }

    private fun rayTrace(): Player? {
        var foundPlayer: Player? = null
        var distance = 0.0

        getAllPlayers()?.forEach { player ->
            if (player != owner && player.isValid) {
                getDistance(player)?.let { current ->
                    if (current < distance || distance == 0.0) {
                        distance = current
                        foundPlayer = player
                    }
                }
            }
        }
        foundPlayer?.let { return it }
        return null
    }

    private fun getDistance(player: Player): Double? {
        TapPlayer.wrapPlayer(player)?.boundingBox?.expand(1.0, 2.0, 1.0)?.calculateRayTrace(position, position?.add(move))?.let {
            val vector = player.location
            return position?.distance(Vector(vector.x, vector.y, vector.z))
        }
        return null
    }

    private fun hit(player: Player) {
        val location = player.location
        Packet.EFFECT.firework(FireworkEffect.builder().color(Color.fromRGB(Random.nextInt(0xFFFFFF)).asRGB()).type(FireworkEffect.Type.STAR).build(), location.x, location.y + 0.9, location.z).sendAll()
        player.noDamageTicks = 0
        player.damage(getModifier(item.type).times(2))
        player.velocity = move?.normalize()?.let { vector -> org.bukkit.util.Vector(vector.x, vector.y, vector.z) }
        remove()
    }

    private fun updateEquipment(players: Collection<Player>) = armorStand?.entityId?.let { Packet.ENTITY.equipment(it, EquipmentSlot.HEAD, item.type.id.let { id -> Tap.ITEM.newItemStack(id, 1, 0 ) }).sendTo(players) }
}
