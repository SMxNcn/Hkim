package cn.hkim.addon.utils

import cn.hkim.addon.utils.HudUtils.lerp
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.awt.Color
import java.util.Optional
import kotlin.math.roundToInt
import kotlin.math.sin

object ColorUtils {
    fun lerpColor(start: Int, end: Int, t: Float): Int {
        fun extractChannel(c: Int, shift: Int) = (c shr shift) and 0xFF
        fun mergeChannel(c: Int, shift: Int) = c shl shift

        val startA = extractChannel(start, 24); val endA = extractChannel(end, 24)
        val startR = extractChannel(start, 16); val endR = extractChannel(end, 16)
        val startG = extractChannel(start, 8);  val endG = extractChannel(end, 8)
        val startB = extractChannel(start, 0);  val endB = extractChannel(end, 0)

        val a = lerp(startA.toFloat(), endA.toFloat(), t).toInt()
        val r = lerp(startR.toFloat(), endR.toFloat(), t).toInt()
        val g = lerp(startG.toFloat(), endG.toFloat(), t).toInt()
        val b = lerp(startB.toFloat(), endB.toFloat(), t).toInt()

        return mergeChannel(a, 24) or mergeChannel(r, 16) or mergeChannel(g, 8) or mergeChannel(b, 0)
    }

    fun hsbOf(argb: Int): FloatArray {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val d = max - min
        var h = when {
            d == 0f -> 0f
            max == r -> (((g - b) / d) % 6f) / 6f
            max == g -> ((b - r) / d + 2f) / 6f
            else -> ((r - g) / d + 4f) / 6f
        }
        if (h < 0f) h += 1f
        val s = if (max == 0f) 0f else d / max
        return floatArrayOf(h, s, max)
    }

    fun hsvToRgb(h: Float, s: Float, v: Float): Int {
        if (s <= 0f) {
            val gray = (v * 255f + 0.5f).toInt()
            return rgb(gray, gray, gray)
        }
        val hh = (h - h.toInt()) * 6f
        val sector = hh.toInt()
        val f = hh - sector
        val p = (v * (1f - s) * 255f + 0.5f).toInt()
        val q = (v * (1f - s * f) * 255f + 0.5f).toInt()
        val t = (v * (1f - s * (1f - f)) * 255f + 0.5f).toInt()
        val v255 = (v * 255f + 0.5f).toInt()
        return when (sector % 6) {
            0 -> rgb(v255, t, p)
            1 -> rgb(q, v255, p)
            2 -> rgb(p, v255, t)
            3 -> rgb(p, q, v255)
            4 -> rgb(t, p, v255)
            else -> rgb(v255, p, q)
        }
    }

    private fun rgb(r: Int, g: Int, b: Int): Int = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b

    fun alphaOf(argb: Int): Int = (argb shr 24) and 0xFF

    fun alphaToPercent(a: Int): Int = (a * 100f / 255f).roundToInt()

    fun percentToAlpha(p: Int): Int = (p * 255f / 100f).roundToInt()

    fun toHexString(color: Int): String {
        val a = (color shr 24) and 0xFF
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return String.format("#%02X%02X%02X%02X", r, g, b, a)
    }

    fun toHexStringRGB(color: Int): String {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return String.format("#%02X%02X%02X", r, g, b)
    }

    fun fromHexString(hex: String): Int? {
        val clean = hex.replace("#", "").trim().uppercase()
        return try {
            when (clean.length) {
                6 -> {
                    val rgb = clean.toLong(16).toInt()
                    rgb or (0xFF shl 24)
                }
                8 -> {
                    val r = clean.substring(0, 2).toInt(16)
                    val g = clean.substring(2, 4).toInt(16)
                    val b = clean.substring(4, 6).toInt(16)
                    val a = clean.substring(6, 8).toInt(16)
                    (a shl 24) or (r shl 16) or (g shl 8) or b
                }
                else -> null
            }
        } catch (_: Exception) { null }
    }

    fun getChromaColor(start: Color, end: Color, index: Int, speed: Int, offset: Int): Color {
        val currentTime = System.nanoTime() / 1_000_000_000.0

        val cOffset = index * (offset / 100.0)
        val cSpeed = speed / 10.0

        val phase = (currentTime * cSpeed + cOffset) * 2.0 * Math.PI
        val progress = (0.5 + 0.5 * sin(phase)).toFloat()

        val r = (start.red + (end.red - start.red) * progress).toInt().coerceIn(0, 255)
        val g = (start.green + (end.green - start.green) * progress).toInt().coerceIn(0, 255)
        val b = (start.blue + (end.blue - start.blue) * progress).toInt().coerceIn(0, 255)

        return Color(r, g, b)
    }

