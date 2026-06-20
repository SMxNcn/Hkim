package cn.hkim.addon.utils

import cn.hkim.addon.Hkim
import cn.hkim.addon.events.impl.TickEvent
import meteordevelopment.orbit.EventHandler
import java.util.concurrent.CopyOnWriteArrayList

open class TickUtils(
    private val ticksPerCycle: Int,
    serverTick: Boolean = false,
    private val task: () -> Unit
) {
    internal var ticks = 0

    init {
        if (serverTick) TickTasks.registerServerTask(this)
        else TickTasks.registerClientTask(this)
    }

    open fun run() {
        if (++ticks < ticksPerCycle) return
        runCatching(task).onFailure { Hkim.logger.error("Error on executing tick tasks.", it) }
        ticks = 0
    }
}

class OneShotTickTask(ticks: Int, serverTick: Boolean = false, task: () -> Unit) : TickUtils(ticks, serverTick, task) {
    override fun run() {
        super.run()
        if (ticks == 0) TickTasks.unregister(this)
    }
}

fun schedule(ticks: Int, serverTick: Boolean = false, task: () -> Unit) {
    OneShotTickTask(ticks, serverTick, task)
}

object TickTasks {
    private val clientTickTasks = CopyOnWriteArrayList<TickUtils>()
    private val serverTickTasks = CopyOnWriteArrayList<TickUtils>()

    fun registerClientTask(task: TickUtils) = clientTickTasks.add(task)
    fun registerServerTask(task: TickUtils) = serverTickTasks.add(task)

    fun unregister(task: TickUtils) {
        clientTickTasks.remove(task)
        serverTickTasks.remove(task)
    }

    @EventHandler
    private fun onClientTick(event: TickEvent.End) {
        clientTickTasks.forEach { it.run() }
    }

    @EventHandler
    private fun onServerTick(event: TickEvent.Server) {
        serverTickTasks.forEach { it.run() }
    }
}