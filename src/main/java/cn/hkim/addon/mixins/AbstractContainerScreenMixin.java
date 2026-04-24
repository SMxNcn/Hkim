package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.events.impl.GuiEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    protected void onInit(CallbackInfo ci) {
        GuiEvent.Open event = new GuiEvent.Open((Screen)(Object)this);
        Hkim.EVENT_BUS.post(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    protected void onClose(CallbackInfo ci) {
        GuiEvent.Close event = new GuiEvent.Close((Screen)(Object) this);
        Hkim.EVENT_BUS.post(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    protected void onExtractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        GuiEvent.Draw event = new GuiEvent.Draw((Screen)(Object) this, graphics, mouseX, mouseY);
        Hkim.EVENT_BUS.post(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    protected void onExtractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        GuiEvent.DrawSlot event = new GuiEvent.DrawSlot((Screen)(Object)this, graphics, slot);
        Hkim.EVENT_BUS.post(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    protected void onSlotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput, CallbackInfo ci) {
        GuiEvent.SlotClick event = new GuiEvent.SlotClick((Screen)(Object) this, slotId, buttonNum);
        Hkim.EVENT_BUS.post(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    protected void onExtractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        GuiEvent.DrawTooltip event = new GuiEvent.DrawTooltip((Screen)(Object)this, graphics, mouseX, mouseY);
        Hkim.EVENT_BUS.post(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    protected void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        GuiEvent.MouseClick mcEvent = new GuiEvent.MouseClick((Screen)(Object) this, event, doubleClick);
        Hkim.EVENT_BUS.post(mcEvent);
        if (mcEvent.isCancelled()) cir.cancel();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    protected void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        GuiEvent.KeyPress kpEvent = new GuiEvent.KeyPress((Screen)(Object)this, event);
        Hkim.EVENT_BUS.post(kpEvent);
        if (kpEvent.isCancelled()) cir.cancel();
    }
}
