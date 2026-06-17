package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.config.settings.SelectorSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.hud.HudAlignment
import cn.hkim.addon.hud.HudElement
import cn.hkim.addon.utils.ServerUtils
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

@ModuleInfo("performance_hud", Category.MISC, false)
object PerformanceHUD : Module("Performance HUD", "Shows performance information on the screen.") {
    private val valueColor by ColorSetting("Value Color", "The color of the metric values.", Color(255, 255, 255).rgb)
    private val direction by SelectorSetting("Direction", "Direction the information is displayed.", listOf("Horizontal", "Vertical"), "Horizontal")
    private val showFPS by BooleanSetting("Show FPS", "Shows the FPS in the HUD.", true)
    private val showTPS by BooleanSetting("Show TPS", "Shows the TPS in the HUD.", true)
    private val showPing by BooleanSetting("Show Ping", "Shows the ping in the HUD.", true)

    private val hud by HudElement("Performance", "Shows performance information on the screen.",
        x = 6f, y = 6f, alignment = HudAlignment.TOP_LEFT
    ) { graphics, _ ->
        if (this@PerformanceHUD.enabled) renderContent(graphics) else Pair(0f, 0f)
    }

    private fun renderContent(graphics: GuiGraphicsExtractor): Pair<Float, Float> {
        if (!showFPS && !showTPS && !showPing) return Pair(0f, 0f)

        val padding = 2
        val lineHeight = mc.font.lineHeight
        val isHorizontal = direction == 0

        var cursorX = padding
        var cursorY = padding
        var maxW = 0

        fun metric(label: String, value: String) {
            val labelW = mc.font.width(label)
            val valueW = mc.font.width(value)
            val metricW = labelW + valueW

            graphics.text(mc.font, label, cursorX, cursorY, ClickGUI.getGuiColor(), true)
            graphics.text(mc.font, value, cursorX + labelW, cursorY, valueColor, true)

            if (isHorizontal) {
                cursorX += metricW
                maxW = cursorX
            } else {
                maxW = maxOf(maxW, cursorX + metricW)
                cursorY += lineHeight
            }
        }

        if (showTPS) metric("TPS: ", "${ServerUtils.averageTps.toInt()} ")
        if (showFPS) metric("FPS: ", "${mc.fps} ")
        if (showPing) metric("Ping: ", "${ServerUtils.averagePing}ms")

        val totalW = maxW + padding
        val totalH = (if (isHorizontal) cursorY + lineHeight else cursorY) + padding
        return Pair(totalW.toFloat(), totalH.toFloat())
    }
}
