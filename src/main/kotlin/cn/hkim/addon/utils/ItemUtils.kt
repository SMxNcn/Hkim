package cn.hkim.addon.utils

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.skyblock.ItemRarity
import cn.hkim.addon.utils.skyblock.rarityRegex
import com.google.gson.JsonParser
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore

inline val ItemStack.customData: CompoundTag
    get() = getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()

inline val ItemStack.itemId: String
    get() = customData.getString("id").orElse("")!!

inline val CompoundTag.itemId: String
    get() = getString("id").orElse("")!!

inline val ItemStack.itemUUID: String
    get() = customData.getString("uuid").orElse("")!!

inline val ItemStack.itemUpgradeLevel: Int
    get() = customData.getInt("upgrade_level").orElse(0)!!

inline val ItemStack.lore: List<Component>
    get() = getOrDefault(DataComponents.LORE, ItemLore.EMPTY).styledLines()

inline val ItemStack.loreString: List<String>
    get() = lore.map { it.string }

inline val ItemStack.petInfo: String
    get() = customData.getString("petInfo").orElse("")!!

inline val ItemStack.isSword: Boolean
    get() = `is` { it.`is`(ItemTags.SWORDS) }

fun getItemRarity(itemStack: ItemStack): ItemRarity? {
    if (itemStack.itemId == "PET") {
        val petInfo = itemStack.petInfo
        if (petInfo.isNotEmpty()) {
            try {
                val json = JsonParser.parseString(petInfo).asJsonObject
                val tier = json.get("tier")?.asString
                if (tier != null) return ItemRarity.entries.find { it.name == tier }
            } catch (_: Exception) {}
        }
    }

    for (i in itemStack.loreString.indices.reversed()) {
        val rarity = rarityRegex.find(itemStack.loreString[i])?.groups?.get(1)?.value ?: continue
        return ItemRarity.entries.find { it.loreName == rarity }
    }
    return null
}

fun getTooltipStyle(rarity: ItemRarity): Identifier = Identifier.withDefaultNamespace(/*"hkim", */rarity.name.lowercase())

fun ItemStack.isEtherwarpItem(): CompoundTag? =
    customData.takeIf { it.getInt("ethermerge").orElse(0) == 1 || it.itemId == "ETHERWARP_CONDUIT" }

fun isSkyBlockItem(stack: ItemStack): Boolean {
    if (stack.isEmpty) return false
    val customData = stack.customData
    return customData.copy().contains("id")
}

fun findItemByID(itemID: String?): Int {
    if (itemID.isNullOrEmpty()) return -1
    val player = mc.player ?: return -1

    return (0 until 36)
        .firstOrNull { slot ->
            val stack = player.inventory.getItem(slot)
            !stack.isEmpty && stack.itemId.contains(itemID, ignoreCase = true)
        } ?: -1
}

fun isNormalRod(slot: Int): Boolean =
    mc.player?.let { player ->
        val stack = player.inventory.getItem(slot)
        !stack.isEmpty && stack.item == Items.FISHING_ROD && !stack.itemId.containsOneOf("SOUL_WHIP", "FLAMING_FLAY", ignoreCase = true)
    } ?: false

fun isLeapItem(slot: Int): Boolean =
    mc.player?.let { player ->
        val stack = player.inventory.getItem(slot)
        !stack.isEmpty && stack.item == Items.PLAYER_HEAD && stack.itemId.containsOneOf("INFINITE_SPIRIT_LEAP", "SPIRIT_LEAP", ignoreCase = true)
    } ?: false

fun findRodSlot(): Int = (0..8).firstOrNull { isNormalRod(it) } ?: -1

fun findLeapSlot(): Int = (0..8).firstOrNull { isLeapItem(it) } ?: -1
