package cn.hkim.addon.utils

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
//? if 26.2 {
//? } else {
//import com.mojang.blaze3d.Blaze3D
//? }
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
//? if 26.2 {
import net.minecraft.util.Util
//? }
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import java.net.URI

object HudUtils {
    data class ScoreboardData(val title: String, val lines: List<String>)
    data class ScoreboardContent(val title: Component, val lines: List<Component>)

    fun getScoreboard(): List<String> {
        val scoreboard = mc.level?.scoreboard ?: return emptyList()
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()
        val scores = scoreboard.listPlayerScores(objective).sortedBy { it.value() }.reversed()
        return listOf(objective.displayName.cleanString) + scores.map { entry ->
            val team = scoreboard.getPlayersTeam(entry.owner)
            PlayerTeam.formatNameForTeam(team, entry.ownerName()).legacy
        }
    }

    fun getScoreboardContent(): ScoreboardContent? {
        val scoreboard = mc.level?.scoreboard ?: return null
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return null
        val scores = scoreboard.listPlayerScores(objective).sortedBy { it.value() }.reversed()
        val components = scores.map { entry ->
            val team = scoreboard.getPlayersTeam(entry.owner)
            PlayerTeam.formatNameForTeam(team, entry.ownerName())
        }
        return ScoreboardContent(objective.displayName, components)
    }

    fun getScoreboardLines(formatted: Boolean = false): ScoreboardData? {
        val content = getScoreboardContent() ?: return null
        if (formatted) {
            val cleanCodes = Regex("§[^0-9a-fk-or]")
            return ScoreboardData(
                content.title.legacy,
                content.lines.map { it.legacy.replace(cleanCodes, "") }
            )
        }
        return ScoreboardData(
            content.title.cleanString,
            content.lines.map { it.cleanString }
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

    @JvmStatic
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

    fun clamp(v: Float, lo: Float, hi: Float): Float = maxOf(lo, minOf(hi, v))

    fun isPointInRect(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Boolean =
        px in x..x + w && py in y..y + h

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
            //? if 26.2 {
            Util.getPlatform().openUri(URI.create(url))
            //? } else {
            //Blaze3D.openUri(URI.create(url))
            //? }
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
