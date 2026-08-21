package cn.hkim.addon.config.settings

import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.HudUtils.lerp
import cn.hkim.addon.utils.HudUtils.lerpColor
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawCircle
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRect
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents

open class BooleanSetting(name: String, desc: String, default: Boolean) : Setting<Boolean>(name, desc, default) {
    private val toggleAnim = GuiAnimation.create(if (value) 1f else 0f, if (value) 1f else 0f)
        .duration(100L)
        .easing(Easing.CUBIC_OUT)

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

        val toggleX = x + width - 28f
        val toggleY = y + 3f
        val toggleW = 22f
        val toggleH = 12f

        val animationProgress = toggleAnim.getValue()
        val boxRadius = 6f
        val knobRadius = 3.5f

        val knobStartX = toggleX + boxRadius - knobRadius
        val knobEndX = toggleX + toggleW - boxRadius - knobRadius
        val knobX = lerp(knobStartX, knobEndX, animationProgress)

        val bgColor = lerpColor(Theme.controlButtonBg, themeColor, animationProgress)
        graphics.drawRoundedRect(toggleX, toggleY, toggleW, toggleH, bgColor, boxRadius)
        graphics.drawCircle(knobX + knobRadius, toggleY + toggleH / 2f, Theme.controlTextActive, knobRadius)

        if (isHovered && HudUtils.isPointInRect(mouseX, mouseY, toggleX, toggleY, toggleW, toggleH)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float, doubleClick: Boolean): Boolean {
        if (button != 0) return false

        val toggleX = x + width - 28f
        val toggleY = y + 3f
        val toggleW = 22f
        val toggleH = 12f

        if (HudUtils.isPointInRect(mouseX, mouseY, toggleX, toggleY, toggleW, toggleH)) {
            set(!get())
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            settingsChanged()
            return true
        }
        return false
    }

    override fun set(newValue: Boolean) {
        super.set(newValue)
        toggleAnim.animateTo(if (newValue) 1f else 0f)
    }
}