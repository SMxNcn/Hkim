package cn.hkim.addon.config.settings

import com.mojang.blaze3d.platform.InputConstants
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.ClickGUIScreen
import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.gui.SkikoEditBox
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawGradientRectMulti
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithShadow
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoSquareClipped
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoTextWidth
import cn.hkim.addon.utils.render.skiko.SkikoGradient
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.PreeditEvent
import net.minecraft.sounds.SoundEvents
import kotlin.math.roundToInt

class ColorPickerPopup(private val setting: ColorSetting) {
    private enum class DragTarget { PANEL, HUE }

    private var isOpen = false
    private var dragTarget: DragTarget? = null

    var anchorX = 0f
    var anchorY = 0f

    private var rectX = 0f
    private var rectY = 0f
    private var rectW = 0f
    private var rectH = 0f

    private var innerX = 0f
    private var hexY = 0f
    private var panelY = 0f
    private var hueY = 0f
    private var alphaX = 0f

    private var cachedH = 0f
    private var cachedS = 0f
    private var cachedB = 1f

    private val alphaBox = SkikoEditBox("100", maxLength = 3).apply {
        textInsetX = TEXT_INSET_X
        textInsetY = TEXT_INSET_Y
        filter = { it.filter(Char::isDigit) }
        responder = { onAlphaTextChanged(it) }
        onConfirm = {
            defocus()
            setting.saveValue()
        }
    }

    val isOpenPopup: Boolean get() = isOpen

    fun open() {
        if (isOpen) return
        isOpen = true
        refreshCache()
        syncAlphaBox()
        Setting.activeModalPopup = setting
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        dragTarget = null
        alphaBox.defocus()
        Setting.activeModalPopup = null
    }

    private fun refreshCache() {
        val hsb = HudUtils.hsbOf(setting.value)
        cachedH = hsb[0]
        cachedS = hsb[1]
        cachedB = hsb[2]
    }

    fun onValueChanged() {
        if (dragTarget == null) {
            refreshCache()
            syncAlphaBox()
        }
    }

    private fun syncAlphaBox() {
        val text = HudUtils.alphaToPercent(HudUtils.alphaOf(setting.value)).toString()
        if (alphaBox.text != text) alphaBox.setText(text)
    }

    private fun onAlphaTextChanged(text: String) {
        val p = text.toIntOrNull()?.coerceIn(0, 100) ?: return
        setting.set((setting.value and 0xFFFFFF) or (HudUtils.percentToAlpha(p) shl 24))
    }

