package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.ClickGUIScreen
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.HudUtils.drawRectWithBorder
import net.minecraft.client.gui.GuiGraphicsExtractor

class TextSetting(name: String, desc: String, override val default: String) : Setting<String>(name, desc) {
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

        val inputX = x + 120f
        val inputW = width - 130f
        val inputY = y + 2f
        val inputH = 16f

        val screen = mc.screen
        val isActive = screen is ClickGUIScreen && screen.activeEditBoxSetting == this

            graphics.drawRectWithBorder(inputX, inputY, inputW, inputH, 0xFF222222.toInt(), 0xFF444444.toInt())
        if (!isActive) {
            graphics.text(mc.font, value, inputX.toInt() + 4, inputY.toInt() + 4, 0xFFFFFFFF.toInt(), false)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (button != 0) return false

        val inputX = x + 120f
        val inputW = width - 130f
        val inputY = y + 2f
        val inputH = 16f

        if (HudUtils.isPointInRect(mouseX, mouseY, inputX, inputY, inputW, inputH)) {
            val screen = mc.screen
            if (screen is ClickGUIScreen) {
                screen.activateEditBox(
                    this,
                    inputX.toInt() + 4, inputY.toInt() + 4, inputW.toInt(), inputH.toInt(),
                    value
                )
            }
            return true
        }
        return false
    }
}