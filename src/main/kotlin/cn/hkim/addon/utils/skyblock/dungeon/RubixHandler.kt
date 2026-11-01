package cn.hkim.addon.utils.skyblock.dungeon

import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class RubixHandler : TerminalHandler(TerminalEnums.RUBIX) {
    private val colorOrder = listOf(DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.GREEN, DyeColor.BLUE, DyeColor.RED)
    private val colorToItem = mapOf(
        DyeColor.ORANGE to Items.STAINED_GLASS_PANE.orange,
        DyeColor.YELLOW to Items.STAINED_GLASS_PANE.yellow,
        DyeColor.GREEN to Items.STAINED_GLASS_PANE.green,
        DyeColor.BLUE to Items.STAINED_GLASS_PANE.blue,
        DyeColor.RED to Items.STAINED_GLASS_PANE.red,
        DyeColor.BLACK to Items.STAINED_GLASS_PANE.black,
    )

    private val requirements = mutableMapOf<Int, Int>()
    private var lastTargetColor: DyeColor? = null
    private val clickedColors = mutableMapOf<Int, DyeColor?>()

    private val ItemStack.rubixColor: DyeColor?
        get() = colorToItem.entries.firstOrNull { item == it.value }?.key

    override fun canSolve(slots: List<Slot>): Boolean =
        slots.count { it.item.rubixColor != null && it.item.rubixColor != DyeColor.BLACK } >= 9

    override fun solve(slots: List<Slot>): List<Int> = emptyList()

    override fun updateSlots(slots: List<Slot>) {
        if (!hasSolved && !canSolve(slots)) return
        confirmPendingClicks(slots, emptyList())
        if (!hasSolved || solution.isEmpty()) {
            if (canSolve(slots)) {
                solution.clear()
                solution.addAll(planSequence(slots))
                hasSolved = true
            }
        }
    }

    private fun planSequence(slots: List<Slot>): List<Int> {
        val panes = slots.mapNotNull { slot -> slot.item.rubixColor?.let { slot.index to it } }
            .filter { it.second != DyeColor.BLACK }
        if (panes.size < 9) return emptyList()

        val target = lastTargetColor ?: colorOrder.minBy { goal ->
            panes.sumOf { (_, color) -> minOf(dist(color, goal), 5 - dist(color, goal)) }
        }
        lastTargetColor = target

        requirements.clear()
        val sequence = mutableListOf<Int>()
        panes.forEach { (index, color) ->
            val d = dist(color, target)
            requirements[index] = d
            val clicks = when {
                d == 0 -> 0
                d <= 2 -> d
                else -> 5 - d
            }
            repeat(clicks) { sequence.add(index) }
        }
        return sequence
    }

    override fun confirmPendingClicks(slots: List<Slot>, targets: List<Int>) {
        val iterator = pendingClicks.entries.iterator()
        while (iterator.hasNext()) {
            val (slotIndex, _) = iterator.next()
            val current = slots.getOrNull(slotIndex)?.item?.rubixColor
            if (current != clickedColors[slotIndex]) {
                iterator.remove()
                clickedColors.remove(slotIndex)
            }
        }
    }

    override fun nextClick(): Int? = solution.firstOrNull { it !in pendingClicks }

    override fun resolveExpired(timeout: Long) {
        val now = System.currentTimeMillis()
        val expired = pendingClicks.filterValues { now - it >= timeout }.keys
        pendingClicks.keys.removeAll(expired)
        expired.sorted().forEach { solution.add(0, it) }
        expired.forEach { clickedColors.remove(it) }
    }

    override fun onMarkClicked(slotIndex: Int, item: ItemStack?) {
        clickedColors[slotIndex] = item?.rubixColor
    }

    override fun clickButton(slotIndex: Int): Int =
        if ((requirements[slotIndex] ?: 0) >= 3) 1 else 0

    private fun dist(pane: DyeColor, goal: DyeColor): Int {
        val p = colorOrder.indexOf(pane)
        val g = colorOrder.indexOf(goal)
        return if (p > g) (g + colorOrder.size) - p else g - p
    }
}
