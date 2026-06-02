package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.ClickGUIScreen
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.nvg.NVGPIPRenderer
import cn.hkim.addon.utils.render.nvg.NVGRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import java.awt.Color

class ColorSetting(name: String, desc: String, override val default: Int) : Setting<Int>(name, desc) {
    init { value = default }

    override fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int
    ): Float {
        val height = 20f
        val isHovered = HudUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

        if (isHovered) {
            graphics.fill(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), 0x15FFFFFF)
        }

        graphics.text(mc.font, name, x.toInt() + 10, y.toInt() + 6, 0xFFCCCCCC.toInt(), false)

        val previewSize = 16f
        val previewX = x + width - previewSize - 10f
        val previewY = y + 2f

        val hexText = toHexString(value)
        val screen = mc.screen
        val isActive = screen is ClickGUIScreen && screen.activeEditBoxSetting == this

        NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
            NVGRenderer.rect(previewX * 2, previewY * 2, 32f, 32f, Color(value), 6f)
            NVGRenderer.hollowRect(previewX * 2, previewY * 2, 32f, 32f, 2f, Color(0x444444), 6f)
        }

        if (!isActive) {
            graphics.text(mc.font, hexText, (previewX - mc.font.width(hexText) - 10).toInt(), y.toInt() + 6, value or 0xFF000000.toInt(), false)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (button != 0) return false

        val previewX = x + width - 26f - 10f
        val previewY = y + 2f
        val clickW = 70f

        if (HudUtils.isPointInRect(mouseX, mouseY, previewX - 46f, previewY, clickW, 16f)) {
            val screen = mc.screen
            if (screen is ClickGUIScreen) {
                val hexValue = toHexString(value)
                screen.activateEditBox(
                    this,
                    (previewX - 50f).toInt() + 8, previewY.toInt() + 4, 70, 16,
                    hexValue
                )
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