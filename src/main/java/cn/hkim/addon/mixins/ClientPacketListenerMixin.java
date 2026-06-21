package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.events.impl.GuiEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cn.hkim.addon.Hkim.mc;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;showNetworkCharts()Z"))
    private boolean showNetworkCharts(boolean original) {
        return true;
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void handleContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (mc.gui.screen() instanceof AbstractContainerScreen<?> containerScreen) {
            GuiEvent.SlotUpdate event = new GuiEvent.SlotUpdate(mc.gui.screen(), packet, containerScreen.getMenu());
            Hkim.EVENT_BUS.post(event);
        }
    }
}