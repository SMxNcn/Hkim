package cn.hkim.addon.utils.skyblock.dungeon

import cn.hkim.addon.utils.hasGlint
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class SelectAllHandler(colorName: String) : TerminalHandler(TerminalEnums.SELECT) {
    private val validPrefixes = when (colorName.lowercase().replace("_", " ")) {
        "black" -> setOf("black", "ink")
        "blue" -> setOf("blue", "lapis")
        "brown" -> setOf("brown", "cocoa")
        "white" -> setOf("white", "bone", "wool")
        "green" -> setOf("green", "cactus")
        "red" -> setOf("red", "rose")
        "yellow" -> setOf("yellow", "dandelion")
        "silver", "light gray" -> setOf("silver", "light gray")
        else -> setOf(colorName.lowercase().replace("_", " "))
    }

    private val blackPane = Items.BLACK_STAINED_GLASS_PANE

    override fun canSolve(slots: List<Slot>): Boolean = slots.any { it.item.isTarget() }

    override fun solve(slots: List<Slot>): List<Int> =
        slots.mapNotNull { slot -> if (slot.item.isTarget()) slot.index else null }

    private fun ItemStack.isTarget(): Boolean {
        if (hasGlint || item == blackPane) return false
        val name = hoverName.string.lowercase()
        return validPrefixes.any { name.startsWith(it) }
    }
}
