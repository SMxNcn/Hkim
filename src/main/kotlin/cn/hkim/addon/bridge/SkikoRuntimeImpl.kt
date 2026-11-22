package cn.hkim.addon.bridge

import cn.hkim.addon.Hkim
import cn.hkim.addon.runtime.BackdropBlur
import cn.hkim.addon.runtime.SkikoRuntime
import cn.hkim.addon.runtime.SkikoTextRun
import cn.hkim.addon.utils.render.pip.BackdropDraw
import cn.hkim.addon.utils.render.pip.BackdropBlurPIP
import cn.hkim.addon.utils.render.pip.BlurPipelines
import cn.hkim.addon.utils.render.pip.SkikoPIP
import cn.hkim.addon.utils.render.skiko.Skiko
import cn.hkim.addon.utils.render.skiko.SkikoFont
import cn.hkim.addon.utils.render.skiko.SkikoGradient
import cn.hkim.addon.utils.render.skiko.SkikoRoundEdge
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.*

class SkikoRuntimeImpl : SkikoRuntime {
    private var pipRegistered = false

    private class Batch(
        val graphics: GuiGraphicsExtractor,
        val x: Float, val y: Float,
        val width: Float, val height: Float,
        val clipRadius: Float,
        val backdrop: BackdropBlur?,
    ) {
        val ops = ArrayList<() -> Unit>()
    }

    private var batch: Batch? = null
    private var batchDepth = 0

    override fun initPipRenderer() {
        if (pipRegistered) return
        pipRegistered = true
        runCatching { BlurPipelines.ensureInitialized() }
            .onFailure { Hkim.logger.error("Failed to register backdrop blur pipelines", it) }
        PictureInPictureRendererRegistry.register { SkikoPIP() }
        PictureInPictureRendererRegistry.register { BackdropBlurPIP() }
    }

    override fun beginBatch(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, clipRadius: Float) {
        beginBatchInternal(graphics, x, y, width, height, clipRadius, null)
    }

    override fun beginBatch(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float, height: Float,
        clipRadius: Float,
        backdrop: BackdropBlur
    ) {
        beginBatchInternal(graphics, x, y, width, height, clipRadius, backdrop)
    }

    private fun beginBatchInternal(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float, height: Float,
        clipRadius: Float,
        backdrop: BackdropBlur?
    ) {
        if (batchDepth++ > 0) return
        batch = Batch(graphics, x, y, width, height, clipRadius, backdrop)
    }

    override fun endBatch() {
        if (batchDepth == 0) return
        if (--batchDepth > 0) return
        val collected = batch ?: return
        batch = null
        if (collected.ops.isEmpty()) return
        val ops = collected.ops
        val clip = collected.clipRadius.coerceAtMost(minOf(collected.width, collected.height) / 2f)
        val backdrop = collected.backdrop
            ?.let { it.copy(cornerRadius = it.cornerRadius ?: clip) }
            ?.let { BackdropDraw(it, collected.x, collected.y, collected.width, collected.height) }
        SkikoPIP.drawSkikoTo(collected.graphics, collected.x, collected.y, collected.width, collected.height, null, null, backdrop) {
            if (clip > 0f) {
                Skiko.push()
                Skiko.clipRoundRect(collected.x, collected.y, collected.width, collected.height, clip)
            }
            ops.forEach { it() }
            if (clip > 0f) Skiko.pop()
        }
    }

    private fun draw(
        graphics: GuiGraphicsExtractor,
        x: Number, y: Number, width: Number, height: Number,
        cacheKey: Any? = null, cacheToken: Int? = null,
        backdrop: BackdropDraw? = null,
        body: () -> Unit
    ) {
        val active = batch
        if (active != null) {
            // Blur inside a batch is batch-level only; per-op requests are ignored.
            active.ops += body
            return
        }
        SkikoPIP.drawSkikoTo(graphics, x, y, width, height, cacheKey, cacheToken, backdrop) { body() }
    }

    override fun drawRoundedRect(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float,
        shadowColor: Int,
        feather: Float,
        spread: Float,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        drawRoundedRectImpl(graphics, x, y, w, h, fillColor, borderColor, borderWidth, radius, shadowColor, feather, spread, cacheKey, cacheToken, null)
    }

