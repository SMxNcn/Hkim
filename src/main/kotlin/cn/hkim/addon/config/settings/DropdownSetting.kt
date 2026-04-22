package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.HudUtils
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.abs

class DropdownSetting(name: String, desc: String, defaultExpanded: Boolean = false) : BooleanSetting(name, desc, defaultExpanded) {
    private var animationProgress = if (value) 1f else 0f
    private var lastUpdateTime = System.currentTimeMillis()
    private val animationSpeed = 0.15f

    init { noSave() }

    override fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int
    ): Float {
        val height = 20f
        val isHovered = HudUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

        updateAnimation()

        graphics.fill(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), 0xFF181818.toInt())

        graphics.fill(
            x.toInt(), y.toInt(),
            x.toInt() + 2, (y + height).toInt(),
            0xFF444444.toInt()
        )

        val centerY = y + height / 2
        val halfHeight = (height / 2) * animationProgress
        val lineTopY = (centerY - halfHeight).toInt()
        val lineBottomY = (centerY + halfHeight).toInt()

        if (lineBottomY > lineTopY) {
            graphics.fill(
                x.toInt(), lineTopY,
                x.toInt() + 2, lineBottomY,
                themeColor
            )
        }

        graphics.text(mc.font, name, x.toInt() + 12, y.toInt() + 6, 0xFFEEEEEE.toInt(), true)

        val iconX = x + width - 16f
        val iconY = y + 6f
        graphics.text(
            mc.font,
            if (get()) "▲" else "▼",
            iconX.toInt(),
            iconY.toInt(),
            if (isHovered) 0xFFFFFFFF.toInt() else 0xFF888888.toInt(),
            false
        )

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (button != 0) return false
        if (HudUtils.isPointInRect(mouseX, mouseY, x, y, width, 20f)) {
            set(!get())
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