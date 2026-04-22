package cn.hkim.addon.features.impl

import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo

@ModuleInfo("click_gui", category = Category.MISC, true)
object ClickGUI : Module("Click GUI", "Click GUI settings.") {
    private val themeColor by ColorSetting("Theme Color", "Theme color of click gui.", 0xFF4A90E2.toInt())

    fun getGuiColor() = themeColor
}