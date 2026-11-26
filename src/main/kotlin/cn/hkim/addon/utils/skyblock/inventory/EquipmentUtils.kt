package cn.hkim.addon.utils.skyblock.inventory

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.features.impl.SwapOptions
import cn.hkim.addon.utils.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
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
        val missing = itemIds.filter { findItemByID(it) == -1 }
        if (missing.isNotEmpty()) {
            modMessage("§cItem(s) not found in inventory: ${missing.joinToString(", ")}")
        }
        if (slots.isEmpty()) return false

        return try {
            withTimeout(TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    callback = { result -> cont.resume(result) }
                    pendingSlots = slots
                    currentIndex = 0
                    isProcessing = false
                    isActive = true
                    startSwap(SwapInfo("Equipment", "Swapping §a${slots.size}§r item(s)"))
                    sendCommand("stats")
                }
            }
        } catch (_: TimeoutCancellationException) {
            reset()
            false
        }
    }

    override fun matchesTitle(title: String) = title.contains("Stats & Equipment")

    override fun handleGuiOpen(title: String) {
        schedule(randomDelay(SwapOptions.startClickTicks.toInt(), 2).toInt()) {
            if (!isProcessing) { processNextItem() }
        }
    }

    private fun processNextItem() {
        if (currentIndex >= pendingSlots.size) {
            closeGui()
            return
        }

        isProcessing = true

        mc.player?.clickPlayerInventorySlot(pendingSlots[currentIndex], containerId)
        currentIndex++

        if (currentIndex >= pendingSlots.size) {
            closeGui()
        } else {
            schedule((6..8).random()) { processNextItem() }
        }
    }

    private fun closeGui() {
        schedule(SwapOptions.closeTicks.toInt()) {
            mc.player?.closeContainer()
            callback?.invoke(true)
            reset()
        }
    }

    override fun reset() {
        pendingSlots = emptyList()
        currentIndex = 0
        super.reset()
    }
}