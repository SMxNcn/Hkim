package cn.hkim.addon.config.clickgui

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.features.Module
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.pip.ShapeRenderer.drawRoundedRectWithBorder
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
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

    private var animatedExpandedHeight = 0f

    private var targetEnabled = module.enabled
    private var lerpEnabled = if (targetEnabled) 1f else 0f

    private val settingHeight = 20f
    private val settingGap = 3f

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
        get() = 44f + expandAnim.getValue()

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
        val cardH = 44f
        val currentExpandedH = expandAnim.getValue()

        val isHovered = mouseY in visibleTop..visibleBottom
            && HudUtils.isPointInRect(mouseX, mouseY, x, y, width, 44f)

        val borderColor = HudUtils.lerpColor(0xFF333333.toInt(), themeColor, lerpEnabled)
        val nameColor = HudUtils.lerpColor(0xFFFFFFFF.toInt(), themeColor, lerpEnabled)
        val bgColor = if (isHovered) 0x10BFBFBF else 0x10222222

        graphics.drawRoundedRectWithBorder(x, y, width, totalHeight, bgColor, borderColor, 1f, 4f)
        if (targetExpanded) {
            graphics.horizontalLine((x + 12f).toInt(), (x + width - 12f).toInt(), (y + cardH).toInt(), Color(0x30FFFFFF, true).rgb)
        }

        graphics.text(mc.font, Component.literal(module.name).withStyle(ChatFormatting.BOLD), x.toInt() + 14, y.toInt() + 12, nameColor, false)
        graphics.text(mc.font, module.description, x.toInt() + 14, y.toInt() + 28, 0xFF888888.toInt(), false)

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
                            val indent = 24f
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

    fun handleClick(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float, visibleTop: Float, visibleBottom: Float): Boolean {
        val cardH = 44f

        if (mouseY in visibleTop..visibleBottom
            && HudUtils.isPointInRect(mouseX, mouseY, x, y, width, cardH)) {
            when (button) {
                0 -> { module.toggle(); return true }
                1 -> {
                    if (visibleSettings.isNotEmpty()) {
                        targetExpanded = !targetExpanded
                        if (targetExpanded) {
                            animatedExpandedHeight = calculateCurrentVisibleHeight() + 8f
                            expandAnim.animateTo(animatedExpandedHeight)
                        } else {
                            expandAnim.animateTo(0f)
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
                val indent = 24f
                if (setting.mouseClicked(mouseX, mouseY, button, x + indent, sy, width - indent * 2)) {
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
            val indent = 24f
            return draggingSetting!!.mouseDragged(
                mouseX, mouseY, button, 0f, 0f,
                x + indent, draggingSettingY, width - indent * 2
            )
        }
        return false
    }

    fun handleRelease(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (draggingSetting != null) {
            val indent = 24f
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
