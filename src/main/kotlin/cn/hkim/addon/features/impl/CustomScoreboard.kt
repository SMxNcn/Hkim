package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.ModuleConfig
import cn.hkim.addon.config.settings.*
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.hud.HudAlignment
import cn.hkim.addon.hud.HudElement
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.HudUtils.gradientText
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import java.awt.Color

@ModuleInfo("custom_scoreboard", Category.MISC)
object CustomScoreboard : Module("Custom Scoreboard", "Scoreboard background & last-line replacement.") {
    private const val EXAMPLE_TITLE = "§e§lSKYBLOCK"
    private val EXAMPLE_LINES = listOf(
        "§6  5,234,567",
        "§d  ◆ 1,234",
        "§a  ✔ 42",
        "§b  ✦ 128",
        "§e  Coins 100K",
        "§7  §m━━━━━━━━━",
        "§d§l✿ BMW ✿"
    )

    private val showBackground by BooleanSetting("Background", "", true)
    private val bgColor by ColorSetting("Color", "", 0x4C000000).depends { showBackground }
    private val padding by NumberSetting("Padding", "Space between content and background edge.", 2f, 0f, 6f, 0.5f).depends { showBackground }
    private val replaceIpLine by BooleanSetting("Replace Last Line", "Replace the last row (e.g. server IP) with custom text.", true)
    private val customServerIp by TextSetting("Custom IP Line", "Use & instead of § for colour codes.", "").depends { replaceIpLine }
    private val centerIpLine by BooleanSetting("Center IP Line", "Center the custom IP line horizontally.", false).depends { replaceIpLine }
    private val lastLineGradient by BooleanSetting("IP Gradient", "Render the custom IP line with a colour gradient.", false).depends { replaceIpLine }
    private val gradStart by ColorSetting("Gradient Start", "", Color(255, 85, 255).rgb).depends { replaceIpLine && lastLineGradient }
    private val gradEnd by ColorSetting("Gradient End", "", Color(85, 255, 255).rgb).depends { replaceIpLine && lastLineGradient }
    private val gradSpeed by NumberSetting("Gradient Speed", "", 5f, 1f, 10f, 1f).depends { replaceIpLine && lastLineGradient }

    private val resetPosition by ActionSetting("Reset Position", "Reset scoreboard position to default (middle-right).") {
        hud.hudAlignment = HudAlignment.MIDDLE_RIGHT
        hud.anchorX = -100f
        hud.anchorY = -25f
        hud.hudScale = 1f
        prevContentWidth = 0f
        initialPositionSet = false
        ModuleConfig.saveConfig()
    }

    private var prevContentWidth = 0f
    private var initialPositionSet = false

    private val hud by HudElement("Scoreboard", "Replaces the vanilla scoreboard.",
        alignment = HudAlignment.TOP_LEFT
    ) { graphics, _ ->
        if (!this@CustomScoreboard.enabled) return@HudElement Pair(0f, 0f)
        val size = renderScoreboard(graphics)
        afterRender(size)
        size
    }.onFirstRender { hud ->
        hud.anchorX = 50f
        hud.anchorY = 50f
    }

    private fun renderScoreboard(graphics: GuiGraphicsExtractor): Pair<Float, Float> {
        val data = resolveLines() ?: return Pair(0f, 0f)
        return renderBoard(graphics, data)
    }

