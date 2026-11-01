package cn.hkim.addon.config.settings

import cn.hkim.addon.compat.input.InputCompat
import cn.hkim.addon.compat.input.KeyType
import cn.hkim.addon.compat.input.MouseButton
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoCenteredText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents

class KeybindSetting(
    name: String,
    desc: String,
    defaultKey: Int = InputCompat.KEY_NONE,
    private val defaultType: KeyType = KeyType.KEYBOARD,
) : Setting<Int>(name, desc, defaultKey) {
    override var value: Int = defaultKey

    /** Whether [value] is a mouse button rather than a keyboard key. */
    var keyType: KeyType = defaultType
        private set

    internal var isBinding = false

    override fun render(
        graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float, themeColor: Int,
        delta: Float, visibleTop: Float, visibleBottom: Float
    ): Float {
        val height = Theme.SETTING_HEIGHT
        val isHovered = computeIsHovered(mouseX, mouseY, x, y, width, height, visibleTop, visibleBottom)

        if (isHovered || isBinding) {
            graphics.drawRoundedRectWithBorder(x, y, width, height, Theme.controlHover, 0, 0f, 3f)
        }

        graphics.drawSkikoText(name, x + 6f, y + 3f, Theme.CARD_FONT_SIZE, Theme.controlText)

        val btnX = x + width - 80f
        val btnY = y + 2f
        val btnW = 70f
        val btnH = 14f

        val isBtnHovered = HudUtils.isPointInRect(mouseX, mouseY, btnX, btnY, btnW, btnH)
        val displayText = when {
            isBinding -> "Press..."
            value == InputCompat.KEY_NONE -> "None"
            else -> InputCompat.keyDisplayName(value, keyType)
        }

        val btnColor = if (isBinding || isBtnHovered) themeColor else Theme.controlBorderHover
        graphics.drawRoundedRectWithBorder(btnX, btnY, btnW, btnH, Theme.controlButtonBg, btnColor, 1f, 3f)

        graphics.drawSkikoCenteredText(displayText, btnX + btnW / 2f, btnY + 1.5f, Theme.CARD_FONT_SIZE, Theme.controlTextActive)

        if (isBtnHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float, doubleClick: Boolean): Boolean {
        if (isBinding) return true

        if (button != InputCompat.MOUSE_LEFT) return false
        val btnX = x + width - 80f
        val btnY = y + 2f
        val btnW = 70f
        val btnH = 14f

        if (HudUtils.isPointInRect(mouseX, mouseY, btnX, btnY, btnW, btnH)) {
            isBinding = true
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            return true
        }
        return false
    }

    fun handleKey(keyCode: Int): Boolean {
        if (!isBinding) return false

        if (keyCode == InputCompat.KEY_ESCAPE) {
            value = InputCompat.KEY_NONE
            keyType = KeyType.KEYBOARD
            isBinding = false
            settingsChanged()
            return true
        }

        if (keyCode == InputCompat.KEY_NONE) return false

        value = keyCode
        keyType = KeyType.KEYBOARD
        isBinding = false
        settingsChanged()
        playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
        return true
    }

    fun handleMouseButton(button: Int): Boolean {
        if (!isBinding) return false

        // Left/right clicks are used to operate the UI, not to bind a key.
        when (InputCompat.mouseButton(button)) {
            MouseButton.LEFT, MouseButton.RIGHT -> {
                isBinding = false
                return true
            }
            else -> Unit
        }

        value = button
        keyType = KeyType.MOUSE
        isBinding = false
        settingsChanged()
        playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
        return true
    }

    fun matches(key: com.mojang.blaze3d.platform.InputConstants.Key): Boolean =
        InputCompat.matches(key, value, keyType)

    /** Load a keybind from its persisted name (stable or legacy). */
    fun setFromName(name: String) {
        val binding = InputCompat.keyCode(name)
        if (binding == null) {
            value = InputCompat.KEY_NONE
            keyType = KeyType.KEYBOARD
        } else {
            value = binding.code
            keyType = binding.type
        }
    }

    override fun reset() {
        value = default
        keyType = defaultType
    }

    companion object {
        @JvmStatic
        fun keyCodeToName(code: Int, type: KeyType): String = InputCompat.keyName(code, type)

        @JvmStatic
        fun nameToKeyCode(name: String): Int = InputCompat.keyCode(name)?.code ?: InputCompat.KEY_NONE
    }
}
