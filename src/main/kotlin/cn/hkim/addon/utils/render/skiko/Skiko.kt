package cn.hkim.addon.utils.render.skiko

import cn.hkim.addon.runtime.AssetInstaller
import org.jetbrains.skia.*
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLengthContext
import org.joml.Matrix3x2fc
import java.awt.Color
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.max
import kotlin.math.round

object Skiko {
    private val typefaces = HashMap<SkikoFont, Typeface>()
    private val imageCache = HashMap<SkikoImage, CachedImage>()
    private var canvas: Canvas? = null
    private var frameSaveCount = 0
    private var alphaFactor = 1f

    val defaultFont = SkikoFont.system()

    private const val SVG_RASTER_SCALE = 8f
    private const val SVG_MAX_RASTER = 2048

    private fun currentCanvas(): Canvas {
        return canvas ?: throw IllegalStateException("Skiko frame has not started.")
    }

    fun beginFrame(canvas: Canvas, width: Float, height: Float, dpr: Float) {
        this.canvas = canvas
        this.alphaFactor = 1f
        frameSaveCount = canvas.save()
        canvas.clipRect(Rect.makeXYWH(0f, 0f, width, height))
        canvas.scale(dpr, dpr)
    }

    fun endFrame() {
        currentCanvas().restoreToCount(frameSaveCount)
        canvas = null
        frameSaveCount = 0
        alphaFactor = 1f
    }

    fun push() {
        currentCanvas().save()
    }

    fun pop() {
        currentCanvas().restore()
    }

    fun scale(x: Number, y: Number) {
        currentCanvas().scale(x.toFloat(), y.toFloat())
    }

    fun scale(n: Number) = scale(n, n)

    fun translate(x: Number, y: Number) {
        currentCanvas().translate(x.toFloat(), y.toFloat())
    }

    fun rotate(radians: Number) {
        currentCanvas().rotate(Math.toDegrees(radians.toDouble()).toFloat())
    }

    fun transform(matrix: Matrix3x2fc) {
        currentCanvas().concat(matrix.toSkikoMatrix())
    }

    fun globalAlpha(amount: Number) {
        alphaFactor = amount.toFloat().coerceIn(0f, 1f)
    }

