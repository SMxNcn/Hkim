package cn.hkim.addon.utils

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.mixins.accessors.KeyMappingAccessor
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

fun String.containsOneOf(vararg options: String, ignoreCase: Boolean = false): Boolean =
    containsOneOf(options.toList(), ignoreCase)

fun Any?.equalsOneOf(vararg options: Any?): Boolean =
    options.any { this == it }

fun String.matchesOneOf(vararg options: Regex): Boolean =
    options.any { this.matches(it) }

fun String.containsOneOf(options: Collection<String>, ignoreCase: Boolean = false): Boolean =
    options.any { this.contains(it, ignoreCase) }

fun String.startsWithOneOf(vararg options: String, ignoreCase: Boolean = false): Boolean =
    options.any { this.startsWith(it, ignoreCase) }

inline val Entity.renderX: Double
    get() = xo + (x - xo) * mc.deltaTracker.getGameTimeDeltaPartialTick(true)

inline val Entity.renderY: Double
    get() = yo + (y - yo) * mc.deltaTracker.getGameTimeDeltaPartialTick(true)

inline val Entity.renderZ: Double
    get() = zo + (z - zo) * mc.deltaTracker.getGameTimeDeltaPartialTick(true)

inline val Entity.renderPos: Vec3
    get() = Vec3(renderX, renderY, renderZ)

inline val Entity.renderBoundingBox: AABB
    get() = boundingBox.move(renderX - x, renderY - y, renderZ - z)

fun isPlayerInArea(corner1: BlockPos, corner2: BlockPos, playerPos: BlockPos): Boolean {
    val minX = minOf(corner1.x, corner2.x)
    val maxX = maxOf(corner1.x, corner2.x)
    val minY = minOf(corner1.y, corner2.y)
    val maxY = maxOf(corner1.y, corner2.y)
    val minZ = minOf(corner1.z, corner2.z)
    val maxZ = maxOf(corner1.z, corner2.z)

    return playerPos.x in minX..maxX &&
            playerPos.y in minY..maxY &&
            playerPos.z in minZ..maxZ
}

fun leapTo(name: String, screenHandler: AbstractContainerScreen<*>) {
    val player = mc.player ?: return
    val index = screenHandler.menu.slots.subList(11, 16).firstOrNull {
        it.item?.hoverName?.string?.substringAfter(' ').equals(name.clean, ignoreCase = true)
    }?.index ?: return
    mc.gameMode?.handleContainerInput(screenHandler.menu.containerId, index, 0, ContainerInput.PICKUP, player)
}

private val romanMap = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
fun romanToInt(s: String): Int {
    return if (s.matches(Regex("^[0-9]+$"))) s.toInt()
    else {
        var result = 0
        for (i in 0 until s.length - 1) {
            val current = romanMap[s[i]] ?: 0
            val next = romanMap[s[i + 1]] ?: 0
            result += if (current < next) -current else current
        }
        result + (romanMap[s.last()] ?: 0)
    }
}

fun clickInventorySlot(slot: Int, containerId: Int, rightClick: Boolean = false) {
    if (mc.screen == null) return
    val player = mc.player ?: return

    mc.execute {
        mc.gameMode?.handleContainerInput(containerId, slot, if (rightClick) 1 else 0, ContainerInput.PICKUP, player)
    }
}

fun clickPlayerInventorySlot(slot: Int, containerId: Int) {
    if (mc.screen == null) return
    val player = mc.player ?: return
    val container = player.containerMenu

    val containerSlots = container.slots.size
    val actualSlot: Int

    when (slot) {
        in 0..8 -> actualSlot = containerSlots - 9 + slot
        in 9..35 -> {
            val containerBaseSlots = containerSlots - 36
            if (containerBaseSlots < 0) return
            actualSlot = containerBaseSlots + (slot - 9)
        }
        else -> return
    }

    if (actualSlot !in 0 until containerSlots) return

    mc.execute {
        mc.gameMode?.handleContainerInput(containerId, actualSlot, 0, ContainerInput.PICKUP, player)
    }
}

fun rightClick() {
    val key = mc.options.keyUse
    val actualKey = (key as KeyMappingAccessor).boundKey
    KeyMapping.set(actualKey, true)
    KeyMapping.click(actualKey)
    KeyMapping.set(actualKey, false)
}

fun leftClick() {
    val key = mc.options.keyAttack
    val actualKey = (key as KeyMappingAccessor).boundKey
    KeyMapping.set(actualKey, true)
    KeyMapping.click(actualKey)
    KeyMapping.set(actualKey, false)
}