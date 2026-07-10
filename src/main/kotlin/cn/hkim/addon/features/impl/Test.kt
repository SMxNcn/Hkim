package cn.hkim.addon.features.impl

import cn.hkim.addon.config.settings.*
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.playSoundAtPlayer
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import org.lwjgl.glfw.GLFW

@ModuleInfo("test", Category.MISC)
object Test : Module("Test", "A Test Module.") {
    private val dropdown1 by DropdownSetting("Dropdown 1", "D1", false)
    private val test1 by BooleanSetting("Boolean 1", "这是一个Boolean", true).depends { dropdown1 }
    private val num1 by NumberSetting("Number 1", "这是一个Number", 0.5f, 0.1f, 10f, 0.1f).depends { dropdown1 }
    private val color1 by ColorSetting("Color 1", "这是一个Color", 0xFF008B8B.toInt()).depends { dropdown1 }
    private val text1 by TextSetting("Text 1", "这是一段Text", "哎嘿").depends { dropdown1 }
    private val dropdown2 by DropdownSetting("Dropdown 2", "D2", false)
    private val selector1 by SelectorSetting("Selector 1", "这是一个Selector", listOf("1", "2", "3", "4", "5", "6"), "1").depends { dropdown2 }
    private val action1 by ActionSetting("Action 1", "这是一个Action") {
        val zxf = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("hkim", "zxf2"))
        playSoundAtPlayer(zxf)
    }
    private val key1 by KeybindSetting("Keybind 1", "这是一个按键绑定", GLFW.GLFW_KEY_UNKNOWN)
}