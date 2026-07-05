package cn.hkim.addon.utils.skyblock

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.impl.AutoFish
import cn.hkim.addon.features.impl.CropNuker
import cn.hkim.addon.features.impl.Nuker
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.playSoundAtPlayer
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.Relative

object FailSafeUtils {
    private val macroModules = mutableSetOf(AutoFish, CropNuker, Nuker)

    private var lastTriggerTime = 0L
    private const val TRIGGER_COOLDOWN_MS = 1500L

    private val zxf2Sound = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("hkim", "zxf2"))
    private var lastSelectedSlot: Int = -1
    private var lastMacroEnabledTime = 0L
    private var wasMacroEnabled = false

    @EventHandler
    private fun onPacket(event: PacketReceiveEvent) {
        if (!LocationUtils.inSkyBlock) return

        when (val packet = event.packet) {
            is ClientboundPlayerPositionPacket -> {
                val player = mc.player ?: return
                val pos = packet.change.position

                val hasPosition = Relative.X in packet.relatives || Relative.Y in packet.relatives || Relative.Z in packet.relatives
                val hasRotation = Relative.X_ROT in packet.relatives || Relative.Y_ROT in packet.relatives

                if (!hasPosition && !hasRotation) return

                if (hasPosition) {
                    val dx = if (Relative.X in packet.relatives) pos.x else pos.x - player.x
                    val dy = if (Relative.Y in packet.relatives) pos.y else pos.y - player.y
                    val dz = if (Relative.Z in packet.relatives) pos.z else pos.z - player.z
                    val distSq = dx * dx + dy * dy + dz * dz

                    val inGracePeriod = System.currentTimeMillis() - lastMacroEnabledTime < 2000L
                    if (inGracePeriod && distSq < 9.0) return

                    if (distSq >= 0.0025) {
                        trigger("Teleport")
                        return
                    }
                }

                if (hasRotation) {
                    trigger("Rotation")
                }
            }
        }
    }

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        if (!LocationUtils.inSkyBlock) {
            lastSelectedSlot = -1
            wasMacroEnabled = false
            return
        }

        val nowEnabled = hasAnyMacroEnabled()

        if (!nowEnabled) {
            lastSelectedSlot = -1
            wasMacroEnabled = false
            return
        }

        if (!wasMacroEnabled) {
            lastMacroEnabledTime = System.currentTimeMillis()
        }
        wasMacroEnabled = true

        val player = mc.player ?: return
        val currentSlot = player.inventory.selectedSlot
        if (lastSelectedSlot != -1 && currentSlot != lastSelectedSlot) {
            trigger("Held Item Change")
        }
        lastSelectedSlot = currentSlot
    }

    @EventHandler
    private fun onWorldUnload(event: WorldEvent.Unload) {
        if (!LocationUtils.inSkyBlock) return
        trigger("World Change")
    }

    fun trigger(reason: String) {
        if (!hasAnyMacroEnabled()) return
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < TRIGGER_COOLDOWN_MS) return
        lastTriggerTime = now

        for (item in macroModules) {
            when (item) {
                is CropNuker -> if (item.enabled) item.stop()
                is Module -> if (item.enabled) item.disable()
            }
        }

        lastSelectedSlot = mc.player?.inventory?.selectedSlot ?: -1

        modMessage("§cAlert! Macro check!")
        modMessage("§c§l⚠ FailSafe§r §8-> §c$reason")
        playSoundAtPlayer(zxf2Sound)
    }

    private fun hasAnyMacroEnabled(): Boolean {
        for (item in macroModules) {
            when (item) {
                is CropNuker -> if (item.enabled) return true
                is Module -> if (item.enabled) return true
            }
        }
        return false
    }
}