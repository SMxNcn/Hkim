package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.Animations;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.3: 原 ItemInHandRenderer 的逻辑部分（高度插值/换物品判定）移入
 * net.minecraft.client.player.FirstPersonHandsAndItems。
 */
@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {

    @Shadow
    private float mainHandHeight;

    @Shadow
    private float offHandHeight;

    @Shadow
    private float oMainHandHeight;

    @Shadow
    private float oOffHandHeight;

    @Inject(method = "tick", at = @At("TAIL"))
    private void maintainCustomEquipHeights(CallbackInfo ci) {
        if (Animations.INSTANCE.shouldNoEquipReset()) {
            this.oMainHandHeight = 1.0f;
            this.mainHandHeight = 1.0f;
            this.oOffHandHeight = 1.0f;
            this.offHandHeight = 1.0f;
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    private float overrideAttackStrengthScale(float originalValue) {
        if (Animations.INSTANCE.shouldNoEquipReset() || Animations.INSTANCE.shouldStopSwing()) return 1f;
        return originalValue;
    }

    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void forceInstantItemSwap(ItemStack currentlyVisibleItem, ItemStack expectedItem, LocalPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (Animations.INSTANCE.shouldNoEquipReset()) cir.setReturnValue(true);
    }
}
