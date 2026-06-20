package cn.hkim.addon.utils

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.PacketReceiveEvent
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket
import kotlin.math.min

object ServerUtils {
    private var prevTime = 0L
    var averageTps = 20f
        private set

    var currentPing: Int = 0
        private set

    var averagePing: Int = 0
        private set

    @EventHandler
    private fun onPacket(event: PacketReceiveEvent) {
        when (event.packet) {
            is ClientboundSetTimePacket -> updateTps()
            is ClientboundPongResponsePacket -> updatePing(event.packet.time)
        }
    }

    private fun updateTps() {
        val now = System.currentTimeMillis()
        if (prevTime != 0L) {
            val interval = now - prevTime + 1
            averageTps = (20000f / interval).coerceIn(0f, 20f)
        }
        prevTime = now
    }

    private fun updatePing(sentTime: Long) {
        currentPing = (System.currentTimeMillis() - sentTime).toInt().coerceAtLeast(0)

        val pingLog = mc.debugOverlay.pingLogger
        val sampleSize = min(pingLog.size(), 20)
        if (sampleSize == 0) {
            averagePing = currentPing
            return
        }

        var total = 0L
        for (i in 0 until sampleSize) total += pingLog[i]

        averagePing = (total / sampleSize).toInt()
    }
}
