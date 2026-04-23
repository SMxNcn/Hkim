package cn.hkim.addon.features.impl

import cn.hkim.addon.config.settings.ActionSetting
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo

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
    @JvmStatic val disableReSwing by BooleanSetting("Disable Re-Swing", "Prevents the swing animation from playing again if you try to swing while already swinging.", false)

    private val noEquipReset by BooleanSetting("No Equip Reset", "Disables the equipping animation when switching items.", false)
    private val noSwing by BooleanSetting("No Swing", "Prevents your item from visually swinging forward.", false)
    private val reset by ActionSetting("Reset positions", "Resets all animations.") { settings.filterIsInstance<NumberSetting>().forEach { it.reset() } }

    fun shouldNoEquipReset() = enabled && noEquipReset

    fun shouldStopSwing() = enabled && noSwing

    fun shouldNotSwing() = enabled && disableReSwing
}