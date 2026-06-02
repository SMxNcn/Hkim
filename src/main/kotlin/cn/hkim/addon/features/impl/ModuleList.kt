package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.hud.HudElement
import cn.hkim.addon.utils.HudUtils.getChromaColor
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

@ModuleInfo("module_list", Category.MISC, false)
object ModuleList : Module("Module List", "Enabled features list.") {
    private val chromaColor by BooleanSetting("Use Chroma Color", "", false)
    private val staticColor by ColorSetting("Static Color", "", Color(5, 186, 115).rgb).depends { !chromaColor }
    private val startColor by ColorSetting("Start Color", "", Color(200, 200, 200).rgb).depends { chromaColor }
    private val endColor by ColorSetting("End Color", "", Color(131, 131, 131).rgb).depends { chromaColor }
    private val chromaSpeed by NumberSetting("Chroma Speed ", "", 5, 1, 10, 1).depends { chromaColor }

    private var timeOffset = 0L

    private val moduleNames = listOf("AutoClicker", "AutoGG", "AutoLeap", "AutoWardrobe", "ChatCommands", "CropNuker", "Etherwarp", "EtherwarpRouter", "HurtCamera", "Nametags", "AutoI4")

    private var positionInitialized = false

    private val hud by HudElement("Array List", "Render enabled modules.") { graphics, tick ->
        if (this@ModuleList.enabled && !mc.options.hideGui) {
            this@ModuleList.renderContent(graphics, tick)
        } else {
            Pair(0f, 0f)
        }
    }

    override fun render(graphics: GuiGraphicsExtractor, tickTracker: DeltaTracker) {
        if (!positionInitialized) {
            if (!hud.loadedFromConfig) {
                val maxW = moduleNames.maxOf { mc.font.width(it) }
                hud.anchorX = mc.window.guiScaledWidth - (maxW + 6f) - 2f
                hud.anchorY = 4f
            }
            positionInitialized = true
        }
        super.render(graphics, tickTracker)
    }

    private fun renderContent(graphics: GuiGraphicsExtractor, tickTracker: DeltaTracker): Pair<Float, Float> {
        if (moduleNames.isEmpty()) return Pair(0f, 0f)

        val bounds = hud.getScreenBounds()
        val hudCx = bounds.x + bounds.w / 2f
        val hudCy = bounds.y + bounds.h / 2f
        val isLeft = hudCx < mc.window.guiScaledWidth / 2f
        val isTop  = hudCy < mc.window.guiScaledHeight / 2f

        val sorted = if (isTop) moduleNames.sortedByDescending { mc.font.width(it) }
                     else moduleNames.sortedBy { mc.font.width(it) }

        timeOffset = (timeOffset + 1) % 360

        val fontHeight = mc.font.lineHeight
        val spacingVal = 3
        val bgPadding = 2
        val cw = hud.contentWidth.toInt()

        var y = bgPadding

        for ((index, moduleName) in sorted.withIndex()) {
            val moduleWidth = mc.font.width(moduleName)
            val textColor = if (chromaColor) getChromaColor(Color(startColor), Color(endColor), index + 1, chromaSpeed.toInt(), index / 2).rgb else staticColor

            val itemWidth = moduleWidth + bgPadding * 2 + 2

            if (isLeft) {
                graphics.fill(0, y - bgPadding, itemWidth, y + fontHeight + 1, Color(53, 53, 53, 115).rgb)
                graphics.fill(0, y - bgPadding, 1, y + fontHeight + 1, textColor)
                graphics.text(mc.font, moduleName, bgPadding + 2, y, textColor, true)
            } else {
                val bgLeft = cw - itemWidth
                graphics.fill(bgLeft, y - bgPadding, cw, y + fontHeight + 1, Color(53, 53, 53, 115).rgb)
                graphics.text(mc.font, moduleName, cw - moduleWidth - bgPadding - 2, y, textColor, true)
                graphics.fill(cw - 1, y - bgPadding, cw, y + fontHeight + 1, textColor)
            }

            y += fontHeight + spacingVal
        }

        val maxW = moduleNames.maxOf { mc.font.width(it) }
        return Pair(maxW + 6f, (moduleNames.size * 12).toFloat())
    }
}