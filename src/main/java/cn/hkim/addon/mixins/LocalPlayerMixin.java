package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.AutoSprint;
import cn.hkim.addon.utils.RotationUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//? if 26.2 {
//? } else {
//import cn.hkim.addon.features.impl.CleanView;
//? }
import net.minecraft.client.player.LocalPlayer;
//? if 26.2 {
//? } else {
/*import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
*///? }
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if 26.2 {
//? } else {
//import org.spongepowered.asm.mixin.injection.Redirect;
//? }
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Unique
    private float hkim$savedYaw;

    @Unique
    private float hkim$savedPitch;

    @Unique
    private float hkim$lastSentYaw;

    @Unique
    private boolean hkim$hasLastSentYaw;

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickEnd(CallbackInfo ci) {
        if (!RotationUtils.isSilentAiming() && !RotationUtils.isStoppingAiming()) return;
        RotationUtils.applyModelRotation(RotationUtils.getServerYaw());
    }

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void beforeSendPosition(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!RotationUtils.isSilentAiming() && !RotationUtils.isStoppingAiming()) {
            this.hkim$hasLastSentYaw = false;
            return;
        }

        this.hkim$savedYaw = self.getYRot();
        this.hkim$savedPitch = self.getXRot();

        float yaw = RotationUtils.getServerYaw();
        if (this.hkim$hasLastSentYaw) {
            float diff = yaw - this.hkim$lastSentYaw;
            yaw -= (float) Math.round(diff / 360.0) * 360.0f;
        }
        this.hkim$lastSentYaw = yaw;
        this.hkim$hasLastSentYaw = true;

        self.setYRot(yaw);
        self.setXRot(RotationUtils.getServerPitch());
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void afterSendPosition(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!RotationUtils.isSilentAiming() && !RotationUtils.isStoppingAiming()) return;

        self.setYRot(this.hkim$savedYaw);
        self.setXRot(this.hkim$savedPitch);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"))
    private boolean onSprintCheck(boolean original) {
        return original || AutoSprint.INSTANCE.getEnabled();
    }

    //? if 26.2 {
    //? } else {
    /*@Redirect(method = "tickSpinningEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"))
    private float hkim$disableNauseaSpin(LocalPlayer player, Holder<MobEffect> effect, float delta) {
        if (CleanView.shouldDisableDebuffs() && effect == MobEffects.NAUSEA) return 0.0F;
        return player.getEffectBlendFactor(effect, delta);
    }
    *///? }
}
