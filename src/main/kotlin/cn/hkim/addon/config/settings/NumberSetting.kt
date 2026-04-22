package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.utils.HudUtils
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.abs
import kotlin.math.roundToInt

class NumberSetting(name: String, desc: String, override val default: Number, val min: Number, val max: Number, val step: Number) : Setting<Number>(name, desc) {
    private var isDragging = false

    private var animationProgress: Float
    private var targetProgress: Float
    private var lastUpdateTime = System.currentTimeMillis()
    private val animationSpeed = 0.3f

    init {
        val initialVal = snapToStep(default)
        targetProgress = calculateProgress(initialVal)
        animationProgress = targetProgress
    }

    override var value: Number = snapToStep(default)
        set(newValue) {
            field = snapToStep(newValue)
        }

    override fun set(newValue: Number) {
        val snapped = snapToStep(newValue)
        if (value != snapped) {
            value = snapped
            targetProgress = calculateProgress(snapped)
            lastUpdateTime = System.currentTimeMillis()
        }
    }

    override fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int
    ): Float {
        val height = 20f
        val isHovered = HudUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

        updateAnimation()

        if (isHovered) {
            graphics.fill(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), 0x15FFFFFF)
        }

        graphics.text(mc.font, name, x.toInt() + 10, y.toInt() + 6, 0xFFCCCCCC.toInt(), false)

        val valueText = if (default is Int) value.toInt().toString() else String.format("%.1f", value.toDouble())
        val valueWidth = mc.font.width(valueText)
        graphics.text(mc.font, valueText, (x + width - valueWidth - 8).toInt(), y.toInt() + 6, themeColor, false)

        val sliderX = x + 120f
        val sliderY = y + 8f
        val sliderW = width - 150f - 15f
        val sliderH = 4f

        graphics.fill(sliderX.toInt(), sliderY.toInt(), (sliderX + sliderW).toInt(), (sliderY + sliderH).toInt(), 0xFF3A3A3A.toInt())

        val displayProgress = if (isDragging) targetProgress else animationProgress
        val filledW = (sliderW * displayProgress).toInt()
        if (filledW > 0) {
            graphics.fill(sliderX.toInt(), sliderY.toInt(), sliderX.toInt() + filledW, (sliderY + sliderH).toInt(), themeColor)
        }

        val knobX = sliderX + filledW - 3f
        graphics.fill(knobX.toInt(), (sliderY - 2).toInt(), (knobX + 6).toInt(), (sliderY + sliderH + 2).toInt(), themeColor)

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (button != 0) return false

        val sliderX = x + 120f
        val sliderY = y + 8f
        val sliderW = width - 150f - 15f

        if (HudUtils.isPointInRect(mouseX, mouseY, sliderX, sliderY - 2f, sliderW, 12f)) {
            isDragging = true
            updateValueFromMouse(mouseX, sliderX, sliderW)
            return true
        }
        return false
    }

    override fun mouseDragged(mouseX: Float, mouseY: Float, button: Int, deltaX: Float, deltaY: Float, x: Float, y: Float, width: Float): Boolean {
        if (isDragging && button == 0) {
            val sliderX = x + 120f
            val sliderW = width - 150f - 15f

            updateValueFromMouse(mouseX, sliderX, sliderW)
            return true
        }
        return false
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (isDragging && button == 0) {
            isDragging = false
            return true
        }
        return false
    }

    private fun updateValueFromMouse(mx: Float, sliderX: Float, sliderW: Float) {
        val ratio = ((mx - sliderX) / sliderW).coerceIn(0.0F, 1.0F)
        val newValue = min.toDouble() + ratio * (max.toDouble() - min.toDouble())
        set(if (default is Int) newValue.roundToInt() else newValue)
    }

    private fun snapToStep(value: Number): Number {
        val stepVal = step.toDouble()
        if (stepVal <= 0) return value

        val raw = value.toDouble()
        val steps = ((raw - min.toDouble()) / stepVal).roundToInt()
        var snapped = min.toDouble() + steps * stepVal

        snapped = snapped.coerceIn(min.toDouble(), max.toDouble())

        return if (default is Int) snapped.roundToInt() else snapped
    }

    private fun updateAnimation() {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 10f
        lastUpdateTime = currentTime

        if (!isDragging) {
            val step = animationSpeed * deltaTime.coerceAtMost(3f)
            animationProgress = when {
                abs(animationProgress - targetProgress) < 0.01f -> targetProgress
                animationProgress < targetProgress -> (animationProgress + step).coerceAtMost(targetProgress)
                else -> (animationProgress - step).coerceAtLeast(targetProgress)
            }
        } else {
            animationProgress = targetProgress
        }
    }

    private fun calculateProgress(value: Number): Float {
        val minVal = min.toDouble()
        val maxVal = max.toDouble()
        val valVal = value.toDouble()

        if (maxVal == minVal) return 0f

        return ((valVal - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
    }
}