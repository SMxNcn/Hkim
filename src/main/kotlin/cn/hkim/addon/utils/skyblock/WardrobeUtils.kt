package cn.hkim.addon.utils.skyblock

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.utils.clickInventorySlot
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.sendCommand
import kotlinx.coroutines.future.await
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import java.util.concurrent.CompletableFuture

object WardrobeUtils {
    private const val NEXT_PAGE_SLOT = 53
    private val titleRegex = Regex("Wardrobe \\((\\d)/(\\d)\\)")

    private var pendingFuture: CompletableFuture<Boolean>? = null
    private var targetSlot = -1
    private var targetPage = 1
    private var containerId = -1
    private var calledFromThis = false
    private var isProcessing = false

    @EventHandler
    fun onGuiOpen(event: GuiEvent.Open) {
        containerId = mc.player?.containerMenu?.containerId ?: return
        if (!calledFromThis || isProcessing) return
        handleGuiOpen(event.screen)
    }

    /**
     * Equips armor from the wardrobe.
     * @param index Equipment index (1-9), corresponds to slot `index + 35`.
     * @param page Target wardrobe page (1-3), defaults to 1.
     */
    suspend fun swapArmorTo(index: Int, page: Int = 1): Boolean {
        if (index !in 1..9) return false
        if (page !in 1..3) return false

        val future = CompletableFuture<Boolean>()
        pendingFuture = future
        targetSlot = index + 35
        targetPage = page
        calledFromThis = true
        isProcessing = false

        sendCommand("wardrobe")

        schedule(100) {
            val f = pendingFuture
            if (f != null && !f.isDone) {
                reset()
                f.complete(false)
            }
        }

        return future.await()
    }

    private fun handleGuiOpen(screen: Screen?) {
        if (pendingFuture == null) return
        val chest = (screen as? AbstractContainerScreen<*>) ?: return

        val matchResult = titleRegex.find(chest.title.string) ?: return
        val currentPage = matchResult.groupValues[1].toIntOrNull() ?: return
        val totalPages = matchResult.groupValues[2].toIntOrNull() ?: return

        if (targetPage > totalPages) return
        isProcessing = true

        schedule((5..7).random()) {
            if (currentPage == targetPage) {
                clickArmor()
            } else if (currentPage < targetPage) {
                clickInventorySlot(NEXT_PAGE_SLOT, containerId)
                isProcessing = false
            } else {
                reset()
                pendingFuture?.complete(false)
            }
        }
    }

    private fun clickArmor() {
        clickInventorySlot(targetSlot, containerId)
        schedule((5..8).random()) {
            reset()
            mc.player?.closeContainer()
            pendingFuture?.complete(true)
        }
    }

    private fun reset() {
        pendingFuture = null
        targetSlot = -1
        targetPage = 1
        containerId = -1
        calledFromThis = false
        isProcessing = false
    }
}