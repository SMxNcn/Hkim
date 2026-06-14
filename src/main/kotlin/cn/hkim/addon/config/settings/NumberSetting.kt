package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.nvg.NVGPIPRenderer
import cn.hkim.addon.utils.render.nvg.NVGRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import java.awt.Color
import kotlin.math.abs
import kotlin.math.roundToInt

class NumberSetting(name: String, desc: String, override val default: Float, val min: Float, val max: Float, val step: Float) : Setting<Float>(name, desc) {
    private var isDragging = false

    private var animationProgress: Float
    private var targetProgress: Float
    private var lastUpdateTime = System.currentTimeMillis()
    private val animationSpeed = 0.15f

    init {
        val initialVal = snapToStep(default)
        targetProgress = calculateProgress(initialVal)
        animationProgress = targetProgress
    }

    override var value: Float = snapToStep(default)
        set(newValue) {
            field = snapToStep(newValue)
        }

    override fun set(newValue: Float) {
        val snapped = snapToStep(newValue)
        if (value != snapped) {
            value = snapped
            targetProgress = calculateProgress(snapped)
            lastUpdateTime = System.currentTimeMillis()
        }
    }

    override fun reset() {
        super.reset()
        settingsChanged()
        targetProgress = calculateProgress(default)
        animationProgress = targetProgress
        lastUpdateTime = System.currentTimeMillis()
        isDragging = false
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

        val decimals = when {
            step.toDouble() % 1.0 == 0.0 -> 0
            step.toDouble() in 0.01..<0.1 -> 2
            step.toDouble() < 1.0 -> 1
            else -> 2
        }
        val valueText = if (default is Int) value.toInt().toString() else String.format("%.${decimals}f", value.toDouble())
        val valueWidth = mc.font.width(valueText)
        graphics.text(mc.font, valueText, (x + width - valueWidth - 8).toInt(), y.toInt() + 6, themeColor, false)

        val sliderX = x + 120f
        val sliderY = y + 8f
        val sliderW = width - 150f - 15f
        val sliderH = 4f

        val displayProgress = if (isDragging) targetProgress else animationProgress
        val filledW = sliderW * displayProgress
        val knobX = sliderX + filledW - 3f

        NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
            NVGRenderer.rect(sliderX * 2, sliderY * 2, sliderW * 2, sliderH * 2, Color(0x3A3A3A), 4f)

            if (filledW > 0) {
                NVGRenderer.rect(sliderX * 2, sliderY * 2, filledW * 2, sliderH * 2, Color(themeColor), 4f)
            }

            NVGRenderer.rect(knobX * 2, (sliderY - 2) * 2, 16f, (sliderH + 4) * 2, Color(themeColor), 8f)
            NVGRenderer.hollowRect(knobX * 2, (sliderY - 2) * 2, 16f, (sliderH + 4) * 2, 1f, Color(0xA0181818.toInt(), true), 8f)
        }

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
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
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
            settingsChanged()
            isDragging = false
            return true
        }
        return false
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