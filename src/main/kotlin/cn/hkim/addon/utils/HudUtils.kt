package cn.hkim.addon.utils

import cn.hkim.addon.Hkim
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.Util
import java.awt.Color
import java.net.URI
import kotlin.math.sin

object HudUtils {
    fun GuiGraphicsExtractor.drawRectWithBorder(x: Float, y: Float, width: Float, height: Float, fillColor: Int, borderColor: Int? = null, borderWidth: Int = 1) {
        fill(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), fillColor)

        if (borderColor != null) {
            fill(x.toInt(), y.toInt(), (x + width).toInt(), y.toInt() + borderWidth, borderColor)
            fill(x.toInt(), (y + height - borderWidth).toInt(), (x + width).toInt(), (y + height).toInt(), borderColor)
            fill(x.toInt(), y.toInt(), x.toInt() + borderWidth, (y + height).toInt(), borderColor)
            fill((x + width - borderWidth).toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), borderColor)
        }
    }

    fun GuiGraphicsExtractor.drawVerticalSeparator(x: Float, y: Float, height: Float, color: Int, width: Int = 1) {
        fill(x.toInt(), y.toInt(), x.toInt() + width, (y + height).toInt(), color)
    }

    fun GuiGraphicsExtractor.drawHorizontalSeparator(x: Float, y: Float, width: Float, color: Int, height: Int = 1) {
        fill(x.toInt(), y.toInt(), (x + width).toInt(), y.toInt() + height, color)
    }

    fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t.coerceIn(0f, 1f)

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

    fun isPointInRect(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Boolean =
        px in x..x + w && py in y..y + h

    fun GuiGraphicsExtractor.hollowFill(x: Int, y: Int, width: Int, height: Int, thickness: Int, color: Color) {
        fill(x, y, x + width, y + thickness, color.rgb)
        fill(x, y + height - thickness, x + width, y + height, color.rgb)
        fill(x, y + thickness, x + thickness, y + height - thickness, color.rgb)
        fill(x + width - thickness, y + thickness, x + width, y + height - thickness, color.rgb)
    }

    fun Color.multiplyAlpha(factor: Float): Color {
        return Color(red, green, blue, (alpha.toFloat() * factor).coerceIn(0f, 255f).toInt())
    }

    fun openUrl(url: String) {
        try {
            Util.getPlatform().openUri(URI.create(url))
        } catch (e: Exception) {
            Hkim.logger.error("Failed to open URL: $url", e)
        }
    }

    fun playModuleSound(state: Boolean) {
        val enable = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("hkim", "enable"))
        val disable = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("hkim", "disable"))
        if (state) playSoundAtPlayer(enable, 0.7f, 0.7f)
        else playSoundAtPlayer(disable, 0.7f, 0.7f)
    }
}

object Colors {
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
    @JvmField val BLACK = Color(0, 0, 0)
}