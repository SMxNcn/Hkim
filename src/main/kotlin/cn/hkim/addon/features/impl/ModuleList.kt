package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.features.ModuleManager
import cn.hkim.addon.hud.HudElement
import cn.hkim.addon.utils.HudUtils.getChromaColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

@ModuleInfo("module_list", Category.MISC, false)
object ModuleList : Module("Module List", "Enabled features list.") {
    private val chromaColor by BooleanSetting("Use Chroma Color", "", false)
    private val staticColor by ColorSetting("Static Color", "", Color(5, 186, 115).rgb).depends { !chromaColor }
    private val startColor by ColorSetting("Start Color", "", Color(200, 200, 200).rgb).depends { chromaColor }
    private val endColor by ColorSetting("End Color", "", Color(131, 131, 131).rgb).depends { chromaColor }
    private val chromaSpeed by NumberSetting("Chroma Speed ", "", 5f, 1f, 10f, 1f).depends { chromaColor }

    private var timeOffset = 0L

    private val moduleNames: List<String>
        get() = ModuleManager.getEnabledToName()

    private val hud by HudElement("Array List", "Render enabled modules.") { graphics, _ ->
        if (this@ModuleList.enabled && !mc.gui.hud.isHidden()) {
            this@ModuleList.renderContent(graphics)
        } else {
            Pair(0f, 0f)
        }
    }.onFirstRender { hud ->
        val maxW = moduleNames.maxOfOrNull { mc.font.width(it) } ?: 50
        hud.anchorX = mc.window.guiScaledWidth - (maxW + 6f) - 2f
        hud.anchorY = 4f
    }

    private fun renderContent(graphics: GuiGraphicsExtractor): Pair<Float, Float> {
        val names = moduleNames
        if (names.isEmpty()) return Pair(0f, 0f)

        val bounds = hud.getScreenBounds()
        val hudCx = bounds.x + bounds.w / 2f
        val hudCy = bounds.y + bounds.h / 2f
        val isLeft = hudCx < mc.window.guiScaledWidth / 2f
        val isTop  = hudCy < mc.window.guiScaledHeight / 2f

        val sorted = if (isTop) names.sortedByDescending { mc.font.width(it) }
                     else names.sortedBy { mc.font.width(it) }

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

        val maxW = names.maxOfOrNull { mc.font.width(it) } ?: 50
        return Pair(maxW + 6f, (names.size * 12).toFloat())
    }
}