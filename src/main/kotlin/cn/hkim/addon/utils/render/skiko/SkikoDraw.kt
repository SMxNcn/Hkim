package cn.hkim.addon.utils.render.skiko

import cn.hkim.addon.Hkim
import net.minecraft.client.gui.GuiGraphicsExtractor

object SkikoDraw {
    fun GuiGraphicsExtractor.drawRoundedRect(
        x: Float, y: Float,
        width: Float, height: Float,
        color: Int,
        radius: Float
    ) {
        Hkim.runtime?.drawRoundedRect(this, x, y, width, height, color, 0, 0f, radius)
    }

    fun GuiGraphicsExtractor.drawRoundedRectWithBorder(
        x: Float, y: Float,
        width: Float, height: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float
    ) {
        Hkim.runtime?.drawRoundedRect(this, x, y, width, height, fillColor, borderColor, borderWidth, radius)
    }

    fun GuiGraphicsExtractor.drawSkikoEdgeRoundedRect(
        x: Float, y: Float,
        width: Float, height: Float,
        color: Int,
        radius: Float,
        edge: SkikoRoundEdge
    ) {
        Hkim.runtime?.drawEdgeRoundedRect(this, x, y, width, height, color, radius, edge)
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
        spread: Float
    ) {
        Hkim.runtime?.drawRoundedRect(this, x, y, width, height, fillColor, borderColor, borderWidth, radius, shadowColor, blur, spread)
    }

    fun GuiGraphicsExtractor.drawCircle(
        cx: Float, cy: Float,
        color: Int,
        radius: Float
    ) {
        Hkim.runtime?.drawCircle(this, cx, cy, color, 0, 0f, radius)
    }

    fun GuiGraphicsExtractor.drawSkikoLine(
        x1: Float, y1: Float, x2: Float, y2: Float,
        color: Int,
        thickness: Float = 1f
    ) {
        Hkim.runtime?.drawLine(this, x1, y1, x2, y2, color, thickness)
    }

    fun GuiGraphicsExtractor.drawGradientRectMulti(
        x: Float, y: Float,
        width: Float, height: Float,
        colors: List<Int>,
        positions: FloatArray? = null,
        direction: SkikoGradient = SkikoGradient.LEFT_RIGHT,
        radius: Float = 0f
    ) {
        Hkim.runtime?.drawGradientRectMulti(this, x, y, width, height, colors, positions, direction, radius)
    }

    fun GuiGraphicsExtractor.drawCircleWithBorder(
        cx: Float, cy: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float
    ) {
        Hkim.runtime?.drawCircle(this, cx, cy, fillColor, borderColor, borderWidth, radius)
    }

    fun GuiGraphicsExtractor.drawSkikoImage(
        resourcePath: String,
        x: Float, y: Float,
        width: Float, height: Float,
        radius: Float,
        tintColor: Int = 0,
        rotationDegrees: Float = 0f
    ) {
        Hkim.runtime?.drawImage(this, resourcePath, x, y, width, height, radius, tintColor, rotationDegrees)
    }

    fun GuiGraphicsExtractor.drawSkikoCenteredText(
        text: String,
        centerX: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        Hkim.runtime?.drawCenteredText(this, text, centerX, y, size, color, bold)
    }

    fun skikoTextWidth(text: String, size: Float, bold: Boolean = false): Float =
        Hkim.runtime?.textWidth(text, size, bold) ?: 0f

    fun skikoTextHeight(size: Float, bold: Boolean = false): Float =
        Hkim.runtime?.textHeight(size, bold) ?: 0f

    fun GuiGraphicsExtractor.drawSkikoTextClipped(
        text: String,
        x: Float, y: Float,
        size: Float,
        color: Int,
        clipX: Float, clipY: Float,
        clipW: Float, clipH: Float,
        bold: Boolean = false
    ) {
        Hkim.runtime?.drawClippedText(this, text, x, y, size, color, clipX, clipY, clipW, clipH, bold)
    }

    fun GuiGraphicsExtractor.drawSkikoGradientText(
        text: String,
        x: Float, y: Float,
        size: Float,
        startColor: Int,
        endColor: Int,
        bold: Boolean = false,
        direction: SkikoGradient = SkikoGradient.LEFT_RIGHT
    ) {
        Hkim.runtime?.drawGradientText(this, text, x, y, size, startColor, endColor, bold, direction)
    }

    fun GuiGraphicsExtractor.drawSkikoRectClipped(
        x: Float, y: Float,
        w: Float, h: Float,
        color: Int,
        clipX: Float, clipY: Float,
        clipW: Float, clipH: Float
    ) {
        Hkim.runtime?.drawClippedRect(this, x, y, w, h, color, clipX, clipY, clipW, clipH)
    }

    fun GuiGraphicsExtractor.drawSkikoSquareClipped(
        cx: Float, cy: Float,
        width: Float, height: Float,
        borderColor: Int,
        borderWidth: Float,
        clipX: Float, clipY: Float,
        clipW: Float, clipH: Float,
        clipRadius: Float = 0f
    ) {
        Hkim.runtime?.drawSquareClipped(this, cx, cy, width, height, borderColor, borderWidth, clipX, clipY, clipW, clipH, clipRadius)
    }

    fun GuiGraphicsExtractor.drawSkikoText(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        shadow: Boolean = false
    ) {
        val runtime = Hkim.runtime ?: return
        val width = runtime.textWidth(text, size, bold)
        runtime.drawCenteredText(this, text, x + width / 2f, y, size, color, bold, shadow)
    }

    fun GuiGraphicsExtractor.drawSkikoCenteredTextShadow(
        text: String,
        centerX: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        Hkim.runtime?.drawCenteredText(this, text, centerX, y, size, color, bold, true)
    }
}