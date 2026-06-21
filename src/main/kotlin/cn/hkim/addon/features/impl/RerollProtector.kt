package cn.hkim.addon.features.impl

import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.TextSetting
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.Container
import net.minecraft.world.inventory.ChestMenu

@ModuleInfo("reroll_protector", Category.SKYBLOCK, true)
object RerollProtector : Module("Reroll Protector", "Prevent reroll when rare rewards appear in reward chests.") {
    private val sendRngMessage by BooleanSetting("Send RNG Message", "Send rare item name to party.", true)
    private val message by TextSetting("RNG Message", "Use %i for rng item name, %c for chest name.", "%i in %c!")

    private const val REROLL_BUTTON_ID = 50
    private var hasRareItems = false
    private var hasShownMessage = false
    private var rareItemSlot = -1
    private var lastRareItemName: String? = null
    private var lastCheckedChest: String? = null
    private var lastRawItemName: String? = null

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

    @EventHandler
    private fun onGuiOpen(event: GuiEvent.Open) {
        if (!(LocationUtils.inDungeons || LocationUtils.inKuudra) || LocationUtils.currentArea == Island.DungeonHub) return
        val chest = (event.screen as? AbstractContainerScreen<*>) ?: return
        if (lastCheckedChest != chest.title.string) {
            hasShownMessage = false
            lastCheckedChest = getChestColor(chest.title.string)
        }

        if (!isRewardChest(chest)) return
        val menu = chest.menu as? ChestMenu ?: return
        val container = menu.container

        schedule(4) {
            hasRareItems = hasRareLoot(container)
            if (hasRareItems && !hasShownMessage && lastRareItemName != null) {
                hasShownMessage = true
                sendMessage(lastRawItemName!!, lastCheckedChest!!)
            }
        }
    }

    @EventHandler
    private fun onSlotClock(event: GuiEvent.SlotClick) {
        if (!hasRareItems || event.slotId != REROLL_BUTTON_ID || !(LocationUtils.inDungeons || LocationUtils.inKuudra)) return
        if (event.button == 0 || event.button == 1) {
            event.cancel()
            modMessage("§cReroll button has been §lDISABLED§r§c!")
        }
    }

    @EventHandler
    private fun onGuiClose(event: GuiEvent.Close) {
        resetState()
        lastCheckedChest = null
    }

    @EventHandler
    private fun onWorldChange(event: WorldEvent.Unload) {
        resetState()
    }

    private fun isRewardChest(chest: AbstractContainerScreen<*>) =
        chest.title.string.equalsOneOf("Wood", "Gold", "Diamond", "Emerald", "Obsidian", "Bedrock", "Free", "Paid")

    private fun hasRareLoot(container: Container): Boolean {
        val containerSize = container.containerSize

        for (i in 9..26) {
            if (i >= containerSize) break
            val stack = container.getItem(i)

            if (!stack.isEmpty) {
                val rawDisplayName = stack.displayName.string
                var cleanName = rawDisplayName.clean
                cleanName = cleanName.replace(Regex("\\[(.*?)]"), "$1")

                if (RARE_ITEMS.contains(cleanName)) {
                    rareItemSlot = i
                    lastRareItemName = cleanName
                    lastRawItemName = stack.displayName.legacy.replace(Regex("\\[(.*?)]"), "$1")
                    return true
                }
            }
        }

        return false
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

    private fun resetState() {
        hasRareItems = false
        rareItemSlot = -1
        hasShownMessage = false
        lastRareItemName = null
    }
}