package cn.hkim.addon.utils.skyblock.inventory

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.clickPlayerInventorySlot
import cn.hkim.addon.utils.findItemByID
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.sendCommand
import kotlinx.coroutines.suspendCancellableCoroutine
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import kotlin.coroutines.resume

object EquipmentUtils {
    private var callback: ((Boolean) -> Unit)? = null
    private var pendingSlots = listOf<Int>()
    private var currentIndex = 0
    private var containerId = -1
    var isActive = false
        private set
    private var isProcessing = false

    fun consumeGuiOpen(screen: Screen): Boolean {
        if (!isActive) return false
        containerId = mc.player?.containerMenu?.containerId ?: return false
        handleGuiOpen(screen)
        return true
    }

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
            SwapHandler.startSwap()
            sendCommand("stats")

            schedule(200) {
                if (callback != null) {
                    callback?.invoke(false)
                    reset()
                }
            }
        }
    }

    private fun handleGuiOpen(screen: Screen?) {
        val chest = (screen as? AbstractContainerScreen<*>) ?: run { callback?.invoke(false); return }
        if (!chest.title.string.contains("Your Equipment")) run { callback?.invoke(false); return }

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

    private fun reset() {
        callback = null
        pendingSlots = emptyList()
        isProcessing = false
        currentIndex = 0
        containerId = -1
        isActive = false
        SwapHandler.endSwap()
    }
}