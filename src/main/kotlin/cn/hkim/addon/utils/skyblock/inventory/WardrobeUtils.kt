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
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import kotlin.coroutines.resume

object WardrobeUtils {
    private var callback: ((Boolean) -> Unit)? = null
    private var targetSlot = -1
    private var targetPage = 1
    private var containerId = -1
    var isActive = false
        private set
    private var isProcessing = false

    fun consumeGuiOpen(screen: Screen): Boolean {
        if (!isActive) return false
        if (isProcessing) {
            schedule(0) {
                mc.player?.closeContainer()
                callback?.invoke(true)
                reset()
            }
            return true
        }
        containerId = mc.player?.containerMenu?.containerId ?: return true
        handleGuiOpen(screen)
        return true
    }

    /**
     * Equips armor from the wardrobe.
     * @param index Equipment index (1-9), corresponds to slot `index + 35`.
     * @param page Target wardrobe page (1-3), defaults to 1.
     * @return `true` if the armor was successfully equipped, `false` otherwise.
     */
    suspend fun swapArmorTo(index: Int, page: Int = 1): Boolean {
        if (isActive || isProcessing) return false
        if (index !in 1..9) {
            modMessage("§cWardrobe index must be between 1 and 9!")
            return false
        }
        if (page !in 1..3) {
            modMessage("§cWardrobe page must be between 1 and 3!")
            return false
        }

        return try {
            withTimeout(10000) {
                suspendCancellableCoroutine { cont ->
                    callback = { result -> cont.resume(result) }
                    targetSlot = index + 35
                    targetPage = page
                    isActive = true
                    isProcessing = false
                    SwapHandler.startSwap()
                    sendCommand("wardrobe") // dube update removes page param
                }
            }
        } catch (_: TimeoutCancellationException) {
            reset()
            false
        }
    }

    private fun handleGuiOpen(screen: Screen?) {
        val chest = (screen as? AbstractContainerScreen<*>) ?: return
        val title = chest.title.string
        val (currentPage, totalPages) = parsePageInfo(title) ?: return

        if (targetPage > totalPages) return

        schedule((2..4).random()) {
            when {
                currentPage == targetPage -> performClick()
                currentPage < targetPage -> turnPage()
                else -> reset()
            }
        }
    }

    private fun performClick() {
        isProcessing = true
        mc.player?.clickInventorySlot(targetSlot, containerId)
        schedule(8) {
            if (isProcessing) {
                mc.player?.closeContainer()
                callback?.invoke(true)
                reset()
            }
        }
    }

    private fun parsePageInfo(title: String): Pair<Int, Int>? {
        return SwapOptions.wdTitleRegex.find(title)?.destructured?.let { (current, total) ->
            current.toIntOrNull() to total.toIntOrNull()
        }?.takeIf { it.first != null && it.second != null }
            ?.let { it.first!! to it.second!! }
    }

    private fun turnPage() {
        mc.player?.clickInventorySlot(53, containerId)
        isProcessing = false
    }

    private fun reset() {
        callback = null
        targetSlot = -1
        targetPage = 1
        isActive = false
        isProcessing = false
        containerId = -1
        SwapHandler.endSwap()
    }
}