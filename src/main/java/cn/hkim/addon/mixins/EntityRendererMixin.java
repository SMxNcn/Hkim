package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CleanView;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(Entity entity, Frustum culler, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (CleanView.shouldHideLightning() && entity instanceof LightningBolt) {
            cir.setReturnValue(false);
            return;
        }
        if (CleanView.shouldHideExperienceOrbs() && entity instanceof ExperienceOrb) {
            cir.setReturnValue(false);
        }
    }
}
