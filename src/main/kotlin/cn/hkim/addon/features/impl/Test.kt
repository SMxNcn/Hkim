package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.*
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.events.impl.MouseButtonEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.features.ModuleManager
import cn.hkim.addon.utils.cleanString
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.world.inventory.ChestMenu

@ModuleInfo("test", Category.MISC, true)
object Test : Module("Test", "A Test Module.") {
    private val dropdown1 by DropdownSetting("Dropdown 1", "D1", false)
    private val test1 by BooleanSetting("Boolean 1", "这是一个Boolean", true).key("test1").depends { dropdown1 }
    private val num1 by NumberSetting("Number 1", "这是一个Number", 0.5f, 0.1f, 10f, 0.1f).key("num_1").depends { dropdown1 }
    private val color1 by ColorSetting("Color 1", "这是一个Color", 0xFF008B8B.toInt()).depends { dropdown1 }
    private val text1 by TextSetting("Text 1", "这是一段Text", "哎嘿").key("text1").depends { dropdown1 }
    private val dropdown2 by DropdownSetting("Dropdown 2", "D2", false)
    private val selector1 by SelectorSetting("Selector 1", "这是一个Selector", listOf("1", "二", "③"), "1").depends { dropdown2 }
    private val cleanGui by BooleanSetting("Clean Gui View", "Stop clicking on blank name items in SkyBlock menus.", true).depends { dropdown2 }
    private val action1 by ActionSetting("Action 1", "这是一个Action") { println(ModuleManager.getEnabledToName()) }

    /*@EventHandler
    fun onExtract(event: RenderEvent.Extract) {
        //println("Receive packet: ${event.packet.type()}")
        if (!enabled || !test1) return
        val level = mc.level ?: return
        val player = mc.player ?: return

        for (entity in level.players()) {
            event.drawStyledBox(entity.renderBoundingBox, Color(0xFFFFFF), 1, false)
        }
    }*/

    @EventHandler
    fun onMouse(event: MouseButtonEvent) {
        if (!enabled) return
        if (event.button == 0 && LocationUtils.inSkyBlock) {
            modMessage("Left Click")
        }
    }

    @EventHandler
    fun onGuiClick(event: GuiEvent.SlotClick) {
        if (!cleanGui || !LocationUtils.inSkyBlock) return
        val chest = mc.player?.containerMenu as? ChestMenu ?: return
        val slot = chest.slots.getOrNull(event.slotId) ?: return
        if (slot.hasItem() && slot.item.displayName.cleanString == " ")
            event.cancel()
    }
}