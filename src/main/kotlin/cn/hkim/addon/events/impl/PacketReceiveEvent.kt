package cn.hkim.addon.events.impl

import cn.hkim.addon.events.Cancellable
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet

class PacketReceiveEvent(
    val packet: Packet<*>,
    val connection: Connection
) : Cancellable()