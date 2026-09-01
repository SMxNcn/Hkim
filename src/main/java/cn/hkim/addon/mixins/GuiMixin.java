package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CustomScoreboard;
import cn.hkim.addon.features.impl.ItemFeatures;
import cn.hkim.addon.features.impl.ModuleList;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    public void onExtractEffects(CallbackInfo ci) {
        if (ModuleList.INSTANCE.getEnabled()) ci.cancel();
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    public void onExtractScoreboardSidebar(CallbackInfo ci) {
        if (CustomScoreboard.INSTANCE.getEnabled()) ci.cancel();
    }

    @Inject(method = "extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V", at = @At("HEAD"))
    private void onExtractSlotRarityBackground(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker delta, Player player, ItemStack item, int slot, CallbackInfo ci) {
        ItemFeatures.drawRarityBackground(graphics, x, y, item);
    }
}
