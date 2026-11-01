package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.ItemFeatures;
import cn.hkim.addon.utils.skyblock.LocationUtils;
import cn.hkim.addon.utils.skyblock.inventory.ItemRarity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilMixin {

    @Inject(method = "extractTooltipBackground", at = @At("HEAD"), cancellable = true)
    private static void onExtractTooltipBackground(GuiGraphicsExtractor graphics, int x, int y, int w, int h, Identifier style, CallbackInfo ci) {
        if (!ItemFeatures.INSTANCE.isRarityTooltipEnabled() || !LocationUtils.INSTANCE.getInSkyBlock()) return;
        if (style == null) return;

        ItemRarity rarity = ItemRarity.fromStylePath(style.getPath());
        if (rarity == null) return;
        ci.cancel();

        int x0 = x - 3 - 9;
        int y0 = y - 3 - 9;
        int paddedWidth = w + 3 + 3 + 18;
        int paddedHeight = h + 3 + 3 + 18;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("hkim", "tooltip/background"), x0, y0, paddedWidth, paddedHeight);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("hkim", "tooltip/" + style.getPath() + "_frame"), x0, y0, paddedWidth, paddedHeight);
    }
}
