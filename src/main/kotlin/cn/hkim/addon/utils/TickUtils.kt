package cn.hkim.addon.utils

import cn.hkim.addon.Hkim
import cn.hkim.addon.events.impl.TickEvent
import meteordevelopment.orbit.EventHandler
import java.util.concurrent.CopyOnWriteArrayList

open class TickUtils(
    private val ticksPerCycle: Int,
    private val task: () -> Unit
) {
    internal var ticks = 0

    init {
        TickTasks.registerClientTask(this)
    }

    open fun run() {
        if (++ticks < ticksPerCycle) return
        runCatching(task).onFailure { Hkim.logger.error("Error on executing tick tasks.", it) }
        ticks = 0
    }
}

class OneShotTickTask(ticks: Int, task: () -> Unit) : TickUtils(ticks, task) {
    override fun run() {
        super.run()
        if (ticks == 0) TickTasks.unregister(this)
    }
}

fun schedule(ticks: Int, task: () -> Unit) {
    OneShotTickTask(ticks, task)
}

object TickTasks {
    private val clientTickTasks = CopyOnWriteArrayList<TickUtils>()

    fun registerClientTask(task: TickUtils) = clientTickTasks.add(task)

    fun unregister(task: TickUtils) {
        clientTickTasks.remove(task)
    }

    @EventHandler
    fun onTick(event: TickEvent.End) {
        clientTickTasks.forEach { it.run() }
    }
}