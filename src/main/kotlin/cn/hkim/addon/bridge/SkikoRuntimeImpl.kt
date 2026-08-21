package cn.hkim.addon.bridge

import cn.hkim.addon.runtime.SkikoRuntime
import cn.hkim.addon.utils.render.pip.SkikoPIP
import cn.hkim.addon.utils.render.skiko.Skiko
import cn.hkim.addon.utils.render.skiko.SkikoFont
import cn.hkim.addon.utils.render.skiko.SkikoGradient
import cn.hkim.addon.utils.render.skiko.SkikoRoundEdge
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor

class SkikoRuntimeImpl : SkikoRuntime {
    private var pipRegistered = false

    override fun initPipRenderer() {
        if (pipRegistered) return
        pipRegistered = true
        PictureInPictureRendererRegistry.register { SkikoPIP() }
    }

    override fun drawRoundedRect(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float,
        shadowColor: Int,
        blur: Float,
        spread: Float
    ) {
        if (w <= 0f || h <= 0f) return
        val hasShadow = shadowColor != 0
        val hasBorder = borderColor != 0 && borderWidth > 0f && w > borderWidth && h > borderWidth
        val extend = if (hasShadow) spread + blur * 3f else 0f
        val dx = x - extend
        val dy = y - extend
        val dw = w + extend * 2f
        val dh = h + extend * 2f
        SkikoPIP.drawSkikoTo(graphics, dx, dy, dw, dh) {
            if (hasShadow) {
                Skiko.dropShadow(x, y, w, h, blur, spread, radius, Color(shadowColor, true))
            }
            val fillRadius = radius.coerceAtLeast(0f)
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

    override fun drawEdgeRoundedRect(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        fillColor: Int,
        radius: Float,
        edge: SkikoRoundEdge
    ) {
        if (w <= 0f || h <= 0f) return
        SkikoPIP.drawSkikoTo(graphics, x, y, w, h) {
            Skiko.drawEdgeRoundedRect(x, y, w, h, Color(fillColor, true), radius, edge)
        }
    }

    override fun drawGradientRectMulti(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        colors: List<Int>,
        positions: FloatArray?,
        direction: SkikoGradient,
        radius: Float
    ) {
        if (w <= 0f || h <= 0f || colors.size < 2) return
        SkikoPIP.drawSkikoTo(graphics, x, y, w, h) {
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
        clipRadius: Float
    ) {
        if (width <= 0f || height <= 0f || clipW <= 0f || clipH <= 0f) return
        SkikoPIP.drawSkikoTo(graphics, clipX, clipY, clipW, clipH) {
            if (clipRadius > 0f) {
                Skiko.push()
                Skiko.clipRoundRect(clipX, clipY, clipW, clipH, clipRadius)
            }
            Skiko.hollowRect(cx - width / 2f, cy - height / 2f, width, height, borderWidth, Color(borderColor, true), 0f)
            if (clipRadius > 0f) Skiko.pop()
        }
    }

    override fun drawLine(
        graphics: GuiGraphicsExtractor,
        x1: Float, y1: Float, x2: Float, y2: Float,
        color: Int,
        thickness: Float
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

        SkikoPIP.drawSkikoTo(graphics, minX, minY, w, h) {
            Skiko.line(x1, y1, x2, y2, thickness, Color(color, true))
        }
    }

    override fun drawCircle(
        graphics: GuiGraphicsExtractor,
        cx: Float, cy: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float
    ) {
        if (radius <= 0f) return
        val minX = floor(cx - radius)
        val minY = floor(cy - radius)
        val maxX = ceil(cx + radius)
        val maxY = ceil(cy + radius)

        SkikoPIP.drawSkikoTo(graphics, minX, minY, maxX - minX, maxY - minY) {
            if (borderColor != 0 && borderWidth > 0f) {
                Skiko.circle(cx, cy, radius, Color(borderColor, true))
                Skiko.circle(cx, cy, (radius - borderWidth).coerceAtLeast(0.1f), Color(fillColor, true))
            }
            else {
                Skiko.circle(cx, cy, radius, Color(fillColor, true))
            }
        }
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
        shadow: Boolean
    ) {
        if (text.isEmpty() || size <= 0f) return
        val font = if (bold) SkikoFont.system(bold = true) else SkikoFont.system()
        val width = Skiko.textWidth(text, size, font)
        val height = Skiko.textHeight(size, font) + 2f
        val x = centerX - width / 2f
        if (shadow) {
            SkikoPIP.drawSkikoTo(graphics, x - 2f, y - 2f, width + 4f, height + 4f) {
                Skiko.textShadow(text, x, y, size, Color(color, true), font)
            }
        }
        else {
            SkikoPIP.drawSkikoTo(graphics, x - 2f, y - 2f, width + 4f, height + 4f) {
                Skiko.text(text, x, y, size, Color(color, true), font)
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
        bold: Boolean
    ) {
        if (text.isEmpty() || size <= 0f || clipW <= 0f || clipH <= 0f) return
        val font = if (bold) SkikoFont.system(bold = true) else SkikoFont.system()
        SkikoPIP.drawSkikoTo(graphics, clipX, clipY, clipW, clipH) {
            Skiko.text(text, x, y, size, Color(color, true), font)
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
        direction: SkikoGradient
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

        SkikoPIP.drawSkikoTo(graphics, pipX, pipY, pipW, pipH) {
            Skiko.textGradient(text, x, y, size, width, Color(startColor, true), Color(endColor, true), font, direction)
        }
    }

    override fun drawClippedRect(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        color: Int,
        clipX: Float, clipY: Float, clipW: Float, clipH: Float
    ) {
        if (w <= 0f || h <= 0f || clipW <= 0f || clipH <= 0f) return
        SkikoPIP.drawSkikoTo(graphics, clipX, clipY, clipW, clipH) {
            Skiko.rect(x, y, w, h, Color(color, true))
        }
    }

    override fun drawImage(
        graphics: GuiGraphicsExtractor,
        resourcePath: String,
        x: Float, y: Float,
        w: Float, h: Float,
        radius: Float,
        tintColor: Int,
        rotationDegrees: Float
    ) {
        if (w <= 0f || h <= 0f) return
        SkikoPIP.drawSkikoTo(graphics, x, y, w, h) {
            if (rotationDegrees != 0f) {
                Skiko.push()
                Skiko.translate(x + w / 2f, y + h / 2f)
                Skiko.rotate(Math.toRadians(rotationDegrees.toDouble()).toFloat())
                Skiko.translate(-(x + w / 2f), -(y + h / 2f))
                Skiko.image(Skiko.createImage(resourcePath), x, y, w, h, radius, if (tintColor != 0) tintColor else null)
                Skiko.pop()
            }
            else {
                Skiko.image(Skiko.createImage(resourcePath), x, y, w, h, radius, if (tintColor != 0) tintColor else null)
            }
        }
    }

    override fun status(): String = "bridge loaded, pip=$pipRegistered"
}
