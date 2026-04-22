package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.HudUtils.getChromaColor
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import java.awt.Color

@ModuleInfo("module_list", Category.MISC, false)
object ModuleList : Module("Module List", "Enabled features list.") {
    private val chromaColor by BooleanSetting("Use Chroma Color", "", false)
    private val staticColor by ColorSetting("Static Color", "", Color(5, 186, 115).rgb).depends { !chromaColor }
    private val startColor by ColorSetting("Start Color", "", Color(200, 200, 200).rgb).depends { chromaColor }
    private val endColor by ColorSetting("End Color", "", Color(131, 131, 131).rgb).depends { chromaColor }
    private val chromaSpeed by NumberSetting("Chroma Speed ", "", 5, 1, 10, 1).depends { chromaColor }

    private val listLayer: Identifier = Identifier.fromNamespaceAndPath("hkim", "module_list")
    private var timeOffset = 0L

    init {
        HudElementRegistry.attachElementBefore(VanillaHudElements.SLEEP, listLayer, this::render)
    }

    override fun render(graphics: GuiGraphicsExtractor, tickTracker: DeltaTracker) {
        if (!enabled || mc.options.hideGui) return
//        val moduleList = ModuleManager.getEnabledToName().sortedByDescending { mc.font.width(it) }
        // Test List
        val moduleList = listOf("AutoClicker", "AutoGG", "AutoLeap", "AutoWardrobe", "ChatCommands", "CropNuker", "Etherwarp", "EtherwarpRouter", "HurtCamera", "Nametags", "AutoI4").sortedByDescending { mc.font.width(it) }
        if (moduleList.isEmpty()) return

        timeOffset = (timeOffset + 1) % 360

        val screenWidth = mc.window.guiScaledWidth
        val x = screenWidth - 2
        var y = 4

        val fontHeight = mc.font.lineHeight
        val spacingVal = 3
        val bgPadding = 2

        for ((index, moduleName) in moduleList.withIndex()) {
            val moduleWidth = mc.font.width(moduleName)
            val textColor = if (chromaColor) getChromaColor(Color(startColor), Color(endColor), index + 1, chromaSpeed.toInt(), index / 2).rgb else staticColor

            val bgWidth = moduleWidth + bgPadding * 2
            val textX = x - moduleWidth - bgPadding

            graphics.fill(x - bgWidth - 2, y - bgPadding, x, y + fontHeight + 1, Color(53, 53, 53, 115).rgb)
            graphics.text(mc.font, moduleName, textX - 2, y, textColor, true)
            graphics.fill(x - 1, y - bgPadding, x, y + fontHeight + 1, textColor)

            y += fontHeight + spacingVal
        }
    }
}