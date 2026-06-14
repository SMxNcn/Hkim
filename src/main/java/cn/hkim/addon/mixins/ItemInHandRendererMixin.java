package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.Animations;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow
    private float mainHandHeight;

    @Shadow
    private float offHandHeight;

    @Shadow
    private float oMainHandHeight;

    @Shadow
    private float oOffHandHeight;

    @Shadow
    protected abstract void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float attackValue);

    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0))
    private void applyCustomHandAnimation(PoseStack instance, Operation<Void> original, @Local(argsOnly = true, name = "hand") InteractionHand hand, @Local(argsOnly = true, name = "itemStack") ItemStack itemStack) {
        original.call(instance);

        if (!Animations.INSTANCE.getEnabled() || itemStack.isEmpty() || itemStack.has(DataComponents.MAP_ID)) return;

        float xOffset = Animations.getX();
        float yOffset = Animations.getY();
        float zOffset = Animations.getZ();

        instance.translate(hand == InteractionHand.MAIN_HAND ? xOffset : -xOffset, yOffset, zOffset);
        instance.mulPose(Axis.XP.rotationDegrees(Animations.getPitch()));
        instance.mulPose(Axis.YP.rotationDegrees(Animations.getYaw()));
        instance.mulPose(Axis.ZP.rotationDegrees(Animations.getRoll()));
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void handleCustomSwingAnimation(float attack, PoseStack poseStack, int invert, HumanoidArm arm, CallbackInfo ci) {
        if (!Animations.INSTANCE.shouldStopSwing()) return;
        ci.cancel();

        this.applyItemArmAttackTransform(poseStack, arm, attack);
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void applySizeTransform(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (!Animations.INSTANCE.getEnabled() || itemStack.isEmpty() || itemStack.has(DataComponents.MAP_ID)) return;
        poseStack.scale(Animations.getSize(), Animations.getSize(), Animations.getSize());
    }

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
    private void forceInstantItemSwap(ItemStack currentlyVisibleItem, ItemStack expectedItem, CallbackInfoReturnable<Boolean> cir) {
        if (Animations.INSTANCE.shouldNoEquipReset()) cir.setReturnValue(true);
    }

    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z", ordinal = 1))
    private boolean insertOldSwordAnimationIf(AbstractClientPlayer player, Operation<Boolean> original, @Local(argsOnly = true, name = "itemStack") ItemStack itemStack, @Local(argsOnly = true, name = "poseStack") PoseStack poseStack, @Local(argsOnly = true, name = "attack") float attack, @Local(argsOnly = true, name = "inverseArmHeight") float inverseArmHeight) {
        if (Animations.INSTANCE.shouldApplyOldAnimation(itemStack)) {
            Animations.INSTANCE.animationVanilla(poseStack, inverseArmHeight, attack);
            return false;
        }
        return original.call(player);
    }

    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isAutoSpinAttack()Z"))
    private boolean skipAutoSpinForOldSword(AbstractClientPlayer player, Operation<Boolean> original, @Local(argsOnly = true, name = "itemStack") ItemStack itemStack) {
        if (Animations.INSTANCE.shouldApplyOldAnimation(itemStack)) return false;
        return original.call(player);
    }

    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"))
    private void skipItemArmTransformForOldSword(ItemInHandRenderer instance, PoseStack poseStack, HumanoidArm arm, float inverseArmHeight, Operation<Void> original, @Local(argsOnly = true, name = "itemStack") ItemStack itemStack) {
        if (Animations.INSTANCE.shouldApplyOldAnimation(itemStack)) return;
        original.call(instance, poseStack, arm, inverseArmHeight);
    }

    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"))
    private void skipSwingForOldSword(ItemInHandRenderer instance, float attack, PoseStack poseStack, int invert, HumanoidArm arm, Operation<Void> original, @Local(argsOnly = true, name = "itemStack") ItemStack itemStack) {
        if (Animations.INSTANCE.shouldApplyOldAnimation(itemStack)) return;
        original.call(instance, attack, poseStack, invert, arm);
    }
}
