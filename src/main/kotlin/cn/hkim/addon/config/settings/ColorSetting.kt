package cn.hkim.addon.config.settings

import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents

class ColorSetting(name: String, desc: String, default: Int) : Setting<Int>(name, desc, default) {
    init { value = default }

    override fun set(newValue: Int) {
        super.set(newValue)
        popup.onValueChanged()
    }

    val popup = ColorPickerPopup(this)

    val isOpen: Boolean get() = popup.isOpenPopup

    fun openPopup() = popup.open()
    fun closePopup() = popup.close()

    fun renderPopup(
        graphics: GuiGraphicsExtractor,
        guiW: Float, guiH: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int,
        fontSize: Float
    ) = popup.renderPopup(graphics, guiW, guiH, mouseX, mouseY, themeColor, fontSize)

    fun isPointInPopup(mx: Float, my: Float): Boolean = popup.isPointInPopup(mx, my)
    fun isPointInHex(mx: Float, my: Float): Boolean = popup.isPointInHex(mx, my)
    fun isPointInAlpha(mx: Float, my: Float): Boolean = popup.isPointInAlpha(mx, my)
    fun handlePopupClick(mx: Float, my: Float, button: Int, doubleClick: Boolean): Boolean = popup.handlePopupClick(mx, my, button, doubleClick)
    fun handlePopupDrag(mx: Float, my: Float): Boolean = popup.handlePopupDrag(mx, my)
    fun handlePopupRelease(): Boolean = popup.handlePopupRelease()
    fun saveValue() = settingsChanged()

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

        val previewSize = 14f
        val previewX = x + width - previewSize - 10f
        val previewY = y + 2f
        popup.anchorX = previewX
        popup.anchorY = previewY

        graphics.drawRoundedRectWithBorder(previewX, previewY, previewSize, previewSize, value or 0xFF000000.toInt(), Theme.controlBorder, 1f, 3f)

        if (isHovered && HudUtils.isPointInRect(mouseX, mouseY, previewX, previewY, previewSize, previewSize)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float, doubleClick: Boolean): Boolean {
        if (button != 0) return false

        val previewX = x + width - 14f - 10f
        if (HudUtils.isPointInRect(mouseX, mouseY, previewX, y + 2f, 14f, 14f)) {
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            openPopup()
            return true
        }
        return false
    }
}