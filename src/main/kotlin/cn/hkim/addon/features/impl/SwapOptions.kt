package cn.hkim.addon.features.impl

import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.MouseButtonEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.skyblock.inventory.SwapHandler
import meteordevelopment.orbit.EventHandler
import cn.hkim.addon.compat.input.InputCompat

@ModuleInfo("swap_options", Category.MISC, true)
object SwapOptions : Module("Swap Options", "Extra options for swapping.") {
    val ldTitleRegex = Regex("\\((\\d+)/(\\d+)\\) Loadouts")
    val wdTitleRegex = Regex("\\((\\d+)/(\\d+)\\) Armor Sets")

    val startClickTicks by NumberSetting("Start Click Ticks", "Ticks to start clicking.", 4f, 4f, 10f, 1f)
    val closeTicks by NumberSetting("Close Ticks", "Ticks to close GUI.", 10f, 4f, 10f, 1f)
    val noGui by BooleanSetting("No Gui", "Hide GUI while swapping.", false)
    val showSwapInfo by BooleanSetting("Show Swap Info", "Display current swap info.", true)

    @EventHandler
    private fun onMouseClick(event: MouseButtonEvent) {
        if (noGui && InputCompat.isPrimaryMouseButton(event.button) && SwapHandler.isInSwap) {
            event.cancel()
        }
    }

    override fun toggle() {}
}
