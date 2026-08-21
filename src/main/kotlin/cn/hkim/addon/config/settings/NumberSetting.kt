package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawCircleWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRect
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoTextWidth
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import kotlin.math.roundToInt

class NumberSetting(name: String, desc: String, default: Float, val min: Float, val max: Float, val step: Float, val unit: String = "") : Setting<Float>(name, desc, default) {
    private var isDragging = false
    private var sliderPressed = false

    private val knobAnim = GuiAnimation.create(0f, 0f)
        .duration(150L)
        .easing(Easing.CUBIC_OUT)

    init {
        knobAnim.snapTo(calculateProgress(snapToStep(default)))
    }

    override var value: Float = snapToStep(default)
        set(newValue) {
            field = snapToStep(newValue)
        }

    override fun set(newValue: Float) {
        val snapped = snapToStep(newValue)
        if (value != snapped) {
            value = snapped
            knobAnim.animateTo(calculateProgress(snapped))
        }
    }

    override fun reset() {
        super.reset()
        settingsChanged()
        knobAnim.snapTo(calculateProgress(default))
        isDragging = false
        sliderPressed = false
    }

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

        val valueText = formatValue()
        val valueWidth = skikoTextWidth(valueText, Theme.CARD_FONT_SIZE)

        val sliderW = width - 130f
        val sliderX = x + width - 8f - sliderW
        val sliderY = y + 7f
        val sliderH = 4f

        graphics.drawSkikoText(valueText, sliderX - valueWidth - 10f, y + 3.5f, Theme.CARD_FONT_SIZE, Theme.controlTextActive)

        graphics.drawRoundedRect(sliderX, sliderY, sliderW, sliderH, Theme.controlButtonBg, 2f)

        val displayProgress = if (isDragging) calculateProgress(value) else knobAnim.getValue()
        val filledW = sliderW * displayProgress
        if (filledW > 0f) {
            graphics.drawRoundedRect(sliderX, sliderY, filledW, sliderH, themeColor, 2f)
        }

        val knobRadius = 4f
        val knobCx = sliderX + filledW
        val knobCy = sliderY + sliderH / 2f
        graphics.drawCircleWithBorder(knobCx, knobCy, themeColor, 0xA0181818.toInt(), 0.5f, knobRadius)

        if (isHovered) {
            val isOverSlider = HudUtils.isPointInRect(mouseX, mouseY, sliderX, sliderY - 2f, sliderW, 12f)
            if (isOverSlider) {
                graphics.requestCursor(if (isDragging) CursorTypes.RESIZE_EW else CursorTypes.POINTING_HAND)
            }
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float, doubleClick: Boolean): Boolean {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false

        val sliderW = width - 130f
        val sliderX = x + width - 8f - sliderW
        val sliderY = y + 7f

        if (HudUtils.isPointInRect(mouseX, mouseY, sliderX, sliderY - 2f, sliderW, 12f)) {
            sliderPressed = true
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            updateValueFromMouse(mouseX, sliderX, sliderW)
            return true
        }
        return false
    }

    override fun mouseDragged(mouseX: Float, mouseY: Float, button: Int, deltaX: Float, deltaY: Float, x: Float, y: Float, width: Float): Boolean {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && sliderPressed) {
            isDragging = true
            val sliderW = width - 130f
            val sliderX = x + width - 8f - sliderW

            updateValueFromMouse(mouseX, sliderX, sliderW)
            return true
        }
        return false
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (sliderPressed && button == InputConstants.MOUSE_BUTTON_LEFT) {
            settingsChanged()
            isDragging = false
            sliderPressed = false
            return true
        }
        return false
    }

    override fun mouseScrolled(
        mouseX: Float, mouseY: Float, scrollX: Double, scrollY: Double,
        x: Float, y: Float, width: Float
    ): Boolean {
        if (scrollY == 0.0) return false
        if (!isShiftDown()) return false

        val sliderW = width - 130f
        val sliderX = x + width - 8f - sliderW
        val sliderY = y + 7f
        if (!HudUtils.isPointInRect(mouseX, mouseY, sliderX, sliderY - 2f, sliderW, 12f)) return false

        val direction = if (scrollY > 0) 1f else -1f
        set(value + step * direction)
        settingsChanged()
        return true
    }

    private fun isShiftDown(): Boolean =
        InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT)

    private fun formatValue(): String {
        val decimals = if (step % 1f == 0f) 0 else {
            val str = step.toString()
            val dot = str.indexOf('.')
            if (dot < 0) 0 else str.length - dot - 1
        }
        return String.format("%.${decimals}f", value.toDouble()) + unit
    }

    private fun updateValueFromMouse(mx: Float, sliderX: Float, sliderW: Float) {
        val ratio = ((mx - sliderX) / sliderW).coerceIn(0.0F, 1.0F)
        val newValue = min + ratio * (max - min)
        set(newValue)
    }

    private fun snapToStep(value: Float): Float {
        val stepVal = step.toDouble()
        if (stepVal <= 0) return value

        val raw = value.toDouble()
        val steps = ((raw - min.toDouble()) / stepVal).roundToInt()
        var snapped = min.toDouble() + steps * stepVal

        snapped = snapped.coerceIn(min.toDouble(), max.toDouble())

        return snapped.toFloat()
    }

    private fun calculateProgress(value: Float): Float {
        val minVal = min.toDouble()
        val maxVal = max.toDouble()
        val valVal = value.toDouble()

        if (maxVal == minVal) return 0f

        return ((valVal - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
    }
}