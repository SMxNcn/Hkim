package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.Animations;
import cn.hkim.addon.utils.RotationUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Unique
    private float hkim$savedYRot;

    @Inject(method = "getModifiedSwingDuration", at = @At("RETURN"), cancellable = true)
    private void modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (Animations.INSTANCE.getEnabled() && Animations.getIgnoreHaste()) cir.setReturnValue((int) Animations.getSpeed());
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z", at = @At("HEAD"), cancellable = true)
    private void preventReSwing(InteractionHand hand, SwingAnimation animation, boolean sendToSwingingEntity, CallbackInfoReturnable<Boolean> cir) {
        if (Animations.INSTANCE.shouldNotSwing() && ((LivingEntity) (Object) this).isSwinging()) cir.setReturnValue(false);
    }

    @Unique
    private boolean hkim$shouldOverrideRotation() {
        return RotationUtils.isSilentAiming() || RotationUtils.isStoppingAiming();
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void onTravelHead(Vec3 input, CallbackInfo ci) {
        if (!hkim$shouldOverrideRotation()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.isEffectiveAi()) return;
        this.hkim$savedYRot = self.getYRot();
        self.setYRot(RotationUtils.getServerYaw());
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void onTravelReturn(Vec3 input, CallbackInfo ci) {
        if (!hkim$shouldOverrideRotation()) return;
        ((LivingEntity) (Object) this).setYRot(this.hkim$savedYRot);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void onJumpFromGroundHead(CallbackInfo ci) {
        if (!hkim$shouldOverrideRotation()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        this.hkim$savedYRot = self.getYRot();
        self.setYRot(RotationUtils.getServerYaw());
    }

    @Inject(method = "jumpFromGround", at = @At("RETURN"))
    private void onJumpFromGroundReturn(CallbackInfo ci) {
        if (!hkim$shouldOverrideRotation()) return;
        ((LivingEntity) (Object) this).setYRot(this.hkim$savedYRot);
    }
}
