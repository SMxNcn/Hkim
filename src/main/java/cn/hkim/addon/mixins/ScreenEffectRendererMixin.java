package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CleanView;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {

    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void onSubmitFire(CallbackInfo ci) {
        if (CleanView.shouldHideFireOverlay()) ci.cancel();
    }

    @Redirect(method = "getViewBlockingState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isViewBlocking(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean onIsViewBlocking(BlockState instance, BlockGetter blockGetter, BlockPos blockPos) {
        if (CleanView.shouldSeeThroughBlocks()) return false;
        return instance.isViewBlocking(blockGetter, blockPos);
    }
}
