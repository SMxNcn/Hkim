package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.features.impl.LeapMenu;
import cn.hkim.addon.features.impl.SwapOptions;
import cn.hkim.addon.utils.skyblock.inventory.SwapHandler;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerScreen.class)
public class ContainerScreenMixin {

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void extractBackground(CallbackInfo ci) {
        if (Hkim.mc.screen != null && SwapOptions.INSTANCE.shouldHideGui()) {
            if (LeapMenu.INSTANCE.isLeapMenu(Hkim.mc.screen) ||
                    SwapHandler.INSTANCE.isInSwap()) {
                ci.cancel();
            }
        }
    }
}
