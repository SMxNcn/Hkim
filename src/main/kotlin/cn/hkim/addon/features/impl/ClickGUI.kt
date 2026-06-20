package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.clickgui.ClickGUIScreen
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.config.settings.KeybindSetting
import cn.hkim.addon.events.impl.InputEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import meteordevelopment.orbit.EventHandler
import org.lwjgl.glfw.GLFW

@ModuleInfo("click_gui", category = Category.MISC, true)
object ClickGUI : Module("Click GUI", "Click GUI settings.") {
    private val themeColor by ColorSetting("Theme Color", "Theme color of click gui.", 0xFF3C96DC.toInt())
    private val keybind by KeybindSetting("Click GUI Keybind", "", GLFW.GLFW_KEY_RIGHT_ALT)

    fun getGuiColor() = themeColor

    @EventHandler
    private fun onKeyEvent(event: InputEvent) {
        if (mc.screen != null) return
        if (event.key.value == keybind) mc.setScreen(ClickGUIScreen(null))
    }

    override fun toggle() {}
}