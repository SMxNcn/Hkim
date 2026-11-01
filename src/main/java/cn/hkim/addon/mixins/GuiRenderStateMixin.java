package cn.hkim.addon.mixins;

import cn.hkim.addon.compat.tab.TabAnimation;
import net.minecraft.client.renderer.state.gui.*;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public class GuiRenderStateMixin {

    @Inject(method = "addGuiElement", at = @At("HEAD"))
    private void hkim$trackElement(GuiElementRenderState blitState, CallbackInfo ci) {
        TabAnimation.trackElement(blitState.bounds());
    }

    @Inject(method = "addBlitToCurrentLayer", at = @At("HEAD"))
    private void hkim$trackBlit(BlitRenderState blitState, CallbackInfo ci) {
        TabAnimation.trackElement(blitState.bounds());
    }

    @Inject(method = "addText", at = @At("HEAD"))
    private void hkim$trackText(GuiTextRenderState textState, CallbackInfo ci) {
        TabAnimation.trackElement(textState.bounds());
    }

    @Inject(method = "addItem", at = @At("HEAD"))
    private void hkim$trackItem(GuiItemRenderState itemState, CallbackInfo ci) {
        TabAnimation.trackElement(itemState.bounds());
    }

    @Inject(method = "addPicturesInPictureState", at = @At("HEAD"))
    private void hkim$trackPip(PictureInPictureRenderState picturesInPictureState, CallbackInfo ci) {
        TabAnimation.trackElement(picturesInPictureState.bounds());
    }
}
