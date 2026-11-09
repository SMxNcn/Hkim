package cn.hkim.addon.utils.skyblock.rng

import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.skyblock.DungeonUtils
import cn.hkim.addon.utils.skyblock.Floor
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.slayer.SlayerType
import cn.hkim.addon.utils.skyblock.slayer.SlayerUtils

object RngUtils {
    private val floorRegex = Regex("""Catacombs \((F\d|M\d)\)""", RegexOption.IGNORE_CASE)
    const val GLACITE_KEY = "Glacite"

    fun parseAmount(raw: String): Long? {
        val text = raw.trim().replace(",", "")
        if (text.isEmpty()) return null

        val multiplier = when (text.last().lowercaseChar()) {
            'k' -> 1_000.0
            'm' -> 1_000_000.0
            else -> 1.0
        }
        val numberPart = if (multiplier != 1.0) text.dropLast(1) else text

        return numberPart.toDoubleOrNull()?.let { (it * multiplier).toLong() }
    }

    fun currentMeterKey(): String? {
        if (!LocationUtils.inSkyBlock) return null
        if (LocationUtils.currentArea == Island.Mineshaft) return GLACITE_KEY
        if (LocationUtils.inDungeons) return currentDungeonKey()
        return SlayerUtils.currentType()?.meterKey
    }

    fun currentDungeonKey(): String? {
        if (!LocationUtils.inDungeons) return null
        val floor = DungeonUtils.floor ?: return null
        if (floor == Floor.E) return null
        return floor.name
    }

    fun resolveMeterKey(meterText: String): String? {
        val cleanText = meterText.clean.trim()

        if (cleanText.contains(GLACITE_KEY, ignoreCase = true)) return GLACITE_KEY
        floorRegex.find(cleanText)?.let { return it.groupValues[1].uppercase() }
        SlayerType.fromText(cleanText)?.let { return it.meterKey }

        return currentMeterKey()
    }
}
