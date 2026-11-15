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
import cn.hkim.addon.utils.ColorUtils.gradientText
import cn.hkim.addon.utils.ParsedColoredText
import cn.hkim.addon.utils.parseColoredText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRect
import cn.hkim.addon.utils.toLegacyOrComponent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

@ModuleInfo("custom_scoreboard", Category.MISC)
object CustomScoreboard : Module("Custom Scoreboard", "Scoreboard features.") {
    private val background by DropdownSetting("Background")
    private val showBackground by BooleanSetting("Enabled", "", true).depends { background }
    private val bgColor by ColorSetting("Color", "", 0x4C000000).depends { background && showBackground }
    private val padding by NumberSetting("Padding", "", 2f, 0f, 5f, 1f).depends { background && showBackground }
    private val backgroundRadius by NumberSetting("Radius", "", 0f, 0f, 5f, 1f).depends { background && showBackground }
    private val replaceIpLine by BooleanSetting("Replace Last Line", "Replace the last row.", true)
    private val customServerIp by TextSetting("Custom IP Line", "Use '&' for color codes. Supports &#RRGGBB hex colors.", "").depends { replaceIpLine }
    private val centerIpLine by BooleanSetting("Center IP Line", "Center the custom last line horizontally.", false).depends { hasCustomIpText() }
    private val lastLineGradient by BooleanSetting("IP Gradient", "", false).depends { hasCustomIpText() }
    private val gradient by DropdownSetting("Gradient").depends { hasCustomIpText() && lastLineGradient }
    private val gradStart by ColorSetting("Start Color", "", 0xFF55FF).depends { hasCustomIpText() && lastLineGradient && gradient }
    private val gradEnd by ColorSetting("End Color", "", 0x55FFFF).depends { hasCustomIpText() && lastLineGradient && gradient }
    private val gradSpeed by NumberSetting("Speed", "", 5f, 1f, 10f, 1f).depends { hasCustomIpText() && lastLineGradient && gradient }

    private val resetPosition by ActionSetting("Reset Position", "Reset scoreboard position to default.") {
        hud.hudAlignment = HudAlignment.MIDDLE_RIGHT
        hud.anchorX = -100f
        hud.anchorY = -25f
        hud.hudScale = 1f
        prevContentWidth = 0f
        initialPositionSet = false
        ModuleConfig.saveConfig()
    }

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

    private var prevContentWidth = 0f
    private var initialPositionSet = false

    private var cachedCustomSource: String? = null
    private var cachedCustomLine: ParsedColoredText? = null

    private data class BoardContent(
        val title: Component,
        val lines: List<Component>,
        val customLine: ParsedColoredText?
    )

    private fun customLine(): ParsedColoredText {
        val source = customServerIp
        val cached = cachedCustomLine
        if (cached != null && source == cachedCustomSource) return cached
        return parseColoredText(source).also {
            cachedCustomSource = source
            cachedCustomLine = it
        }
    }

    private fun hasCustomIpText(): Boolean = replaceIpLine && customLine().plain.isNotEmpty()

    private val hud by HudElement("Scoreboard", "Replaces the vanilla scoreboard.",
        alignment = HudAlignment.TOP_LEFT
    ) { graphics ->
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

    private fun resolveLines(): BoardContent? {
        val custom = if (replaceIpLine) customLine() else null

        val content = HudUtils.getScoreboardContent()
        if (content != null && content.lines.isNotEmpty()) {
            return withCustomLine(
                content.title.toLegacyOrComponent(),
                content.lines.map { it.toLegacyOrComponent() },
                custom
            )
        }
        if (mc.level == null) {
            return withCustomLine(
                Component.literal(EXAMPLE_TITLE),
                EXAMPLE_LINES.map { Component.literal(it) },
                custom
            )
        }
        return null
    }

    private fun withCustomLine(title: Component, lines: List<Component>, custom: ParsedColoredText?): BoardContent {
        if (custom == null || lines.isEmpty()) return BoardContent(title, lines, null)
        return BoardContent(title, lines.dropLast(1) + custom.asComponent(), custom)
    }

    private fun renderBoard(graphics: GuiGraphicsExtractor, data: BoardContent): Pair<Float, Float> {
        val font = mc.font
        val fontHeight = font.lineHeight
        val pad = padding.toInt()

        val lines = data.lines
        val lastIdx = lines.size - 1
        val custom = data.customLine
        val customHasText = custom != null && custom.plain.isNotEmpty()
        val useGradient = customHasText && lastLineGradient

        val displayLines = lines.mapIndexed { i, line ->
            if (useGradient && i == lastIdx) Component.literal(custom.formatting) else line
        }

        val titleComp = data.title
        val titleWidth = font.width(titleComp)
        val lineWidths = displayLines.map { font.width(it) }
        var maxW = titleWidth
        for (w in lineWidths) {
            if (w > maxW) maxW = w
        }

        val textLeft = pad + 1
        val totalWidth = maxW + pad * 2 + 3
        val titleH = fontHeight + 2
        val entriesH = displayLines.size * fontHeight + pad * 2
        val totalHeight = titleH + entriesH

        if (showBackground) {
            if (backgroundRadius > 0f) {
                graphics.drawRoundedRect(0f, 0f, totalWidth.toFloat(), totalHeight.toFloat(), bgColor, backgroundRadius)
            } else {
                graphics.fill(0, 0, totalWidth, totalHeight, bgColor)
            }
        }

        graphics.text(font, titleComp, textLeft + (maxW - titleWidth) / 2, pad, 0xFFFFFFFF.toInt(), true)

        val startY = pad + titleH
        for ((i, line) in displayLines.withIndex()) {
            val y = startY + i * fontHeight
            val x = if (customHasText && centerIpLine && i == lastIdx) textLeft + (maxW - lineWidths[i]) / 2 else textLeft
            if (useGradient && i == lastIdx) {
                graphics.gradientText(font, custom.formatting, x, y,
                    gradStart and 0xFFFFFF,
                    gradEnd and 0xFFFFFF,
                    gradSpeed.toInt(), shadow = true)
            } else {
                graphics.text(font, line, x, y, 0xFFFFFFFF.toInt(), true)
            }
        }

        return Pair(totalWidth.toFloat(), totalHeight.toFloat())
    }
}
