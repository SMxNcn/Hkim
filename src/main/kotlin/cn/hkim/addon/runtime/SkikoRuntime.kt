package cn.hkim.addon.runtime

import cn.hkim.addon.utils.render.skiko.SkikoGradient
import cn.hkim.addon.utils.render.skiko.SkikoRoundEdge
import net.minecraft.client.gui.GuiGraphicsExtractor

interface SkikoRuntime : AutoCloseable {
    fun initPipRenderer()

    fun drawRoundedRect(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        fillColor: Int,
        borderColor: Int = 0,
        borderWidth: Float = 0f,
        radius: Float = 0f,
        shadowColor: Int = 0,
        blur: Float = 0f,
        spread: Float = 0f
    )

    fun drawEdgeRoundedRect(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        fillColor: Int,
        radius: Float,
        edge: SkikoRoundEdge
    )

    fun drawCircle(
        graphics: GuiGraphicsExtractor,
        cx: Float, cy: Float,
        fillColor: Int,
        borderColor: Int = 0,
        borderWidth: Float = 0f,
        radius: Float
    )

    fun drawLine(
        graphics: GuiGraphicsExtractor,
        x1: Float, y1: Float, x2: Float, y2: Float,
        color: Int,
        thickness: Float = 1f
    )

    fun drawGradientRectMulti(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        colors: List<Int>,
        positions: FloatArray? = null,
        direction: SkikoGradient = SkikoGradient.LEFT_RIGHT,
        radius: Float = 0f
    )

    fun drawSquareClipped(
        graphics: GuiGraphicsExtractor,
        cx: Float, cy: Float,
        width: Float, height: Float,
        borderColor: Int,
        borderWidth: Float,
        clipX: Float, clipY: Float, clipW: Float, clipH: Float,
        clipRadius: Float = 0f
    )

    fun textWidth(text: String, size: Float, bold: Boolean = false): Float
    fun textHeight(size: Float, bold: Boolean = false): Float

    fun drawCenteredText(
        graphics: GuiGraphicsExtractor,
        text: String,
        centerX: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        shadow: Boolean = false
    )

    fun drawClippedText(
        graphics: GuiGraphicsExtractor,
        text: String,
        x: Float, y: Float,
        size: Float,
        color: Int,
        clipX: Float, clipY: Float, clipW: Float, clipH: Float,
        bold: Boolean = false
    )

    fun drawGradientText(
        graphics: GuiGraphicsExtractor,
        text: String,
        x: Float, y: Float,
        size: Float,
        startColor: Int,
        endColor: Int,
        bold: Boolean = false,
        direction: SkikoGradient = SkikoGradient.LEFT_RIGHT
    )

    fun drawClippedRect(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        color: Int,
        clipX: Float, clipY: Float, clipW: Float, clipH: Float
    )

    fun drawImage(
        graphics: GuiGraphicsExtractor,
        resourcePath: String,
        x: Float, y: Float,
        w: Float, h: Float,
        radius: Float,
        tintColor: Int = 0,
        rotationDegrees: Float = 0f
    )

    fun status(): String

    override fun close() = Unit
}
