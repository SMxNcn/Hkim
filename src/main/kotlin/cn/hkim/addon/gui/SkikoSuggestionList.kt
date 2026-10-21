package cn.hkim.addon.gui

import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoTextClipped
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoBatch
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoTextHeight
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor

class SkikoSuggestionList {
    var backgroundColor: Int = Theme.controlBg
    var borderColor: Int = Theme.controlBorder
    var textColor: Int = Theme.controlText
    var textActiveColor: Int = Theme.controlTextActive
    var rowHoverColor: Int = Theme.controlHover
    var scrollBarColor: Int = Theme.controlBorderHover

    var entries: List<String> = emptyList()
        private set
    var highlighted: Int = 0
        private set

    private var scroll = 0

    val isOpen: Boolean get() = entries.isNotEmpty()
    val highlightedEntry: String? get() = entries.getOrNull(highlighted)
    private val visibleCount: Int get() = minOf(entries.size, MAX_VISIBLE)

    fun setEntries(newEntries: List<String>) {
        if (newEntries == entries) return
        entries = newEntries
        highlighted = 0
        scroll = 0
    }

    fun close() {
        entries = emptyList()
        highlighted = 0
        scroll = 0
    }

    fun highlight(index: Int) {
        if (index !in entries.indices) return
        highlighted = index
        if (highlighted < scroll) scroll = highlighted
        else if (highlighted >= scroll + MAX_VISIBLE) scroll = highlighted - MAX_VISIBLE + 1
    }

    fun moveHighlight(step: Int) {
        if (entries.isEmpty()) return
        highlight((highlighted + step).mod(entries.size))
    }

    fun scrollBy(rows: Int) {
        if (entries.size <= MAX_VISIBLE) return
        scroll = (scroll + rows).coerceIn(0, entries.size - MAX_VISIBLE)
    }

    fun height(lineHeight: Float): Float = visibleCount * lineHeight + PADDING * 2f

    fun contains(mouseX: Float, mouseY: Float, x: Float, y: Float, w: Float, lineHeight: Float): Boolean =
        HudUtils.isPointInRect(mouseX, mouseY, x, y, w, height(lineHeight))

    fun indexAt(mouseX: Float, mouseY: Float, x: Float, y: Float, w: Float, lineHeight: Float): Int {
        if (!contains(mouseX, mouseY, x, y, w, lineHeight)) return -1
        val row = ((mouseY - y - PADDING) / lineHeight).toInt()
        if (row !in 0..<visibleCount) return -1
        return scroll + row
    }

    fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, lineHeight: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int,
    ) {
        val height = height(lineHeight)
        graphics.drawRoundedRectWithBorder(x, y, w, height, backgroundColor, borderColor, 1f, LIST_RADIUS)

        val hovered = indexAt(mouseX, mouseY, x, y, w, lineHeight)
        val textOffset = (lineHeight - skikoTextHeight(TEXT_SIZE)) / 2f
        val rowLeft = x.toInt() + 1
        val rowRight = (x + w).toInt() - 1

        for (row in 0 until visibleCount) {
            val index = scroll + row
            val rowY = y + PADDING + row * lineHeight
            when (index) {
                highlighted -> graphics.fill(
                    rowLeft, rowY.toInt(), rowRight, (rowY + lineHeight).toInt(),
                    (HIGHLIGHT_ALPHA shl 24) or (themeColor and 0x00FFFFFF),
                )
                hovered -> graphics.fill(
                    rowLeft, rowY.toInt(), rowRight, (rowY + lineHeight).toInt(),
                    rowHoverColor,
                )
            }
        }

        graphics.skikoBatch(x, y, w, height, clipRadius = LIST_RADIUS) {
            for (row in 0 until visibleCount) {
                val index = scroll + row
                val rowY = y + PADDING + row * lineHeight
                drawEntry(
                    graphics, entries[index],
                    x + TEXT_INSET, rowY + textOffset, w - TEXT_INSET * 2f, lineHeight,
                    index == highlighted || index == hovered
                )
            }
        }

        if (entries.size > MAX_VISIBLE) {
            val track = height - PADDING * 2f
            val bar = (track * MAX_VISIBLE / entries.size).coerceAtLeast(MIN_BAR)
            val barY = y + PADDING + (track - bar) * scroll / (entries.size - MAX_VISIBLE).toFloat()
            graphics.fill(
                (x + w - 3f).toInt(), barY.toInt(),
                (x + w - 1f).toInt(), (barY + bar).toInt(),
                scrollBarColor,
            )
        }

        if (hovered >= 0) graphics.requestCursor(CursorTypes.POINTING_HAND)
    }

    private fun drawEntry(
        graphics: GuiGraphicsExtractor,
        entry: String, x: Float, y: Float, w: Float, rowHeight: Float,
        active: Boolean,
    ) {
        graphics.drawSkikoTextClipped(entry, x, y, TEXT_SIZE, if (active) textActiveColor else textColor, x, y, w, rowHeight)
    }

    private companion object {
        const val MAX_VISIBLE = 7
        const val PADDING = 2f
        const val TEXT_INSET = 4f
        const val HIGHLIGHT_ALPHA = 0x40
        const val MIN_BAR = 6f
        const val TEXT_SIZE = Theme.CARD_FONT_SIZE

        const val LIST_RADIUS = 2f
    }
}
