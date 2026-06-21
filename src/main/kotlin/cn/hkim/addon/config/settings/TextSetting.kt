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

class TextSetting(name: String, desc: String, default: String) : Setting<String>(name, desc, default) {
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

        val inputX = x + 120f
        val inputW = width - 130f
        val inputY = y + 2f
        val inputH = 16f

        val screen = mc.gui.screen()
        val isActive = screen is ClickGUIScreen && screen.activeEditBoxSetting == this

        val borderColor = if (isActive) themeColor else 0xFF444444.toInt()
        graphics.drawRoundedRectWithBorder(inputX, inputY, inputW, inputH, 0xFF222222.toInt(), borderColor, 1f, 3f)

        if (isActive) {
            val editBox = screen.activeEditBox
            if (editBox != null) {
                editBox.setPosition((inputX + 4).toInt(), (inputY + 4).toInt())
                editBox.setSize(inputW.toInt() - 2, inputH.toInt())
                if (editBox.isFocused) graphics.requestCursor(CursorType.DEFAULT)
                editBox.extractWidgetRenderState(graphics, mouseX.toInt(), mouseY.toInt(), delta)
            }
        } else {
            if (HudUtils.isPointInRect(mouseX, mouseY, inputX, inputY, inputW, inputH)) {
                graphics.requestCursor(CursorTypes.POINTING_HAND)
            }
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
            val screen = mc.gui.screen()
            if (screen is ClickGUIScreen) {
                screen.activateEditBox(
                    this,
                    inputX.toInt() + 4, inputY.toInt() + 4, inputW.toInt(), inputH.toInt(),
                    value
                )
                playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            }
            return true
        }
        return false
    }
}