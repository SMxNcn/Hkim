package cn.hkim.addon.utils.skyblock

import cn.hkim.addon.Hkim
import net.minecraft.resources.Identifier

enum class RollType(val displayName: String) {
    CATA("Cata"),
    GLACITE("Glacite"),
    KUUDRA("Kuudra")
}

object InfoUtils {
    private var tips: List<String>? = null

    private class RollTable(items: List<WeightedItem>) {
        val weightedItems: List<WeightedItem>
        val totalWeight: Double

        init {
            val total = items.sumOf { it.weight }
            weightedItems = if (total < 1.0) {
                items + WeightedItem(null, 1.0 - total)
            } else {
                items
            }
            totalWeight = weightedItems.sumOf { it.weight }
        }
    }

    private data class WeightedItem(val name: String?, val weight: Double)

    private val rollData = mapOf(
        RollType.CATA to RollTable(listOf(
            WeightedItem("Master Skull - Tier 5", 0.004416),
            WeightedItem("Thunderlord VII", 0.004247),
            WeightedItem("Fifth Master Star", 0.00334),
            WeightedItem("Implosion", 0.002781),
            WeightedItem("Shadow Warp", 0.002781),
            WeightedItem("Wither Shield", 0.002781),
            WeightedItem("Necron's Handle", 0.002),
            WeightedItem("Dark Claymore", 0.001181),
            WeightedItem("Necron Dye", 0.0005),
            WeightedItem("Shiny Necron's Handle", 0.0001),
        )),
        RollType.GLACITE to RollTable(listOf(
            WeightedItem("Caged Wisp", 0.0028),
            WeightedItem("Skeleton Key", 0.0028),
            WeightedItem("Shattered Locket", 0.0014),
            WeightedItem("Frostbitten Dye", 0.0001),
        )),
        RollType.KUUDRA to RollTable(listOf(
            WeightedItem("Burning Kuudra Core", 0.0051),
            WeightedItem("Ananke Feather", 0.0043),
            WeightedItem("Fatal Tempo I", 0.0023),
            WeightedItem("Inferno I", 0.0023),
            WeightedItem("Tormentor", 0.001),
            WeightedItem("Hellstorm Wand", 0.001),
            WeightedItem("Tentacle Dye", 0.00005),
        ))
    )

    fun randomTip(): String? {
        if (tips == null) {
            try {
                val id = Identifier.fromNamespaceAndPath("hkim", "lore.txt")
                tips = Hkim.mc.resourceManager.getResource(id).get().open().bufferedReader().readLines()
            } catch (_: Exception) {
                tips = emptyList()
                return null
            }
        }
        val list = tips ?: return null
        if (list.isEmpty()) return null
        return list.random()
    }

    fun formatLocation(): String {
        val pos = Hkim.mc.player?.blockPosition() ?: return "Unknown"
        val coord = "x: ${pos.x}, y: ${pos.y}, z: ${pos.z}"
        val zonePrefix = if (LocationUtils.isCurrentArea(Island.Rift)) "ф" else "⏣"
        val location = when {
            LocationUtils.inDungeons -> "${LocationUtils.currentArea.displayName} ${DungeonUtils.floor?.name ?: ""}"
            LocationUtils.inKuudra -> "${LocationUtils.currentArea.displayName} ${LocationUtils.kuudraTier.ifEmpty { "" }}"
            else -> "${LocationUtils.currentArea.displayName} - $zonePrefix ${LocationUtils.getCurrentZone() ?: ""}"
        }
        return "$coord | $location"
    }

    fun roll(type: RollType, times: Int = 10): Map<String, Int> {
        val table = rollData[type] ?: return emptyMap()
        if (table.totalWeight <= 0) return emptyMap()

        val results = mutableMapOf<String, Int>()
        repeat(times) {
            val random = Math.random() * table.totalWeight
            var accumulated = 0.0
            for (item in table.weightedItems) {
                accumulated += item.weight
                if (random <= accumulated) {
                    item.name?.let { name ->
                        results[name] = results.getOrDefault(name, 0) + 1
                    }
                    break
                }
            }
        }
        return results
    }

    fun formatRollResult(type: RollType, results: Map<String, Int>): String? {
        if (results.isEmpty()) return null
        val table = rollData[type] ?: return null
        val weightMap = table.weightedItems.filter { it.name != null }.associateBy { it.name }
        return results.entries.joinToString(" ") { (item, count) ->
            val pct = weightMap[item]?.let { "%.2f%%".format(it.weight * 100) } ?: "?"
            "$item ×$count ($pct)"
        }
    }
}
