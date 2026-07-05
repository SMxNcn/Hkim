package cn.hkim.addon.utils.skyblock.inventory

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.utils.clickInventorySlot
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.sendCommand
import cn.hkim.addon.utils.skyblock.LocationUtils
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import kotlin.coroutines.resume

object WardrobeUtils {
//    private val titleRegex = Regex("\\((\\d+)/(\\d+)\\) Armor Sets")

    private var callback: ((Boolean) -> Unit)? = null
    private var targetSlot = -1
    private var targetPage = 1
    private var containerId = -1
    private var calledFromThis = false
    private var isProcessing = false

    @EventHandler
    private fun onGuiOpen(event: GuiEvent.Open) {
        containerId = mc.player?.containerMenu?.containerId ?: return
        if (!calledFromThis) return
        if (isProcessing) return
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
                    calledFromThis = true
                    isProcessing = false
                    sendCommand("wardrobe $page")
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
            if (currentPage == targetPage) performClick()
            else reset()
        }
    }

    private fun performClick() {
        isProcessing = true
        mc.player?.clickInventorySlot(targetSlot, containerId)
        schedule((4..6).random()) {
            mc.player?.closeContainer()
            callback?.invoke(true)
            reset()
        }
    }

    private fun parsePageInfo(title: String): Pair<Int, Int>? {
        // Replace dynamic Regex with immutable object-level private val after Main server update.
        val pattern = if (LocationUtils.inAlphaServer) "\\((\\d+)/(\\d+)\\) Armor Sets"
        else "Wardrobe \\((\\d+)/(\\d+)\\)"
        return /*titleRegex*/Regex(pattern).find(title)?.destructured?.let { (current, total) ->
            current.toIntOrNull() to total.toIntOrNull()
        }?.takeIf { it.first != null && it.second != null }
            ?.let { it.first!! to it.second!! }
    }

    private fun reset() {
        callback = null
        targetSlot = -1
        targetPage = 1
        calledFromThis = false
        isProcessing = false
        containerId = -1
    }
}