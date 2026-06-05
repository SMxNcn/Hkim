package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CustomScoreboard;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class ScoreboardMixin {

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    public void onExtractScoreboardSidebar(CallbackInfo ci) {
        if (CustomScoreboard.INSTANCE.getEnabled()) ci.cancel();
    }
}
