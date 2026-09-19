package cn.hkim.addon.utils.render.skiko

import cn.hkim.addon.Hkim
import net.minecraft.client.gui.GuiGraphicsExtractor

object SkikoDraw {
    fun GuiGraphicsExtractor.skikoBatch(
        x: Float, y: Float,
        width: Float, height: Float,
        clipRadius: Float = 0f,
        block: () -> Unit
    ) {
        val runtime = Hkim.runtime
        if (runtime == null) {
            block()
            return
        }
        runtime.beginBatch(this, x, y, width, height, clipRadius)
        try {
            block()
        } finally {
            runtime.endBatch()
        }
    }

    fun GuiGraphicsExtractor.drawRoundedRect(
        x: Float, y: Float,
        width: Float, height: Float,
        color: Int,
        radius: Float,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawRoundedRect(this, x, y, width, height, color, 0, 0f, radius, cacheKey = cacheKey, cacheToken = cacheToken)
    }

    fun GuiGraphicsExtractor.drawRoundedRectWithBorder(
        x: Float, y: Float,
        width: Float, height: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawRoundedRect(this, x, y, width, height, fillColor, borderColor, borderWidth, radius, cacheKey = cacheKey, cacheToken = cacheToken)
    }

    fun GuiGraphicsExtractor.drawSkikoEdgeRoundedRect(
        x: Float, y: Float,
        width: Float, height: Float,
        color: Int,
        radius: Float,
        edge: SkikoRoundEdge,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawEdgeRoundedRect(this, x, y, width, height, color, radius, edge, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawRoundedRectWithShadow(
        x: Float, y: Float,
        width: Float, height: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float,
        shadowColor: Int,
        blur: Float,
        spread: Float,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawRoundedRect(this, x, y, width, height, fillColor, borderColor, borderWidth, radius, shadowColor, blur, spread, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawCircle(
        cx: Float, cy: Float,
        color: Int,
        radius: Float,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawCircle(this, cx, cy, color, 0, 0f, radius, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawSkikoArc(
        cx: Float, cy: Float,
        radius: Float,
        startAngle: Float,
        sweepAngle: Float,
        thickness: Float,
        color: Int,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawArc(this, cx, cy, radius, startAngle, sweepAngle, thickness, color, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawSkikoParallelSector(
        cx: Float, cy: Float,
        innerRadius: Float,
        outerRadius: Float,
        startAngle: Float,
        endAngle: Float,
        gap: Float,
        color: Int,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawParallelSector(this, cx, cy, innerRadius, outerRadius, startAngle, endAngle, gap, color, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawSkikoSector(
        cx: Float, cy: Float,
        radius: Float,
        startAngle: Float,
        sweepAngle: Float,
        color: Int,
        innerRadius: Float = 0f,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawSector(this, cx, cy, innerRadius, radius, startAngle, sweepAngle, color, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawSkikoLine(
        x1: Float, y1: Float, x2: Float, y2: Float,
        color: Int,
        thickness: Float = 1f,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawLine(this, x1, y1, x2, y2, color, thickness, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawGradientRectMulti(
        x: Float, y: Float,
        width: Float, height: Float,
        colors: List<Int>,
        positions: FloatArray? = null,
        direction: SkikoGradient = SkikoGradient.LEFT_RIGHT,
        radius: Float = 0f,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawGradientRectMulti(this, x, y, width, height, colors, positions, direction, radius, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawCircleWithBorder(
        cx: Float, cy: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawCircle(this, cx, cy, fillColor, borderColor, borderWidth, radius, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawSkikoImage(
        resourcePath: String,
        x: Float, y: Float,
        width: Float, height: Float,
        radius: Float,
        tintColor: Int = 0,
        rotationDegrees: Float = 0f,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawImage(this, resourcePath, x, y, width, height, radius, tintColor, rotationDegrees, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawSkikoCenteredText(
        text: String,
        centerX: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawCenteredText(this, text, centerX, y, size, color, bold, cacheKey = cacheKey, cacheToken = cacheToken)
    }

    private const val METRIC_CACHE_LIMIT = 512
    private val widthCache = HashMap<MetricKey, Float>()
    private val heightCache = HashMap<HeightKey, Float>()

    private data class MetricKey(val text: String, val size: Float, val bold: Boolean)
    private data class HeightKey(val size: Float, val bold: Boolean)

    fun skikoTextWidth(text: String, size: Float, bold: Boolean = false): Float {
        val key = MetricKey(text, size, bold)
        widthCache[key]?.let { return it }
        val width = Hkim.runtime?.textWidth(text, size, bold) ?: 0f
        if (widthCache.size >= METRIC_CACHE_LIMIT) widthCache.clear()
        widthCache[key] = width
        return width
    }

    fun skikoTextHeight(size: Float, bold: Boolean = false): Float {
        val key = HeightKey(size, bold)
        heightCache[key]?.let { return it }
        val height = Hkim.runtime?.textHeight(size, bold) ?: 0f
        if (heightCache.size >= METRIC_CACHE_LIMIT) heightCache.clear()
        heightCache[key] = height
        return height
    }

    fun skikoCacheToken(vararg values: Any?): Int = values.contentHashCode()

    fun GuiGraphicsExtractor.drawSkikoTextClipped(
        text: String,
        x: Float, y: Float,
        size: Float,
        color: Int,
        clipX: Float, clipY: Float,
        clipW: Float, clipH: Float,
        bold: Boolean = false,
        clipRadius: Float = 0f,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawClippedText(this, text, x, y, size, color, clipX, clipY, clipW, clipH, bold, clipRadius, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawSkikoGradientText(
        text: String,
        x: Float, y: Float,
        size: Float,
        startColor: Int,
        endColor: Int,
        bold: Boolean = false,
        direction: SkikoGradient = SkikoGradient.LEFT_RIGHT,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawGradientText(this, text, x, y, size, startColor, endColor, bold, direction, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawSkikoRectClipped(
        x: Float, y: Float,
        w: Float, h: Float,
        color: Int,
        clipX: Float, clipY: Float,
        clipW: Float, clipH: Float,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawClippedRect(this, x, y, w, h, color, clipX, clipY, clipW, clipH, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawSkikoSquareClipped(
        cx: Float, cy: Float,
        width: Float, height: Float,
        borderColor: Int,
        borderWidth: Float,
        clipX: Float, clipY: Float,
        clipW: Float, clipH: Float,
        clipRadius: Float = 0f,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawSquareClipped(this, cx, cy, width, height, borderColor, borderWidth, clipX, clipY, clipW, clipH, clipRadius, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawSkikoText(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        shadow: Boolean = false,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        val runtime = Hkim.runtime ?: return
        val width = runtime.textWidth(text, size, bold)
        runtime.drawCenteredText(this, text, x + width / 2f, y, size, color, bold, shadow, cacheKey, cacheToken)
    }

    fun GuiGraphicsExtractor.drawSkikoCenteredTextShadow(
        text: String,
        centerX: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        cacheKey: Any? = null,
        cacheToken: Int? = null
    ) {
        Hkim.runtime?.drawCenteredText(this, text, centerX, y, size, color, bold, true, cacheKey, cacheToken)
    }
}