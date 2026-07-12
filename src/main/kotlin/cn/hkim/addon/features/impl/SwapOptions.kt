package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.skyblock.inventory.SwapHandler
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier

@ModuleInfo("swap_options", Category.MISC)
object SwapOptions : Module("Swap Options", "Extra options for swapping.") {
    val ldTitleRegex = Regex("\\((\\d+)/(\\d+)\\) Loadouts")
    val wdTitleRegex = Regex("\\((\\d+)/(\\d+)\\) Armor Sets")

    val closeTicks by NumberSetting("Close Ticks", "Ticks to close GUI.", 10f, 5f, 20f, 1f)
    val noGui by BooleanSetting("No Gui", "Hide GUI while swapping.", false)
    val showSwapInfo by BooleanSetting("Show Swap Info", "Display current swap info.", true)

    private val swapInfo: Identifier = Identifier.fromNamespaceAndPath("hkim", "swap_info")

    init {
        HudElementRegistry.attachElementBefore(VanillaHudElements.SLEEP, swapInfo, this::render)
    }

    override fun render(graphics: GuiGraphicsExtractor, tickTracker: DeltaTracker) {
        val text = SwapHandler.swapInfo ?: return
        if (!shouldHideGui() || !showSwapInfo || mc.options.hideGui) return
        val width = mc.font.width(text)
        val x = (mc.window.guiScaledWidth - width) / 2
        val y = mc.window.guiScaledHeight / 2 + 12

        graphics.text(mc.font, text, x, y, 0xFFFFFFFF.toInt(), true)
        super.render(graphics, tickTracker)
    }

    fun shouldHideGui(): Boolean = enabled && noGui
}