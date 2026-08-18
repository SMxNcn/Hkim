package cn.hkim.addon.config.settings

import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoImage
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor

class DropdownSetting(name: String, desc: String = "", defaultExpanded: Boolean = false) : BooleanSetting(name, desc, defaultExpanded) {
    private val expandAnim = GuiAnimation.create(if (value) 1f else 0f, if (value) 1f else 0f)
        .duration(200L)
        .easing(Easing.CUBIC_OUT)

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

        graphics.fill(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), Theme.controlRowBg)

        graphics.fill(x.toInt(), y.toInt(), x.toInt() + 1, (y + height).toInt(), Theme.controlBorder)

        val animationProgress = expandAnim.getValue()
        val centerY = y + height / 2
        val halfHeight = (height / 2) * animationProgress
        val lineTopY = (centerY - halfHeight).toInt()
        val lineBottomY = (centerY + halfHeight).toInt()

        if (lineBottomY > lineTopY) {
            graphics.fill(
                x.toInt(), lineTopY,
                x.toInt() + 1, lineBottomY,
                themeColor
            )
        }

        graphics.drawSkikoText(name, x + 12f, y + 3f, Theme.CARD_FONT_SIZE, Theme.controlTextActive)

        val iconX = x + width - 16f
        val iconY = y + 3f
        val rotation = expandAnim.getValue() * 180f
        graphics.drawSkikoImage(
            "assets/hkim/textures/clickgui/chevron_down.svg",
            iconX, iconY, 12f, 12f, 0f,
            if (isHovered) Theme.controlTextActive else Theme.controlTextMuted,
            rotation
        )

        if (isHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float, doubleClick: Boolean): Boolean {
        if (button != 0) return false
        if (HudUtils.isPointInRect(mouseX, mouseY, x, y, width, 20f)) {
            set(!get())
            return true
        }
        return false
    }

    override fun set(newValue: Boolean) {
        super.set(newValue)
        expandAnim.animateTo(if (newValue) 1f else 0f)
    }
}