package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.SelectorSetting
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.skyblock.LocationUtils.inDungeons
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket

@ModuleInfo("close_chest", Category.SKYBLOCK)
object CloseChest : Module("Close Chest", "Instantly close secret chests.") {
    private val mode by SelectorSetting("Close Mode", "Auto-close or close on any key.", listOf("Auto", "Any Key"), "Auto")

    private val secretChestKeys = setOf("container.chest", "container.chestDouble")

    private fun Component.isSecretChestTitle(): Boolean {
        val key = (contents as? TranslatableContents)?.key
        return (key != null && key in secretChestKeys) || siblings.any { it.isSecretChestTitle() }
    }

    @EventHandler
    private fun onPacket(event: PacketReceiveEvent) {
        if (!enabled || !inDungeons) return
        val packet = event.packet as? ClientboundOpenScreenPacket ?: return
        val isSecretChest = packet.title.isSecretChestTitle()

        if (mode == 0 && isSecretChest) {
            mc.connection?.send(ServerboundContainerClosePacket(packet.containerId))
            event.cancel()
        }
    }

    @EventHandler
    private fun onGuiClick(event: GuiEvent.MouseClick) {
        if (!enabled || mode != 1 || !inDungeons) return
        val isSecretChest = event.screen.title.isSecretChestTitle()

        if (isSecretChest) mc.player?.closeContainer()
    }

    @EventHandler
    private fun onGuiKey(event: GuiEvent.KeyPress) {
        if (!enabled || mode != 1 || !inDungeons) return
        val isSecretChest = event.screen.title.isSecretChestTitle()

        if (isSecretChest) mc.player?.closeContainer()
    }
}