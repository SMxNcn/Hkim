package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CustomScoreboard;
import cn.hkim.addon.hud.HudBlurBackground;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void hkim$renderHudBlurBackground(GuiGraphicsExtractor graphics, DeltaTracker tickTracker, CallbackInfo ci) {
        HudBlurBackground.render(graphics);
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    public void onExtractScoreboardSidebar(CallbackInfo ci) {
        if (CustomScoreboard.INSTANCE.getEnabled()) ci.cancel();
    }
}
