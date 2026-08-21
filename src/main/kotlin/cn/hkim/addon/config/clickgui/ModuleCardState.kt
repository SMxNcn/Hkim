package cn.hkim.addon.config.clickgui

import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.features.Module
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRect
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoLine
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ModuleCardState(val module: Module) {
    var targetExpanded = false
        private set

    private val expandAnim = GuiAnimation.create(0f, 0f)
        .duration(150L)
        .easing(Easing.CUBIC_OUT)

    private val openAnim = GuiAnimation.create(0f, 0f)
        .duration(150L)
        .easing(Easing.CUBIC_OUT)

    private var animatedExpandedHeight = 0f

    private var targetEnabled = module.enabled
    private var lerpEnabled = if (targetEnabled) 1f else 0f

    private val settingHeight = Theme.SETTING_HEIGHT
    private val settingGap = Theme.SETTING_GAP

    private var draggingSetting: Setting<*>? = null
    private var draggingSettingY: Float = 0f

    private val visibleSettings: List<Setting<*>>
        get() {
            val visible = module.settings.filter { it.isVisible() }
            for (s in module.settings) {
                if (s is DropdownSetting && s !in visible && s.get()) {
                    s.set(false)
                }
            }
            return visible
        }

    val totalHeight: Float
        get() = Theme.CARD_HEIGHT + expandAnim.getValue()

    fun update(deltaTime: Float) {
        val factor = min(1f, deltaTime * 10f)

        if (targetEnabled != module.enabled) targetEnabled = module.enabled
        lerpEnabled = HudUtils.lerp(lerpEnabled, if (targetEnabled) 1f else 0f, factor)

        if (targetExpanded) {
            val currentH = calculateCurrentVisibleHeight() + 8f
            if (abs(currentH - animatedExpandedHeight) > 0.5f) {
                animatedExpandedHeight = currentH
                expandAnim.animateTo(currentH)
            }
        }
    }

    fun render(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float, visibleTop: Float, visibleBottom: Float, themeColor: Int, delta: Float): Float {
        val cardH = Theme.CARD_HEIGHT
        val currentExpandedH = expandAnim.getValue()

        val isHovered = Setting.activeModalPopup == null
            && mouseY in visibleTop..visibleBottom
            && HudUtils.isPointInRect(mouseX, mouseY, x, y, width, Theme.CARD_HEIGHT)

        val enabledEdge = HudUtils.lerpColor(0xFF000000.toInt(), themeColor, Theme.cardEdgeEnabledFactor)
        val edgeColor = HudUtils.lerpColor(Theme.cardEdgeDisabled, enabledEdge, lerpEnabled)
        val nameColor = HudUtils.lerpColor(Theme.textNameMuted, themeColor, lerpEnabled)
        val fillColor = if (isHovered) Theme.cardFillHover else Theme.cardFill

        graphics.drawRoundedRectWithBorder(x + 1f, y + 1f, width, totalHeight, edgeColor, 0, 0f, 4f)
        graphics.drawRoundedRect(x, y, width, totalHeight, fillColor, 4f)

        val openProgress = openAnim.getValue()
        if (openProgress > 0.01f) {
            val halfLen = ((width - 24f) * openProgress) / 2f
            if (halfLen > 0f) {
                val lineY = y + cardH + 0.5f
                graphics.drawSkikoLine(x + width / 2f - halfLen, lineY, x + width / 2f + halfLen, lineY, Color(0x30FFFFFF, true).rgb, 1f)
            }
        }

        graphics.drawSkikoText(module.name, x + 14f, y + 8f, Theme.CARD_FONT_SIZE, nameColor, bold = true)
        graphics.drawSkikoText(module.description, x + 14f, y + 23f, Theme.CARD_FONT_SIZE, Theme.textMuted)

        if (isHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }

        if (currentExpandedH > 0.01f) {
            val scissorTop = y + cardH
            val scissorBottom = y + totalHeight

            if (scissorBottom > visibleTop && scissorTop < visibleBottom) {
                val sX1 = (x - 4f).toInt()
                val sY1 = max(scissorTop, visibleTop).toInt()
                val sX2 = (x + width + 4f).toInt()
                val sY2 = min(scissorBottom, visibleBottom).toInt()
                if (sX2 > sX1 && sY2 > sY1) {
                    graphics.enableScissor(sX1, sY1, sX2, sY2)

                    var sy = y + cardH + 6f
                    for (setting in visibleSettings) {
                        val settingTop = sy
                        val settingBottom = sy + settingHeight + settingGap
                        if (settingBottom >= scissorTop && settingTop <= scissorBottom) {
                            val indent = 12f
                            setting.render(graphics, x + indent, sy, width - indent * 2, mouseX, mouseY, themeColor, delta, visibleTop, visibleBottom)
                        }
                        sy += settingHeight + settingGap
                    }

                    graphics.disableScissor()
                }
            }
        }

        return totalHeight
    }

    fun handleClick(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float, visibleTop: Float, visibleBottom: Float, doubleClick: Boolean = false): Boolean {
        val cardH = Theme.CARD_HEIGHT

        if (mouseY in visibleTop..visibleBottom
            && HudUtils.isPointInRect(mouseX, mouseY, x, y, width, cardH)) {
            when (button) {
                InputConstants.MOUSE_BUTTON_LEFT -> { module.toggle(); return true }
                InputConstants.MOUSE_BUTTON_RIGHT -> {
                    if (visibleSettings.isNotEmpty()) {
                        targetExpanded = !targetExpanded
                        if (targetExpanded) {
                            animatedExpandedHeight = calculateCurrentVisibleHeight() + 8f
                            expandAnim.animateTo(animatedExpandedHeight)
                            openAnim.animateTo(1f)
                        } else {
                            expandAnim.animateTo(0f)
                            openAnim.animateTo(0f)
                        }
                    }
                    return true
                }
            }
        }

        if (expandAnim.getValue() > 0.01f) {
            var sy = y + cardH + 6f
            for (setting in visibleSettings) {
                if (!setting.isVisible()) continue
                if (sy + settingHeight < visibleTop || sy > visibleBottom) {
                    sy += settingHeight + settingGap
                    continue
                }
                val indent = 12f
                if (setting.mouseClicked(mouseX, mouseY, button, x + indent, sy, width - indent * 2, doubleClick)) {
                    draggingSetting = setting
                    draggingSettingY = sy
                    return true
                }
                sy += settingHeight + settingGap
            }
        }

        return false
    }

    fun handleDrag(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (draggingSetting != null) {
            val indent = 12f
            return draggingSetting!!.mouseDragged(
                mouseX, mouseY, button, 0f, 0f,
                x + indent, draggingSettingY, width - indent * 2
            )
        }
        return false
    }

    fun handleScroll(mouseX: Float, mouseY: Float, scrollX: Double, scrollY: Double, x: Float, y: Float, width: Float, visibleTop: Float, visibleBottom: Float): Boolean {
        if (expandAnim.getValue() <= 0.01f) return false

        val cardH = Theme.CARD_HEIGHT
        var sy = y + cardH + 6f
        for (setting in visibleSettings) {
            if (!setting.isVisible()) continue
            if (sy + settingHeight < visibleTop || sy > visibleBottom) {
                sy += settingHeight + settingGap
                continue
            }
            val indent = 12f
            if (setting.mouseScrolled(mouseX, mouseY, scrollX, scrollY, x + indent, sy, width - indent * 2)) {
                return true
            }
            sy += settingHeight + settingGap
        }
        return false
    }

    fun handleRelease(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (draggingSetting != null) {
            val indent = 12f
            draggingSetting!!.mouseReleased(
                mouseX, mouseY, button,
                x + indent, draggingSettingY, width - indent * 2
            )
            draggingSetting = null
            return true
        }
        return false
    }

    private fun calculateCurrentVisibleHeight(): Float {
        return visibleSettings.sumOf { (settingHeight + settingGap).toDouble() }.toFloat()
    }
}
