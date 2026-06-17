package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CleanView;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectFogEnvironment.class)
public class MobEffectFogEnvironmentMixin {

    @Inject(method = "isApplicable", at = @At("HEAD"), cancellable = true)
    private void onIsApplicable(FogType fogType, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof LivingEntity)) return;
        if (!CleanView.shouldHideBlindness()) return;

        MobEffectFogEnvironment self = (MobEffectFogEnvironment) (Object) this;
        Holder<MobEffect> effect = self.getMobEffect();
        ResourceKey<MobEffect> blindnessKey = MobEffects.BLINDNESS.unwrapKey().orElse(null);
        ResourceKey<MobEffect> darknessKey = MobEffects.DARKNESS.unwrapKey().orElse(null);
        if (darknessKey != null && blindnessKey != null && (effect.is(blindnessKey) || effect.is(darknessKey))) {
            cir.setReturnValue(false);
        }
    }
}
