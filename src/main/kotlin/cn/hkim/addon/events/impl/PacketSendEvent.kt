package cn.hkim.addon.events.impl

import cn.hkim.addon.Hkim
import cn.hkim.addon.events.Cancellable
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet

class PacketSendEvent(
    packet: Packet<*>,
    val connection: Connection
) : Cancellable() {
    val isCommand: Boolean

    init {
        isCancelled = false
        isCommand = isCommandPacket(packet)
    }

    private fun isCommandPacket(packet: Packet<*>): Boolean {
        return try {
            val packetClass = packet.javaClass.simpleName
            packetClass == "ServerboundChatPacket"
        } catch (e: Exception) {
            Hkim.logger.error(e.stackTrace)
            false
        }
    }
}