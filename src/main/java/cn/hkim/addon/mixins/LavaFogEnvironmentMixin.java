package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CleanView;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LavaFogEnvironment.class)
public class LavaFogEnvironmentMixin {

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void onSetupFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!CleanView.shouldDisableLavaFog()) return;

        fog.environmentalStart = renderDistance * 0.8f;
        fog.environmentalEnd = renderDistance;
        fog.skyEnd = fog.environmentalEnd;
        fog.cloudEnd = fog.environmentalEnd;
        ci.cancel();
    }
}
