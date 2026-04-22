package cn.hkim.addon.utils.skyblock

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.utils.equalsOneOf
import cn.hkim.addon.utils.startsWithOneOf
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket

object LocationUtils {
    var inSkyBlock: Boolean = false
        private set

    var currentArea: Island = Island.Unknown
        private set

    inline val inDungeons: Boolean get() = currentArea == Island.Dungeon

    @EventHandler
    fun onPacket(event: PacketReceiveEvent) {
        if (event.packet is ClientboundSetObjectivePacket) {
            if (!inSkyBlock) inSkyBlock = event.packet.objectiveName == "SBScoreboard"
        }

        if (event.packet is ClientboundPlayerInfoUpdatePacket) {
            if (!isCurrentArea(Island.Unknown) || event.packet.actions().none { it.equalsOneOf(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME) }) return
            val area = event.packet.entries().find { it.displayName?.string?.startsWithOneOf("Area: ", "Dungeon: ") == true }?.displayName?.string ?: return
            currentArea = Island.entries.firstOrNull { area.contains(it.displayName, true) } ?: Island.Unknown
        }
    }

    @EventHandler
    fun onWorldLoad() {
        currentArea = if (mc.isSingleplayer) Island.SinglePlayer else Island.Unknown
        inSkyBlock = false
    }

    fun isCurrentArea(vararg areas: Island): Boolean =
        if (currentArea == Island.SinglePlayer) true
        else areas.any { currentArea == it }
}