package cn.hkim.addon.events.impl

import cn.hkim.addon.utils.render.RenderConsumer
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext

class RenderEvent {
    class Extract(context: LevelExtractionContext, val consumer: RenderConsumer)
    class Last(val context: LevelRenderContext)
}