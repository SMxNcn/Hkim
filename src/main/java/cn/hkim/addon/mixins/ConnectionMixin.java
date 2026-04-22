package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.events.impl.PacketReceiveEvent;
import cn.hkim.addon.events.impl.PacketSendEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"), cancellable = true)
    private void channelRead0(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        Connection connection = (Connection) (Object) this;
        PacketReceiveEvent event = new PacketReceiveEvent(packet, connection);
        Hkim.EVENT_BUS.post(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void handleSendPacket(Packet<?> packet, CallbackInfo ci) {
        Connection connection = (Connection) (Object) this;
        PacketSendEvent event = new PacketSendEvent(packet, connection);
        Hkim.EVENT_BUS.post(event);
        if (event.isCancelled()) ci.cancel();
    }
}
