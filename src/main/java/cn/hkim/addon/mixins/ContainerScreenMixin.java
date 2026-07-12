package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.features.impl.LeapMenu;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerScreen.class)
public class ContainerScreenMixin {

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void extractBackground(CallbackInfo ci) {
        if (Hkim.mc.screen != null && LeapMenu.INSTANCE.isLeapMenu(Hkim.mc.screen)) {
            ci.cancel();
        }
    }
}
