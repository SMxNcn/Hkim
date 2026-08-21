package cn.hkim.addon.gui

import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithShadow
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoTextWidth
import net.minecraft.client.gui.GuiGraphicsExtractor

object SkikoTooltip {
    private const val TICK_MS = 50L
    private const val CALL_GAP_RESET_MS = 200L

    private var hoverStartNanos = 0L
    private var lastCallNanos = 0L
    private var lastContentHash = 0

    private var pending: PendingTooltip? = null

    private class PendingTooltip(
        val lines: List<String>,
        val mouseX: Float, val mouseY: Float,
        val backgroundColor: Int, val borderColor: Int, val borderWidth: Float,
        val textColor: Int, val radius: Float, val padding: Float, val textSize: Float,
        val shadow: Boolean,
        val ready: Boolean
    )

    @JvmStatic
    fun beginFrame() {
        pending = null
    }

    @JvmStatic
    fun flush(graphics: GuiGraphicsExtractor) {
        val p = pending ?: return
        pending = null
        if (!p.ready) return

        val guiW = graphics.guiWidth().toFloat()
        val guiH = graphics.guiHeight().toFloat()

        val maxContentW = (guiW - 20f - p.padding * 2).coerceAtLeast(60f)
        val lines = p.lines.flatMap { wrapLine(it, maxContentW, p.textSize) }
        val lineHeight = p.textSize * 1.2f
        val width = (lines.maxOfOrNull { skikoTextWidth(it, p.textSize) } ?: 0f).coerceAtMost(maxContentW) + p.padding * 2
        val height = lines.size * lineHeight + p.padding * 2

        val extend = if (p.shadow) 10f else 0f
        var x = p.mouseX + 12f
        var y = p.mouseY - 12f
        if (x + width > guiW - extend) x = p.mouseX - 12f - width
        if (y + height > guiH - extend) y = p.mouseY - height - 12f
        x = x.coerceIn(extend, (guiW - width - extend).coerceAtLeast(extend))
        y = y.coerceIn(extend, (guiH - height - extend).coerceAtLeast(extend))
        if (x < 0f || y < 0f || x + width > guiW || y + height > guiH) {
            println("OOB tooltip rect: x=$x y=$y w=$width h=$height gui=${guiW}x$guiH mouse=(${p.mouseX},${p.mouseY}) lines=${lines.size}")
        }

        if (p.shadow) {
            graphics.drawRoundedRectWithShadow(x, y, width, height, p.backgroundColor, p.borderColor, p.borderWidth, p.radius, 0x60000000, 3f, 1f)
        } else {
            graphics.drawRoundedRectWithBorder(x, y, width, height, p.backgroundColor, p.borderColor, p.borderWidth, p.radius)
        }
        lines.forEachIndexed { i, line ->
            if (line.isNotEmpty()) {
                graphics.drawSkikoText(line, x + p.padding, y + p.padding + i * lineHeight - 0.5f, p.textSize, p.textColor)
            }
        }
    }

    private fun wrapLine(line: String, maxWidth: Float, textSize: Float): List<String> {
        if (line.isEmpty() || skikoTextWidth(line, textSize) <= maxWidth) return listOf(line)
        val out = mutableListOf<String>()
        val cur = StringBuilder()

        fun flush() {
            if (cur.isNotEmpty()) {
                out.add(cur.toString())
                cur.setLength(0)
            }
        }

        for (word in line.split(' ')) {
            val candidate = if (cur.isEmpty()) word else "$cur $word"
            if (skikoTextWidth(candidate, textSize) <= maxWidth) {
                cur.setLength(0)
                cur.append(candidate)
                continue
            }
            flush()
            if (skikoTextWidth(word, textSize) > maxWidth) {
                for (ch in word) {
                    if (cur.isNotEmpty() && skikoTextWidth(cur.toString() + ch, textSize) > maxWidth) flush()
                    cur.append(ch)
                }
            } else {
                cur.append(word)
            }
        }
        flush()
        return out
    }

    fun GuiGraphicsExtractor.drawTooltip(
        text: String,
        mouseX: Float, mouseY: Float,
        delayTicks: Int = 0,
        backgroundColor: Int = Theme.tooltipBg,
        borderColor: Int = Theme.tooltipBorder,
        borderWidth: Float = 1f,
        textColor: Int = Theme.tooltipText,
        radius: Float = 4f,
        padding: Float = 4f,
        textSize: Float = 8f,
        shadow: Boolean = true
    ) {
        drawTooltip(text.lines(), mouseX, mouseY, delayTicks, backgroundColor, borderColor,
            borderWidth, textColor, radius, padding, textSize, shadow)
    }

    fun drawTooltip(
        lines: List<String>,
        mouseX: Float, mouseY: Float,
        delayTicks: Int = 0,
        backgroundColor: Int = Theme.tooltipBg,
        borderColor: Int = Theme.tooltipBorder,
        borderWidth: Float = 1f,
        textColor: Int = Theme.tooltipText,
        radius: Float = 4f,
        padding: Float = 2f,
        textSize: Float = 8f,
        shadow: Boolean = true
    ) {
        if (lines.isEmpty()) return

        val now = System.nanoTime()
        val sinceLastCallMs = (now - lastCallNanos) / 1_000_000L
        lastCallNanos = now

        val hash = lines.joinToString("\n").hashCode()
        if (hash != lastContentHash || sinceLastCallMs > CALL_GAP_RESET_MS) {
            hoverStartNanos = now
            lastContentHash = hash
        }
        val ready = (now - hoverStartNanos) / 1_000_000L >= delayTicks * TICK_MS

        pending = PendingTooltip(
            lines, mouseX, mouseY,
            backgroundColor, borderColor, borderWidth,
            textColor, radius, padding, textSize,
            shadow, ready
        )
    }
}
