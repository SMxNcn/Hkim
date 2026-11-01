package cn.hkim.addon.features.impl

import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.render.island.IslandHost
import cn.hkim.addon.utils.render.island.IslandQueue
import cn.hkim.addon.utils.render.island.IslandRenderer
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor

@ModuleInfo("dynamic_island", Category.MISC)
object DynamicIsland : Module("Dynamic Island", "Apple-style dynamic island HUD element.") {
    private val radius by NumberSetting("Radius", "Corner radius of the background.", 8f, 0f, 10f, 1f)
    private val backgroundColor by ColorSetting("Background Color", "", 0xB3000000.toInt())
    private val animationDuration by NumberSetting("Animation Duration", "", 150f, 50f, 500f, 10f, "ms")
    private val tabOnIsland by BooleanSetting("Island Tab List", "Morph the tab list.", true)

    override fun render(graphics: GuiGraphicsExtractor, tickTracker: DeltaTracker) {
        if (!enabled) return
        if (!canRenderTabOnIsland && IslandHost.listShowing) return

        IslandRenderer.render(graphics, radius, backgroundColor, animationDurationMs)
        super.render(graphics, tickTracker)
    }

    fun renderStandalone(graphics: GuiGraphicsExtractor) {
        if (enabled || IslandHost.listShowing) return
        IslandRenderer.render(graphics, radius, backgroundColor, animationDurationMs)
    }

    val animationDurationMs: Long get() = animationDuration.toLong()

    val canRenderTabOnIsland: Boolean get() = enabled && tabOnIsland

    override fun onDisable() = IslandQueue.clear()
}
