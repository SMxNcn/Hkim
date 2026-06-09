package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CustomScoreboard;
import cn.hkim.addon.features.impl.ModuleList;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    public void onExtractEffects(CallbackInfo ci) {
        if (ModuleList.INSTANCE.getEnabled()) ci.cancel();
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    public void onExtractScoreboardSidebar(CallbackInfo ci) {
        if (CustomScoreboard.INSTANCE.getEnabled()) ci.cancel();
    }
}
