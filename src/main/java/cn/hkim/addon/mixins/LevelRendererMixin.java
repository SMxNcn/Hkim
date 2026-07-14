package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CameraHelper;
import cn.hkim.addon.features.impl.CleanView;
import cn.hkim.addon.features.impl.FreeCam;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @ModifyArg(method = "update(Lnet/minecraft/client/Camera;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;cullTerrain(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Z)V"), index = 2)
    private boolean onCullTerrain(boolean isSpectator) {
        return FreeCam.isFreecamActive() || CameraHelper.canCameraClip() || CleanView.shouldSeeThroughBlocks() || isSpectator;
    }
}