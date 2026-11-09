package cn.hkim.addon.utils.skyblock.slayer

import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.skyblock.LocationUtils

object SlayerUtils {
    fun currentType(): SlayerType? {
        val areaType = fromArea() ?: return null
        val questType = fromScoreboardQuest()
        return if (questType == null || questType == areaType) areaType else null
    }

    private fun fromScoreboardQuest(): SlayerType? {
        val lines = HudUtils.getScoreboard().map { it.clean.trim() }
        val questIndex = lines.indexOfFirst { it == "Slayer Quest" }
        if (questIndex == -1) return null

        for (line in lines.drop(questIndex + 1).take(3)) {
            if (line.isEmpty()) continue
            return SlayerType.entries.firstOrNull { type ->
                line.startsWith(type.displayName, ignoreCase = true) ||
                    line.startsWith(type.meterName, ignoreCase = true)
            } ?: continue
        }
        return null
    }

    private fun fromArea(): SlayerType? {
        val zone = LocationUtils.getCurrentZone()?.clean?.trim() ?: return null
        return when {
            zone.containsAny("Graveyard", "Crypts") -> SlayerType.REVENANT
            zone.containsAny("Arachne's Burrow", "Arachne's Sanctuary", "Burning Desert") -> SlayerType.TARANTULA
            zone.containsAny("Ruins", "Howling Cave", "Soul Cave", "Spirit Cave") -> SlayerType.SVEN
            zone.containsAny("Void Sepulture", "Zealot Bruiser Hideout", "Dragon's Nest") -> SlayerType.VOID
            zone.containsAny("Stronghold", "Smoldering Tomb") -> SlayerType.INFERNO
            zone.containsAny("Stillgore Château", "Oubliette") -> SlayerType.VAMPIRE
            else -> null
        }
    }

    private fun String.containsAny(vararg values: String): Boolean =
        values.any { contains(it, ignoreCase = true) }
}
