package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.ItemStar;
import cn.hkim.addon.utils.ItemUtilsKt;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.item.ItemStack;

import static cn.hkim.addon.utils.ItemUtilsKt.isSkyBlockItem;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {

    @Unique
    ItemStack itemStack;

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemCount(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private void getStack(Font font, ItemStack itemStack, int x, int y, String countText, CallbackInfo ci) {
        this.itemStack = itemStack.copy();
    }

    @ModifyArg(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemCount(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private String modifyCountText(String string) {
        if (itemStack == null || itemStack.isEmpty() || !isSkyBlockItem(itemStack)) return string;
        int upgradeLevel = ItemUtilsKt.getItemUpgradeLevel(itemStack);
        if (upgradeLevel >= 1 && ItemStar.INSTANCE.getEnabled()) return String.valueOf(upgradeLevel);
        return string;
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
