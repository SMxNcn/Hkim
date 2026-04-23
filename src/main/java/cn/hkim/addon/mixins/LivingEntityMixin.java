package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.Animations;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Shadow
    public boolean swinging;

    @ModifyExpressionValue(method = "updateSwingTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCurrentSwingDuration()I"))
    private int modifySwingDuration(int original) {
        if (Animations.INSTANCE.getEnabled() && Animations.getIgnoreHaste()) return Animations.getSpeed().intValue();
        return original;
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"), cancellable = true)
    private void preventReSwing(InteractionHand hand, CallbackInfo ci) {
        if (Animations.INSTANCE.shouldNotSwing() && this.swinging) ci.cancel();
    }
}
