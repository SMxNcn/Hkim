package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.MouseButtonEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.WebBlock

@ModuleInfo("anti_place", Category.MISC)
object AntiPlace : Module("Anti Place", "Prevent placing block-based weapons and items on the ground.") {
    private val ABILITY_REGEX = Regex("(?:⦾\\s*)?Ability:\\s+.+\\s+RIGHT CLICK")
    private val EXTRA_ITEM_IDS = setOf(
        "ASCENSION_ROPE", "ATOMINIZER", "CHARMINIZER", "CHUM_BUCKET", "FISHING_NET", "GIFT", "KUUDRA_CHUNK", "SACK", "SHINY_ORB", "SLICE_OF",
        "SUMMONING_EYE", "SUPER_SCRUBBER", "TUBA"
    )

    @EventHandler
    private fun onMouseClick(event: MouseButtonEvent) {
        if (!enabled || event.button != 1) return
        if (!LocationUtils.inSkyBlock || LocationUtils.isCurrentArea(Island.PrivateIsland)) return
        val stack = mc.player?.mainHandItem ?: return
        if (!isBlockCarrier(stack)) return

        if (shouldAllowRightClick(stack)) return
        event.isCancelled = true
    }

    private fun hasRightClickAction(stack: ItemStack): Boolean {
        val lore = stack.loreString
        return lore.any { line ->
            line.clean.containsOneOf("PET ITEM", "Right-click to", "Right-click on your summoned pet") ||
            ABILITY_REGEX.matches(line)
        }
    }

    private fun shouldAllowRightClick(stack: ItemStack): Boolean {
        if (hasRightClickAction(stack)) return true
        if (stack.itemId.containsOneOf(EXTRA_ITEM_IDS)) return true
        return false
    }

    @JvmStatic
    fun shouldCancelPlacement(stack: ItemStack): Boolean {
        if (!enabled) return false
        if (hasRightClickAction(stack)) return true
        if (stack.itemId.containsOneOf(EXTRA_ITEM_IDS)) return true
        val item = stack.item
        return item is BlockItem && item.block is WebBlock
    }
}