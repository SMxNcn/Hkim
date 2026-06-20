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
    private val wardrobe by KeybindSetting("Wardrobe", "Opens the wardrobe menu.")
    private val equipment by KeybindSetting("Equipment", "Opens the equipment menu.")
    private val potion by KeybindSetting("Potion Bag", "Open the potion bag.")
    private val cookie by KeybindSetting("Cookie Menu", "Opens the cookie menu.")

    @EventHandler
    private fun onKeyPressed(event: InputEvent) {
        if (!enabled || mc.screen != null || !LocationUtils.inSkyBlock) return
        when (event.key.value) {
            pet -> sendCommand("pets")
            wardrobe -> sendCommand("wardrobe")
            equipment -> sendCommand("equipment")
            potion -> sendCommand("potionbag")
            cookie -> sendCommand("boostercookie")
        }
    }
}