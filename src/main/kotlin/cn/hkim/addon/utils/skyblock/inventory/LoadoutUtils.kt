package cn.hkim.addon.utils.skyblock.inventory

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.features.impl.SwapOptions
import cn.hkim.addon.utils.clickInventorySlot
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.sendCommand
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

object LoadoutUtils : SwapHandler() {
    private var targetIndex = -1
    private var targetPage = 1

    /**
     * Swaps to a loadout by index (1-27).
     * @param index Loadout index (1-27). Page is auto-calculated: (index - 1) / 12 + 1.
     *              Slot position within page: (index - 1) % 12, mapped to the 4x3 grid.
     * @return `true` if the loadout was successfully equipped, `false` otherwise.
     */
    suspend fun swapLoadoutTo(index: Int): Boolean {
        if (isActive || isProcessing) return false
        if (index !in 1..27) {
            modMessage("§cLoadout index must be between 1 and 27!")
            return false
        }

        return try {
            withTimeout(10000) {
                suspendCancellableCoroutine { cont ->
                    callback = { result -> cont.resume(result) }
                    targetIndex = index
                    targetPage = LoadoutLayout.getPage(index)
                    isActive = true
                    isProcessing = false
                    startSwap("Swapping to Loadout #$index")
                    sendCommand("loadout")
                }
            }
        } catch (_: TimeoutCancellationException) {
            reset()
            false
        }
    }

    override fun handleGuiOpen(title: String) {
        val (currentPage, totalPages) = parsePageInfo(title) ?: return

        if (targetPage > totalPages) return

        schedule((4..6).random()) {
            when {
                currentPage == targetPage -> performClick()
                currentPage < targetPage -> turnPage()
                else -> reset()
            }
        }
    }

    private fun performClick() {
        isProcessing = true
        val slot = LoadoutLayout.getSlotId(targetIndex)
        mc.player?.clickInventorySlot(slot, containerId)
        schedule((SwapOptions.closeTicks.toInt())) {
            mc.player?.closeContainer()
            callback?.invoke(true)
            reset()
        }
    }

    private fun parsePageInfo(title: String): Pair<Int, Int>? {
        return SwapOptions.ldTitleRegex.find(title)?.destructured?.let { (current, total) ->
            current.toIntOrNull() to total.toIntOrNull()
        }?.takeIf { it.first != null && it.second != null }
            ?.let { it.first!! to it.second!! }
    }

    private fun turnPage() {
        mc.player?.clickInventorySlot(44, containerId)
        isProcessing = false
    }

    override fun reset() {
        targetIndex = -1
        targetPage = 1
        super.reset()
    }

    private object LoadoutLayout {
        fun getSlotId(index: Int): Int {
            val offset = (index - 1) % 12
            val row = offset / 3
            val col = offset % 3
            return 14 + row * 9 + col
        }

        fun getPage(index: Int): Int = (index - 1) / 12 + 1
    }
}
