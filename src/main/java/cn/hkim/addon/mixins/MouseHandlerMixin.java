package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.events.impl.MouseButtonEvent;
import cn.hkim.addon.utils.KeyAction;
import cn.hkim.addon.utils.RotationUtils;
import cn.hkim.addon.utils.ViewLock;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onButton(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        if (action != 1 || Hkim.mc.level == null || Hkim.mc.player == null || Hkim.mc.gui.screen() != null) return;
        int button = rawButtonInfo.button();
        KeyAction keyAction = KeyAction.get(1);
        MouseButtonEvent event = MouseButtonEvent.get(button, keyAction);
        Hkim.EVENT_BUS.post(event);

        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void onTurnPlayerHead(CallbackInfo ci) {
        if (isMouseLocked()) {
            accumulatedDX = 0.0;
            accumulatedDY = 0.0;
        }
    }

    @Inject(method = "turnPlayer", at = @At("TAIL"))
    private void onTurnPlayer(CallbackInfo ci) {
        if (Hkim.mc.player != null) {
            RotationUtils.syncClientRotation(
                Hkim.mc.player.getYRot(),
                Hkim.mc.player.getXRot()
            );
        }
    }

    @Unique
    private static boolean isMouseLocked() {
        return ViewLock.isLocked();
    }
}
