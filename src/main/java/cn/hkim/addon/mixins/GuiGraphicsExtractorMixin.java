package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.ItemFeatures;
import cn.hkim.addon.utils.ItemUtilsKt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    @Unique
    private static final Logger SCISSOR_LOG = LoggerFactory.getLogger("HkimScissorGuard");

    @Inject(method = "enableScissor(IIII)V", at = @At("HEAD"), cancellable = true)
    private void guardZeroAreaScissor(int x, int y, int x2, int y2, CallbackInfo ci) {
        int w = x2 - x;
        int h = y2 - y;
        if (w <= 0 || h <= 0) {
            SCISSOR_LOG.warn("Skipping zero-area scissor push ({}x{}) at ({},{}..{},{})", w, h, x, y, x2, y2);
            ci.cancel();
        }
    }
}
