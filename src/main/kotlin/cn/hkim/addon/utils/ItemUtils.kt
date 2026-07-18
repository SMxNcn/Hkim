package cn.hkim.addon.utils

import cn.hkim.addon.Hkim.mc
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState

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

inline val ItemStack.isSword: Boolean
    get() = `is` { it.`is`(ItemTags.SWORDS) }

val strengthRegex = Regex("Strength: \\+(\\d+)")

inline val ItemStack.strength: Int
    get() = this.loreString.firstOrNull {
        it.contains("Strength:")
    }?.let { lineString ->
        strengthRegex.find(lineString)?.groups?.get(1)?.value?.toIntOrNull()
    } ?: 0

inline val ItemStack.hasGlint: Boolean
    get() = get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) ?: false

inline val ItemStack.hasEthermerge: Boolean
    get() = customData.getInt("ethermerge").orElse(0) == 1

fun isSkyBlockItem(stack: ItemStack): Boolean {
    if (stack.isEmpty) return false
    val customData = stack.customData
    return customData.copy().contains("id")
}

fun findItemByID(itemID: String?, hotbar: Boolean = false): Int {
    if (itemID.isNullOrEmpty()) return -1
    val player = mc.player ?: return -1

    return (0 until if (hotbar) 9 else 36)
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

private fun isPlant(state: BlockState): Boolean {
    return state.`is`(BlockTags.REPLACEABLE_BY_TREES) ||
            state.`is`(BlockTags.MAINTAINS_FARMLAND) ||
            state.`is`(BlockTags.SAPLINGS) ||
            state.`is`(BlockTags.FLOWERS)
}

fun isBuildingBlock(stack: ItemStack?): Boolean {
    if (stack == null || stack.isEmpty) return false
    val item = stack.item
    if (item !is BlockItem) return false
    val block = item.block
    val state = block.defaultBlockState()

    if (state.isSolidRender) return true

    return !isBlockCarrier(block, state)
}

fun isBlockCarrier(stack: ItemStack?): Boolean {
    if (stack == null || stack.isEmpty) return false
    val item = stack.item
    if (item !is BlockItem) return false
    return isBlockCarrier(item.block, item.block.defaultBlockState())
}

private fun isBlockCarrier(block: Block, state: BlockState): Boolean {
    if (state.isSolidRender) return false
    return block is AbstractSkullBlock ||
            block is FlowerPotBlock ||
            isPlant(state) ||
            block is MushroomBlock ||
            block is CactusBlock ||
            block is SugarCaneBlock ||
            block is KelpBlock ||
            block is SeagrassBlock ||
            block is LilyPadBlock ||
            block is ChorusFlowerBlock ||
            block is ChorusPlantBlock ||
            block is CocoaBlock ||
            block is WebBlock ||
            block is SporeBlossomBlock ||
            block is AzaleaBlock ||
            block is FrogspawnBlock
}
