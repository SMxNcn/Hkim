package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.ModuleConfig
import cn.hkim.addon.config.settings.*
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.hud.HudAlignment
import cn.hkim.addon.hud.HudBlurBackground
import cn.hkim.addon.hud.HudElement
import cn.hkim.addon.runtime.BackdropBlur
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.ColorUtils.gradientText
import cn.hkim.addon.utils.ParsedColoredText
import cn.hkim.addon.utils.parseColoredText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawBackdropBlur
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRect
import cn.hkim.addon.utils.toLegacyOrComponent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

@ModuleInfo("custom_scoreboard", Category.MISC)
object CustomScoreboard : Module("Custom Scoreboard", "Scoreboard features.") {
    private val background by DropdownSetting("Background")
    private val showBackground by BooleanSetting("Enabled", "", true).depends { background }
    private val bgColor by ColorSetting("Color", "", 0x66000000).depends { background && showBackground }
    private val padding by NumberSetting("Padding", "Extra padding added on edges.", 2f, 0f, 5f, 1f).depends { background && showBackground }
    private val backgroundRadius by NumberSetting("Radius", "", 0f, 0f, 5f, 1f).depends { background && showBackground }
    private val blurRadius by NumberSetting("Blur Radius", "", 0f, 0f, 20f, 1f).depends { background && showBackground }
    private val textShadow by BooleanSetting("Text Shadow", "Draw scoreboard text with a shadow.", false)
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
        hud.anchorX = -4f
        hud.anchorY = -25f
        hud.hudScale = 1f
        prevContentWidth = 0f
        initialPositionSet = false
        ModuleConfig.saveConfig()
    }

    private const val EXAMPLE_TITLE = "§e§lTitle"
    private val EXAMPLE_LINES = listOf(
        "§6  5,234,567",
        "§d  ◆ 1,234",
        "§a  ✔ 42",
        "§b  ✦ 128",
        "§e  Coins 100K",
        "§7  §m━━━━━━━━━",
        "§eexample.com"
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

    private fun hasCustomIpText(): Boolean = replaceIpLine && customLine().plain.isNotBlank()

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
    }.apply {
        contentWidth = 0f
        contentHeight = 0f
    }

    init {
        HudBlurBackground.register("hkim:custom_scoreboard") { graphics ->
            if (!this@CustomScoreboard.enabled || !showBackground) return@register
            if (!hud.isVisible()) return@register
            val bounds = hud.getScreenBounds()
            if (bounds.w <= 0f || bounds.h <= 0f) return@register
            val scale = hud.hudScale
            val blur = BackdropBlur(
                radius = blurRadius * scale,
                cornerRadius = backgroundRadius * scale,
                tint = bgColor,
            )
            if (blur.enabled) {
                graphics.drawBackdropBlur(bounds.x, bounds.y, bounds.w, bounds.h, blur)
            } else {
                graphics.drawRoundedRect(
                    bounds.x, bounds.y, bounds.w, bounds.h,
                    bgColor,
                    backgroundRadius * scale,
                )
            }
        }
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
                hud.setScreenX(newLeft, newW * scale)
            } else if (currentLeft < 2f) {
                val maxLeft = (screenW - newW * scale - 4f).coerceAtLeast(4f)
                val newLeft = 4f.coerceAtMost(maxLeft)
                hud.setScreenX(newLeft, newW * scale)
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

        hud.setScreenX(clampedLeft, newW * scale)
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

    private fun List<Component>.trimTrailingBlankLines(): List<Component> =
        dropLastWhile { it.string.isBlank() }

    private fun withCustomLine(title: Component, lines: List<Component>, custom: ParsedColoredText?): BoardContent {
        if (custom == null || custom.plain.isBlank() || lines.isEmpty()) {
            return BoardContent(title, lines.trimTrailingBlankLines(), null)
        }
        val base = lines.dropLast(1)
        return BoardContent(title, (base + custom.asComponent()).trimTrailingBlankLines(), custom)
    }

    private fun renderBoard(graphics: GuiGraphicsExtractor, data: BoardContent): Pair<Float, Float> {
        val font = mc.font
        val fontHeight = font.lineHeight
        val borderPad = 1
        val pad = borderPad + padding.toInt()
        val titleGap = 1

        val lines = data.lines
        val lastIdx = lines.size - 1
        val custom = data.customLine
        val customHasText = custom != null && custom.plain.isNotBlank()
        val useGradient = customHasText && lastLineGradient

        val displayLines = lines.mapIndexed { i, line ->
            if (useGradient && i == lastIdx) Component.literal(custom.formatting) else line
        }

        val titleComp = data.title
        val titleWidth = font.width(titleComp)
        val lineWidths = displayLines.map { font.width(it) }
        val contentW = maxOf(lineWidths.maxOrNull() ?: 0, titleWidth)

        val titleX = pad + (contentW - titleWidth) / 2
        val titleY = pad
        val linesTop = titleY + fontHeight + titleGap
        val lineTops = displayLines.indices.map { linesTop + it * fontHeight }
        val contentBottom = if (displayLines.isEmpty()) titleY + fontHeight else lineTops.last() + fontHeight
        val totalWidth = pad + contentW + pad
        val totalHeight = contentBottom + pad

        graphics.text(font, titleComp, titleX, titleY, 0xFFFFFFFF.toInt(), textShadow)

        for ((i, line) in displayLines.withIndex()) {
            val x = if (customHasText && centerIpLine && i == lastIdx) {
                pad + (contentW - lineWidths[i]) / 2
            } else {
                pad
            }
            if (useGradient && i == lastIdx) {
                graphics.gradientText(font, custom.formatting, x, lineTops[i],
                    gradStart and 0xFFFFFF,
                    gradEnd and 0xFFFFFF,
                    gradSpeed.toInt(), shadow = textShadow)
            } else {
                graphics.text(font, line, x, lineTops[i], 0xFFFFFFFF.toInt(), textShadow)
            }
        }

        return Pair(totalWidth.toFloat(), totalHeight.toFloat())
    }
}
