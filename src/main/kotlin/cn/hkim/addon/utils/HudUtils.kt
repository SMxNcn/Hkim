package cn.hkim.addon.utils

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Util
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import java.awt.Color
import java.net.URI
import kotlin.math.sin

object HudUtils {
    data class ScoreboardData(val title: String, val lines: List<String>)

    fun getScoreboard(): List<String> {
        val scoreboard = mc.level?.scoreboard ?: return emptyList()
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()
        val scores = scoreboard.listPlayerScores(objective).sortedBy { it.value() }.reversed()
        return listOf(objective.displayName.cleanString) + scores.map { entry ->
            val team = scoreboard.getPlayersTeam(entry.owner)
            PlayerTeam.formatNameForTeam(team, entry.ownerName()).legacy
        }
    }

    fun getScoreboardLines(formatted: Boolean = false): ScoreboardData? {
        val scoreboard = mc.level?.scoreboard ?: return null
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return null
        val scores = scoreboard.listPlayerScores(objective).sortedBy { it.value() }.reversed()
        val components = scores.map { entry ->
            val team = scoreboard.getPlayersTeam(entry.owner)
            PlayerTeam.formatNameForTeam(team, entry.ownerName())
        }
        if (formatted) {
            val cleanCodes = Regex("§[^0-9a-fk-or]")
            return ScoreboardData(
                objective.displayName.legacy,
                components.map { it.legacy.replace(cleanCodes, "") }
            )
        }
        return ScoreboardData(
            objective.displayName.cleanString,
            components.map { it.cleanString }
        )
    }

    private fun GuiGraphicsExtractor.renderScaledText(
        renderer: (Int, Int) -> Unit,
        x: Int,
        y: Int,
        scale: Float
    ) {
        if (scale == 1.0f) {
            renderer(x, y)
            return
        }

        this.pose().pushMatrix()
        this.pose().scale(scale, scale)
        renderer((x / scale).toInt(), (y / scale).toInt())
        this.pose().popMatrix()
    }

    fun GuiGraphicsExtractor.scaledText(
        font: Font,
        text: Component,
        x: Int,
        y: Int,
        color: Int,
        shadow: Boolean = false,
        scale: Float = 1.0f
    ) {
        renderScaledText({ sx, sy ->
            this.text(font, text, sx, sy, color, shadow)
        }, x, y, scale)
    }

    fun GuiGraphicsExtractor.scaledText(
        font: Font,
        text: String,
        x: Int,
        y: Int,
        color: Int,
        shadow: Boolean = false,
        scale: Float = 1.0f
    ) {
        renderScaledText({ sx, sy ->
            this.text(font, text, sx, sy, color, shadow)
        }, x, y, scale)
    }

    fun GuiGraphicsExtractor.gradientText(
        font: Font,
        text: String,
        x: Int,
        y: Int,
        startColor: Int,
        endColor: Int,
        chromaSpeed: Int = 5,
        chromaOffset: Int = 2,
        shadow: Boolean = false,
    ) {
        if (text.isEmpty()) return
        val start = Color(startColor)
        val end = Color(endColor)
        var currentX = x
        for (i in text.indices) {
            val ch = text[i].toString()
            val color = getChromaColor(start, end, i, chromaSpeed, chromaOffset).rgb
            this.text(font, ch, currentX, y, color, shadow)
            currentX += font.width(ch)
        }
    }

    fun GuiGraphicsExtractor.drawRectWithBorder(x: Float, y: Float, width: Float, height: Float, fillColor: Int, borderColor: Int? = null, borderWidth: Float = 1f) {
        this.fill(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), fillColor)

        if (borderColor != null) {
            this.drawHorizontalLine(x, y, width, borderColor, borderWidth)
            this.drawHorizontalLine(x, y + height - borderWidth, width, borderColor, borderWidth)
            this.drawVerticalLine(x, y, height, borderColor, borderWidth)
            this.drawVerticalLine(x + width - borderWidth, y, height, borderColor, borderWidth)
        }
    }

    fun GuiGraphicsExtractor.drawVerticalSeparator(x: Float, y: Float, height: Float, color: Int, width: Int = 1) {
        fill(x.toInt(), y.toInt(), x.toInt() + width, (y + height).toInt(), color)
    }

    fun GuiGraphicsExtractor.drawHorizontalSeparator(x: Float, y: Float, width: Float, color: Int, height: Int = 1) {
        fill(x.toInt(), y.toInt(), (x + width).toInt(), y.toInt() + height, color)
    }

    fun GuiGraphicsExtractor.drawVerticalLine(x: Float, y: Float, height: Float, color: Int, lineWidth: Float = 1f) {
        if (lineWidth <= 0f || height <= 0f) return
        val intW = lineWidth.toInt()
        if (lineWidth == intW.toFloat()) {
            fill(x.toInt(), y.toInt(), x.toInt() + intW, (y + height).toInt(), color)
            return
        }

        this.pose().pushMatrix()
        this.pose().translate(x, y)
        this.pose().scale(lineWidth, 1f)
        this.fill(0, 0, 1, height.toInt(), color)
        this.pose().popMatrix()
    }

    fun GuiGraphicsExtractor.drawHorizontalLine(x: Float, y: Float, width: Float, color: Int, lineWidth: Float = 1f) {
        if (lineWidth <= 0f || width <= 0f) return
        val intW = lineWidth.toInt()
        if (lineWidth == intW.toFloat()) {
            fill(x.toInt(), y.toInt(), (x + width).toInt(), y.toInt() + intW, color)
            return
        }

        this.pose().pushMatrix()
        this.pose().translate(x, y)
        this.pose().scale(1f, lineWidth)
        this.fill(0, 0, width.toInt(), 1, color)
        this.pose().popMatrix()
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

    fun Color.multiplyAlpha(factor: Float): Color {
        return Color(red, green, blue, (alpha.toFloat() * factor).coerceIn(0f, 255f).toInt())
    }

    val Color.rGL: Float get() = red / 255f
    val Color.gGL: Float get() = green / 255f
    val Color.bGL: Float get() = blue / 255f
    val Color.aGL: Float get() = alpha / 255f

    inline val mouseX: Float
        get() = mc.mouseHandler.xpos().toFloat()

    inline val mouseY: Float
        get() = mc.mouseHandler.ypos().toFloat()

    fun getQuadrant(x: Int, y: Int): Int =
        when {
            x >= mc.window.guiScaledWidth / 2 -> if (y >= mc.window.guiScaledHeight / 2) 4 else 2
            else -> if (y >= mc.window.guiScaledHeight / 2) 3 else 1
        }

    fun openUrl(url: String) {
        try {
            Util.getPlatform().openUri(URI.create(url))
        } catch (e: Exception) {
            Hkim.logger.error("Failed to open URL: $url", e)
        }
    }

    fun setTitle(title: String) {
        mc.gui.hud.setTimes(0, 20, 5)
        mc.gui.hud.setTitle(Component.literal(title))
    }

    fun alert(title: String, playSound: Boolean = true) {
        setTitle(title)
        if (playSound) playSoundAtPlayer(SoundEvents.NOTE_BLOCK_PLING.value())
    }

    fun playModuleSound(state: Boolean) {
        val enable = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("hkim", "enable"))
        val disable = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("hkim", "disable"))
        if (state) playSoundAtPlayer(enable, 0.7f, 0.7f)
        else playSoundAtPlayer(disable, 0.7f, 0.7f)
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