    fun renderPopup(
        graphics: GuiGraphicsExtractor,
        guiW: Float, guiH: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int,
        fontSize: Float
    ) {
        if (!isOpen) return

        rectW = PANEL_SIZE + POPUP_PAD * 2
        rectH = HEX_H + GAP + PANEL_SIZE + GAP + HUE_BAR_H + POPUP_PAD * 2

        rectX = HudUtils.clamp(anchorX + 16f - rectW, SCREEN_MARGIN, guiW - SCREEN_MARGIN - rectW).roundToInt().toFloat()
        rectY = HudUtils.clamp(anchorY + 22f, SCREEN_MARGIN, guiH - SCREEN_MARGIN - rectH).roundToInt().toFloat()
        if (rectY + rectH > guiH - SCREEN_MARGIN) {
            rectY = HudUtils.clamp(anchorY - SCREEN_MARGIN - rectH, SCREEN_MARGIN, guiH - SCREEN_MARGIN - rectH).roundToInt().toFloat()
        }

        innerX = rectX + POPUP_PAD
        val rowY = rectY + POPUP_PAD
        panelY = rowY + HEX_H + GAP
        hueY = panelY + PANEL_SIZE + GAP
        alphaX = innerX + HEX_W + H_GAP

        graphics.drawRoundedRectWithShadow(
            rectX, rectY, rectW, rectH,
            Theme.controlBgTranslucent, Theme.controlBorder, 1f, 6f,
            0x80000000.toInt(), 3f, 1f
        )

        val screen = mc.gui.screen()
        if (screen is ClickGUIScreen && screen.activeEditBoxSetting == setting && screen.activeSkikoEditBox != null) {
            screen.activeSkikoEditBox!!.render(graphics, innerX, hexY, HEX_W, HEX_H, mouseX, mouseY, themeColor, fontSize)
        } else {
            graphics.drawRoundedRectWithBorder(innerX, hexY, HEX_W, HEX_H, Theme.controlBg, Theme.controlBorder, 1f, 3f)
            if (HudUtils.isPointInRect(mouseX, mouseY, innerX, hexY, HEX_W, HEX_H)) {
                graphics.requestCursor(CursorTypes.IBEAM)
            }
            graphics.drawSkikoText(HudUtils.toHexStringRGB(setting.value), innerX + TEXT_INSET_X, hexY + TEXT_INSET_Y, fontSize, Theme.controlTextActive)
        }

        alphaBox.render(graphics, alphaX, hexY, ALPHA_W, HEX_H, mouseX, mouseY, themeColor, fontSize)
        val percentX = alphaX + ALPHA_W + H_GAP
        val percentW = skikoTextWidth("%", fontSize)
        graphics.drawSkikoText("%", percentX + (PERCENT_W - percentW) / 2f, hexY + TEXT_INSET_Y, fontSize, Theme.controlTextMuted)

        val pure = HudUtils.hsvToRgb(cachedH, 1f, 1f)
        graphics.drawGradientRectMulti(innerX, panelY, PANEL_SIZE, PANEL_SIZE, listOf(0xFFFFFFFF.toInt(), pure), null, SkikoGradient.LEFT_RIGHT, RADIUS)
        graphics.drawGradientRectMulti(innerX, panelY, PANEL_SIZE, PANEL_SIZE, listOf(0x00FFFFFF, 0xFF000000.toInt()), null, SkikoGradient.TOP_BOTTOM, RADIUS)
        graphics.drawRoundedRectWithBorder(innerX, panelY, PANEL_SIZE, PANEL_SIZE, 0, Theme.controlBorder, 1f, RADIUS)

        if (HudUtils.isPointInRect(mouseX, mouseY, innerX, panelY, PANEL_SIZE, PANEL_SIZE + GAP + HUE_BAR_H)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }

        val sx = innerX + PANEL_SIZE * cachedS
        val sy = panelY + PANEL_SIZE * (1f - cachedB)
        graphics.drawSkikoSquareClipped(sx, sy, 5f, 5f, 0xFFFFFFFF.toInt(), 1f, innerX, panelY, PANEL_SIZE, PANEL_SIZE, RADIUS)

        graphics.drawGradientRectMulti(innerX, hueY, PANEL_SIZE, HUE_BAR_H, HUE_COLORS, null, SkikoGradient.LEFT_RIGHT, RADIUS)
        graphics.drawRoundedRectWithBorder(innerX, hueY, PANEL_SIZE, HUE_BAR_H, 0, Theme.controlBorder, 1f, RADIUS)

        val hx = innerX + PANEL_SIZE * cachedH
        val hy = hueY + HUE_BAR_H / 2f
        graphics.drawSkikoSquareClipped(hx, hy, 5f, 9f, 0xFFFFFFFF.toInt(), 1f, innerX, hueY, PANEL_SIZE, HUE_BAR_H, RADIUS)
    }

    fun isPointInPopup(mx: Float, my: Float): Boolean =
        isOpen && HudUtils.isPointInRect(mx, my, rectX, rectY, rectW, rectH)

    fun isPointInHex(mx: Float, my: Float): Boolean =
        isOpen && HudUtils.isPointInRect(mx, my, innerX, hexY, HEX_W, HEX_H)

    fun isPointInAlpha(mx: Float, my: Float): Boolean =
        isOpen && HudUtils.isPointInRect(mx, my, alphaX, hexY, ALPHA_W, HEX_H)

