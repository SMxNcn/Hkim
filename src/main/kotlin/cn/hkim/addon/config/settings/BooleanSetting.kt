package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.HudUtils.lerp
import cn.hkim.addon.utils.HudUtils.lerpColor
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.pip.ShapeRenderer.drawCircle
import cn.hkim.addon.utils.render.pip.ShapeRenderer.drawRoundedRect
import cn.hkim.addon.utils.render.pip.ShapeRenderer.drawRoundedRectWithBorder
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents

open class BooleanSetting(name: String, desc: String, override val default: Boolean) : Setting<Boolean>(name, desc) {
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
        val height = 20f
        val isHovered = (visibleTop == -1f || mouseY in visibleTop..visibleBottom) && HudUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

        if (isHovered) {
            graphics.drawRoundedRectWithBorder(x, y, width, height, 0x15FFFFFF, 0, 0f, 3f)
        }

        graphics.text(mc.font, name, x.toInt() + 10, y.toInt() + 6, 0xFFCCCCCC.toInt(), false)

        val toggleX = x + width - 34f
        val toggleY = y + 4f
        val toggleW = 28f
        val toggleH = 12f

        val animationProgress = toggleAnim.getValue()
        val knobStartX = toggleX + 2f
        val knobEndX = toggleX + toggleW - 10f
        val knobX = lerp(knobStartX, knobEndX, animationProgress)

        val bgColor = lerpColor(0xFF3A3A3A.toInt(), themeColor, animationProgress)
        graphics.drawRoundedRect(toggleX, toggleY, toggleW, toggleH, bgColor, 6f)
        graphics.drawCircle(knobX + 4f, toggleY + toggleH / 2f, 0xFFFFFFFF.toInt(), 4.5f)

        if (isHovered && HudUtils.isPointInRect(mouseX, mouseY, toggleX, toggleY, toggleW, toggleH)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (button != 0) return false

        val toggleX = x + width - 34f
        val toggleY = y + 4f
        val toggleW = 28f
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