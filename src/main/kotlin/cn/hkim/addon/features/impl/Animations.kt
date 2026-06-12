package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.ActionSetting
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.isSword
import cn.hkim.addon.utils.toRadians
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.item.ItemStack
import org.joml.Quaternionf
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

@ModuleInfo("animations", Category.RENDER, false)
object Animations : Module("Animations", "Changes the appearance of the first-person view model.") {
    @JvmStatic val size by NumberSetting("Size", "Scales the held item. Default: 1", 1f, 0.1f, 3.0f, 0.05f)
    @JvmStatic val x by NumberSetting("X", "Moves the held item. Default: 0", 0f, -2.5f, 1.5f, 0.05f)
    @JvmStatic val y by NumberSetting("Y", "Moves the held item. Default: 0", 0f, -1.5f, 1.5f, 0.05f)
    @JvmStatic val z by NumberSetting("Z", "Moves the held item. Default: 0", 0f, -1.5f, 3.0f, 0.05f)
    @JvmStatic val yaw by NumberSetting("Yaw", "Rotates your held item. Default: 0", 0f, -180f, 180f, 1f)
    @JvmStatic val pitch by NumberSetting("Pitch", "Rotates your held item. Default: 0", 0f, -180f, 180f, 1f)
    @JvmStatic val roll by NumberSetting("Roll", "Rotates your held item. Default: 0", 0f, -180f, 180f, 1f)
    @JvmStatic val ignoreHaste by BooleanSetting("Ignore Effects", "Makes the chosen speed override haste modifiers.", false)
    @JvmStatic val speed by NumberSetting("Speed", "Speed of the swing animation.", 6, 0, 32, 1).depends { ignoreHaste }
    @JvmStatic val oldAnimation by BooleanSetting("Old Animation", "1.7 block animation.", false)
    @JvmStatic val disableReSwing by BooleanSetting("Disable Re-Swing", "Prevents the swing animation from playing again if you try to swing while already swinging.", false)

    private val noEquipReset by BooleanSetting("No Equip Reset", "Disables the equipping animation when switching items.", false)
    private val noSwing by BooleanSetting("No Swing", "Prevents your item from visually swinging forward.", false)
    private val reset by ActionSetting("Reset positions", "Resets all animations.") {
        settings.filterIsInstance<NumberSetting>()
            .filter { it.configKey in listOf("x", "y", "z", "yaw", "pitch", "roll") }
            .forEach { it.reset() }
    }

    fun shouldNoEquipReset() = enabled && noEquipReset

    fun shouldStopSwing() = enabled && noSwing

    fun shouldNotSwing() = enabled && disableReSwing

    fun shouldApplyOldAnimation(itemStack: ItemStack): Boolean {
        return enabled && oldAnimation && mc.options.keyUse.isDown && itemStack.isSword
    }

    fun animationVanilla(poseStack: PoseStack, equipProgress: Float, swingProgress: Float) {
        poseStack.translate(0.56f, -0.52f + equipProgress * -0.6f, -0.72f)
        val f1 = sin(swingProgress * swingProgress * PI).toFloat()
        val f2 = sin(sqrt(swingProgress) * PI).toFloat()
        poseStack.mulPose(Quaternionf().rotateY(toRadians(45.0f + f1 * -20f)))
        poseStack.mulPose(Quaternionf().rotateZ(toRadians(f2 * -20f)))
        poseStack.mulPose(Quaternionf().rotateX(toRadians(f2 * -80f)))
        poseStack.mulPose(Quaternionf().rotateY(toRadians(-45.0f)))
        poseStack.translate(-0.2f, 0.126f, 0.2f)
        poseStack.mulPose(Quaternionf().rotateXYZ(toRadians(-102.25f), toRadians(15.0f), toRadians(80.0f)))
    }
}