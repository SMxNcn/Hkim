package cn.hkim.addon.utils.skyblock.slayer

import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.containsOneOf
import cn.hkim.addon.utils.skyblock.LocationUtils

object SlayerUtils {
    var state: SlayerState = SlayerState.NOT_ACTIVE
        private set

    var lastKillTimeSeconds: Double = 0.0
        private set

    @Volatile
    private var serverTicks = 0L
    private var combatStartTick = -1L

    fun onServerTick() {
        serverTicks++
    }

    fun updateState(): Double? {
        val lines = scoreboardLines()
        val hasQuest = lines.any { it == "Slayer Quest" }
        val inCombat = lines.any { it == "Slay the boss!" }

        val newState = when {
            !hasQuest -> SlayerState.NOT_ACTIVE
            lines.any { it == "Boss slain!" } -> SlayerState.BOSS_SLAIN
            inCombat -> SlayerState.IN_COMBAT
            else -> SlayerState.SUMMONING
        }

        var killTime: Double? = null
        if (state == SlayerState.IN_COMBAT && newState != SlayerState.IN_COMBAT) {
            if (combatStartTick >= 0) {
                killTime = ((serverTicks - combatStartTick) / 20.0).coerceAtLeast(0.0)
                lastKillTimeSeconds = killTime
            }
            combatStartTick = -1L
        } else if (newState == SlayerState.IN_COMBAT && state != SlayerState.IN_COMBAT) {
            combatStartTick = serverTicks
        }

        state = newState
        return killTime
    }

    fun resetState() {
        state = SlayerState.NOT_ACTIVE
        combatStartTick = -1L
    }

    fun currentType(): SlayerType? {
        val areaType = fromArea() ?: return null
        val questType = fromScoreboardQuest()
        return if (questType == null || questType == areaType) areaType else null
    }

    private fun scoreboardLines(): List<String> =
        HudUtils.getScoreboard().map { it.clean.trim() }

    private fun fromScoreboardQuest(): SlayerType? {
        val lines = scoreboardLines()
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
            zone.containsOneOf("Graveyard", "Crypts", ignoreCase = true) -> SlayerType.REVENANT
            zone.containsOneOf("Arachne's Burrow", "Arachne's Sanctuary", "Burning Desert", ignoreCase = true) -> SlayerType.TARANTULA
            zone.containsOneOf("Ruins", "Howling Cave", "Soul Cave", "Spirit Cave", ignoreCase = true) -> SlayerType.SVEN
            zone.containsOneOf("Void Sepulture", "Zealot Bruiser Hideout", "Dragon's Nest", ignoreCase = true) -> SlayerType.VOID
            zone.containsOneOf("Stronghold", "Smoldering Tomb", ignoreCase = true) -> SlayerType.INFERNO
            zone.containsOneOf("Stillgore Château", "Oubliette", ignoreCase = true) -> SlayerType.VAMPIRE
            else -> null
        }
    }
}

enum class SlayerState {
    NOT_ACTIVE,
    SUMMONING,
    IN_COMBAT,
    BOSS_SLAIN
}
