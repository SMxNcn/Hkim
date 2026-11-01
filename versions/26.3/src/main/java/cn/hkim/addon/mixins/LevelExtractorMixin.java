package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CleanView;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {

    @Inject(method = "getViewBlockingState", at = @At("RETURN"), cancellable = true)
    private static void onGetViewBlockingState(CallbackInfoReturnable<BlockState> cir) {
        if (CleanView.shouldSeeThroughBlocks()) cir.setReturnValue(null);
    }

    @Redirect(method = "extractPlayerState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"))
    private float hkim$disableNauseaWobble(LocalPlayer player, Holder<MobEffect> effect, float delta) {
        if (CleanView.shouldDisableDebuffs() && effect == MobEffects.NAUSEA) return 0.0F;
        return player.getEffectBlendFactor(effect, delta);
    }
}
