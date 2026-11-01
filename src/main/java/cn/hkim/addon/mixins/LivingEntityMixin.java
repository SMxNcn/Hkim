package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.Animations;
import cn.hkim.addon.utils.RotationUtils;
//? if 26.2 {
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//? }
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
//? if 26.2 {
//? } else {
//import net.minecraft.world.item.component.SwingAnimation;
//? }
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
//? if 26.2 {
import org.spongepowered.asm.mixin.Shadow;
//? }
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if 26.2 {
//? } else {
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? }

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    //? if 26.2 {
    @Shadow
    public boolean swinging;
    //? }

    @Unique
    private float hkim$savedYRot;

    //? if 26.2 {
    @ModifyExpressionValue(method = {"updateSwingTime", "swing(Lnet/minecraft/world/InteractionHand;Z)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCurrentSwingDuration()I"))
    private int modifySwingDuration(int original) {
        if (Animations.INSTANCE.getEnabled() && Animations.getIgnoreHaste()) return (int) Animations.getSpeed();
        return original;
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"), cancellable = true)
    private void preventReSwing(InteractionHand hand, CallbackInfo ci) {
        if (Animations.INSTANCE.shouldNotSwing() && this.swinging) ci.cancel();
    }
    //? } else {
    /*@Inject(method = "getModifiedSwingDuration", at = @At("RETURN"), cancellable = true)
    private void modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (Animations.INSTANCE.getEnabled() && Animations.getIgnoreHaste()) cir.setReturnValue((int) Animations.getSpeed());
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z", at = @At("HEAD"), cancellable = true)
    private void preventReSwing(InteractionHand hand, SwingAnimation animation, boolean sendToSwingingEntity, CallbackInfoReturnable<Boolean> cir) {
        if (Animations.INSTANCE.shouldNotSwing() && ((LivingEntity) (Object) this).isSwinging()) cir.setReturnValue(false);
    }
    *///? }

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
