package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.Animations;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3: ItemInHandRenderer 拆分为 FirstPersonHandsAndItemsRenderer（渲染）
 * + FirstPersonHandsAndItems（逻辑，见 FirstPersonHandsAndItemsMixin）。
 * isUsingItem/isAutoSpinAttack 从方法调用改为 AvatarRenderState 字段读取。
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {

    @Shadow
    private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float attackValue) {
    }

    @WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0))
    private void applyCustomHandAnimation(PoseStack instance, Operation<Void> original,
                                          @Local(argsOnly = true, name = "hand") InteractionHand hand,
                                          @Local(argsOnly = true, name = "itemStack") ItemStack itemStack) {
        original.call(instance);

        if (!Animations.INSTANCE.getEnabled() || itemStack.isEmpty() || itemStack.has(DataComponents.MAP_ID)) return;

        float xOffset = Animations.getX();
        float yOffset = Animations.getY();
        float zOffset = Animations.getZ();

        instance.translate(hand == InteractionHand.MAIN_HAND ? xOffset : -xOffset, yOffset, zOffset);
        instance.mulPose(Axis.XP.rotationDegrees(Animations.getPitch()));
        instance.mulPose(Axis.YP.rotationDegrees(Animations.getYaw()));
        instance.mulPose(Axis.ZP.rotationDegrees(Animations.getRoll()));

        // 26.3: renderItem 调用被重构移除，物品缩放并入此处（整体缩放，含手臂）
        float size = Animations.getSize();
        instance.scale(size, size, size);
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void handleCustomSwingAnimation(float attack, PoseStack poseStack, int invert, HumanoidArm arm, CallbackInfo ci) {
        if (!Animations.INSTANCE.shouldStopSwing()) return;
        ci.cancel();

        this.applyItemArmAttackTransform(poseStack, arm, attack);
    }

    @ModifyExpressionValue(method = "submitArmWithItem", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;isUsingItem:Z", ordinal = 1))
    private boolean insertOldSwordAnimationIf(boolean original,
                                              @Local(argsOnly = true, name = "hand") InteractionHand hand,
                                              @Local(argsOnly = true, name = "itemStack") ItemStack itemStack,
                                              @Local(argsOnly = true, name = "poseStack") PoseStack poseStack,
                                              @Local(argsOnly = true, name = "attack") float attack,
                                              @Local(argsOnly = true, name = "inverseArmHeight") float inverseArmHeight,
                                              @Local(name = "avatarRenderState") AvatarRenderState avatarRenderState) {
        if (Animations.INSTANCE.shouldApplyOldAnimation(itemStack)) {
            HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? avatarRenderState.mainArm : avatarRenderState.mainArm.getOpposite();
            Animations.INSTANCE.animationVanilla(poseStack, arm, inverseArmHeight, attack);
            return false;
        }
        return original;
    }

    @ModifyExpressionValue(method = "submitArmWithItem", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;isAutoSpinAttack:Z"))
    private boolean skipAutoSpinForOldSword(boolean original,
                                            @Local(argsOnly = true, name = "itemStack") ItemStack itemStack) {
        if (Animations.INSTANCE.shouldApplyOldAnimation(itemStack)) return false;
        return original;
    }

    @WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"))
    private void skipItemArmTransformForOldSword(FirstPersonHandsAndItemsRenderer instance, PoseStack poseStack, HumanoidArm arm, float inverseArmHeight, Operation<Void> original,
                                                 @Local(argsOnly = true, name = "itemStack") ItemStack itemStack) {
        if (Animations.INSTANCE.shouldApplyOldAnimation(itemStack)) return;
        original.call(instance, poseStack, arm, inverseArmHeight);
    }

    @WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"))
    private void skipSwingForOldSword(FirstPersonHandsAndItemsRenderer instance, float attack, PoseStack poseStack, int invert, HumanoidArm arm, Operation<Void> original,
                                      @Local(argsOnly = true, name = "itemStack") ItemStack itemStack) {
        if (Animations.INSTANCE.shouldApplyOldAnimation(itemStack)) return;
        original.call(instance, attack, poseStack, invert, arm);
    }
}
