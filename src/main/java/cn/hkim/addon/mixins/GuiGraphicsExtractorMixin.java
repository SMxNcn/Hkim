package cn.hkim.addon.mixins;

import cn.hkim.addon.compat.tab.TabAnimation;
import cn.hkim.addon.features.impl.ItemFeatures;
import cn.hkim.addon.utils.ItemUtilsKt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cn.hkim.addon.utils.ItemUtilsKt.isSkyBlockItem;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {

    @WrapOperation(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemCount(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private void replaceCountWithUpgradeLevel(GuiGraphicsExtractor instance, Font font, ItemStack itemStack, int x, int y, String countText, Operation<Void> original) {
        String text = countText;
        if (ItemFeatures.INSTANCE.isStarDisplayEnabled() && isSkyBlockItem(itemStack)) {
            int upgradeLevel = ItemUtilsKt.getItemUpgradeLevel(itemStack);
            if (upgradeLevel >= 1) text = String.valueOf(upgradeLevel);
        }
        original.call(instance, font, itemStack, x, y, text);
    }

    @Inject(method = "fill(IIIII)V", at = @At("HEAD"), cancellable = true)
    private void dropTabBackground(int x0, int y0, int x1, int y1, int col, CallbackInfo ci) {
        int left = Math.min(x0, x1);
        int top = Math.min(y0, y1);
        int right = Math.max(x0, x1);
        int bottom = Math.max(y0, y1);
        if (right <= left || bottom <= top) return;

        Matrix3x2fStack pose = ((GuiGraphicsExtractor) (Object) this).pose();
        ScreenRectangle bounds = new ScreenRectangle(left, top, right - left, bottom - top).transformMaxBounds(pose);
        if (TabAnimation.dropTabBackground(col, bounds)) ci.cancel();
    }

    @ModifyVariable(method = "fill(IIIII)V", at = @At("HEAD"), argsOnly = true, name = "col")
    private int fadeTabFill(int col) {
        return TabAnimation.fadeTabColor(col);
    }

    @ModifyVariable(method = "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIFFFFI)V", at = @At("HEAD"), argsOnly = true, name = "color")
    private int fadeTabBlit(int color) {
        return TabAnimation.fadeTabColor(color);
    }

    @ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"), argsOnly = true, name = "color")
    private int fadeTabText(int color) {
        return TabAnimation.fadeTabColor(color);
    }
}
