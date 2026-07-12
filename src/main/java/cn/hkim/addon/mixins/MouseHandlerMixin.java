package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.events.impl.MouseButtonEvent;
import cn.hkim.addon.features.impl.FreeCam;
import cn.hkim.addon.utils.KeyAction;
import cn.hkim.addon.utils.RotationUtils;
import cn.hkim.addon.utils.ViewLock;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MouseHandler.class, priority = 100)
public class MouseHandlerMixin {

    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onButton(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        if (action != 1 || Hkim.mc.level == null || Hkim.mc.player == null || Hkim.mc.screen != null) return;
        int button = rawButtonInfo.button();
        KeyAction keyAction = KeyAction.get(1);
        MouseButtonEvent event = MouseButtonEvent.get(button, keyAction);
        Hkim.EVENT_BUS.post(event);

        if (event.isCancelled()) ci.cancel();
    }

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void freecam$onTurnPlayer(LocalPlayer player, double yRot, double xRot) {
        if (FreeCam.onPlayerTurn(yRot, xRot) && !isMouseLocked()) {
            player.turn(yRot, xRot);
        }
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void onTurnPlayerHead(CallbackInfo ci) {
        if (isMouseLocked() && !FreeCam.isFreecamActive()) {
            accumulatedDX = 0.0;
            accumulatedDY = 0.0;
        }
    }

    @Inject(method = "turnPlayer", at = @At("TAIL"))
    private void onTurnPlayerTail(CallbackInfo ci) {
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
