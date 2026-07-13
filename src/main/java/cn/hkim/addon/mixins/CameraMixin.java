package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CameraHelper;
import cn.hkim.addon.features.impl.FreeCam;
import cn.hkim.addon.mixins.accessors.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cn.hkim.addon.Hkim.mc;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract float getMaxZoom(float cameraDist);

    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Unique
    private float hkim$transitionProgress = 0.0f;

    @Unique
    private CameraType hkim$lastCameraType = CameraType.FIRST_PERSON;

    @Unique
    private CameraType hkim$prevCameraType = CameraType.FIRST_PERSON;

    @Unique
    private boolean hkim$inTransition = false;

    @Unique
    private long hkim$transitionStartMs = -1;

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
    private float onGetMaxZoom(Camera instance, float cameraDist) {
        float zoom;
        if (CameraHelper.canCameraClip()) zoom = CameraHelper.getDistance();
        else zoom = getMaxZoom(CameraHelper.getDistance());
        if (hkim$inTransition) zoom *= easeOutCubic(hkim$transitionProgress);
        return zoom;
    }

    @Inject(method = "alignWithEntity", at = @At("HEAD"), cancellable = true)
    private void onAlignWithEntity(float partialTicks, CallbackInfo ci) {
        if (FreeCam.isFreecamActive()) {
            this.detached = true;
            setRotation(FreeCam.getCamYRot(), FreeCam.getCamXRot());
            setPosition(FreeCam.getCamX(), FreeCam.getCamY(), FreeCam.getCamZ());
            hkim$lastCameraType = mc.options.getCameraType();
            ci.cancel();
            return;
        }

        CameraType current = mc.options.getCameraType();
        if (current != hkim$lastCameraType) {
            if (CameraHelper.isTransitionActive()) {
                hkim$transitionStartMs = System.currentTimeMillis();
                hkim$inTransition = true;
                hkim$transitionProgress = current.isFirstPerson() ? 1.0f : 0.0f;
            } else {
                hkim$inTransition = false;
                hkim$transitionProgress = current.isFirstPerson() ? 0.0f : 1.0f;
                hkim$transitionStartMs = -1;
            }
            hkim$prevCameraType = hkim$lastCameraType;
            hkim$lastCameraType = current;
        }
        if (hkim$inTransition) {
            long elapsed = System.currentTimeMillis() - hkim$transitionStartMs;
            float durationMs = (float) CameraHelper.getTransitionDurationMs();
            float raw = Math.min(1.0f, elapsed / durationMs);
            if (current.isFirstPerson()) hkim$transitionProgress = 1.0f - raw;
            else hkim$transitionProgress = raw;
            if (raw >= 1.0f) {
                hkim$transitionProgress = current.isFirstPerson() ? 0.0f : 1.0f;
                hkim$inTransition = false;
                hkim$transitionStartMs = -1;
            }
        }
    }

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void onAlignWithEntityTail(float partialTicks, CallbackInfo ci) {
        if (!hkim$inTransition) return;

        CameraType current = mc.options.getCameraType();

        if (current.isFirstPerson()) {
            Camera self = (Camera) (Object) this;
            CameraAccessor cam = (CameraAccessor) this;

            float fullZoom;
            if (CameraHelper.canCameraClip()) fullZoom = CameraHelper.getDistance();
            else fullZoom = getMaxZoom(CameraHelper.getDistance());

            Vec3 eyePos = self.position();
            Vector3f offsetWorld = new Vector3f(0.0f, 0.0f, fullZoom);
            if (hkim$prevCameraType.isMirrored() && mc.player != null) {
                float yaw = mc.player.getViewYRot(partialTicks);
                float pitch = mc.player.getViewXRot(partialTicks);
                float rad = (float) Math.PI / 180.0f;
                Quaternionf mirror = new Quaternionf()
                    .rotationYXZ(-yaw * rad, pitch * rad, 0.0f);
                offsetWorld.rotate(mirror);
            } else {
                offsetWorld.rotate(self.rotation());
            }
            Vec3 thirdPersonPos = new Vec3(
                eyePos.x + (double) offsetWorld.x(),
                eyePos.y + (double) offsetWorld.y(),
                eyePos.z + (double) offsetWorld.z()
            );
            cam.invokeSetPosition(thirdPersonPos.lerp(eyePos, easeOutCubic(1.0f - hkim$transitionProgress)));
        }
    }

    @Unique
    private static float easeOutCubic(float x) {
        return 1.0f - (float) Math.pow(1.0f - x, 3);
    }

}
