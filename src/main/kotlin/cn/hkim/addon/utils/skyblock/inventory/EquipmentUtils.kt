package cn.hkim.addon.utils.skyblock.inventory

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.clickPlayerInventorySlot
import cn.hkim.addon.utils.findItemByID
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.sendCommand
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object EquipmentUtils : SwapHandler() {
    private var pendingSlots = listOf<Int>()
    private var currentIndex = 0

    /**
     * Swaps multiple equipment items in sequence.
     * @param itemIds List of item IDs to be clicked in order.
     * @return True if all swaps are successful, false otherwise.
     */
    suspend fun swapEquipment(itemIds: List<String>): Boolean {
        if (isActive || isProcessing) return false
        val slots = itemIds.mapNotNull { findItemByID(it).takeIf { slot -> slot != -1 } }
        if (slots.isEmpty()) return false

        return suspendCancellableCoroutine { cont ->
            callback = { result -> cont.resume(result) }
            pendingSlots = slots
            currentIndex = 0
            isProcessing = false
            isActive = true
            startSwap("Swapping ${slots.size} equipment(s)")
            sendCommand("stats")

            schedule(200) {
                if (callback != null) {
                    callback?.invoke(false)
                    reset()
                }
            }
        }
    }

    override fun handleGuiOpen(title: String) {
        if (!title.contains("Your Equipment")) {
            callback?.invoke(false)
            return
        }

        schedule((6..8).random()) {
            if (!isProcessing) { processNextItem() }
        }
    }

    private fun processNextItem() {
        if (currentIndex >= pendingSlots.size) {
            schedule((4..6).random()) {
                mc.player?.closeContainer()
                callback?.invoke(true)
                reset()
            }
            return
        }

        isProcessing = true
        println(currentIndex)

        mc.player?.clickPlayerInventorySlot(pendingSlots[currentIndex], containerId)
        currentIndex++

        schedule((8..10).random()) { processNextItem() }
    }

    override fun reset() {
        pendingSlots = emptyList()
        currentIndex = 0
        super.reset()
    }
}