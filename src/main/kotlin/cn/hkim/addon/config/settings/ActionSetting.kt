package cn.hkim.addon.config.settings

import com.mojang.blaze3d.platform.InputConstants
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoCenteredText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents

class ActionSetting(name: String, desc: String, val action: () -> Unit) : Setting<Unit>(name, desc, Unit) {
    override var value: Unit = Unit
    fun execute() = action()
    init { noSave() }

    override fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int,
        delta: Float, visibleTop: Float, visibleBottom: Float
    ): Float {
        val height = Theme.SETTING_HEIGHT
        val isHovered = computeIsHovered(mouseX, mouseY, x, y, width, height, visibleTop, visibleBottom)

        if (isHovered) {
            graphics.drawRoundedRectWithBorder(x, y, width, height, Theme.controlHover, 0, 0f, 3f)
        }

        graphics.drawSkikoText(name, x + 10f, y + 3f, Theme.CARD_FONT_SIZE, Theme.controlText)

        val btnX = x + width - 80f
        val btnY = y + 2f
        val btnW = 70f
        val btnH = 14f

        val isBtnHovered = HudUtils.isPointInRect(mouseX, mouseY, btnX, btnY, btnW, btnH)
        val btnColor = if (isBtnHovered) themeColor else Theme.controlBorderHover
        graphics.drawRoundedRectWithBorder(btnX, btnY, btnW, btnH, Theme.controlButtonBg, btnColor, 1f, 3f)

        graphics.drawSkikoCenteredText("Execute", btnX + btnW / 2f, btnY + 1.5f, Theme.CARD_FONT_SIZE, Theme.controlTextActive)

        if (isBtnHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float, doubleClick: Boolean): Boolean {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false

        val btnX = x + width - 80f
        val btnY = y + 2f
        val btnW = 70f
        val btnH = 14f

        if (HudUtils.isPointInRect(mouseX, mouseY, btnX, btnY, btnW, btnH)) {
            execute()
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            return true
        }
        return false
    }
}