    fun Color.multiplyAlpha(factor: Float): Color {
        return Color(red, green, blue, (alpha.toFloat() * factor).coerceIn(0f, 255f).toInt())
    }

    fun Color.multiplyShade(shade: Float): Color {
        val f = 1f - shade.coerceIn(0f, 1f)
        return Color((red * f).toInt(), (green * f).toInt(), (blue * f).toInt(), alpha)
    }

    val Color.rGL: Float get() = red / 255f
    val Color.gGL: Float get() = green / 255f
    val Color.bGL: Float get() = blue / 255f
    val Color.aGL: Float get() = alpha / 255f

    fun GuiGraphicsExtractor.gradientText(
        font: Font,
        text: String,
        x: Int,
        y: Int,
        startColor: Int,
        endColor: Int,
        chromaSpeed: Int = 5,
        shadow: Boolean = false,
    ) {
        if (text.isEmpty()) return
        val chromaText = buildGradientComponent(text, startColor, endColor, chromaSpeed)

        this.text(font, chromaText, x, y, 0xFFFFFFFF.toInt(), shadow)
    }
}

object Colors {
    @JvmField val BLACK = Color(0, 0, 0)
    @JvmField val MINECRAFT_DARK_BLUE = Color(0, 0, 170)
    @JvmField val MINECRAFT_DARK_GREEN = Color(0, 170, 0)
    @JvmField val MINECRAFT_DARK_AQUA = Color(0, 170, 170)
    @JvmField val MINECRAFT_DARK_RED = Color(170, 0, 0)
    @JvmField val MINECRAFT_DARK_PURPLE = Color(170, 0, 170)
    @JvmField val MINECRAFT_GOLD = Color(255, 170, 0)
    @JvmField val MINECRAFT_GRAY = Color(170, 170, 170)
    @JvmField val MINECRAFT_DARK_GRAY = Color(85, 85, 85)
    @JvmField val MINECRAFT_BLUE = Color(85, 85, 255)
    @JvmField val MINECRAFT_GREEN = Color(85, 255, 85)
    @JvmField val MINECRAFT_AQUA = Color(85, 255, 255)
    @JvmField val MINECRAFT_RED = Color(255, 85, 85)
    @JvmField val MINECRAFT_LIGHT_PURPLE = Color(255, 85, 255)
    @JvmField val MINECRAFT_YELLOW = Color(255, 255, 85)
    @JvmField val WHITE = Color(255, 255, 255)
}

private val LEGACY_COLOR_CODES: Map<Int, String> = ChatFormatting.entries
    .mapNotNull { format ->
        TextColor.fromLegacyFormat(format)?.let { color -> color.value to format.toString() }
    }
    .toMap()

internal fun getLegacyColorCode(rgb: Int): String? = LEGACY_COLOR_CODES[rgb]

fun colorCodeBefore(text: String, index: Int): String? {
    var i = index
    while (i >= 2 && text[i - 2] == '§') {
        val code = text[i - 1]
        val format = ChatFormatting.getByCode(code) ?: return null
        if (TextColor.fromLegacyFormat(format) != null) return "§$code"
        i -= 2
    }
    return null
}

data class LegacyTextRun(val text: String, val color: Int, val bold: Boolean)

/** Splits a legacy (section-sign) string into visible text runs, tracking color and bold. */
fun parseLegacyRuns(text: String, baseColor: Int, baseBold: Boolean = false): List<LegacyTextRun> {
    if (text.indexOf('§') < 0) return listOf(LegacyTextRun(text, baseColor, baseBold))

    val runs = ArrayList<LegacyTextRun>()
    val current = StringBuilder()
    var color = baseColor
    var bold = baseBold

    fun flush() {
        if (current.isEmpty()) return
        runs += LegacyTextRun(current.toString(), color, bold)
        current.setLength(0)
    }

    var i = 0
    while (i < text.length) {
        val ch = text[i]
        if (ch == '§' && i + 1 < text.length) {
            val format = ChatFormatting.getByCode(text[i + 1])
            if (format != null) {
                if (format == ChatFormatting.RESET) {
                    flush()
                    color = baseColor
                    bold = baseBold
                } else {
                    val textColor = TextColor.fromLegacyFormat(format)
                    if (textColor != null) {
                        flush()
                        color = 0xFF000000.toInt() or (textColor.value and 0xFFFFFF)
                        bold = baseBold
                    } else if (format == ChatFormatting.BOLD) {
                        flush()
                        bold = true
                    }
                }
                i += 2
                continue
            }
        }
        current.append(ch)
        i++
    }
    flush()
    return runs
}

