package cn.hkim.addon.events.impl

import cn.hkim.addon.events.Cancellable
import net.minecraft.network.chat.Component
import net.minecraft.world.BossEvent
import java.util.UUID

class BossBarRenderEvent @JvmOverloads constructor(
    val bar: BossEvent,
    val stage: Stage,
    val x: Int = 0,
    val y: Int = 0,
) : Cancellable() {
    enum class Stage {
        PRE_LAYOUT,
        BAR,
    }

    val id: UUID get() = bar.id
    val title: Component get() = bar.name
    val progress: Float get() = bar.progress
    val minValue: Float = 0f
    val maxValue: Float = 1f
    val currentValue: Float get() = bar.progress
    val color: BossEvent.BossBarColor get() = bar.color
    val overlay: BossEvent.BossBarOverlay get() = bar.overlay
}
