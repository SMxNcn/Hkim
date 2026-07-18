package cn.hkim.addon.events.impl

import cn.hkim.addon.events.Cancellable
import net.minecraft.network.protocol.Packet

class PacketSendEvent(val packet: Packet<*>) : Cancellable()