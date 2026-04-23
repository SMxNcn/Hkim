package cn.hkim.addon.events

import cn.hkim.addon.Hkim
import cn.hkim.addon.events.impl.RenderEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.features.ModuleManager.initOrbit
import cn.hkim.addon.utils.render.RenderBatchManager
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

class EventDispatcher {
    companion object {
        fun postEvents() {
            ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
                Hkim.EVENT_BUS.post(WorldEvent.Load())
            }

            ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
                Hkim.EVENT_BUS.post(WorldEvent.Unload())
            }

            LevelRenderEvents.END_EXTRACTION.register { handler ->
                Hkim.EVENT_BUS.post(RenderEvent.Extract(handler, RenderBatchManager.renderConsumer))
            }

            LevelRenderEvents.END_MAIN.register { handler ->
                Hkim.EVENT_BUS.post(RenderEvent.Last(handler))
            }

            LevelRenderEvents.END_MAIN.register(RenderBatchManager::renderBatch)

            Registry.register(BuiltInRegistries.SOUND_EVENT, Identifier.fromNamespaceAndPath("hkim", "enable"),
                SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("hkim", "enable")))
            Registry.register(BuiltInRegistries.SOUND_EVENT, Identifier.fromNamespaceAndPath("hkim", "disable"),
                SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("hkim", "disable")))
        }

        fun registerListeners(vararg listeners: Any) {
            initOrbit()
            listeners.forEach { Hkim.EVENT_BUS.subscribe(it) }
        }
    }
}