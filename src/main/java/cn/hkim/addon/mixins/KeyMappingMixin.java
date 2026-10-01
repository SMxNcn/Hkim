package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.compat.tab.TabAnimation;
import cn.hkim.addon.events.impl.InputEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void onKeyPressed(InputConstants.Key key, CallbackInfo ci) {
        InputEvent event = new InputEvent(key);
        Hkim.EVENT_BUS.post(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void forcePlayerListKey(CallbackInfoReturnable<Boolean> cir) {
        if (!TabAnimation.INSTANCE.getForcingListKey()) return;
        if ((Object) this != Hkim.mc.options.keyPlayerList) return;
        cir.setReturnValue(true);
    }
}
