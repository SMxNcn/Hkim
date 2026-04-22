package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.ItemStar;
import cn.hkim.addon.utils.ItemUtilsKt;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
