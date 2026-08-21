package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.CleanView;
import cn.hkim.addon.features.impl.Nametags;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        if (Nametags.shouldRemoveGlowing() && Nametags.isDungeonTeammate(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "displayFireAnimation", at = @At("HEAD"), cancellable = true)
    private void onDisplayFireAnimation(CallbackInfoReturnable<Boolean> cir) {
        if (!CleanView.shouldHideEntityFire()) return;
        Entity self = (Entity) (Object) this;
        if (!(self instanceof LocalPlayer)) {
            cir.setReturnValue(false);
        }
    }
}