fun Component.hasNonLegacyColor(): Boolean {
    var extended = false
    this.visit({ style: Style, _: String ->
        val rgb = style.color?.value
        if (rgb != null && getLegacyColorCode(rgb) == null) extended = true
        Optional.empty<Unit>()
    }, Style.EMPTY)
    return extended
}

private val INVALID_LEGACY_CODE = Regex("§[^0-9a-fk-or]")

fun Component.toLegacyOrComponent(): Component =
    if (hasNonLegacyColor()) this else Component.literal(legacy.replace(INVALID_LEGACY_CODE, ""))

fun String.withColor(color: Int) = Component.literal(this).withColor(color)

inline val Int.shadow
    get() = (this and 0xFF000000.toInt()) or ((this and 0x00FCFCFC) ushr 2)

/** Build a [Component] where each visible character gets a chroma-gradient color between [startArgb] and [endArgb],
 *  with accumulated §-style formatting (§lomnkr). Set [speed] to 0 for a static gradient. */
fun buildGradientComponent(text: String, startArgb: Int, endArgb: Int, speed: Int): Component {
    val startCol = Color(startArgb and 0xFFFFFF, false)
    val endCol = Color(endArgb and 0xFFFFFF, false)
    val parts = mutableListOf<Component>()
    var style = Style.EMPTY
    var charPos = 0
    val chars = text.toCharArray()
    var i = 0
    while (i < chars.size) {
        if (chars[i] == '§' && i + 1 < chars.size) {
            style = when (chars[i + 1]) {
                'l' -> style.withBold(true)
                'o' -> style.withItalic(true)
                'n' -> style.withUnderlined(true)
                'm' -> style.withStrikethrough(true)
                'k' -> style.withObfuscated(true)
                'r' -> Style.EMPTY
                else -> style
            }
            i += 2
            continue
        }
        val gradCol = ColorUtils.getChromaColor(startCol, endCol, charPos, speed, 2)
        val charStyle = style.withColor(TextColor.fromRgb(gradCol.rgb))
        parts.add(Component.literal(chars[i].toString()).withStyle(charStyle))
        charPos++
        i++
    }
    return if (parts.isEmpty()) Component.literal("")
    else {
        val root = Component.literal("")
        for (part in parts) root.append(part)
        root
    }
}


class ParsedColoredText(val plain: String, val component: Component, val formatting: String) {
    val legacy: String by lazy { component.legacy }
    val hasExtendedColor: Boolean by lazy { component.hasNonLegacyColor() }
    fun asComponent(): Component = if (hasExtendedColor) component else Component.literal(legacy)
}

private fun hexValueAt(text: String, start: Int): Int? {
    if (start + 6 > text.length) return null
    for (i in start until start + 6) {
        if (text[i].digitToIntOrNull(16) == null) return null
    }
    return text.substring(start, start + 6).toInt(16)
}

fun parseColoredText(text: String): ParsedColoredText {
    val plain = StringBuilder(text.length)
    val formatting = StringBuilder(text.length + 8)
    val root = Component.empty()
    val pending = StringBuilder()
    var style = Style.EMPTY

    fun flush() {
        if (pending.isEmpty()) return
        root.append(Component.literal(pending.toString()).withStyle(style))
        pending.setLength(0)
    }

    fun applyColor(rgb: Int) {
        flush()
        style = Style.EMPTY.withColor(rgb)
        formatting.append("§r")
    }

    var i = 0
    while (i < text.length) {
        val ch = text[i]
        if ((ch == '&' || ch == '§') && i + 1 < text.length) {
            val code = text[i + 1]
            val format = ChatFormatting.getByCode(code)
            if (format != null) {
                flush()
                style = style.applyLegacyFormat(format)
                val c = code.lowercaseChar()
                if (c in 'k'..'o' || c == 'r') formatting.append('§').append(c)
                else formatting.append("§r")
                i += 2
                continue
            }
            if (code == '#') {
                val rgb = hexValueAt(text, i + 2)
                if (rgb != null) {
                    applyColor(rgb)
                    i += 8
                    continue
                }
            }
        }
        plain.append(ch)
        formatting.append(ch)
        pending.append(ch)
        i++
    }
    flush()
    return ParsedColoredText(plain.toString(), root, formatting.toString())
}
