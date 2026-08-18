package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.ClickGUIScreen
import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoTextClipped
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoTextWidth
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents

class TextSetting(name: String, desc: String, default: String) : Setting<String>(name, desc, default) {
    init { value = default }

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

        val inputX = x + 85f
        val inputW = width - 95f
        val inputY = y + 1f
        val inputH = 16f

        val screen = mc.gui.screen()
        val isActive = screen is ClickGUIScreen && screen.activeEditBoxSetting == this

        if (isActive) {
            screen.activeSkikoEditBox?.render(graphics, inputX, inputY, inputW, inputH, mouseX, mouseY, themeColor, Theme.CARD_FONT_SIZE)
        } else {
            graphics.drawRoundedRectWithBorder(inputX, inputY, inputW, inputH, Theme.controlBg, Theme.controlBorder, 1f, 3f)
            if (HudUtils.isPointInRect(mouseX, mouseY, inputX, inputY, inputW, inputH)) {
                graphics.requestCursor(CursorTypes.IBEAM)
            }
            // 预览文本：超宽时在框内左右循环滚动（跑马灯），裁剪到输入框背景内
            drawPreviewText(graphics, value, inputX, inputY, inputW, inputH)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    /** 预览文本：超宽时窗口在文本上左右往返滚动（ping-pong），单程 2s，裁剪于框内 */
    private fun drawPreviewText(
        graphics: GuiGraphicsExtractor,
        text: String,
        boxX: Float, boxY: Float, boxW: Float, boxH: Float
    ) {
        val size = Theme.CARD_FONT_SIZE
        val textW = skikoTextWidth(text, size)
        // 滚动范围补偿：5f 左内边距 + 1f 右内缩 + 2f 尾部缓冲 → 末尾字符完整滚入
        val scrollable = textW - boxW + 8f

        if (scrollable <= 0f) {
            graphics.drawSkikoText(text, boxX + 5f, boxY + 2.5f, size, Theme.controlTextActive)
            return
        }

        // 移动 1.5s → 端点停留 0.5s → 反向移动 1.5s → 端点停留 0.5s
        val moveMs = 3000L
        val holdMs = 500L
        val cycleMs = moveMs * 2 + holdMs * 2
        val t = (System.currentTimeMillis() % cycleMs).toFloat()
        val progress = when {
            t < moveMs -> t / moveMs                                  // 前进 0→1
            t < moveMs + holdMs -> 1f                                 // 尾部停留
            t < moveMs * 2 + holdMs -> 2f - (t - holdMs) / moveMs     // 后退 1→0
            else -> 0f                                                // 头部停留
        }
        val offset = progress * scrollable

        // 裁剪区左右各内缩 1px（文字不贴边）
        graphics.drawSkikoTextClipped(text, boxX + 5f - offset, boxY + 2.5f, size, Theme.controlTextActive, boxX + 1f, boxY, boxW - 2f, boxH)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float, doubleClick: Boolean): Boolean {
        if (button != 0) return false

        val inputX = x + 85f
        val inputW = width - 95f
        val inputY = y + 1f
        val inputH = 16f

        if (HudUtils.isPointInRect(mouseX, mouseY, inputX, inputY, inputW, inputH)) {
            val screen = mc.gui.screen()
            if (screen is ClickGUIScreen) {
                if (screen.activeEditBoxSetting == this) {
                    // 已聚焦：点击定位光标（含双击选词）
                    screen.activeSkikoEditBox?.handleMouseClicked(mouseX, mouseY, inputX, inputY, inputW, inputH, doubleClick)
                } else {
                    screen.activateSkikoEditBox(this, value)
                    playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
                }
            }
            return true
        }
        return false
    }
}