package cn.hkim.addon.mixins;

import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DownloadedPackSource.class)
public class DownloadedPackSourceMixin {

    @ModifyArg(method = "loadRequestedPacks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/Pack;<init>(Lnet/minecraft/server/packs/PackLocationInfo;Lnet/minecraft/server/packs/repository/Pack$ResourcesSupplier;Lnet/minecraft/server/packs/repository/Pack$Metadata;Lnet/minecraft/server/packs/PackSelectionConfig;)V"), index = 3)
    private PackSelectionConfig replacePackSelectionConfig(PackSelectionConfig original) {
        return new PackSelectionConfig(true, Pack.Position.BOTTOM, false);
    }
}
