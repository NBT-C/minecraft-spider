package com.heledron.spideranimation.spider

import com.heledron.spideranimation.utilities.*
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Pig
import org.bukkit.entity.minecart.CommandMinecart
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import java.io.Closeable

class Mountable(val spider: Spider): SpiderComponent {
    val pig = ModelPartRenderer<Pig>()
    var marker = ModelPartRenderer<ArmorStand>()

    var closable = mutableListOf<Closeable>()

    fun getRider() = marker.entity?.passengers?.firstOrNull()

    init {
        closable += pig
        closable += marker

        closable += addEventListener(object : Listener {
            @EventHandler
            fun onInteract(event: PlayerInteractEntityEvent) {
                val pigEntity = pig.entity
                if (event.rightClicked != pigEntity) return
                if (event.hand != EquipmentSlot.HAND) return

                // if right click with saddle, add saddle (automatic)
                if (event.player.inventory.itemInMainHand.type == Material.SADDLE && !pigEntity.hasSaddle()) {
                    playSound(pigEntity.location, Sound.ENTITY_PIG_SADDLE, 1.0f, 1.0f)
                }

                // if right click with empty hand, remove saddle
                if (event.player.isSneaking && event.player.inventory.itemInMainHand.type.isAir) {
                    pigEntity.setSaddle(false)
                }
            }
        })

        // when player mounts the pig, switch them to the marker entity
        closable += addEventListener(object : Listener {
            @EventHandler
            fun onMount(event: VehicleEnterEvent) {
                if (event.vehicle != pig.entity) return
                val player = event.entered
                val marker = marker.entity ?: return

                event.isCancelled = true
                marker.addPassenger(player)
            }
        })
    }

    override fun render() {
        val location = spider.location.clone().add(spider.velocity)

        val pigLocation = location.clone().add(Vector(.0, -.4, .0))
        val markerLocation = location.clone().add(Vector(.0, .5, .0))

        pig.render(ModelPart(
            clazz = Pig::class.java,
            location = pigLocation,
            init = {
                it.setGravity(false)
                it.setAI(false)
                it.isInvisible = true
                it.isInvulnerable = true
                it.isSilent = true
                it.isCollidable = false
            }
        ))

        marker.render(ModelPart(
            clazz = ArmorStand::class.java,
            location = markerLocation,
            init = {
                it.setGravity(false)
                it.isInvisible = true
                it.isInvulnerable = true
                it.isSilent = true
                it.isCollidable = false
                it.isMarker = true
            },
            update = update@{
                if (getRider() == null) return@update

                // This is the only way to preserve passengers when teleporting.
                // Paper has a TeleportFlag, but it is not supported by Spigot.
                // https://jd.papermc.io/paper/1.21/io/papermc/paper/entity/TeleportFlag.EntityState.html
                runCommandSilently("execute as ${it.uniqueId} at @s run tp ${markerLocation.x} ${markerLocation.y} ${markerLocation.z}")
            }
        ))
    }

    override fun close() {
        closable.forEach { it.close() }
    }
}

fun runCommandSilently(command: String) {
    val server = org.bukkit.Bukkit.getServer()
    val location = org.bukkit.Bukkit.getWorlds().first().spawnLocation
    spawnEntity(location, CommandMinecart::class.java) {
        it.setCommand(command)
        server.dispatchCommand(it, command)
        it.remove()
    }
}