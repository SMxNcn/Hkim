package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import net.minecraft.client.gui.GuiGraphicsExtractor

class DropdownSetting(name: String, desc: String, defaultExpanded: Boolean = false) : BooleanSetting(name, desc, defaultExpanded) {
    private val expandAnim = GuiAnimation.create(if (value) 1f else 0f, if (value) 1f else 0f)
        .duration(100L)
        .easing(Easing.CUBIC_OUT)

    init { noSave() }

    override fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int,
        delta: Float, visibleTop: Float, visibleBottom: Float
    ): Float {
        val height = 20f
        val isHovered = (visibleTop == -1f || mouseY in visibleTop..visibleBottom) && HudUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

        graphics.fill(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), 0x80181818.toInt())

        graphics.fill(x.toInt(), y.toInt(), x.toInt() + 2, (y + height).toInt(), 0xFF444444.toInt())

        val animationProgress = expandAnim.getValue()
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

    override fun set(newValue: Boolean) {
        super.set(newValue)
        expandAnim.animateTo(if (newValue) 1f else 0f)
    }
}