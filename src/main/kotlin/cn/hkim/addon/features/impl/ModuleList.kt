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
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoTextWidth
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

@ModuleInfo("module_list", Category.MISC)
object ModuleList : Module("Module List", "Enabled features list.") {
    private val chromaColor by BooleanSetting("Use Chroma Color", "", false)
    private val staticColor by ColorSetting("Static Color", "", Color(5, 186, 115).rgb).depends { !chromaColor }
    private val startColor by ColorSetting("Start Color", "", Color(200, 200, 200).rgb).depends { chromaColor }
    private val endColor by ColorSetting("End Color", "", Color(131, 131, 131).rgb).depends { chromaColor }
    private val chromaSpeed by NumberSetting("Chroma Speed ", "", 5f, 1f, 10f, 1f).depends { chromaColor }
    private val skikoRender by BooleanSetting("Skiko Render", "Render HUD with Skiko.", false)

    private var timeOffset = 0L

    private val moduleNames: List<String>
        get() = ModuleManager.getEnabledToName()

    private val hud by HudElement("Array List", "Render enabled modules.") { graphics ->
        if (this@ModuleList.enabled && !mc.gui.hud.isHidden) {
            this@ModuleList.renderContent(graphics)
        } else {
            Pair(0f, 0f)
        }
    }.onFirstRender { hud ->
        val size = mc.font.lineHeight.toFloat()
        val maxW = moduleNames.maxOfOrNull { if (skikoRender) skikoTextWidth(it, size) else mc.font.width(it).toFloat() } ?: 50f
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

        timeOffset = (timeOffset + 1) % 360

        val size = mc.font.lineHeight.toFloat()
        val widthOf: (String) -> Float = if (skikoRender) { s -> skikoTextWidth(s, size) }
                                         else { s -> mc.font.width(s).toFloat() }
        val sorted = if (isTop) names.sortedByDescending(widthOf)
                     else names.sortedBy(widthOf)

        return if (skikoRender) renderSkiko(graphics, sorted, isLeft)
                else renderVanilla(graphics, sorted, isLeft)
    }

    private fun renderVanilla(graphics: GuiGraphicsExtractor, sorted: List<String>, isLeft: Boolean): Pair<Float, Float> {
        val fontHeight = mc.font.lineHeight
        val spacingVal = 3
        val bgPadding = 2
        val cw = hud.contentWidth.toInt()

        var y = bgPadding

        for ((index, moduleName) in sorted.withIndex()) {
            val moduleWidth = mc.font.width(moduleName)
            val textColor = rowColor(index)

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

        val maxW = sorted.maxOfOrNull { mc.font.width(it) } ?: 50
        return Pair(maxW + 6f, (sorted.size * 12).toFloat())
    }

    private fun renderSkiko(graphics: GuiGraphicsExtractor, sorted: List<String>, isLeft: Boolean): Pair<Float, Float> {
        val size = mc.font.lineHeight.toFloat()
        val spacingVal = 3
        val bgPadding = 2
        val step = mc.font.lineHeight + spacingVal
        val cw = hud.contentWidth.toInt()

        var y = bgPadding.toFloat()

        for ((index, moduleName) in sorted.withIndex()) {
            val moduleWidth = skikoTextWidth(moduleName, size)
            val textColor = rowColor(index)

            val itemWidth = moduleWidth.toInt() + bgPadding * 2 + 2
            val bgTop = (y - bgPadding).toInt()
            val bgBottom = (y - bgPadding + step).toInt()

            if (isLeft) {
                graphics.fill(0, bgTop, itemWidth, bgBottom, Color(53, 53, 53, 115).rgb)
                graphics.fill(0, bgTop, 1, bgBottom, textColor)
                graphics.drawSkikoText(moduleName, bgPadding + 2f, y - 2f, size, textColor)
            } else {
                val bgLeft = cw - itemWidth
                graphics.fill(bgLeft, bgTop, cw, bgBottom, Color(53, 53, 53, 115).rgb)
                graphics.drawSkikoText(moduleName, (cw - moduleWidth - bgPadding - 2), y - 2f, size, textColor)
                graphics.fill(cw - 1, bgTop, cw, bgBottom, textColor)
            }

            y += step
        }

        val maxW = sorted.maxOfOrNull { skikoTextWidth(it, size) } ?: 50f
        val totalH = sorted.size * step + bgPadding
        return Pair(maxW + 6f, totalH.toFloat())
    }

    private fun rowColor(index: Int): Int =
        if (chromaColor) getChromaColor(Color(startColor), Color(endColor), index + 1, chromaSpeed.toInt(), 8).rgb else staticColor
}