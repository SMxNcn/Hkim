package cn.hkim.addon.config.settings

import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoCenteredText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoImage
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents

class SelectorSetting(name: String, desc: String, val options: List<String>, default: String) : Setting<Int>(name, desc, options.indexOf(default).coerceAtLeast(0)) {
    companion object {
        @JvmStatic var scrollFocused: SelectorSetting? = null
    }

    private var _selected: String = options.firstOrNull() ?: ""

    var onSelect: ((String) -> Unit)? = null
    init { value = this.default }
    fun select(option: String) {
        if (option in options) {
            _selected = option
            value = options.indexOf(option)
            onSelect?.invoke(option)
        }
    }
    fun getSelected(): String = _selected

    val isScrollFocused: Boolean get() = scrollFocused == this

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

        val selectorX = x + width - 100f
        val selectorW = 90f
        val selectorY = y + 2f
        val selectorH = 14f

        val focused = isScrollFocused
        graphics.drawRoundedRectWithBorder(selectorX, selectorY, selectorW, selectorH, Theme.controlBg, if (focused) themeColor else Theme.controlBorder, 1f, 3f)

        val leftArrowHovered = HudUtils.isPointInRect(mouseX, mouseY, selectorX, selectorY, 14f, selectorH)
        val rightArrowHovered = HudUtils.isPointInRect(mouseX, mouseY, selectorX + selectorW - 16f, selectorY, 14f, selectorH)
        val leftArrowColor = if (leftArrowHovered) themeColor else Theme.controlTextMuted
        val rightArrowColor = if (rightArrowHovered) themeColor else Theme.controlTextMuted
        val optionsX = (selectorX + selectorW / 2).toInt()

        graphics.drawSkikoImage("assets/hkim/textures/clickgui/chevron_left.svg", selectorX + 2f, selectorY + 1f, 12f, 12f, 0f, tintColor = leftArrowColor)
        graphics.drawSkikoImage("assets/hkim/textures/clickgui/chevron_right.svg", selectorX + selectorW - 14f, selectorY + 1f, 12f, 12f, 0f, tintColor = rightArrowColor)

        graphics.drawSkikoCenteredText(_selected, optionsX.toFloat(), selectorY + 1.5f, Theme.CARD_FONT_SIZE, Theme.controlTextActive)

        if (leftArrowHovered || rightArrowHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float, doubleClick: Boolean): Boolean {
        if (button != 0) return false

        val selectorX = x + width - 100f
        val selectorW = 90f
        val selectorY = y + 2f
        val selectorH = 14f

        if (HudUtils.isPointInRect(mouseX, mouseY, selectorX, selectorY, 14f, selectorH)) {
            scrollFocused = null
            return previous()
        }

        if (HudUtils.isPointInRect(mouseX, mouseY, selectorX + selectorW - 16f, selectorY, 14f, selectorH)) {
            scrollFocused = null
            return next()
        }

        if (HudUtils.isPointInRect(mouseX, mouseY, selectorX + 14f, selectorY, selectorW - 30f, selectorH)) {
            scrollFocused = if (scrollFocused == this) null else this
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            return true
        }
        return false
    }

    override fun mouseScrolled(mouseX: Float, mouseY: Float, scrollX: Double, scrollY: Double, x: Float, y: Float, width: Float): Boolean {
        if (scrollFocused != this) return false

        return if (scrollY < 0) next() else previous()
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