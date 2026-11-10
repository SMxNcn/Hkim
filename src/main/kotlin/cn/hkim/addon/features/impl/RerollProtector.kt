package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.TextSetting
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.mixins.accessors.AbstractContainerScreenAccessor
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.Container
import net.minecraft.world.inventory.ChestMenu

@ModuleInfo("reroll_protector", Category.SKYBLOCK, true)
object RerollProtector : Module("Reroll Protector", "Prevent reroll rare rewards.") {
    private val sendRngMessage by BooleanSetting("Send RNG Message", "Send rare item name to party.", true)
    private val message by TextSetting("RNG Message", "Use %i for rng item name, %c for chest name.", "%i in %c!")

    private const val REROLL_BUTTON_ID = 50
    private var hasShownMessage = false

    private val RARE_ITEMS = setOf(
        "Shiny Necron's Handle", "Necron's Handle",
        "Implosion", "Wither Shield",
        "Shadow Warp", "Dark Claymore",
        "Giant's Sword", "Shadow Fury",
        "Necron Dye", "Livid Dye",
        "Master Skull - Tier 5", "Fifth Master Star",
        "Fourth Master Star", "Third Master Star",
        "Second Master Star", "First Master Star",
        "Spirit Mask", "Tentacle Dye",
        "Hellstorm Wand", "Tormentor"
    )

    private data class RareItem(val legacyName: String)

    @EventHandler
    private fun onWorldLoad(event: WorldEvent.Load) {
        hasShownMessage = false
    }

    @EventHandler
    private fun onGuiOpen(event: GuiEvent.Open) {
        if (!enabled) return
        val chest = event.screen as? AbstractContainerScreen<*> ?: return
        if (!inDungeonOrKuudra() || !isRewardChest(chest)) return
        val menu = chest.menu as? ChestMenu ?: return
        val container = menu.container

        schedule(2) {
            if (mc.gui.screen() !== chest) return@schedule
            if (hasShownMessage) return@schedule
            val rare = findRareItem(container) ?: return@schedule
            hasShownMessage = true
            sendMessage(rare.legacyName, getChestColor(chest.title.string))
        }
    }

    @EventHandler
    private fun onSlotClick(event: GuiEvent.SlotClick) {
        if (!enabled || event.slotId != REROLL_BUTTON_ID) return
        if (!isRareRewardChest(event.screen)) return
        event.cancel()
        modMessage("§cReroll button has been §lDISABLED§r§c!")
    }

    @EventHandler
    private fun onKeyPress(event: GuiEvent.KeyPress) {
        if (!enabled || !mc.options.keyDrop.matches(event.input)) return
        if (!isRareRewardChest(event.screen)) return
        val hovered = (event.screen as AbstractContainerScreenAccessor).hoveredSlot ?: return
        if (hovered.index != REROLL_BUTTON_ID) return
        event.cancel()
        modMessage("§cReroll button has been §lDISABLED§r§c!")
    }

    private fun inDungeonOrKuudra() = LocationUtils.inDungeons || LocationUtils.inKuudra

    private fun isRareRewardChest(screen: Screen?): Boolean {
        if (!inDungeonOrKuudra()) return false
        val chest = screen as? AbstractContainerScreen<*> ?: return false
        if (!isRewardChest(chest)) return false
        val menu = chest.menu as? ChestMenu ?: return false
        return findRareItem(menu.container) != null
    }

    private fun isRewardChest(chest: AbstractContainerScreen<*>) =
        chest.title.string.equalsOneOf("Wood", "Gold", "Diamond", "Emerald", "Obsidian", "Bedrock", "Free", "Paid")

    private fun findRareItem(container: Container): RareItem? {
        val containerSize = container.containerSize

        for (i in 9..26) {
            if (i >= containerSize) break
            val stack = container.getItem(i)
            if (stack.isEmpty) continue

            val cleanName = stack.displayName.string.clean.replace(Regex("\\[(.*?)]"), "$1")
            if (RARE_ITEMS.contains(cleanName)) {
                val legacyName = stack.displayName.legacy.replace(Regex("\\[(.*?)]"), "$1")
                return RareItem(legacyName)
            }
        }

        return null
    }

    private fun sendMessage(itemName: String, chestName: String) {
        val chatMessage = message.replace("%i", itemName).replace("%c", chestName)
        if (sendRngMessage) sendCommand("pc KM » ${chatMessage.clean}")
        modMessage("§dRng Item §7in $chestName§7! ($itemName§7)")
    }

    private fun getChestColor(chestName: String): String {
        return when (chestName.clean) {
            "Bedrock" -> "§8$chestName Chest"
            "Obsidian" -> "§5$chestName Chest"
            "Emerald" -> "§2$chestName Chest"
            "Diamond" -> "§b$chestName Chest"
            "Gold" -> "§6$chestName Chest"
            else -> "§f$chestName Chest" // Wood, Free, Paid
        }
    }
}