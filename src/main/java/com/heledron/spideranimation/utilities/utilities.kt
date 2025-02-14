package com.heledron.spideranimation.utilities

import com.heledron.spideranimation.SpiderAnimationPlugin
import net.md_5.bungee.api.ChatMessageType
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.util.RayTraceResult
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.*
import java.io.Closeable


fun runLater(delay: Long, task: () -> Unit): Closeable {
    val plugin = SpiderAnimationPlugin.instance
    val handler = plugin.server.scheduler.runTaskLater(plugin, task, delay)
    return Closeable {
        handler.cancel()
    }
}

fun interval(delay: Long, period: Long, task: () -> Unit): Closeable {
    val plugin = SpiderAnimationPlugin.instance
    val handler = plugin.server.scheduler.runTaskTimer(plugin, task, delay, period)
    return Closeable {
        handler.cancel()
    }
}

fun addEventListener(listener: Listener): Closeable {
    val plugin = SpiderAnimationPlugin.instance
    plugin.server.pluginManager.registerEvents(listener, plugin)
    return Closeable {
        org.bukkit.event.HandlerList.unregisterAll(listener)
    }
}


class SeriesScheduler {
    var time = 0L

    fun sleep(time: Long) {
        this.time += time
    }

    fun run(task: () -> Unit) {
        runLater(time, task)
    }
}

class EventEmitter {
    val listeners = mutableListOf<() -> Unit>()
    fun listen(listener: () -> Unit): Closeable {
        listeners.add(listener)
        return Closeable { listeners.remove(listener) }
    }

    fun emit() {
        for (listener in listeners) listener()
    }
}


fun createNamedItem(material: org.bukkit.Material, name: String): ItemStack {
    val item = ItemStack(material)
    val itemMeta = item.itemMeta ?: throw Exception("ItemMeta is null")
    itemMeta.setItemName(ChatColor.RESET.toString() + name)
    item.itemMeta = itemMeta
    return item
}

fun firstPlayer(): org.bukkit.entity.Player? {
    return Bukkit.getOnlinePlayers().firstOrNull()
}

fun sendDebugMessage(message: String) {
    sendActionBar(firstPlayer() ?: return, message)
}

fun sendActionBar(player: org.bukkit.entity.Player, message: String) {
//    player.sendActionBar(message)
    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent(message))
}

fun raycastGround(location: Location, direction: Vector, maxDistance: Double): RayTraceResult? {
    return location.world!!.rayTraceBlocks(location, direction, maxDistance, FluidCollisionMode.NEVER, true)
}

fun isOnGround(location: Location): Boolean {
    return raycastGround(location, DOWN_VECTOR, 0.001) != null
}

data class CollisionResult(val position: Vector, val offset: Vector)

fun resolveCollision(location: Location, direction: Vector): CollisionResult? {
    val ray = location.world!!.rayTraceBlocks(location.clone().subtract(direction), direction, direction.length(), FluidCollisionMode.NEVER, true)
    if (ray != null) {
        val newLocation = ray.hitPosition.toLocation(location.world!!)
        return CollisionResult(newLocation.toVector(), ray.hitPosition.subtract(location.toVector()))
    }

    return null
}

fun playSound(location: Location, sound: org.bukkit.Sound, volume: Float, pitch: Float) {
    location.world!!.playSound(location, sound, volume, pitch)
}

fun <T : Entity> spawnEntity(location: Location, clazz: Class<T>, initializer: (T) -> Unit): T {
    return location.world!!.spawn(location, clazz, initializer)
}

fun spawnParticle(particle: org.bukkit.Particle, location: Location, count: Int, offsetX: Double, offsetY: Double, offsetZ: Double, extra: Double) {
    location.world!!.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra)
}

fun <T> spawnParticle(particle: org.bukkit.Particle, location: Location, count: Int, offsetX: Double, offsetY: Double, offsetZ: Double, extra: Double, data: T) {
    location.world!!.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, data)
}


fun lookingAtPoint(eye: Vector, direction: Vector, point: Vector, tolerance: Double): Boolean {
    val pointDistance = eye.distance(point)
    val lookingAtPoint = eye.clone().add(direction.clone().multiply(pointDistance))
    return lookingAtPoint.distance(point) < tolerance
}

fun centredTransform(xSize: Float, ySize: Float, zSize: Float): Transformation {
    return Transformation(
        Vector3f(-xSize / 2, -ySize / 2, -zSize / 2),
        AxisAngle4f(0f, 0f, 0f, 1f),
        Vector3f(xSize, ySize, zSize),
        AxisAngle4f(0f, 0f, 0f, 1f)
    )
}

fun transformFromMatrix(matrix: Matrix4f): Transformation {
    val translation = matrix.getTranslation(Vector3f())
    val rotation = matrix.getUnnormalizedRotation(Quaternionf())
    val scale = matrix.getScale(Vector3f())

    return Transformation(translation, rotation, scale, Quaternionf())
}

fun applyTransformationWithInterpolation(entity: BlockDisplay, transformation: Transformation) {
    if (entity.transformation != transformation) {
        entity.transformation = transformation
        entity.interpolationDelay = 0
    }
}

fun applyTransformationWithInterpolation(entity: BlockDisplay, matrix: Matrix4f) {
    applyTransformationWithInterpolation(entity, transformFromMatrix(matrix))
}