    override fun drawRoundedRect(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float,
        shadowColor: Int,
        feather: Float,
        spread: Float,
        cacheKey: Any?,
        cacheToken: Int?,
        backdrop: BackdropBlur
    ) {
        drawRoundedRectImpl(graphics, x, y, w, h, fillColor, borderColor, borderWidth, radius, shadowColor, feather, spread, cacheKey, cacheToken, backdrop)
    }

    private fun drawRoundedRectImpl(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float,
        shadowColor: Int,
        feather: Float,
        spread: Float,
        cacheKey: Any?,
        cacheToken: Int?,
        backdrop: BackdropBlur?
    ) {
        if (w <= 0f || h <= 0f) return
        val hasShadow = shadowColor != 0
        val hasBorder = borderColor != 0 && borderWidth > 0f && w > borderWidth && h > borderWidth
        val extend = if (hasShadow) spread + feather * 3f else 0f
        val dx = x - extend
        val dy = y - extend
        val dw = w + extend * 2f
        val dh = h + extend * 2f
        val fillRadius = radius.coerceAtLeast(0f)
        val backdropDraw = backdrop
            ?.takeIf { it.enabled }
            ?.let { it.copy(cornerRadius = it.cornerRadius ?: fillRadius) }
            ?.let { BackdropDraw(it, x, y, w, h) }
        draw(graphics, dx, dy, dw, dh, cacheKey, cacheToken, backdropDraw) {
            if (hasShadow) {
                Skiko.dropShadow(x, y, w, h, feather, spread, radius, Color(shadowColor, true))
            }
            if (hasBorder) {
                if (w > borderWidth * 2f && h > borderWidth * 2f) {
                    Skiko.rect(
                        x + borderWidth, y + borderWidth,
                        w - borderWidth * 2f, h - borderWidth * 2f,
                        Color(fillColor, true),
                        (fillRadius - borderWidth).coerceAtLeast(0f)
                    )
                }
                val half = borderWidth / 2f
                Skiko.hollowRect(
                    x + half, y + half,
                    w - borderWidth, h - borderWidth,
                    borderWidth, Color(borderColor, true),
                    (fillRadius - half).coerceAtLeast(0f)
                )
            }
            else {
                Skiko.rect(x, y, w, h, Color(fillColor, true), fillRadius)
            }
        }
    }

    override fun drawBackdropBlur(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        backdrop: BackdropBlur,
        cacheKey: Any?
    ) {
        if (w <= 0f || h <= 0f || !backdrop.enabled) return
        if (batch != null) return
        SkikoPIP.drawBackdropBlurTo(graphics, x, y, w, h, backdrop, cacheKey)
    }

    override fun drawEdgeRoundedRect(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        fillColor: Int,
        radius: Float,
        edge: SkikoRoundEdge,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (w <= 0f || h <= 0f) return
        draw(graphics, x, y, w, h, cacheKey, cacheToken) {
            Skiko.drawEdgeRoundedRect(x, y, w, h, Color(fillColor, true), radius, edge)
        }
    }

    override fun drawGradientRectMulti(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        colors: List<Int>,
        positions: FloatArray?,
        direction: SkikoGradient,
        radius: Float,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        drawGradientRectMultiImpl(graphics, x, y, w, h, colors, positions, direction, radius, cacheKey, cacheToken, null)
    }

    override fun drawGradientRectMulti(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        colors: List<Int>,
        positions: FloatArray?,
        direction: SkikoGradient,
        radius: Float,
        cacheKey: Any?,
        cacheToken: Int?,
        backdrop: BackdropBlur
    ) {
        drawGradientRectMultiImpl(graphics, x, y, w, h, colors, positions, direction, radius, cacheKey, cacheToken, backdrop)
    }

    private fun drawGradientRectMultiImpl(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        colors: List<Int>,
        positions: FloatArray?,
        direction: SkikoGradient,
        radius: Float,
        cacheKey: Any?,
        cacheToken: Int?,
        backdrop: BackdropBlur?
    ) {
        if (w <= 0f || h <= 0f || colors.size < 2) return
        val backdropDraw = backdrop
            ?.takeIf { it.enabled }
            ?.let { it.copy(cornerRadius = it.cornerRadius ?: radius.coerceAtLeast(0f)) }
            ?.let { BackdropDraw(it, x, y, w, h) }
        draw(graphics, x, y, w, h, cacheKey, cacheToken, backdropDraw) {
            Skiko.gradientRectMulti(x, y, w, h, colors.map { Color(it, true) }, positions, direction, radius)
        }
    }

