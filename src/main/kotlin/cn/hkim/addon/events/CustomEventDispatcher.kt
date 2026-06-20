package cn.hkim.addon.events

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.*
import cn.hkim.addon.features.impl.FarmingHelper
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.MayorData
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket
import net.minecraft.world.entity.Relative
import java.util.*

object CustomEventDispatcher {
    private val visitRegex = Regex("\\[SkyBlock] (?:\\[.*?] )?(.*?) is visiting Your Garden!")
    private val pestSpawnRegex = Regex("(?:A ൠ Pest has appeared|\\d+ ൠ Pest have spawned) in Plot - (\\d{1,2})!")
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

    @EventHandler
    private fun onPacket(event: PacketReceiveEvent) {
        if (LocationUtils.currentArea != Island.Garden) return

        when (event.packet) {
            is ClientboundPlayerPositionPacket -> {
                if (mc.player?.mainHandItem?.itemId?.containsOneOf(FarmingHelper.specialItemList) == true) return
                val pkt = event.packet
                val isTeleport = pkt.relatives.any { it in EnumSet.of(Relative.X, Relative.Y, Relative.Z) }
                val isRotation = pkt.relatives.any { it == Relative.Y_ROT || it == Relative.X_ROT }
                if (isTeleport) Hkim.EVENT_BUS.post(GardenEvent.FailSafe("Teleport"))
                else if (isRotation) Hkim.EVENT_BUS.post(GardenEvent.FailSafe("Rotation"))
            }
            is ClientboundSetHeldSlotPacket -> {
                Hkim.EVENT_BUS.post(GardenEvent.FailSafe("Held Item Change"))
            }
        }
    }

    @EventHandler
    private fun onWorldUnload(event: WorldEvent.Unload) {
        if (LocationUtils.currentArea != Island.Garden) return
        Hkim.EVENT_BUS.post(GardenEvent.FailSafe("World Change"))
    }

    private fun getCurrentPlot(): Int? {
        val cleanRegex = Regex("§[0-9a-fk-or]")
        val plotRegex = Regex("Plot - (\\d+)")
        return HudUtils.getScoreboard().firstNotNullOfOrNull { line ->
            plotRegex.find(line.replace(cleanRegex, ""))?.groupValues[1]?.toIntOrNull()
        }
    }

    private fun getCurrentPestCount(plot: Int): Int {
        val cleanRegex = Regex("§[0-9a-fk-or]")
        val pestRegex = Regex("Plot - $plot(?: ൠ x(\\d+))?")
        return HudUtils.getScoreboard().firstNotNullOfOrNull { line ->
            pestRegex.find(line.replace(cleanRegex, ""))?.groupValues?.getOrNull(1)?.toIntOrNull()
        } ?: 0
    }
}