    fun handlePopupClick(mx: Float, my: Float, button: Int, doubleClick: Boolean): Boolean {
        if (!isOpen || button != InputConstants.MOUSE_BUTTON_LEFT) return false
        if (!isPointInPopup(mx, my)) return false

        if (!isPointInAlpha(mx, my)) alphaBox.defocus()

        if (HudUtils.isPointInRect(mx, my, innerX, hexY, HEX_W, HEX_H)) {
            alphaBox.defocus()
            val screen = mc.gui.screen()
            if (screen is ClickGUIScreen) {
                if (screen.activeEditBoxSetting == setting) {
                    screen.activeSkikoEditBox?.handleMouseClicked(mx, my, innerX, hexY, HEX_W, HEX_H, doubleClick)
                } else {
                    screen.activateSkikoEditBox(setting, HudUtils.toHexStringRGB(setting.value), maxLength = 7, insetY = TEXT_INSET_Y)
                    playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
                }
            }
            return true
        }

        if (HudUtils.isPointInRect(mx, my, alphaX, hexY, ALPHA_W, HEX_H)) {
            val screen = mc.gui.screen()
            if (screen is ClickGUIScreen) screen.deactivateEditBox()
            if (!alphaBox.isFocused) {
                playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            }
            alphaBox.focus()
            alphaBox.handleMouseClicked(mx, my, alphaX, hexY, ALPHA_W, HEX_H, doubleClick)
            return true
        }

        if (HudUtils.isPointInRect(mx, my, innerX, panelY, PANEL_SIZE, PANEL_SIZE)) {
            dragTarget = DragTarget.PANEL
            updateFromPanel(mx, my)
            return true
        }

        if (HudUtils.isPointInRect(mx, my, innerX, hueY, PANEL_SIZE, HUE_BAR_H)) {
            dragTarget = DragTarget.HUE
            updateFromHue(mx, my)
            return true
        }
        return false
    }

    fun handlePopupDrag(mx: Float, my: Float): Boolean {
        if (alphaBox.handleMouseDragged(mx, my)) return true
        when (dragTarget) {
            DragTarget.PANEL -> updateFromPanel(mx, my)
            DragTarget.HUE -> updateFromHue(mx, my)
            null -> return false
        }
        return true
    }

    fun handlePopupRelease(): Boolean {
        alphaBox.handleMouseReleased()
        if (dragTarget == null) return false
        dragTarget = null
        setting.saveValue()
        return true
    }

    fun handleKeyPressed(event: KeyEvent): Boolean =
        alphaBox.isFocused && alphaBox.handleKeyPressed(event)

    fun handleCharTyped(event: CharacterEvent): Boolean =
        alphaBox.isFocused && alphaBox.handleCharTyped(event)

    fun handlePreedit(event: PreeditEvent?): Boolean {
        if (!alphaBox.isFocused) return false
        if (event == null) {
            alphaBox.clearPreedit()
            return true
        }
        return alphaBox.handlePreedit(event)
    }

    private fun argbOf(h: Float, s: Float, b: Float): Int {
        val rgb = HudUtils.hsvToRgb(h.coerceIn(0f, 1f), s.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
        return (rgb and 0xFFFFFF) or (setting.value and 0xFF000000.toInt())
    }

    private fun updateFromPanel(mx: Float, my: Float) {
        cachedS = ((mx - innerX) / PANEL_SIZE).coerceIn(0f, 1f)
        cachedB = 1f - ((my - panelY) / PANEL_SIZE).coerceIn(0f, 1f)
        setting.set(argbOf(cachedH, cachedS, cachedB))
    }

    private fun updateFromHue(mx: Float, my: Float) {
        cachedH = ((mx - innerX) / PANEL_SIZE).coerceIn(0f, 1f)
        setting.set(argbOf(cachedH, cachedS, cachedB))
    }

    companion object {
        private const val TEXT_INSET_X = 5f
        private const val TEXT_INSET_Y = 1f

        private const val PANEL_SIZE = 100f
        private const val RADIUS = 3f
        private const val HEX_W = 46f
        private const val HEX_H = 13f
        private const val ALPHA_W = 22f
        private const val H_GAP = 3f
        private const val PERCENT_W = 12f
        private const val HUE_BAR_H = 12f
        private const val GAP = 6f
        private const val POPUP_PAD = 6f
        private const val SCREEN_MARGIN = 10f

        private val HUE_COLORS: List<Int> by lazy {
            (0..6).map { HudUtils.hsvToRgb(it / 6f, 1f, 1f) }
        }
    }
}
