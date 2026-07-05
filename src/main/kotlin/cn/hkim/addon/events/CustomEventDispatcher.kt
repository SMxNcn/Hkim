package cn.hkim.addon.events

import cn.hkim.addon.Hkim
import cn.hkim.addon.events.impl.*
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.MayorData
import meteordevelopment.orbit.EventHandler

object CustomEventDispatcher {
    private val visitRegex = Regex("\\[SkyBlock] (?:\\[.*?] )?(.*?) is visiting Your Garden!")
    private val pestSpawnRegex = Regex("(?:A ൠ Pest has appeared|\\d+ ൠ Pest have spawned) in Plot - (\\d{1,2})!")
    private val cleanRegex = Regex("§[0-9a-fk-or]")
    private val plotRegex = Regex("Plot - (\\d+)")
    private var activePestPlot = -1
    private var lastPestCount = -1

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        if (LocationUtils.currentArea != Island.Garden || activePestPlot == -1) return

        val currentPlot = getCurrentPlot() ?: return
        if (currentPlot != activePestPlot) return

        val currentPestCount = getCurrentPestCount(activePestPlot)

        if (lastPestCount > 0 && currentPestCount == 0) {
            Hkim.EVENT_BUS.post(GardenEvent.PestKilled())
            activePestPlot = -1
        }

        lastPestCount = currentPestCount
    }

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        if (LocationUtils.currentArea != Island.Garden) return

        visitRegex.find(event.message)?.let { visitMatcher ->
            val playerName = visitMatcher.groupValues[1].trim()
            Hkim.EVENT_BUS.post(GardenEvent.GuestVisit(playerName))
        }

        pestSpawnRegex.find(event.message)?.let { pestMatcher ->
            activePestPlot = pestMatcher.groupValues[1].toInt()
            Hkim.EVENT_BUS.post(GardenEvent.PestSpawned(activePestPlot))

            schedule((MayorData.pestSpawnCooldown - 10) * 20, true) {
                Hkim.EVENT_BUS.post(GardenEvent.PestReady())
            }
        }

        if (event.message.contains("Everybody unlocks exclusive perks!")) MayorData.fetchData()
    }

    private val pestRegexCache = HashMap<Int, Regex>(32)

    private fun getCurrentPlot(): Int? {
        return HudUtils.getScoreboard().firstNotNullOfOrNull { line ->
            plotRegex.find(line.replace(cleanRegex, ""))?.groupValues[1]?.toIntOrNull()
        }
    }

    private fun getCurrentPestCount(plot: Int): Int {
        val pestRegex = pestRegexCache.getOrPut(plot) {
            Regex("Plot - $plot(?: ൠ x(\\d+))?")
        }
        return HudUtils.getScoreboard().firstNotNullOfOrNull { line ->
            pestRegex.find(line.replace(cleanRegex, ""))?.groupValues?.getOrNull(1)?.toIntOrNull()
        } ?: 0
    }
}
