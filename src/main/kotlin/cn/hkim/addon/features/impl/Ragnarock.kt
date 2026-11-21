package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.TextSetting
import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.HudUtils.alert
import cn.hkim.addon.utils.itemId
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.render.island.IslandQueue
import cn.hkim.addon.utils.render.island.IslandRenderer
import cn.hkim.addon.utils.sendCommand
import cn.hkim.addon.utils.strength
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents

@ModuleInfo("ragnarock", Category.SKYBLOCK)
object Ragnarock : Module("Ragnarock", "Alerts when you cast the Ragnarock.") {
    private val castAlert by BooleanSetting("Cast Alert", "Alerts when you cast Ragnarock.", true)
    private val cancelAlert by BooleanSetting("Cancel Alert", "Alerts when Ragnarock is cancelled.", true)
    private val islandMessage by BooleanSetting("Island Message", "Shows messages on island.", false)
    private val strengthGainedMessage by BooleanSetting("Strength Gained", "Shows Ragnarock strength gained.", true)
    private val announceStrengthGained by BooleanSetting("Announce Strength", "Announce gained strength in party chat.", false).depends { strengthGainedMessage }
    private val customStrengthMessage by TextSetting("Announce Message", "Strength gained message. %s for strength gained.", "Gained strength from Ragnarock: %s").depends { announceStrengthGained }

    private val cancelRegex = Regex("Ragnarock was cancelled due to (?:being hit|taking damage)!")

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        if (!enabled) return
        if (cancelAlert && event.message.matches(cancelRegex)) alert("§cRagnarock Cancelled!")
    }

    @EventHandler
    private fun onPacket(event: PacketReceiveEvent) {
        if (!enabled || event.packet !is ClientboundSoundPacket) return
        if (event.packet.pitch == 1.4920635f && mc.player?.mainHandItem?.itemId == "RAGNAROCK_AXE" &&
            SoundEvents.WOLF_SOUNDS.entries.any {
                (_, variant) -> variant.adultSounds().deathSound() == event.packet.sound || variant.babySounds().deathSound() == event.packet.sound
            }) {
            val strengthGained = ((mc.player?.mainHandItem?.strength ?: return) * 1.5).toInt()
            if (islandMessage) {
                IslandRenderer.showMessage(
                    "Ragnarock",
                    "⚔ §4+$strengthGained§r strength for §a10s",
                    durationMs = 2000L,
                    priority = IslandQueue.PRIORITY_MODULE,
                    key = "ragnarock",
                )
            } else {
                if (castAlert) alert("§aCasted Rag")
                if (strengthGainedMessage) modMessage("Gained strength: §4$strengthGained")
            }
            if (strengthGainedMessage && announceStrengthGained) {
                val message = customStrengthMessage
                    .ifBlank { "Gained strength from Ragnarock: %s" }.replace("%s", strengthGained.toString(), true)
                sendCommand("pc $message")
            }
        }
    }
}