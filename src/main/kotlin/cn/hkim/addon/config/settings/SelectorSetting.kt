package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.HudUtils.drawRectWithBorder
import cn.hkim.addon.utils.playSoundAtPlayer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents

class SelectorSetting(name: String, desc: String, val options: List<String>, default: String) : Setting<Int>(name, desc) {
    override val default: Int = options.indexOf(default).coerceAtLeast(0)
    private var _selected: String = options.firstOrNull() ?: ""
    init { value = this.default }
    fun select(option: String) { if (option in options) { _selected = option; value = options.indexOf(option) } }
    fun getSelected(): String = _selected

    override fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int
    ): Float {
        val height = 20f
        val isHovered = HudUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

        if (isHovered) {
            graphics.fill(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), 0x15FFFFFF)
        }

        graphics.text(mc.font, name, x.toInt() + 10, y.toInt() + 6, 0xFFCCCCCC.toInt(), false)

        val selectorX = x + width - 100f
        val selectorW = 90f
        val selectorY = y + 2f
        val selectorH = 16f

        graphics.drawRectWithBorder(selectorX, selectorY, selectorW, selectorH, 0xFF222222.toInt(), 0xFF444444.toInt())

        val leftArrowHovered = HudUtils.isPointInRect(mouseX, mouseY, selectorX, selectorY, 14f, selectorH)
        val rightArrowHovered = HudUtils.isPointInRect(mouseX, mouseY, selectorX + selectorW - 16f, selectorY, 14f, selectorH)
        val leftArrowColor = if (leftArrowHovered) themeColor else 0xFF888888.toInt()
        val rightArrowColor = if (rightArrowHovered) themeColor else 0xFF888888.toInt()
        val optionsX = (selectorX + selectorW / 2).toInt()

        graphics.text(mc.font, "<", selectorX.toInt() + 6, selectorY.toInt() + 4, leftArrowColor, false)
        graphics.text(mc.font, ">", (selectorX + selectorW - 12).toInt(), selectorY.toInt() + 4, rightArrowColor, false)

        graphics.centeredText(mc.font, _selected, optionsX, selectorY.toInt() + 4, 0xFFFFFFFF.toInt())

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (button != 0) return false

        val selectorX = x + width - 100f
        val selectorW = 90f
        val selectorY = y + 2f
        val selectorH = 16f

        if (HudUtils.isPointInRect(mouseX, mouseY, selectorX, selectorY, 14f, selectorH)) {
            return previous()
        }

        if (HudUtils.isPointInRect(mouseX, mouseY, selectorX + selectorW - 16f, selectorY, 14f, selectorH)) {
            return next()
        }
        return false
    }

    private fun next(): Boolean {
        val nextIndex = (value + 1) % options.size
        select(options[nextIndex])
        playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
        settingsChanged()
        return true
    }

    private fun previous(): Boolean {
        val prevIndex = if (value - 1 < 0) options.size - 1 else value - 1
        select(options[prevIndex])
        playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
        settingsChanged()
        return true
    }
}