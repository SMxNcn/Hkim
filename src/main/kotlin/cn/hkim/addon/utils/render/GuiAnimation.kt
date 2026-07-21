package cn.hkim.addon.utils.render

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

class GuiAnimation internal constructor(
    private var from: Float = 0f,
    private var to: Float = 1f,
    private var duration: Long = 300L,
    private var easing: Easing = Easing.LINEAR
) {
    private var startTime = -1L
    private var running = false
    private var completed = false

    private var currentValue = from
    private var currentProgress = 0f

    private var lastEvalTime = -1L

    private fun eval() {
        if (!running || completed) return
        val now = System.currentTimeMillis()
        if (now == lastEvalTime) return
        lastEvalTime = now

        val elapsed = now - startTime
        if (elapsed >= duration) {
            currentValue = to
            currentProgress = 1f
            running = false
            completed = true
            return
        }

        currentProgress = applyEasing(elapsed.toFloat() / duration, easing)
        currentValue = from + (to - from) * currentProgress
    }

    fun getValue(): Float {
        eval()
        return if (running || completed) currentValue else from
    }

    fun getProgress(): Float {
        eval()
        return if (running || completed) currentProgress else 0f
    }

    internal fun update() {
        eval()
    }

    fun start(): GuiAnimation {
        startTime = System.currentTimeMillis()
        lastEvalTime = startTime
        running = true
        completed = false
        currentValue = from
        currentProgress = 0f
        return this
    }

    fun reset() {
        startTime = -1L
        lastEvalTime = -1L
        running = false
        completed = false
        currentValue = from
        currentProgress = 0f
    }

    fun reverse(): GuiAnimation {
        val temp = from
        from = to
        to = temp
        return start()
    }

    fun isRunning() = running
    fun isCompleted() = completed

    fun from(value: Float): GuiAnimation {
        from = value
        if (!running && !completed) currentValue = value
        return this
    }

    fun to(value: Float): GuiAnimation {
        to = value
        return this
    }

    fun duration(millis: Long): GuiAnimation {
        duration = millis
        return this
    }

    fun easing(easing: Easing): GuiAnimation {
        this.easing = easing
        return this
    }

    fun animateTo(target: Float): GuiAnimation {
        eval()
        from = currentValue
        to = target
        return start()
    }

    companion object {
        fun create(from: Float = 0f, to: Float = 1f): GuiAnimation {
            return GuiAnimation(from, to, 300L, Easing.LINEAR)
        }

        fun applyEasing(t: Float, easing: Easing): Float {
            if (t <= 0f) return 0f
            if (t >= 1f) return 1f

            return when (easing) {
                Easing.LINEAR -> t

                Easing.SINE_IN -> 1f - cos(t * PI.toFloat() / 2f)
                Easing.SINE_OUT -> sin(t * PI.toFloat() / 2f)
                Easing.SINE_IN_OUT -> (1f - cos(PI.toFloat() * t)) / 2f

                Easing.QUAD_IN -> t * t
                Easing.QUAD_OUT -> t * (2f - t)
                Easing.QUAD_IN_OUT -> if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t

                Easing.CUBIC_IN -> t * t * t
                Easing.CUBIC_OUT -> (t - 1f).let { it * it * it + 1f }
                Easing.CUBIC_IN_OUT -> if (t < 0.5f) 4f * t * t * t
                else (t - 1f).let { it * it * (2f * it - 2f) + 1f }
            }
        }
    }
}

fun Float.smoothDamp(target: Float, speed: Float, deltaTime: Float): Float {
    return this + (target - this) * (1f - exp(-speed * deltaTime))
}