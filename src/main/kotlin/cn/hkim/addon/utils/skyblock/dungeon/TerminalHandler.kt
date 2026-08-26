package cn.hkim.addon.utils.skyblock.dungeon

import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

abstract class TerminalHandler(val type: TerminalEnums) {
    val timeOpened = System.currentTimeMillis()

    val solution: MutableList<Int> = mutableListOf()

    protected val pendingClicks = mutableMapOf<Int, Long>()
    var hasSolved = false
        protected set
    var isClicked = false
        private set

    open fun updateSlots(slots: List<Slot>) {
        if (!hasSolved && !canSolve(slots)) return
        val targets = solve(slots)
        confirmPendingClicks(slots, targets)
        solution.clear()
        solution.addAll(targets.filterNot { it in pendingClicks })
        hasSolved = true
    }

    protected open fun canSolve(slots: List<Slot>): Boolean = true

    protected abstract fun solve(slots: List<Slot>): List<Int>

    protected open fun confirmPendingClicks(slots: List<Slot>, targets: List<Int>) {
        pendingClicks.keys.removeAll { it !in targets }
    }

    open fun nextClick(): Int? = solution.firstOrNull()

    open fun clickButton(slotIndex: Int): Int = 0

    fun markClicked(slotIndex: Int, item: ItemStack? = null) {
        isClicked = true
        pendingClicks[slotIndex] = System.currentTimeMillis()
        solution.remove(slotIndex)
        onMarkClicked(slotIndex, item)
    }

    protected open fun onMarkClicked(slotIndex: Int, item: ItemStack?) {}

    fun resetClicked() {
        isClicked = false
    }

    open fun resolveExpired(timeout: Long) {
        val now = System.currentTimeMillis()
        pendingClicks.keys.removeAll { now - (pendingClicks[it] ?: 0) >= timeout }
    }
}
