package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CleanView;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.3: getViewBlockingState 从 ScreenEffectRenderer 移入 LevelExtractor，
 * isViewBlocking 的签名也加了 AABB 参数。在 RETURN 处直接置 null 等效于旧的
 * isViewBlocking -> false 逻辑（无遮挡方块）。
 */
@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {

    @Inject(method = "getViewBlockingState", at = @At("RETURN"), cancellable = true)
    private static void onGetViewBlockingState(CallbackInfoReturnable<BlockState> cir) {
        if (CleanView.shouldSeeThroughBlocks()) cir.setReturnValue(null);
    }
}
