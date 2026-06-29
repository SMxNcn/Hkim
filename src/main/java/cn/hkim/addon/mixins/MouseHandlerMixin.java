package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.events.impl.MouseButtonEvent;
import cn.hkim.addon.utils.KeyAction;
import cn.hkim.addon.utils.RotationUtils;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MouseHandler.class, priority = 100)
public class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onButton(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        if (action != 1 || Hkim.mc.level == null || Hkim.mc.player == null || Hkim.mc.screen != null) return;
        int button = rawButtonInfo.button();
        KeyAction keyAction = KeyAction.get(1);
        MouseButtonEvent event = MouseButtonEvent.get(button, keyAction);
        Hkim.EVENT_BUS.post(event);

        if (event.isCancelled()) ci.cancel();
    }
//
//    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
//    private void onScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
//        if (Hkim.mc.level == null || Hkim.mc.player == null) return;
//        MouseScrollEvent event = MouseScrollEvent.get(yoffset);
//        Hkim.EVENT_BUS.post(event);
//
//        if (event.isCancelled()) ci.cancel();
//    }

    @Inject(method = "turnPlayer", at = @At("TAIL"))
    private void onTurnPlayer(CallbackInfo ci) {
        if (Hkim.mc.player != null) {
            RotationUtils.syncClientRotation(
                Hkim.mc.player.getYRot(),
                Hkim.mc.player.getXRot()
            );
        }
    }
}
