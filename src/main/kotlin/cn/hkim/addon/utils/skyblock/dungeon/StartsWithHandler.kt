package cn.hkim.addon.utils.skyblock.dungeon

import cn.hkim.addon.utils.hasGlint
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class StartsWithHandler(private val letter: String) : TerminalHandler(TerminalEnums.START_WITH) {
    /** 天生自带附魔光效的原版物品（豁免 glint 排除）。 */
    private val naturallyGlinted = setOf(
        Items.NETHER_STAR,
        Items.EXPERIENCE_BOTTLE,
        Items.ENCHANTED_GOLDEN_APPLE,
        Items.ENCHANTED_BOOK,
        Items.WRITTEN_BOOK,
    )

    override fun canSolve(slots: List<Slot>): Boolean =
        slots.any { it.item.isTarget() }

    override fun solve(slots: List<Slot>): List<Int> =
        slots.mapNotNull { slot ->
            if (slot.item.isTarget()) slot.index else null
        }

    private fun ItemStack.isTarget(): Boolean {
        if (!hoverName.string.startsWith(letter, true)) return false
        return !hasGlint || item in naturallyGlinted
    }
}
