package cn.hkim.addon.events

import cn.hkim.addon.Hkim
import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.events.impl.GardenEvent
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.cleanString
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.MayorData
import cn.hkim.addon.utils.skyblock.farming.PestTracker
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

object CustomEventDispatcher {
    private val visitRegex = Regex("\\[SkyBlock] (?:\\[.*?] )?(.*?) is visiting Your Garden!")
    private val pestSpawnRegex = Regex("(?:A \uE018 Pest has appeared|\\d+ \uE018 Pest have spawned) in Plot - (\\d{1,2})!")
    private val aliveRegex = Regex("Alive: (\\d+)")
    private val plotsRegex = Regex("Plots:\\s*([\\d, ]+)")
    private val plotRegex = Regex("Plot - (\\d+)")
    private var lastAliveCount = -1

    @EventHandler
    private fun onPacketReceive(event: PacketReceiveEvent) {
        when (event.packet) {
            is ClientboundSystemChatPacket -> {
                val chatEvent = ChatReceiveEvent(event.packet.content, event.packet.content.cleanString)
                Hkim.EVENT_BUS.post(chatEvent)
                if (chatEvent.isCancelled) event.cancel()
            }

            is ClientboundPlayerInfoUpdatePacket -> {
                if (LocationUtils.currentArea != Island.Garden) return
                if (event.packet.actions().none { it == ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER || it == ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME }) return
                handleTabList(event.packet)
            }
        }
    }

    private fun handleTabList(packet: ClientboundPlayerInfoUpdatePacket) {
        var aliveCount = -1

        for (entry in packet.entries()) {
            val display = entry.displayName()?.cleanString ?: continue

            aliveRegex.find(display)?.let { m ->
                aliveCount = m.groupValues[1].toIntOrNull() ?: -1
            }

            plotsRegex.find(display)?.let { m ->
                PestTracker.pestPlots = m.groupValues[1]
                    .split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()
            }
        }

        if (aliveCount == -1) return
        PestTracker.aliveCount = aliveCount

        if (lastAliveCount > 0 && aliveCount == 0 && isOnPestPlot()) {
            PestTracker.clearKnownPests()
            PestTracker.pestPlots = emptySet()
            Hkim.EVENT_BUS.post(GardenEvent.PestKilled())
            PestTracker.lastPestPlot = -1
        }
        lastAliveCount = aliveCount
    }

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        if (LocationUtils.currentArea != Island.Garden) return

        visitRegex.find(event.message)?.let { visitMatcher ->
            val playerName = visitMatcher.groupValues[1].trim()
            Hkim.EVENT_BUS.post(GardenEvent.GuestVisit(playerName))
        }

        pestSpawnRegex.find(event.message)?.let { pestMatcher ->
            val plot = pestMatcher.groupValues[1].toInt()
            PestTracker.lastPestPlot = plot
            lastAliveCount = -1
            Hkim.EVENT_BUS.post(GardenEvent.PestSpawned(plot))

            schedule((MayorData.pestSpawnCooldown - 10) * 20, true) {
                Hkim.EVENT_BUS.post(GardenEvent.PestReady())
            }
        }

        if (event.message.contains("Everybody unlocks exclusive perks!")) MayorData.fetchData()
    }

    private fun getCurrentPlot(): Int? {
        return HudUtils.getScoreboard().firstNotNullOfOrNull { line ->
            plotRegex.find(line.clean)?.groupValues?.get(1)?.toIntOrNull()
        }
    }

    private fun isOnPestPlot(): Boolean {
        val plot = getCurrentPlot() ?: return false
        return plot == PestTracker.lastPestPlot
    }
}
