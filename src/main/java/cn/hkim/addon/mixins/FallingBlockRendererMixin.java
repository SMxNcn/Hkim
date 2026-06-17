package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CleanView;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockRenderer.class)
public class FallingBlockRendererMixin {

    @Inject(method = "shouldRender(Lnet/minecraft/world/entity/item/FallingBlockEntity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(FallingBlockEntity entity, Frustum culler, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (CleanView.shouldHideFallingBlocks()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
