package cn.hkim.addon.utils.skyblock

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.utils.HudUtils.getScoreboard
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.equalsOneOf
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.startsWithOneOf
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import kotlin.jvm.optionals.getOrNull

object LocationUtils {
    var inSkyBlock: Boolean = false
        private set

    var currentArea: Island = Island.Unknown
        private set

    var kuudraTier: String = ""
        private set

    private val kuudraTierRegex = Regex("Kuudra's Hollow \\(T(\\d)\\)")

    inline val inDungeons: Boolean get() = currentArea == Island.Dungeon
    inline val inKuudra: Boolean get() = currentArea == Island.Kuudra
    @Suppress("unused")
    inline val inAlphaServer: Boolean get() = inSkyBlock && mc.currentServer?.ip?.contains("alpha.hypixel.net") == true

    @EventHandler
    private fun onPacketReceive(event: PacketReceiveEvent) {
        when (event.packet) {
            is ClientboundSetObjectivePacket -> {
                if (!inSkyBlock) inSkyBlock = event.packet.objectiveName == "SBScoreboard"
            }
            is ClientboundPlayerInfoUpdatePacket -> {
                if (!isCurrentArea(Island.Unknown) || event.packet.actions().none { it.equalsOneOf(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME) }) return
                val area = event.packet.entries().find { it.displayName?.string?.startsWithOneOf("Area: ", "Dungeon: ") == true }?.displayName?.string ?: return
                currentArea = Island.entries.firstOrNull { area.contains(it.displayName, true) } ?: Island.Unknown
            }

            is ClientboundSetPlayerTeamPacket -> {
                val text = event.packet.parameters.getOrNull()?.let { it.playerPrefix.string + it.playerSuffix.string }?.clean ?: return
                kuudraTierRegex.find(text)?.groupValues?.get(1)?.let { kuudraTier = "T$it" }
            }
        }
    }

    @EventHandler
    private fun onWorldLoad(event: WorldEvent.Load) {
        schedule(2) {
            currentArea = if (mc.isSingleplayer) Island.SinglePlayer else Island.Unknown
            inSkyBlock = false
        }
    }

    @EventHandler
    private fun onWorldUnload(event: WorldEvent.Unload) {
        currentArea = Island.Unknown
        inSkyBlock = false
        kuudraTier = ""
    }

    fun isCurrentArea(vararg areas: Island): Boolean =
        if (currentArea == Island.SinglePlayer) true
        else areas.any { currentArea == it }

    fun getCurrentZone(): String? {
        val scoreboard = getScoreboard()
        if (scoreboard.isEmpty()) return null
        for (line in scoreboard) {
            Regex("[⏣ф\uE067\uE020]\\s*(.+)").find(line.clean)?.let {
                return it.groupValues[1].trim()
            }
        }
        return null
    }
}