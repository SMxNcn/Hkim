package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CleanView;
import cn.hkim.addon.features.impl.FreeCam;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(at = @At("HEAD"), method = "render")
    private void freecam$onRender(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        FreeCam.onRenderTick();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"))
    private float cleanView$disableNauseaSpinTick(LocalPlayer player, Holder<MobEffect> effect, float delta) {
        if (CleanView.shouldDisableDebuffs() && effect == MobEffects.NAUSEA) return 0.0F;
        return player.getEffectBlendFactor(effect, delta);
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"))
    private float cleanView$disableNauseaWobble(LocalPlayer player, Holder<MobEffect> effect, float delta) {
        if (CleanView.shouldDisableDebuffs() && effect == MobEffects.NAUSEA) return 0.0F;
        return player.getEffectBlendFactor(effect, delta);
    }
}
