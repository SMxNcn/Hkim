package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.HudUtils.alert
import cn.hkim.addon.utils.ViewLock
import cn.hkim.addon.utils.holdKey
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.waypoints.FarmingWaypoints
import net.minecraft.world.phys.Vec3
import kotlin.math.roundToInt

object CropNuker {
    var enabled = false
        private set

    private var currentActionId: Int = -1
    private var lastActionId: Int = -1
    private var pendingStartId: Int = -1
    private var delayTicks = 0

    fun toggleNuker() {
        if (enabled) stop() else start()
        modMessage("§6Crop Nuker${if (enabled) "§a enabled" else "§c disabled"}.")
    }

    fun start() {
        if (enabled) return
        val waypoints = FarmingWaypoints.currentWaypoints
        if (waypoints.isEmpty()) {
            modMessage("§7Waypoints not loaded.")
            return
        }
        val startId = resolveStartId(waypoints)
        delayTicks = 0
        enabled = true
        currentActionId = startId
        pendingStartId = -1
        lastActionId = -1
        ViewLock.lock(this)
    }

    fun stop() {
        if (!enabled) return
        enabled = false
        delayTicks = 0
        lastActionId = currentActionId
        resetInput()
        ViewLock.unlock(this)
    }

    private fun resolveStartId(waypoints: List<FarmingWaypoints.WaypointData>): Int {
        if (pendingStartId != -1 && waypoints.any { it.id == pendingStartId }) {
            return pendingStartId
        }
        if (lastActionId != -1 && waypoints.any { it.id == lastActionId }) {
            return lastActionId
        }

        return waypoints.first().id
    }

    fun onTick() {
        if (!enabled) return
        val action = generateAction()
        applyInput(action)
    }

    private fun generateAction(): FarmingWaypoints.Action {
        val waypoints = FarmingWaypoints.currentWaypoints
        if (waypoints.isEmpty() || currentActionId == -1) {
            stop()
            lastActionId = -1
            return FarmingWaypoints.Action()
        }

        var currentActionIndex = waypoints.indexOfFirst { it.id == currentActionId }
        if (currentActionIndex == -1) {
            currentActionId = waypoints.first().id
            currentActionIndex = 0
        }
        val currentWaypoint = waypoints[currentActionIndex]

        val nextIndex = currentActionIndex + 1
        if (nextIndex >= waypoints.size) {
            stop()
            alert("§aRoute completed")
            lastActionId = -1
            return FarmingWaypoints.Action()
        }

        val nextWaypoint = waypoints[nextIndex]

        val playerPos = mc.player?.position()?.add(0.0, -0.5, 0.0) ?: return FarmingWaypoints.Action()
        val distToNext = playerPos.distanceTo(Vec3.atCenterOf(nextWaypoint.blockPos))

        if (distToNext >= 0.6) {
            delayTicks = 0
            return currentWaypoint.action
        }

        if (delayTicks > 0) {
            if (--delayTicks > 0) return FarmingWaypoints.Action()
            currentActionId = nextWaypoint.id
            return nextWaypoint.action
        }

        val ticks = (FarmingHelper.waypointSwitchDelay / 50f).roundToInt()
        if (ticks <= 0) {
            currentActionId = nextWaypoint.id
            return nextWaypoint.action
        }
        delayTicks = ticks
        return FarmingWaypoints.Action()
    }

    private fun applyInput(action: FarmingWaypoints.Action) {
        holdKey(mc.options.keyUp, action.forward)
        holdKey(mc.options.keyDown, action.back)
        holdKey(mc.options.keyLeft, action.left)
        holdKey(mc.options.keyRight, action.right)
        holdKey(mc.options.keyAttack, action.leftClick)
    }

    private fun resetInput() {
        holdKey(mc.options.keyUp, false)
        holdKey(mc.options.keyDown, false)
        holdKey(mc.options.keyLeft, false)
        holdKey(mc.options.keyRight, false)
        holdKey(mc.options.keyAttack, false)
    }

    fun setCurrentActionIndex(index: Int): Boolean {
        val waypoints = FarmingWaypoints.currentWaypoints
        if (waypoints.isEmpty()) return false
        val wp = waypoints.getOrNull(index - 1) ?: return false

        delayTicks = 0
        if (enabled) currentActionId = wp.id
        else pendingStartId = wp.id
        return true
    }

    fun resetRoute() {
        if (enabled) stop()
        currentActionId = -1
        lastActionId = -1
        pendingStartId = -1
    }
}
