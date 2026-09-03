package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CleanView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = net.irisshaders.iris.uniforms.CommonUniforms.class)
public class CommonUniformsMixin {

    @Inject(method = "getBlindness", at = @At("HEAD"), cancellable = true)
    private static void getBlindness(CallbackInfoReturnable<Float> cir) {
        if (CleanView.shouldDisableDebuffs()) cir.setReturnValue(0f);
    }

    @Inject(method = "getDarknessFactor", at = @At("HEAD"), cancellable = true)
    private static void getDarknessFactor(CallbackInfoReturnable<Float> cir) {
        if (CleanView.shouldDisableDebuffs()) cir.setReturnValue(0f);
    }
}
