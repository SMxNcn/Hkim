package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.Animations;
import cn.hkim.addon.utils.RotationUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Shadow
    public boolean swinging;

    @Unique
    private float hkim$savedYRot;

    @ModifyExpressionValue(method = "updateSwingTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCurrentSwingDuration()I"))
    private int modifySwingDuration(int original) {
        if (Animations.INSTANCE.getEnabled() && Animations.getIgnoreHaste()) return (int) Animations.getSpeed();
        return original;
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"), cancellable = true)
    private void preventReSwing(InteractionHand hand, CallbackInfo ci) {
        if (Animations.INSTANCE.shouldNotSwing() && this.swinging) ci.cancel();
    }

    @Unique
    private boolean shouldOverride() {
        return RotationUtils.isSilentAiming() || RotationUtils.isStoppingAiming();
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void onTravelHead(Vec3 input, CallbackInfo ci) {
        if (!shouldOverride()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.isEffectiveAi()) return;
        this.hkim$savedYRot = self.getYRot();
        self.setYRot(RotationUtils.getServerYaw());
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void onTravelReturn(Vec3 input, CallbackInfo ci) {
        if (!shouldOverride()) return;
        ((LivingEntity) (Object) this).setYRot(this.hkim$savedYRot);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void onJumpFromGroundHead(CallbackInfo ci) {
        if (!shouldOverride()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        this.hkim$savedYRot = self.getYRot();
        self.setYRot(RotationUtils.getServerYaw());
    }

    @Inject(method = "jumpFromGround", at = @At("RETURN"))
    private void onJumpFromGroundReturn(CallbackInfo ci) {
        if (!shouldOverride()) return;
        ((LivingEntity) (Object) this).setYRot(this.hkim$savedYRot);
    }
}
