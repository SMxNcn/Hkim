package cn.hkim.addon.utils.render.island

import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation

class Tween(private val easing: Easing, private val durationMs: () -> Long) {
    private var activeDurationMs = 1L

    private var from = 0f
    private var to = 0f
    private var startTime = 0L

    fun snap(value: Float) {
        from = value
        to = value
        startTime = 0L
    }

    fun target(value: Float, now: Long) {
        if (value == to) return
        from = get(now)
        to = value
        startTime = now
        activeDurationMs = durationMs().coerceAtLeast(1L)
    }

    fun get(now: Long): Float {
        if (startTime == 0L) return to
        val progress = ((now - startTime).toFloat() / activeDurationMs).coerceIn(0f, 1f)
        return from + (to - from) * GuiAnimation.applyEasing(progress, easing)
    }
}