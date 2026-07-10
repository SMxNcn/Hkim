package cn.hkim.addon.features.impl

import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo

@ModuleInfo("swap_options", Category.MISC)
object SwapOptions : Module("Swap Options", "Extra options for swapping.") {
    val ldTitleRegex = Regex("\\((\\d+)/(\\d+)\\) Loadouts")
    val wdTitleRegex = Regex("\\((\\d+)/(\\d+)\\) Armor Sets")

    val noGui by BooleanSetting("No Gui", "Hide GUI while swapping.", false)
    val closeTicks by NumberSetting("Close Ticks", "Ticks to close GUI.", 10f, 5f, 20f, 1f)

    fun shouldHideGui(): Boolean = enabled && noGui
}