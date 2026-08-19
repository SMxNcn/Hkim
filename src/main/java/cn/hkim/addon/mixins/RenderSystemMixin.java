package cn.hkim.addon.mixins;

import cn.hkim.addon.gui.BackgroundShader;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @Inject(method = "bindDefaultUniforms", at = @At("RETURN"))
    private static void hkim$bindBackgroundUniforms(RenderPass renderPass, CallbackInfo ci) {
        BackgroundShader.bindUniforms(renderPass);
    }
}