    override fun drawSquareClipped(
        graphics: GuiGraphicsExtractor,
        cx: Float, cy: Float,
        width: Float, height: Float,
        borderColor: Int,
        borderWidth: Float,
        clipX: Float, clipY: Float, clipW: Float, clipH: Float,
        clipRadius: Float,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (width <= 0f || height <= 0f || clipW <= 0f || clipH <= 0f) return
        draw(graphics, clipX, clipY, clipW, clipH, cacheKey, cacheToken) {
            Skiko.push()
            if (clipRadius > 0f) {
                Skiko.clipRoundRect(clipX, clipY, clipW, clipH, clipRadius)
            } else {
                Skiko.clipRect(clipX, clipY, clipW, clipH)
            }
            Skiko.hollowRect(cx - width / 2f, cy - height / 2f, width, height, borderWidth, Color(borderColor, true), 0f)
            Skiko.pop()
        }
    }

    override fun drawLine(
        graphics: GuiGraphicsExtractor,
        x1: Float, y1: Float, x2: Float, y2: Float,
        color: Int,
        thickness: Float,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (thickness <= 0f) return
        val half = thickness / 2f
        val minX = floor(minOf(x1, x2) - half)
        val minY = floor(minOf(y1, y2) - half)
        val maxX = ceil(maxOf(x1, x2) + half)
        val maxY = ceil(maxOf(y1, y2) + half)
        val w = maxX - minX
        val h = maxY - minY
        if (w <= 0f || h <= 0f) return

        draw(graphics, minX, minY, w, h, cacheKey, cacheToken) {
            Skiko.line(x1, y1, x2, y2, thickness, Color(color, true))
        }
    }

    override fun drawCircle(
        graphics: GuiGraphicsExtractor,
        cx: Float, cy: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (radius <= 0f) return
        val minX = floor(cx - radius)
        val minY = floor(cy - radius)
        val maxX = ceil(cx + radius)
        val maxY = ceil(cy + radius)

        draw(graphics, minX, minY, maxX - minX, maxY - minY, cacheKey, cacheToken) {
            if (borderColor != 0 && borderWidth > 0f) {
                Skiko.circle(cx, cy, radius, Color(borderColor, true))
                Skiko.circle(cx, cy, (radius - borderWidth).coerceAtLeast(0.1f), Color(fillColor, true))
            }
            else {
                Skiko.circle(cx, cy, radius, Color(fillColor, true))
            }
        }
    }

