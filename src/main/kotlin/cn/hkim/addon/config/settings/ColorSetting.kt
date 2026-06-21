package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.ClickGUIScreen
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.pip.ShapeRenderer.drawRoundedRectWithBorder
import com.mojang.blaze3d.platform.cursor.CursorType
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents

class ColorSetting(name: String, desc: String, default: Int) : Setting<Int>(name, desc, default) {
    init { value = default }

    override fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int,
        delta: Float, visibleTop: Float, visibleBottom: Float
    ): Float {
        val height = 20f
        val isHovered = (visibleTop == -1f || mouseY in visibleTop..visibleBottom) && HudUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

        if (isHovered) {
            graphics.drawRoundedRectWithBorder(x, y, width, height, 0x15FFFFFF, 0, 0f, 3f)
        }

        graphics.text(mc.font, name, x.toInt() + 10, y.toInt() + 6, 0xFFCCCCCC.toInt(), false)

        val previewSize = 16f
        val previewX = x + width - previewSize - 10f
        val previewY = y + 2f

        val hexText = toHexString(value)
        val screen = mc.gui.screen()
        val isActive = screen is ClickGUIScreen && screen.activeEditBoxSetting == this

        graphics.drawRoundedRectWithBorder(previewX, previewY, 16f, 16f, value or 0xFF000000.toInt(), 0xFF444444.toInt(), 1f, 3f)

        if (isActive) {
            val editBox = screen.activeEditBox
            if (editBox != null) {
                val tw = mc.font.width(hexText)
                val rx = (previewX - tw - 14).toInt()
                val ry = y.toInt() + 6
                editBox.setPosition(rx, ry)
                editBox.setSize(tw + 12, 16)
                if (editBox.isFocused) graphics.requestCursor(CursorType.DEFAULT)
                editBox.extractWidgetRenderState(graphics, mouseX.toInt(), mouseY.toInt(), delta)
            }
        } else {
            val hexAreaX = (previewX - mc.font.width(hexText) - 14).toInt()
            val hexAreaW = mc.font.width(hexText) + 8
            if (HudUtils.isPointInRect(mouseX, mouseY, (hexAreaX - 4).toFloat(), previewY, hexAreaW.toFloat(), 16f)) {
                graphics.requestCursor(CursorTypes.POINTING_HAND)
            }
            graphics.text(mc.font, hexText, hexAreaX, y.toInt() + 6, value or 0xFF000000.toInt(), false)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (button != 0) return false

        val previewX = x + width - 16f - 10f
        val previewY = y + 2f
        val text = toHexString(value)
        val tw = mc.font.width(text)
        val rx = (previewX - tw - 14).toInt()
        val ry = y.toInt() + 6

        if (HudUtils.isPointInRect(mouseX, mouseY, (rx - 4).toFloat(), previewY, (tw + 8).toFloat(), 16f)) {
            val screen = mc.gui.screen()
            if (screen is ClickGUIScreen) {
                screen.activateEditBox(this, rx, ry, tw + 12, 16, text)
                playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            }
            return true
        }
        return false
    }

    companion object {
        fun toHexString(color: Int): String {
            val a = (color shr 24) and 0xFF
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            return String.format("#%02X%02X%02X%02X", r, g, b, a)
        }

        fun fromHexString(hex: String): Int {
            val clean = hex.replace("#", "").trim().uppercase()
            return when (clean.length) {
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
                else -> throw IllegalArgumentException("Invalid hex color: $hex (expected 6 or 8 hex digits)")
            }
        }
    }
}