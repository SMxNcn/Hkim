package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.ClickGUIScreen
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.HudUtils.drawRectWithBorder
import cn.hkim.addon.utils.playSoundAtPlayer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents

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

        val hexText = String.format("#%06X", value and 0x00FFFFFF)
        val screen = mc.screen
        val isActive = screen is ClickGUIScreen && screen.activeEditBoxSetting == this

        graphics.drawRectWithBorder(previewX, previewY, 16f, 16f, value or 0xFF000000.toInt(), 0xFF444444.toInt())
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
                val hexValue = String.format("#%06X", value and 0x00FFFFFF)
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
}