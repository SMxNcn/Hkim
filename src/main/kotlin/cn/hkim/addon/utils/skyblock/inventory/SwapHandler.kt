package cn.hkim.addon.utils.skyblock.inventory

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.features.impl.SwapOptions
import cn.hkim.addon.utils.cleanString
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.player.ClientInput
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.world.entity.player.Input

abstract class SwapHandler {
    companion object {
        @JvmStatic var isInSwap = false
            private set
        var swapInfo: String? = null
        private var savedInput: ClientInput? = null

        fun startSwap(info: String? = null) {
            isInSwap = true
            swapInfo = info
            saveAndStopInput()
        }

        fun endSwap() {
            isInSwap = false
            swapInfo = null
            restoreInput()
        }

        private fun saveAndStopInput() {
            val player = mc.player ?: return
            savedInput = player.input
            player.input = object : ClientInput() {
                init {
                    keyPresses = Input(false, false, false, false, false, false, false)
                }
                override fun tick() {}
            }
        }

        private fun restoreInput() {
            val saved = savedInput ?: return
            mc.player?.input = saved
            savedInput = null
        }
    }

    var isActive = false
        protected set
    var isProcessing = false
        protected set
    var containerId = -1
        protected set
    protected var callback: ((Boolean) -> Unit)? = null

    init {
        Hkim.EVENT_BUS.subscribe(this)
    }

    @EventHandler
    protected open fun onPacket(event: PacketReceiveEvent) {
        if (!SwapOptions.shouldHideGui() || !isInSwap || !isActive) return
        val player = mc.player ?: return
        when (val packet = event.packet) {
            is ClientboundOpenScreenPacket -> {
                mc.execute {
                    player.containerMenu = packet.type.create(packet.containerId, player.inventory)
                    consumeGuiOpen(packet.title)
                }
                event.cancel()
            }
        }
    }

    fun consumeGuiOpen(title: Component): Boolean {
        if (!isActive) return false
        if (isProcessing) return true
        containerId = mc.player?.containerMenu?.containerId ?: return true
        handleGuiOpen(title.cleanString)
        return true
    }

    protected abstract fun handleGuiOpen(title: String)

    protected open fun reset() {
        callback = null
        isActive = false
        isProcessing = false
        containerId = -1
        endSwap()
    }
}