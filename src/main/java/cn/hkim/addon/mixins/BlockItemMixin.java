package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.AntiPlace;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void onPlace(BlockPlaceContext placeContext, CallbackInfoReturnable<InteractionResult> cir) {
        if (AntiPlace.shouldCancelPlacement(placeContext.getItemInHand())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
