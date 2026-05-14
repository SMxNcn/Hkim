package cn.hkim.addon.utils.render.nvg

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import net.minecraft.resources.Identifier
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL3.*
import java.awt.Color
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

// Origin: https://github.com/odtheking/Odin/blob/main/src/main/kotlin/com/odtheking/odin/utils/ui/rendering/NVGRenderer.kt

object NVGRenderer {
    val defaultFont = Font("Default", mc.resourceManager.getResource(Identifier.fromNamespaceAndPath("hkim", "font.ttf")).get().open())
    private val fontMap = HashMap<Font, NVGFont>()
    private val fontBounds = FloatArray(4)

    private val images = HashMap<Image, NVGImage>()

    private val nvgPaint = NVGPaint.malloc()
    private val nvgColor = NVGColor.malloc()
    private val nvgColor2: NVGColor = NVGColor.malloc()

    private var scissor: Scissor? = null
    private var drawing: Boolean = false
    var vg: Long = -1L
        private set

    @JvmStatic
    var isInitialized: Boolean = false
        private set

    @JvmStatic
    fun init() {
        if (isInitialized) return

        vg = nvgCreate(0 or NVG_ANTIALIAS or NVG_STENCIL_STROKES)
        if (vg == -1L) {
            Hkim.logger.error("Failed to create NanoVG context.")
            return
        }

        Hkim.logger.info("Created NanoVG context. $vg")
        isInitialized = true
    }

    fun destroy() {
        if (!isInitialized) return

        if (vg != -1L) {
            nvgDelete(vg)
            vg = -1L
        }

        isInitialized = false
    }

    @JvmStatic
    fun beginFrame(width: Float, height: Float) {
        if (drawing) throw IllegalStateException("[NVGRenderer] Already drawing, but called beginFrame")
        if (!isInitialized) return
        val dpr = devicePixelRatio()
        nvgBeginFrame(vg, width / dpr, height / dpr, dpr)
        nvgTextAlign(vg, NVG_ALIGN_LEFT or NVG_ALIGN_TOP)
        drawing = true
    }

    @JvmStatic
    fun endFrame() {
        if (!drawing) throw IllegalStateException("[NVGRenderer] Not drawing, but called endFrame")
        if (!isInitialized) return
        nvgEndFrame(vg)
        drawing = false
    }

    fun push() = nvgSave(vg)

    fun pop() = nvgRestore(vg)

    fun scale(x: Float, y: Float) = nvgScale(vg, x, y)

    fun translate(x: Float, y: Float) = nvgTranslate(vg, x, y)

    fun rotate(amount: Float) = nvgRotate(vg, amount)

    fun globalAlpha(amount: Float) = nvgGlobalAlpha(vg, amount.coerceIn(0f, 1f))

    fun pushScissor(x: Float, y: Float, w: Float, h: Float) {
        scissor = Scissor(scissor, x, y, w + x, h + y)
        scissor?.applyScissor()
    }

    fun popScissor() {
        nvgResetScissor(vg)
        scissor = scissor?.previous
        scissor?.applyScissor()
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Color) {
        nvgBeginPath(vg)
        nvgMoveTo(vg, x1, y1)
        nvgLineTo(vg, x2, y2)
        nvgStrokeWidth(vg, thickness)
        color(color)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Color, radius: Float) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h + .5f, radius)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Color) {
        nvgBeginPath(vg)
        nvgRect(vg, x, y, w, h + .5f)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Color, radius: Float) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h, radius)
        nvgStrokeWidth(vg, thickness)
        nvgPathWinding(vg, NVG_HOLE)
        color(color)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun gradientRect(x: Float, y: Float, w: Float, h: Float, color1: Color, color2: Color, gradient: Gradient, radius: Float) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h, radius)
        gradient(color1, color2, x, y, w, h, gradient)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun text(text: String, x: Float, y: Float, size: Float, color: Color, font: Font) {
        println("text called: '$text' x=$x y=$y size=$size fontId=${getFontID(font)} fontName=${font.name} color=$color vg=$vg")
        nvgFontSize(vg, size)
        nvgFontFaceId(vg, getFontID(font))
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgText(vg, x, y + .5f, text)
    }

    fun textShadow(text: String, x: Float, y: Float, size: Float, color: Color, font: Font) {
        nvgFontFaceId(vg, getFontID(font))
        nvgFontSize(vg, size)
        color(Color(0, 0, 0))
        nvgFillColor(vg, nvgColor)
        nvgText(vg, round(x + 2f), round(y + 2f), text)

        color(color)
        nvgFillColor(vg, nvgColor)
        nvgText(vg, round(x), round(y), text)
    }

    fun textWidth(text: String, size: Float, font: Font): Float {
        nvgFontSize(vg, size)
        nvgFontFaceId(vg, getFontID(font))
        return nvgTextBounds(vg, 0f, 0f, text, fontBounds)
    }

    // ---- [ Other functions ] ----

    fun loadFontResource(resourcePath: String, fontName: String): Font? {
        return try {
            val location = Identifier.parse(resourcePath)
            val resource = mc.resourceManager.getResource(location)

            if (resource.isPresent) {
                Font(fontName, resource.get().open())
            } else {
                Hkim.logger.warn("Font resource not found: $resourcePath")
                null
            }
        } catch (e: Exception) {
            Hkim.logger.error("Failed to load font: $resourcePath", e)
            null
        }
    }


    private fun color(color: Color) {
        nvgRGBAf(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f,
            nvgColor
        )
    }

    private fun color(color1: Color, color2: Color) {
        nvgRGBAf(color1.red / 255f, color1.green / 255f, color1.blue / 255f, color1.alpha / 255f,
            nvgColor
        )
        nvgRGBAf(color2.red / 255f, color2.green / 255f, color2.blue / 255f, color2.alpha / 255f,
            nvgColor2
        )
    }

    private fun gradient(color1: Color, color2: Color, x: Float, y: Float, w: Float, h: Float, direction: Gradient) {
        color(color1, color2)
        when (direction) {
            Gradient.LeftToRight -> nvgLinearGradient(
                vg, x, y, x + w, y,
                nvgColor,
                nvgColor2,
                nvgPaint
            )
            Gradient.TopToBottom -> nvgLinearGradient(
                vg, x, y, x, y + h,
                nvgColor,
                nvgColor2,
                nvgPaint
            )
        }
    }

    private fun getFontID(font: Font): Int {
        return fontMap.getOrPut(font) {
            val buffer = font.buffer()
            NVGFont(nvgCreateFontMem(vg, font.name, buffer, false), buffer)
        }.id
    }

    private class Scissor(val previous: Scissor?, val x: Float, val y: Float, val maxX: Float, val maxY: Float) {
        fun applyScissor() {
            if (previous == null) nvgScissor(vg, x, y, maxX - x, maxY - y)
            else {
                val x = max(x, previous.x)
                val y = max(y, previous.y)
                val width = max(0f, (min(maxX, previous.maxX) - x))
                val height = max(0f, (min(maxY, previous.maxY) - y))
                nvgScissor(vg, x, y, width, height)
            }
        }
    }

    fun devicePixelRatio(): Float {
        return try {
            val window = mc.window
            val fbw = window.width
            val ww = window.screenWidth
            if (ww == 0) 1f else fbw.toFloat() / ww.toFloat()
        } catch (_: Throwable) {
            1f
        }
    }

    private data class NVGImage(var count: Int, val nvg: Int)
    private data class NVGFont(val id: Int, val buffer: ByteBuffer)

}