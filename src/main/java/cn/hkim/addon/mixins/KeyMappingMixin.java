package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.compat.tab.TabAnimation;
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
    private static void hkim$ignoreUnknownClick(InputConstants.Key key, CallbackInfo ci) {
        if (key.equals(InputConstants.UNKNOWN)) ci.cancel();
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private static void hkim$ignoreUnknownSet(InputConstants.Key key, boolean down, CallbackInfo ci) {
        if (key.equals(InputConstants.UNKNOWN)) ci.cancel();
    }

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void forcePlayerListKey(CallbackInfoReturnable<Boolean> cir) {
        if (!TabAnimation.INSTANCE.getForcingListKey()) return;
        //? if 26.2 {
        if ((Object) this != Hkim.mc.options.keyPlayerList) return;
        //? } else {
        //if ((Object) this != net.minecraft.client.Minecraft.getInstance().options.keyPlayerList) return;
        //? }
        cir.setReturnValue(true);
    }
}