    fun pushScissor(x: Number, y: Number, w: Number, h: Number) {
        currentCanvas().save()
        currentCanvas().clipRect(Rect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat()))
    }

    fun popScissor() {
        currentCanvas().restore()
    }

    fun clipRoundRect(x: Number, y: Number, w: Number, h: Number, radius: Number) {
        currentCanvas().clipRRect(
            RRect.makeLTRB(x.toFloat(), y.toFloat(), x.toFloat() + w.toFloat(), y.toFloat() + h.toFloat(), radius.toFloat()),
            ClipMode.INTERSECT
        )
    }

    fun line(x1: Number, y1: Number, x2: Number, y2: Number, thickness: Number, color: Color) {
        paint(color, PaintMode.STROKE).use {
            it.strokeWidth = thickness.toFloat()
            currentCanvas().drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), it)
        }
    }

    fun drawEdgeRoundedRect(x: Number, y: Number, w: Number, h: Number, color: Color, radius: Number, edge: SkikoRoundEdge) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fw = w.toFloat()
        val fh = h.toFloat()
        val fr = radius.toFloat().coerceAtLeast(0f)
        val radii = when (edge) {
            SkikoRoundEdge.TOP -> floatArrayOf(fr, fr, fr, fr, 0f, 0f, 0f, 0f)
            SkikoRoundEdge.BOTTOM -> floatArrayOf(0f, 0f, 0f, 0f, fr, fr, fr, fr)
            SkikoRoundEdge.LEFT -> floatArrayOf(fr, fr, 0f, 0f, 0f, 0f, fr, fr)
            SkikoRoundEdge.RIGHT -> floatArrayOf(0f, 0f, fr, fr, fr, fr, 0f, 0f)
        }

        paint(color).use {
            currentCanvas().drawRRect(RRect.makeComplexLTRB(fx, fy, fx + fw, fy + fh, radii), it)
        }
    }

    fun rect(x: Number, y: Number, w: Number, h: Number, color: Color, radius: Number) {
        paint(color).use {
            currentCanvas().drawRRect(RRect.makeLTRB(x.toFloat(), y.toFloat(), x.toFloat() + w.toFloat(), y.toFloat() + h.toFloat(), radius.toFloat()), it)
        }
    }

    fun rect(x: Number, y: Number, w: Number, h: Number, color: Color) {
        paint(color).use {
            currentCanvas().drawRect(Rect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat()), it)
        }
    }

    fun hollowRect(x: Number, y: Number, w: Number, h: Number, thickness: Number, color: Color, radius: Number) {
        paint(color, PaintMode.STROKE).use {
            it.strokeWidth = thickness.toFloat()
            currentCanvas().drawRRect(RRect.makeLTRB(x.toFloat(), y.toFloat(), x.toFloat() + w.toFloat(), y.toFloat() + h.toFloat(), radius.toFloat()), it)
        }
    }

    fun gradientRect(
        x: Number,
        y: Number,
        w: Number,
        h: Number,
        color1: Color,
        color2: Color,
        gradient: SkikoGradient,
        radius: Float
    ) {
        paint(Color.WHITE).use { fill ->
            fill.shader = linearGradient(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color1, color2, gradient)
            currentCanvas().drawRRect(RRect.makeLTRB(x.toFloat(), y.toFloat(), x.toFloat() + w.toFloat(), y.toFloat() + h.toFloat(), radius), fill)
        }
    }

    fun gradientRectMulti(
        x: Number,
        y: Number,
        w: Number,
        h: Number,
        colors: List<Color>,
        positions: FloatArray? = null,
        gradient: SkikoGradient,
        radius: Float
    ) {
        if (colors.size < 2) return
        paint(Color.WHITE).use { fill ->
            fill.shader = multiLinearGradient(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), colors, positions, gradient)
            currentCanvas().drawRRect(RRect.makeLTRB(x.toFloat(), y.toFloat(), x.toFloat() + w.toFloat(), y.toFloat() + h.toFloat(), radius), fill)
        }
    }

    fun dropShadow(
        x: Number, y: Number, width: Number, height: Number,
        blur: Number, spread: Number, radius: Number,
        color: Color = Color(0, 0, 0, 125)
    ) {
        paint(color).use { p ->
            p.maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blur.toFloat(), true)
            val spreadF = spread.toFloat()
            currentCanvas().drawRRect(
                RRect.makeLTRB(
                    x.toFloat() - spreadF,
                    y.toFloat() - spreadF,
                    x.toFloat() + width.toFloat() + spreadF,
                    y.toFloat() + height.toFloat() + spreadF,
                    radius.toFloat()
                ),
                p
            )
        }
    }

    fun circle(x: Number, y: Number, radius: Number, color: Color) {
        paint(color).use {
            currentCanvas().drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), it)
        }
    }

    fun text(text: String, x: Number, y: Number, size: Number, color: Color, font: SkikoFont = defaultFont) {
        paint(color).use { fill ->
            val skiaFont = skikoFont(font, size.toFloat())
            currentCanvas().drawString(text, x.toFloat(), y.toFloat() - skiaFont.metrics.ascent, skiaFont, fill)
        }
    }

    fun textGradient(
        text: String,
        x: Number,
        y: Number,
        size: Number,
        width: Number,
        color1: Color,
        color2: Color,
        font: SkikoFont = defaultFont,
        direction: SkikoGradient = SkikoGradient.LEFT_RIGHT
    ) {
        if (text.isEmpty()) return

        paint(Color.WHITE).use { fill ->
            val sizeF = size.toFloat()
            fill.shader = linearGradient(x.toFloat(), y.toFloat() - sizeF, width.toFloat(), sizeF, color1, color2, direction)
            val skiaFont = skikoFont(font, sizeF)
            currentCanvas().drawString(text, x.toFloat(), y.toFloat() - skiaFont.metrics.ascent, skiaFont, fill)
        }
    }

    fun textShadow(text: String, x: Number, y: Number, size: Number, color: Color, font: SkikoFont = defaultFont) {
        text(text, round(x.toFloat() + 2f), round(y.toFloat() + 2f), size, Color.BLACK, font)
        text(text, round(x.toFloat()), round(y.toFloat()), size, color, font)
    }

    fun textWidth(text: String, size: Number, font: SkikoFont = defaultFont): Float {
        return skikoFont(font, size.toFloat()).measureTextWidth(text)
    }

    fun textHeight(size: Number, font: SkikoFont = defaultFont): Float {
        val metrics = skikoFont(font, size.toFloat()).metrics
        return metrics.descent - metrics.ascent
    }

    fun drawWrappedString(
        text: String,
        x: Number,
        y: Number,
        w: Number,
        size: Number,
        color: Color,
        font: SkikoFont = defaultFont,
        lineHeight: Number = 1f
    ) {
        var cursorY = y.toFloat()
        val spacing = size.toFloat() * lineHeight.toFloat()
        wrap(text, w.toFloat(), size.toFloat(), font).forEach { line ->
            text(line, x, cursorY, size, color, font)
            cursorY += spacing
        }
    }

    fun createImage(resourcePath: String): SkikoImage {
        val image = imageCache.keys.find { it.location == resourcePath } ?: SkikoImage(resourcePath)
        val cached = imageCache.getOrPut(image) { CachedImage(0, loadImage(image)) }
        cached.count++
        return image
    }

    fun deleteImage(image: SkikoImage) {
        val cached = imageCache[image] ?: return
        cached.count--
        if (cached.count > 0) return

        cached.image.close()
        imageCache.remove(image)
    }

    fun image(image: SkikoImage, x: Number, y: Number, w: Number, h: Number, radius: Number, tint: Int? = null) {
        val rect = Rect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
        val canvas = currentCanvas()
        canvas.save()
        canvas.clipRRect(
            RRect.makeLTRB(x.toFloat(), y.toFloat(), x.toFloat() + w.toFloat(), y.toFloat() + h.toFloat(), radius.toFloat()),
            ClipMode.INTERSECT
        )
        if (tint != null) {
            paint(Color.WHITE).use { p ->
                p.colorFilter = ColorFilter.makeBlend(tint, BlendMode.SRC_IN)
                canvas.drawImageRect(getImage(image), rect, p)
            }
        }
        else {
            canvas.drawImageRect(getImage(image), rect)
        }
        canvas.restore()
    }

    fun image(path: String, x: Number, y: Number, w: Number, h: Number, radius: Number, tint: Int? = null) {
        val existing = imageCache.keys.find { it.location == path }
        image(existing ?: createImage(path), x, y, w, h, radius, tint)
    }

    fun image(image: SkikoImage, x: Number, y: Number, w: Number, h: Number) {
        currentCanvas().drawImageRect(getImage(image), Rect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat()))
    }

    private fun getImage(image: SkikoImage): Image {
        return imageCache[image]?.image ?: throw IllegalStateException("Image (${image.location}) doesn't exist")
    }

    private fun loadImage(image: SkikoImage): Image {
        if (!image.isSvg) return Image.makeFromEncoded(image.bytes)

        val data = Data.makeFromBytes(image.bytes)
        val dom = SVGDOM(data)
        val root = dom.root ?: throw IllegalStateException("Failed to read SVG root: ${image.location}")
        val intrinsic = root.getIntrinsicSize(SVGLengthContext(256f, 256f, 96f))
        val intrinsicW = max(1, intrinsic.x.toInt())
        val intrinsicH = max(1, intrinsic.y.toInt())

        val viewBox = root.viewBox
        val contentW = if (viewBox != null && viewBox.width > 0f) viewBox.width else intrinsicW.toFloat()
        val contentH = if (viewBox != null && viewBox.height > 0f) viewBox.height else intrinsicH.toFloat()
        val scale = minOf(SVG_RASTER_SCALE, SVG_MAX_RASTER / maxOf(contentW, contentH))
        val width = max(1, (contentW * scale).toInt())
        val height = max(1, (contentH * scale).toInt())

        val surface = Surface.makeRaster(ImageInfo(width, height, ColorType.N32, ColorAlphaType.PREMUL, ColorSpace.sRGB))
        val canvas = surface.canvas
        canvas.clear(0)
        canvas.save()
        canvas.scale(width / intrinsicW.toFloat(), height / intrinsicH.toFloat())
        dom.setContainerSize(width.toFloat(), height.toFloat())
        dom.render(canvas)
        canvas.restore()
        val snapshot = surface.makeImageSnapshot()
        surface.close()
        dom.close()
        data.close()
        return snapshot
    }

    fun skikoFont(font: SkikoFont, size: Float): Font {
        val typeface = typefaces.getOrPut(font) { resolveTypeface(font) }
        return Font(typeface, size).apply { edging = FontEdging.SUBPIXEL_ANTI_ALIAS }
    }

    private fun resolveTypeface(font: SkikoFont): Typeface {
        val manager = FontMgr.default
        return when {
            font.family != null -> {
                val set = manager.matchFamily(font.family)
                if (font.bold && set.count() > 0) {
                    manager.matchFamilyStyle(font.family, FontStyle.BOLD)
                        ?: set.getTypeface(0) ?: Typeface.makeEmpty()
                }
                else if (set.count() > 0) set.getTypeface(0) ?: Typeface.makeEmpty()
                else Typeface.makeEmpty()
            }
            font.location != null ->
                manager.makeFromData(Data.makeFromBytes(font.bytes)) ?: Typeface.makeEmpty()
            else -> {
                val files = externalFontFiles(font.bold)
                files.firstNotNullOfOrNull { file ->
                    manager.makeFromData(Data.makeFromBytes(Files.readAllBytes(file)))
                } ?: throw IllegalStateException(
                    "No loadable font files in ${AssetInstaller.fontsDir()} — " +
                        "PreLaunch should have installed them; restart the game"
                )
            }
        }
    }

    private fun externalFontFiles(bold: Boolean): List<Path> {
        val dir = AssetInstaller.fontsDir()
        if (!Files.isDirectory(dir)) return emptyList()
        val fonts = Files.list(dir).use { stream ->
            stream.filter { path ->
                val name = path.fileName.toString().lowercase()
                name.endsWith(".ttf") || name.endsWith(".otf")
            }.sorted().toList()
        }
        if (fonts.isEmpty()) return emptyList()
        val (boldFonts, regularFonts) = fonts.partition {
            it.fileName.toString().lowercase().contains("bold")
        }
        return if (bold) boldFonts + regularFonts else regularFonts + boldFonts
    }

    private fun paint(color: Color, mode: PaintMode = PaintMode.FILL): Paint {
        return Paint()
            .setARGB(color.alpha, color.red, color.green, color.blue)
            .setAlphaf((color.alpha / 255f) * alphaFactor)
            .also {
                it.mode = mode
                it.isAntiAlias = true
            }
    }

    private fun linearGradient(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color1: Color,
        color2: Color,
        direction: SkikoGradient
    ): Shader {
        return multiLinearGradient(x, y, w, h, listOf(color1, color2), null, direction)
    }

    private fun multiLinearGradient(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        colors: List<Color>,
        positions: FloatArray?,
        direction: SkikoGradient
    ): Shader {
        val (x0, y0, x1, y1) = when (direction) {
            SkikoGradient.LEFT_RIGHT -> floatArrayOf(x, y, x + w, y)
            SkikoGradient.TOP_BOTTOM -> floatArrayOf(x, y, x, y + h)
        }
        val gradient = Gradient(
            Gradient.Colors(colors.map { it.toColor4f() }.toTypedArray(), positions, FilterTileMode.CLAMP, null),
            Gradient.Interpolation()
        )
        return Shader.makeLinearGradient(x0, y0, x1, y1, gradient, null)
    }

    private fun Color.toColor4f(): Color4f {
        return Color4f(red / 255f, green / 255f, blue / 255f, alpha / 255f)
    }

    fun wrap(text: String, maxWidth: Float, size: Float, font: SkikoFont = defaultFont): List<String> {
        val lines = mutableListOf<String>()
        var line = StringBuilder()

        fun flush() {
            if (line.isNotEmpty()) {
                lines.add(line.toString())
                line = StringBuilder()
            }
        }

        fun width(s: String) = textWidth(s, size, font)

        fun appendWord(word: String, spaceBefore: Boolean) {
            if (spaceBefore && line.isNotEmpty()) {
                val candidate = "$line $word"
                if (width(candidate) <= maxWidth) {
                    line.append(' ').append(word)
                    return
                }
                flush()
            }
            for (ch in word) {
                if (line.isNotEmpty() && width(line.toString() + ch) > maxWidth) {
                    flush()
                }
                line.append(ch)
            }
        }

        val words = text.split(' ')
        words.forEachIndexed { index, word -> appendWord(word, index > 0) }
        flush()
        return lines
    }
    
    private fun Matrix3x2fc.toSkikoMatrix(): Matrix33 {
        return Matrix33(
            m00(), m10(), m20(),
            m01(), m11(), m21(),
            0f, 0f, 1f
        )
    }

    private data class CachedImage(var count: Int, val image: Image)
}