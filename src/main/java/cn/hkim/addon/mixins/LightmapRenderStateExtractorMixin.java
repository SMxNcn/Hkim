package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.FullBright;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {

    @Inject(method = "extract", at = @At("RETURN"))
    private void onExtract(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        if (!FullBright.INSTANCE.getEnabled()) return;

        renderState.needsUpdate = true;
        renderState.blockFactor = 2.0f;
        renderState.skyFactor = 2.0f;
        renderState.brightness = 1.0f;
        renderState.darknessEffectScale = 0.0f;
        renderState.nightVisionEffectIntensity = 1.0f;
        renderState.bossOverlayWorldDarkening = 0.0f;

        renderState.blockLightTint = new Vector3f(1.0f, 1.0f, 1.0f);
        renderState.skyLightColor = new Vector3f(1.0f, 1.0f, 1.0f);
        renderState.ambientColor = new Vector3f(1.0f, 1.0f, 1.0f);
        renderState.nightVisionColor = new Vector3f(1.0f, 1.0f, 1.0f);
    }
}
