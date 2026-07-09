package cn.hkim.addon.utils.skyblock.mining

import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils

object MineralFilter {
    private val GLACITE_ZONES = setOf("Dwarven Base Camp", "Fossil Research Center", "Glacite Tunnels", "Grandpa Wolf's Cave", "Great Glacite Lake")
    private val GOLD_ZONES = setOf("Royal Mines", "Mines of Divan")

    private fun isDwarvenZoneValid(): Boolean {
        if (!LocationUtils.isCurrentArea(Island.DwarvenMines)) return false
        val zone = LocationUtils.getCurrentZone()?.clean ?: return true
        return zone !in GLACITE_ZONES
    }

    private fun isGoldZoneValid(): Boolean {
        val zone = LocationUtils.getCurrentZone()?.clean ?: return false
        return zone in GOLD_ZONES
    }

    private fun isGlaciteZoneValid(): Boolean {
        if (!LocationUtils.isCurrentArea(Island.DwarvenMines)) return false
        val zone = LocationUtils.getCurrentZone()?.clean ?: return false
        return zone in GLACITE_ZONES
    }

    fun isMineralAllowed(
        mineralType: MineralType,
        targetType: Int,
        ignoreOthers: Boolean,
        royalDivan: Boolean = false,
        inDwarvenOnly: Boolean = false,
        allowedTypes: Set<MineralType> = emptySet(),
    ): Boolean {
        // 0 -> "Gold", 1 -> "Mithril", 2 -> "Gemstones", 3 -> "Glacite"
        when (targetType) {
            0 -> {
                if (mineralType.category != MineralCategory.ORE) return false
                if (ignoreOthers && mineralType != MineralType.GOLD) return false
                if (royalDivan && !isGoldZoneValid()) return false
            }
            1 -> {
                if (mineralType != MineralType.MITHRIL) return false
                if (inDwarvenOnly && !isDwarvenZoneValid()) return false
            }
            2 -> {
                if (mineralType !in allowedTypes) return false
            }
            3 -> {
                val isGem = mineralType.category == MineralCategory.GEMSTONES
                val isGlaciteMetal = mineralType == MineralType.UMBER || mineralType == MineralType.TUNGSTEN
                if (!isGem && !isGlaciteMetal) return false
                if (!isGlaciteZoneValid()) return false
            }
        }
        return true
    }

    fun buildAllowedTypes(toggles: Map<MineralType, Boolean>): Set<MineralType> =
        toggles.filter { it.value }.keys
}
