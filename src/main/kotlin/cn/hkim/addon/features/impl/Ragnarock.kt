package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.HudUtils.alert
import cn.hkim.addon.utils.itemId
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.sendCommand
import cn.hkim.addon.utils.strength
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents

@ModuleInfo("ragnarock", Category.SKYBLOCK)
object Ragnarock : Module("Ragnarock", "Alerts when you cast the Ragnarock or it gets cancelled.") {
    private val castAlert by BooleanSetting("Cast alert", "Alerts when you cast Ragnarock.", true)
    private val cancelAlert by BooleanSetting("Cancel alert", "Alerts when Ragnarock is cancelled.", true)
    private val strengthGainedMessage by BooleanSetting("Strength gained", "Shows Ragnarock strength gained.", true)
    private val announceStrengthGained by BooleanSetting("Announce strength", "Announce gained strength in party chat.", false).depends { strengthGainedMessage }

    private val cancelRegex = Regex("Ragnarock was cancelled due to (?:being hit|taking damage)!")

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        if (cancelAlert && event.message.matches(cancelRegex)) alert("§cRagnarock Cancelled!")
    }

    @EventHandler
    private fun onPacket(event: PacketReceiveEvent) {
        if (event.packet !is ClientboundSoundPacket) return
        if (event.packet.pitch == 1.4920635f && mc.player?.mainHandItem?.itemId == "RAGNAROCK_AXE" &&
            SoundEvents.WOLF_SOUNDS.entries.any {
                (_, variant) -> variant.adultSounds().deathSound() == event.packet.sound || variant.babySounds().deathSound() == event.packet.sound
            }) {
            if (castAlert) alert("§aCasted Rag")
            val strengthGained = ((mc.player?.mainHandItem?.strength ?: return) * 1.5).toInt()
            if (strengthGainedMessage) {
                modMessage("§7Gained strength: §4$strengthGained")
                if (announceStrengthGained) sendCommand("pc Gained strength from Ragnarock: $strengthGained")
            }
        }
    }
}