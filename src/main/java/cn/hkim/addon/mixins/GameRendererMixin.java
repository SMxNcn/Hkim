package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.FreeCam;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(at = @At("HEAD"), method = "render")
    private void freecam$onRender(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        FreeCam.onRenderTick();
    }
}
