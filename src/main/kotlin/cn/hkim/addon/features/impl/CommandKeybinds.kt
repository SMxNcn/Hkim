package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.KeybindSetting
import cn.hkim.addon.events.impl.InputEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.sendCommand
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler

@ModuleInfo("command_keybinds", Category.SKYBLOCK)
object CommandKeybinds : Module("Command Keybinds", "Various keybinds for common skyblock commands.") {
    private val pet by KeybindSetting("Pets", "Open the pets menu.")
    private val armorWd by KeybindSetting("Wardrobe", "Opens the wardrobe menu.")
    private val equipmentWd by KeybindSetting("Equipment Wardrobe", "Opens the equipment wardrobe menu.")
    private val stats by KeybindSetting("Stats", "Open the stats menu. (Swap single equipment)")
    private val potion by KeybindSetting("Potion Bag", "Open the potion bag.")
    private val cookie by KeybindSetting("Cookie Menu", "Opens the cookie menu.")

    @EventHandler
    private fun onKeyPressed(event: InputEvent) {
        if (!enabled || mc.screen != null || !LocationUtils.inSkyBlock) return
        when (event.key.value) {
            pet -> sendCommand("pets")
            armorWd -> sendCommand("wardrobe")
            equipmentWd -> sendCommand("equipment")
            stats -> sendCommand("stats")
            potion -> sendCommand("potionbag")
            cookie -> sendCommand("boostercookie")
        }
    }
}