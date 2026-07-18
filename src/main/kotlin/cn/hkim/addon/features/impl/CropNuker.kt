package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.HudUtils.alert
import cn.hkim.addon.utils.holdKey
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.waypoints.FarmingWaypoints
import net.minecraft.world.phys.Vec3

object CropNuker {
    var enabled = false
        private set

    private var currentActionId: Int = -1
    private var lastActionId: Int = -1

    private var hasCompleted: Boolean = false

    fun toggleNuker() {
        if (enabled) stop() else start()
        modMessage("§6Crop Nuker${if (enabled) "§a enabled" else "§c disabled"}.")
    }

    fun start() {
        if (enabled) return
        val waypoints = FarmingWaypoints.currentWaypoints
        if (FarmingWaypoints.currentWaypoints.isEmpty()) {
            modMessage("§7Waypoints not loaded.")
            return
        }
        enabled = true
        currentActionId = if (lastActionId != -1 && waypoints.any { it.id == lastActionId })
            lastActionId
        else waypoints.first().id
        lastActionId = -1
        hasCompleted = false
    }

    fun stop() {
        if (!enabled) return
        enabled = false
        lastActionId = currentActionId
        resetInput()
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

        val currentActionIndex = waypoints.indexOfFirst { it.id == currentActionId }
        val currentWaypoint = waypoints[currentActionIndex]

        val nextIndex = currentActionIndex + 1
        val hasNext = nextIndex < waypoints.size

        if (!hasNext) {
            stop()
            alert("§aRoute completed")
            lastActionId = -1
            return FarmingWaypoints.Action()
        }

        val nextWaypoint = waypoints[nextIndex]

        val playerPos = mc.player?.position()?.add(0.0, -0.5, 0.0) ?: return FarmingWaypoints.Action()
        val distToNext = playerPos.distanceTo(Vec3.atCenterOf(nextWaypoint.blockPos))

        if (distToNext >= 0.6) return currentWaypoint.action
        schedule(3 + (0..3).random()) {
            currentActionId = nextWaypoint.id
        }

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

    fun setCurrentActionId(index: Int) {
        currentActionId = index
    }
}
