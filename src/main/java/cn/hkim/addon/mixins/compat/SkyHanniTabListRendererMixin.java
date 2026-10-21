package cn.hkim.addon.mixins.compat;

import cn.hkim.addon.compat.tab.TabAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "at.hannibal2.skyhanni.features.misc.compacttablist.TabListRenderer", remap = false)
public class SkyHanniTabListRendererMixin {

    // Compatible with SkyHanni's "Toggle Tab" feature.
    @Inject(method = "drawTabList", at = @At("HEAD"), cancellable = true, require = 0)
    private void hkim$suppressDraw(CallbackInfo ci) {
        if (TabAnimation.requestExternalDraw()) ci.cancel();
    }
}
