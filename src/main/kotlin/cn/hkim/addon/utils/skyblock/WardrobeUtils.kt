package cn.hkim.addon.utils.skyblock

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.utils.clickInventorySlot
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.sendCommand
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import kotlin.coroutines.resume

object WardrobeUtils {
    private const val NEXT_PAGE_SLOT = 53
    private val titleRegex = Regex("Wardrobe \\((\\d)/(\\d)\\)")

    private var callback: ((Boolean) -> Unit)? = null
    private var targetSlot = -1
    private var targetPage = 1
    private var containerId = -1
    private var calledFromThis = false
    private var isProcessing = false

    @EventHandler
    private fun onGuiOpen(event: GuiEvent.Open) {
        containerId = mc.player?.containerMenu?.containerId ?: return
        if (!calledFromThis || isProcessing) return
        handleGuiOpen(event.screen)
    }

    /**
     * Equips armor from the wardrobe.
     * @param index Equipment index (1-9), corresponds to slot `index + 35`.
     * @param page Target wardrobe page (1-3), defaults to 1.
     * @return `true` if the armor was successfully equipped, `false` otherwise.
     */
    suspend fun swapArmorTo(index: Int, page: Int = 1): Boolean {
        if (calledFromThis || isProcessing) return false
        if (index !in 1..9 || page !in 1..3) return false

        return try {
            withTimeout(10000) {
                suspendCancellableCoroutine { cont ->
                    callback = { result -> cont.resume(result) }
                    targetSlot = index + 35
                    targetPage = page
                    calledFromThis = true
                    isProcessing = false
                    sendCommand("wardrobe")
                }
            }
        } catch (_: TimeoutCancellationException) {
            reset()
            false
        }
    }

    private fun handleGuiOpen(screen: Screen?) {
        val chest = (screen as? AbstractContainerScreen<*>) ?: return
        val matchResult = titleRegex.find(chest.title.string) ?: return
        val currentPage = matchResult.groupValues[1].toIntOrNull() ?: return
        val totalPages = matchResult.groupValues[2].toIntOrNull() ?: return

        if (targetPage > totalPages) return

        schedule((6..8).random()) {
            if (currentPage == targetPage) {
                clickArmor()
            } else if (currentPage < targetPage) {
                mc.player?.clickInventorySlot(NEXT_PAGE_SLOT, containerId)
                isProcessing = false
            }
        }
    }

    private fun clickArmor() {
        isProcessing = true
        mc.player?.clickInventorySlot(targetSlot, containerId)

        schedule((8..10).random()) {
            mc.player?.closeContainer()
            callback?.invoke(true)
            reset()
        }
    }

    private fun reset() {
        callback = null
        targetSlot = -1
        targetPage = 1
        containerId = -1
        calledFromThis = false
        isProcessing = false
    }
}