package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.events.impl.GuiEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    protected void onExtractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if ((Object) this instanceof AbstractContainerScreen<?>) {
            GuiEvent.DrawBackground event = new GuiEvent.DrawBackground((Screen)(Object) this, graphics, mouseX, mouseY);
            Hkim.EVENT_BUS.post(event);
            if (event.isCancelled()) ci.cancel();
        }
    }
}
