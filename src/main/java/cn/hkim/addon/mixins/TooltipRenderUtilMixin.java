package cn.hkim.addon.mixins;

import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilMixin {

    @Redirect(method = "extractTooltipBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;getBackgroundSprite(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/resources/Identifier;"))
    private static Identifier redirectGetBackgroundSprite(Identifier style) {
        return Identifier.withDefaultNamespace("tooltip/background");
    }
}
