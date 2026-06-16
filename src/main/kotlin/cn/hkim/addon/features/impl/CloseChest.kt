package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.SelectorSetting
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.cleanString
import cn.hkim.addon.utils.equalsOneOf
import cn.hkim.addon.utils.skyblock.LocationUtils.inDungeons
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket

@ModuleInfo("close_chest", Category.SKYBLOCK, false)
object CloseChest : Module("Close Chest", "Allows you to instantly close chests with any key or automatically.") {
    private val mode by SelectorSetting("Mode", "The mode to use, auto will automatically close the chest, any key will make any key input close the chest.", listOf("Auto", "Any Key"), "Auto")

    @EventHandler
    fun onPacket(event: PacketReceiveEvent) {
        if (!inDungeons) return
        val packet = event.packet as? ClientboundOpenScreenPacket ?: return
        val title = packet.title.cleanString
        val isSecretChest = title.equalsOneOf("Chest", "Large Chest")

        if (mode == 0 && isSecretChest) {
            mc.connection?.send(ServerboundContainerClosePacket(packet.containerId))
            event.cancel()
        }
    }

    @EventHandler
    fun onGuiClick(event: GuiEvent.MouseClick) {
        if (mode != 1 || !inDungeons) return
        val title = event.screen.title.string
        val isSecretChest = title.equalsOneOf("Chest", "Large Chest")

        if (isSecretChest) mc.player?.closeContainer()
    }

    @EventHandler
    fun onGuiKey(event: GuiEvent.KeyPress) {
        if (mode != 1 || !inDungeons) return
        val title = event.screen.title.string
        val isSecretChest = title.equalsOneOf("Chest", "Large Chest")

        if (isSecretChest) mc.player?.closeContainer()
    }
}