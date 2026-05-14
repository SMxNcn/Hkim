package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CameraHelper;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract float getMaxZoom(float cameraDist);

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
    private float getMaxZoom(Camera instance, float cameraDist) {
        if (CameraHelper.INSTANCE.canCameraClip()) {
            return CameraHelper.INSTANCE.getDistance();
        }

        return getMaxZoom(CameraHelper.INSTANCE.getDistance());
    }
}