    private fun afterRender(size: Pair<Float, Float>) {
        val (newW, _) = size
        if (newW <= 0f) return

        val scale = hud.hudScale
        val screenW = mc.window.guiScaledWidth

        if (!initialPositionSet) {
            initialPositionSet = true
            prevContentWidth = newW

            val currentLeft = hud.actualX()
            val currentRight = currentLeft + newW * scale

            if (currentRight > screenW - 2f) {
                val newLeft = (screenW - newW * scale - 4f).coerceAtLeast(4f)
                hud.anchorX = newLeft - hud.hudAlignment.baseX(screenW)
            } else if (currentLeft < 2f) {
                val maxLeft = (screenW - newW * scale - 4f).coerceAtLeast(4f)
                val newLeft = 4f.coerceAtMost(maxLeft)
                hud.anchorX = newLeft - hud.hudAlignment.baseX(screenW)
            }
            return
        }

        if (newW == prevContentWidth) return

        val oldLeft = hud.actualX()
        val oldRight = oldLeft + prevContentWidth * scale
        val oldCenterX = (oldLeft + oldRight) / 2f

        val newLeft = if (oldCenterX < screenW / 2f) {
            oldLeft
        } else {
            oldRight - newW * scale
        }

        val maxLeft = (screenW - newW * scale - 4f).coerceAtLeast(4f)
        val clampedLeft = newLeft.coerceIn(4f, maxLeft)

        hud.anchorX = clampedLeft - hud.hudAlignment.baseX(screenW)
        prevContentWidth = newW
    }

    private fun resolveLines(): HudUtils.ScoreboardData? {
        val data = HudUtils.getScoreboardLines(formatted = true)
        if (data != null && data.lines.isNotEmpty()) {
            val lines = data.lines.mapIndexed { i, text ->
                if (i == data.lines.size - 1 && replaceIpLine) customServerIp.replace("&", "§") else text
            }
            return HudUtils.ScoreboardData(data.title, lines)
        }
        if (mc.level == null) {
            val lines = EXAMPLE_LINES.mapIndexed { i, text ->
                if (i == EXAMPLE_LINES.size - 1 && replaceIpLine) customServerIp.replace("&", "§") else text
            }
            return HudUtils.ScoreboardData(EXAMPLE_TITLE, lines)
        }
        return null
    }

    private fun renderBoard(graphics: GuiGraphicsExtractor, data: HudUtils.ScoreboardData): Pair<Float, Float> {
        val font = mc.font
        val fontHeight = font.lineHeight
        val pad = padding.toInt()

        val lines = data.lines
        val lastIdx = lines.size - 1
        val lastLineRaw = if (lastIdx >= 0) lines[lastIdx] else null
        val useGradient = lastLineGradient && lastLineRaw != null

        val cleanLines = if (useGradient) {
            lines.mapIndexed { i, s -> if (i == lastIdx) s.replace(Regex("§."), "") else s }
        } else lines

        val titleComp = Component.literal(data.title)
        val linesComp = cleanLines.map { Component.literal(it) }

        val titleWidth = font.width(titleComp)
        val lineWidths = linesComp.map { font.width(it) }
        var maxW = titleWidth
        for (w in lineWidths) {
            if (w > maxW) maxW = w
        }

        val textLeft = pad + 1
        val totalWidth = maxW + pad * 2 + 3
        val titleH = fontHeight + 2
        val entriesH = linesComp.size * fontHeight + pad * 2
        val totalHeight = titleH + entriesH

        if (showBackground) {
            graphics.fill(0, 0, totalWidth, totalHeight, bgColor)
        }

        graphics.text(font, titleComp, textLeft + (maxW - titleWidth) / 2, pad, 0xFFFFFFFF.toInt(), true)

        val startY = pad + titleH
        for ((i, line) in linesComp.withIndex()) {
            val y = startY + i * fontHeight
            val x = if (centerIpLine && i == lastIdx) textLeft + (maxW - lineWidths[i]) / 2 else textLeft
            if (useGradient && i == lastIdx) {
                graphics.gradientText(font, cleanLines[i], x, y,
                    Color(gradStart and 0xFFFFFF, false).rgb,
                    Color(gradEnd and 0xFFFFFF, false).rgb,
                    gradSpeed.toInt(), shadow = true)
            } else {
                graphics.text(font, line, x, y, 0xFFFFFFFF.toInt(), true)
            }
        }

        return Pair(totalWidth.toFloat(), totalHeight.toFloat())
    }
}
