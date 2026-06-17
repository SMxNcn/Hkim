package cn.hkim.addon.utils

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.PacketReceiveEvent
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket

object ServerUtils {
    private var prevTime = 0L
    var averageTps = 20f
        private set

    var currentPing: Int = 0
        private set

    var averagePing: Int = 0
        private set

    private val pingSamples = mutableListOf<Int>()
    private var lastPingSend = 0L

    @EventHandler
    fun onPacket(event: PacketReceiveEvent) {
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

        if (now - lastPingSend >= 200) {
            lastPingSend = now
            mc.connection?.send(ServerboundPingRequestPacket(now))
        }
    }

    private fun updatePing(sentTime: Long) {
        currentPing = (System.currentTimeMillis() - sentTime).toInt().coerceAtLeast(0)

        pingSamples.add(currentPing)
        if (pingSamples.size > 20) pingSamples.removeAt(0)

        averagePing = pingSamples.average().toInt()
    }
}
