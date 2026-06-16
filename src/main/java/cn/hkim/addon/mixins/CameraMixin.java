package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CameraHelper;
import cn.hkim.addon.mixins.accessors.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.world.phys.Vec3;
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

    @Unique
    private Vec3 hkim$springOff = Vec3.ZERO;

    @Unique
    private Vec3 hkim$springVel = Vec3.ZERO;

    @Unique
    private boolean hkim$springInit = false;

    @Unique
    private double hkim$smoothAbsY = 0.0;

    @Unique
    private float hkim$prevPartialTick = 0.0f;

    @Unique
    private float hkim$transitionProgress = 1.0f;

    @Unique
    private CameraType hkim$lastCameraType = CameraType.FIRST_PERSON;

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

    @Inject(method = "alignWithEntity", at = @At("HEAD"))
    private void onAlignWithEntityHead(float partialTicks, CallbackInfo ci) {
        CameraType current = mc.options.getCameraType();
        if (current != hkim$lastCameraType) {
            boolean fpChanged = current.isFirstPerson() != hkim$lastCameraType.isFirstPerson();
            boolean neitherMirrored = !current.isMirrored() && !hkim$lastCameraType.isMirrored();
            if (fpChanged && neitherMirrored && CameraHelper.isTransitionActive()) {
                hkim$transitionStartMs = System.currentTimeMillis();
                hkim$inTransition = true;
                hkim$transitionProgress = current.isFirstPerson() ? 1.0f : 0.0f;
            } else {
                hkim$inTransition = false;
                hkim$transitionProgress = current.isFirstPerson() ? 0.0f : 1.0f;
                hkim$transitionStartMs = -1;
            }
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
        Camera self = (Camera) (Object) this;
        CameraAccessor cam = (CameraAccessor) this;

        if (hkim$inTransition && mc.options.getCameraType().isFirstPerson()) {
            float fullZoom;
            if (CameraHelper.canCameraClip()) fullZoom = CameraHelper.getDistance();
            else fullZoom = getMaxZoom(CameraHelper.getDistance());
            Vec3 eyePos = self.position();
            Vector3f offsetWorld = new Vector3f(0.0f, 0.0f, fullZoom);
            offsetWorld.rotate(self.rotation());
            Vec3 thirdPersonPos = new Vec3(
                eyePos.x + (double) offsetWorld.x(),
                eyePos.y + (double) offsetWorld.y(),
                eyePos.z + (double) offsetWorld.z()
            );
            cam.invokeSetPosition(thirdPersonPos.lerp(eyePos, easeOutCubic(1.0f - hkim$transitionProgress)));
            return;
        }

        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            hkim$springInit = false; hkim$prevPartialTick = 0.0f; return;
        }
        if (!CameraHelper.isMovementSmoothActive()) {
            hkim$springInit = false; hkim$prevPartialTick = 0.0f; return;
        }

        float zoom;
        if (CameraHelper.canCameraClip()) zoom = CameraHelper.getDistance();
        else zoom = getMaxZoom(CameraHelper.getDistance());
        if (hkim$inTransition) zoom *= easeOutCubic(hkim$transitionProgress);

        Vector3f off = new Vector3f(0.0f, 0.0f, zoom);
        off.rotate(self.rotation());

        Vec3 target = self.position();
        Vec3 eyePos = new Vec3(
            target.x - (double) off.x(),
            target.y - (double) off.y(),
            target.z - (double) off.z()
        );

        if (!hkim$springInit) {
            hkim$springOff = new Vec3(off.x(), off.y(), off.z());
            hkim$springVel = Vec3.ZERO;
            hkim$springInit = true;
            hkim$smoothAbsY = target.y;
            hkim$prevPartialTick = partialTicks;
            cam.invokeSetPosition(target);
            return;
        }

        float dpt = partialTicks - hkim$prevPartialTick;
        if (dpt < 0.0f) dpt += 1.0f;
        dpt = Math.min(1.0f, Math.max(0.0f, dpt));
        hkim$prevPartialTick = partialTicks;
        double dt = dpt * 0.05;

        double sxz = CameraHelper.getSmoothSpeedXZ();
        double sy  = CameraHelper.getSmoothSpeedY();

        double wx = sxz * 10.0, wx2 = wx * wx;

        double dx = hkim$springOff.x - (double) off.x();
        double dz = hkim$springOff.z - (double) off.z();

        double nvx = hkim$springVel.x + (-wx2 * dx - 2.0 * wx * hkim$springVel.x) * dt;
        double nvz = hkim$springVel.z + (-wx2 * dz - 2.0 * wx * hkim$springVel.z) * dt;

        double npx = hkim$springOff.x + nvx * dt;
        double npz = hkim$springOff.z + nvz * dt;

        if (dx * dx + dz * dz < 1e-12
            && nvx * nvx + nvz * nvz < 1e-12) {
            hkim$springOff = new Vec3(off.x(), hkim$springOff.y, off.z());
            hkim$springVel = Vec3.ZERO;
        } else {
            hkim$springOff = new Vec3(npx, hkim$springOff.y, npz);
            hkim$springVel = new Vec3(nvx, 0.0, nvz);
        }

        double rawY = eyePos.y + (double) off.y();
        double syClamped = Math.min(1.0, Math.max(0.01, sy));
        hkim$smoothAbsY += (rawY - hkim$smoothAbsY) * syClamped;

        cam.invokeSetPosition(new Vec3(
            eyePos.x + hkim$springOff.x,
            hkim$smoothAbsY,
            eyePos.z + hkim$springOff.z
        ));
    }

    @Unique
    private static float easeOutCubic(float x) {
        return 1.0f - (float) Math.pow(1.0f - x, 3);
    }
}