    override fun drawArc(
        graphics: GuiGraphicsExtractor,
        cx: Float, cy: Float,
        radius: Float,
        startAngle: Float,
        sweepAngle: Float,
        thickness: Float,
        color: Int,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (radius <= 0f || sweepAngle == 0f || thickness <= 0f) return
        val bounds = arcBounds(cx, cy, 0f, radius, startAngle, sweepAngle, ceil(thickness / 2f), includeCenter = false)
        draw(graphics, bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1], cacheKey, cacheToken) {
            Skiko.arc(cx, cy, radius, startAngle, sweepAngle, thickness, Color(color, true))
        }
    }

    override fun drawSector(
        graphics: GuiGraphicsExtractor,
        cx: Float, cy: Float,
        innerRadius: Float,
        outerRadius: Float,
        startAngle: Float,
        sweepAngle: Float,
        color: Int,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (outerRadius <= 0f || sweepAngle == 0f) return
        val bounds = arcBounds(cx, cy, innerRadius, outerRadius, startAngle, sweepAngle, 1f, includeCenter = innerRadius <= 0f)
        draw(graphics, bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1], cacheKey, cacheToken) {
            Skiko.sector(cx, cy, innerRadius, outerRadius, startAngle, sweepAngle, Color(color, true))
        }
    }

    override fun drawParallelSector(
        graphics: GuiGraphicsExtractor,
        cx: Float, cy: Float,
        innerRadius: Float, outerRadius: Float,
        startAngle: Float, endAngle: Float,
        gap: Float,
        color: Int,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (outerRadius <= 0f || endAngle <= startAngle) return
        val bounds = arcBounds(cx, cy, innerRadius, outerRadius, startAngle, endAngle - startAngle, 1f, includeCenter = innerRadius <= 0f)
        draw(graphics, bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1], cacheKey, cacheToken) {
            Skiko.parallelSector(cx, cy, innerRadius, outerRadius, startAngle, endAngle, gap, Color(color, true))
        }
    }

    private fun arcBounds(
        cx: Float, cy: Float,
        innerRadius: Float, outerRadius: Float,
        startAngle: Float, sweepAngle: Float,
        pad: Float,
        includeCenter: Boolean
    ): FloatArray {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        fun include(angle: Float, radius: Float) {
            if (radius <= 0f) return
            val x = cx + cos(angle) * radius
            val y = cy + sin(angle) * radius
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }

        val endAngle = startAngle + sweepAngle
        val from = minOf(startAngle, endAngle)
        val to = maxOf(startAngle, endAngle)
        val quarter = (PI / 2).toFloat()
        var axisAngle = ceil(from / quarter) * quarter
        val angles = ArrayList<Float>(8).apply {
            add(startAngle)
            add(endAngle)
            while (axisAngle <= to) {
                add(axisAngle)
                axisAngle += quarter
            }
        }
        for (angle in angles) {
            include(angle, outerRadius)
            include(angle, innerRadius)
        }

        if (includeCenter) {
            minX = minOf(minX, cx)
            maxX = maxOf(maxX, cx)
            minY = minOf(minY, cy)
            maxY = maxOf(maxY, cy)
        }

        return floatArrayOf(floor(minX - pad), floor(minY - pad), ceil(maxX + pad), ceil(maxY + pad))
    }

    override fun textWidth(text: String, size: Float, bold: Boolean): Float {
        if (text.isEmpty() || size <= 0f) return 0f
        return Skiko.textWidth(text, size, if (bold) SkikoFont.system(bold = true) else SkikoFont.system())
    }

    override fun textHeight(size: Float, bold: Boolean): Float {
        if (size <= 0f) return 0f
        return Skiko.textHeight(size, if (bold) SkikoFont.system(bold = true) else SkikoFont.system())
    }

    override fun drawCenteredText(
        graphics: GuiGraphicsExtractor,
        text: String,
        centerX: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean,
        shadow: Boolean,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (text.isEmpty() || size <= 0f) return
        val font = if (bold) SkikoFont.system(bold = true) else SkikoFont.system()
        val width = Skiko.textWidth(text, size, font)
        val height = Skiko.textHeight(size, font) + 2f
        val x = centerX - width / 2f
        if (shadow) {
            draw(graphics, x - 1f, y - 1f, width + 2f, height + 2f, cacheKey, cacheToken) {
                Skiko.textShadow(text, x, y, size, Color(color, true), font)
            }
        }
        else {
            draw(graphics, x - 2f, y - 2f, width + 4f, height + 4f, cacheKey, cacheToken) {
                Skiko.text(text, x, y, size, Color(color, true), font)
            }
        }
    }

    override fun drawTextRuns(
        graphics: GuiGraphicsExtractor,
        runs: List<SkikoTextRun>,
        x: Float,
        y: Float,
        size: Float,
        width: Float,
        shadow: Boolean,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (runs.isEmpty() || size <= 0f || width <= 0f) return

        val baseFont = SkikoFont.system()
        val boldFont = SkikoFont.system(bold = true)
        val height = Skiko.textHeight(size, baseFont) + 2f

        if (shadow) {
            draw(graphics, x - 1f, y - 1f, width + 2f, height + 2f, cacheKey, cacheToken) {
                var cursorX = x
                for (run in runs) {
                    if (run.text.isEmpty()) continue
                    val font = if (run.bold) boldFont else baseFont
                    Skiko.textShadow(run.text, cursorX, y, size, Color(run.color, true), font)
                    cursorX += Skiko.textWidth(run.text, size, font)
                }
            }
        } else {
            draw(graphics, x - 2f, y - 2f, width + 4f, height + 4f, cacheKey, cacheToken) {
                var cursorX = x
                for (run in runs) {
                    if (run.text.isEmpty()) continue
                    val font = if (run.bold) boldFont else baseFont
                    Skiko.text(run.text, cursorX, y, size, Color(run.color, true), font)
                    cursorX += Skiko.textWidth(run.text, size, font)
                }
            }
        }
    }

    override fun drawClippedText(
        graphics: GuiGraphicsExtractor,
        text: String,
        x: Float, y: Float,
        size: Float,
        color: Int,
        clipX: Float, clipY: Float, clipW: Float, clipH: Float,
        bold: Boolean,
        clipRadius: Float,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (text.isEmpty() || size <= 0f || clipW <= 0f || clipH <= 0f) return
        val font = if (bold) SkikoFont.system(bold = true) else SkikoFont.system()
        val radius = clipRadius.coerceAtMost(minOf(clipW, clipH) / 2f)
        draw(graphics, clipX, clipY, clipW, clipH, cacheKey, cacheToken) {
            Skiko.push()
            if (radius > 0f) {
                Skiko.clipRoundRect(clipX, clipY, clipW, clipH, radius)
            } else {
                Skiko.clipRect(clipX, clipY, clipW, clipH)
            }
            Skiko.text(text, x, y, size, Color(color, true), font)
            Skiko.pop()
        }
    }

    override fun drawGradientText(
        graphics: GuiGraphicsExtractor,
        text: String,
        x: Float, y: Float,
        size: Float,
        startColor: Int,
        endColor: Int,
        bold: Boolean,
        direction: SkikoGradient,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (text.isEmpty() || size <= 0f) return

        val font = if (bold) SkikoFont.system(bold = true) else SkikoFont.system()
        val width = Skiko.textWidth(text, size, font)
        val height = Skiko.textHeight(size, font) + 4f

        val pipX = floor(x - 2f)
        val pipY = floor(y - 2f)
        val pipW = ceil(width + 4f)
        val pipH = ceil(height + 4f)

        if (pipW <= 0f || pipH <= 0f) return

        draw(graphics, pipX, pipY, pipW, pipH, cacheKey, cacheToken) {
            Skiko.textGradient(text, x, y, size, width, Color(startColor, true), Color(endColor, true), font, direction)
        }
    }

    override fun drawClippedRect(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        color: Int,
        clipX: Float, clipY: Float, clipW: Float, clipH: Float,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (w <= 0f || h <= 0f || clipW <= 0f || clipH <= 0f) return
        draw(graphics, clipX, clipY, clipW, clipH, cacheKey, cacheToken) {
            Skiko.push()
            Skiko.clipRect(clipX, clipY, clipW, clipH)
            Skiko.rect(x, y, w, h, Color(color, true))
            Skiko.pop()
        }
    }

    override fun drawImage(
        graphics: GuiGraphicsExtractor,
        resourcePath: String,
        x: Float, y: Float,
        w: Float, h: Float,
        radius: Float,
        tintColor: Int,
        rotationDegrees: Float,
        cacheKey: Any?,
        cacheToken: Int?
    ) {
        if (w <= 0f || h <= 0f) return
        draw(graphics, x, y, w, h, cacheKey, cacheToken) {
            drawImageBody(resourcePath, x, y, w, h, radius, tintColor, rotationDegrees)
        }
    }

    private fun drawImageBody(
        resourcePath: String,
        x: Float, y: Float,
        w: Float, h: Float,
        radius: Float,
        tintColor: Int,
        rotationDegrees: Float
    ) {
        val image = Skiko.createImage(resourcePath)
        val tint = if (tintColor != 0) tintColor else null
        if (rotationDegrees != 0f) {
            Skiko.push()
            Skiko.translate(x + w / 2f, y + h / 2f)
            Skiko.rotate(Math.toRadians(rotationDegrees.toDouble()).toFloat())
            Skiko.translate(-(x + w / 2f), -(y + h / 2f))
            Skiko.image(image, x, y, w, h, radius, tint)
            Skiko.pop()
        } else {
            Skiko.image(image, x, y, w, h, radius, tint)
        }
    }

    override fun status(): String = "bridge loaded, pip=$pipRegistered"
}
