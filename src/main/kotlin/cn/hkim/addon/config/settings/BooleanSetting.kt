package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.HudUtils.lerp
import cn.hkim.addon.utils.HudUtils.lerpColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.abs

open class BooleanSetting(name: String, desc: String, override val default: Boolean) : Setting<Boolean>(name, desc) {
    private var animationProgress = 0f
    private var lastUpdateTime = System.currentTimeMillis()
    private val animationSpeed = 0.15f

    override var value: Boolean = default
        set(newValue) {
            field = newValue
            animationProgress = if (newValue) 1f else 0f
            lastUpdateTime = System.currentTimeMillis()
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

        val toggleX = x + width - 34f
        val toggleY = y + 4f
        val toggleW = 28f
        val toggleH = 12f

        val trackColor = if (isHovered) 0xFF555555.toInt() else 0xFF3A3A3A.toInt()
        graphics.fill(toggleX.toInt(), toggleY.toInt(), (toggleX + toggleW).toInt(), (toggleY + toggleH).toInt(), trackColor)

        val knobStartX = toggleX + 2f
        val knobEndX = toggleX + toggleW - 10f
        val knobX = lerp(knobStartX, knobEndX, animationProgress)

        val knobStartColor = 0xFF888888.toInt()
        val knobColor = lerpColor(knobStartColor, themeColor, animationProgress)
        graphics.fill(knobX.toInt(), (toggleY + 2).toInt(), (knobX + 8).toInt(), (toggleY + toggleH - 2).toInt(), knobColor)

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (button != 0) return false // 仅左键

        val toggleX = x + width - 34f
        val toggleY = y + 4f
        val toggleW = 28f
        val toggleH = 12f

        if (HudUtils.isPointInRect(mouseX, mouseY, toggleX, toggleY, toggleW, toggleH)) {
            set(!get())
            settingsChanged()
            return true
        }
        return false
    }

    private fun updateAnimation() {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 10f
        lastUpdateTime = currentTime

        val targetProgress = if (value) 1f else 0f
        val step = animationSpeed * deltaTime.coerceAtMost(3f)

        animationProgress = when {
            abs(animationProgress - targetProgress) < 0.01f -> targetProgress
            animationProgress < targetProgress -> (animationProgress + step).coerceAtMost(targetProgress)
            else -> (animationProgress - step).coerceAtLeast(targetProgress)
        }
    }
}