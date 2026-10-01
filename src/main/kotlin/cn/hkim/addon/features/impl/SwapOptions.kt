package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.MouseButtonEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.render.island.IslandQueue
import cn.hkim.addon.utils.render.island.IslandRenderer
import cn.hkim.addon.utils.skyblock.inventory.SwapHandler
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor

@ModuleInfo("swap_options", Category.MISC, true)
object SwapOptions : Module("Swap Options", "Extra options for swapping.") {
    val ldTitleRegex = Regex("\\((\\d+)/(\\d+)\\) Loadouts")
    val wdTitleRegex = Regex("\\((\\d+)/(\\d+)\\) Armor Sets")

    val closeTicks by NumberSetting("Close Ticks", "Ticks to close GUI.", 10f, 5f, 20f, 1f)
    val noGui by BooleanSetting("No Gui", "Hide GUI while swapping.", false)
    val showSwapInfo by BooleanSetting("Show Swap Info", "Display current swap info.", true)

    @EventHandler
    private fun onMouseClick(event: MouseButtonEvent) {
        if (noGui && event.button in 0..2 && SwapHandler.isInSwap) {
            event.cancel()
        }
    }

    override fun render(graphics: GuiGraphicsExtractor, tickTracker: DeltaTracker) {
        super.render(graphics, tickTracker)
        val info = SwapHandler.swapInfo ?: return
        if (!noGui || !showSwapInfo || mc.options.hideGui) return
        IslandRenderer.showMessage(
            info.title,
            info.detail,
            centered = true,
            priority = IslandQueue.PRIORITY_SWAP_INFO,
            key = "swap-info",
        )
    }

    override fun toggle() {}
}