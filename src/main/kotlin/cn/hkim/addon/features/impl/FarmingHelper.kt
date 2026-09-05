package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.*
import cn.hkim.addon.events.impl.*
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.gui.ActionInputScreen
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.HudUtils.alert
import cn.hkim.addon.utils.render.drawStyledBox
import cn.hkim.addon.utils.render.drawText
import cn.hkim.addon.utils.render.drawWireFrameBox
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.farming.PestTracker
import cn.hkim.addon.utils.skyblock.farming.Plot
import cn.hkim.addon.utils.skyblock.inventory.LoadoutUtils.swapLoadoutTo
import cn.hkim.addon.utils.waypoints.FarmingWaypoints
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW

@ModuleInfo("farming_helper", Category.SKYBLOCK)
object FarmingHelper : Module("Farming Helper", "Features for garden farming.") {
    private val allowEdits by BooleanSetting("Allow Edits", "Right-click blocks to add/remove waypoints.", false)
    private val renderWps by BooleanSetting("Render Waypoints", "Render waypoints.", true)
    private val renderOnFarming by BooleanSetting("Render on Farming", "Render waypoints when CropNuker is active.", false)

    private val loadoutDropdown by DropdownSetting("Loadout")
    private val mossyArmorSlot by NumberSetting("Mossy Slot", "Mossy loadout slot.", 1f, 1f, 12f, 1f).depends { loadoutDropdown }
    private val mantidArmorSlot by NumberSetting("Mantid Slot", "Mantid loadout slot.", 2f, 1f, 12f, 1f).depends { loadoutDropdown }

    private val otherDropdown by DropdownSetting("Others")
    private val autoKick by BooleanSetting("Auto Kick", "Auto kick player who visiting your garden.", true).depends { otherDropdown }
    private val ignorePests by BooleanSetting("Ignore Pests", "CropNuker will not respond to pest ready/spawned/killed events.", false).depends { otherDropdown }
    private val moonflowerMode by BooleanSetting("Moonflower Mode", "Set garden time to day for Sunset's Overbloom bonus.", false).depends { otherDropdown }
    private val pestEsp by BooleanSetting("Pest ESP", "Render wireframe boxes on pest armor stands.", false).depends { otherDropdown }
    private val plotScale by NumberSetting("Plot GUI Scale", "Scale of Garden Plot Screen.", 1f, 1f, 2f, 0.1f).depends { otherDropdown }

    private val nukerKeybind by KeybindSetting("Nuker Keybind", "Keybind to toggle nuker.", GLFW.GLFW_KEY_X)

    private var lastHeldSlot: Int = -1
    private var containerId = -1
    val plotScreenScale get() = if (enabled) plotScale else 1f

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        if (!enabled || LocationUtils.currentArea != Island.Garden || mc.gui.screen() != null) return
        CropNuker.onTick()
    }

    @EventHandler
    private fun onMouseClick(event: MouseButtonEvent) {
        if (!enabled || mc.gui.screen() != null) return
        if (LocationUtils.currentArea != Island.Garden) return

        if (!allowEdits || event.button != 1) return

        val pos = reachPosition ?: return
        if (mc.player?.isCrouching == true) {
            val currentAction = FarmingWaypoints.currentWaypoints.find { it.blockPos == pos }?.action ?: FarmingWaypoints.Action()
            mc.gui.setScreen(ActionInputScreen(currentAction) { newAction ->
                FarmingWaypoints.updateAt(pos, newAction)
            })
        } else if (!FarmingWaypoints.removeAt(pos)) {
            FarmingWaypoints.addAt(pos)
        }
    }

    @EventHandler
    private fun onRender(event: RenderEvent.Extract) {
        if (!enabled || LocationUtils.currentArea != Island.Garden) return
        val level = mc.level ?: return

        if (pestEsp) {
            for (entity in level.entitiesForRendering()) {
                if (entity !is ArmorStand || !PestTracker.isPestEntity(entity)) continue
                val headCenter = entity.renderPos.add(0.0, entity.bbHeight - 0.2, 0.0)
                event.drawWireFrameBox(AABB.ofSize(headCenter, 0.8, 0.8, 0.8), Colors.MINECRAFT_DARK_GREEN, 2f)
            }
        }

        if (!renderWps || !renderOnFarming && CropNuker.enabled) return
        for (wp in FarmingWaypoints.currentWaypoints) {
            event.drawStyledBox(AABB(wp.blockPos), Colors.MINECRAFT_GRAY, 1, false)
            event.drawText("#${wp.id}", Vec3.atCenterOf(wp.blockPos).add(0.0, 1.1, 0.0), 1.2f, false)
        }
    }

    @EventHandler
    private fun onPestReady(event: GardenEvent.PestReady) {
        if (!enabled || ignorePests || !CropNuker.enabled) return
        val player = mc.player ?: return
        lastHeldSlot = player.inventory.selectedSlot

        Hkim.scope.launch {
            delay(randomDelay(100, 100))
            CropNuker.stop()
            if (!swapLoadoutTo(mantidArmorSlot.toInt())) {
                modMessage("§cFailed to swap loadout to Mantid slot!")
                return@launch
            }
            CropNuker.start()
        }
    }

    @EventHandler
    private fun onPestSpawned(event: GardenEvent.PestSpawned) {
        if (!enabled) return
        PestTracker.lastPestPlot = event.plot
        if (!CropNuker.enabled || ignorePests) return

        Hkim.scope.launch {
            delay(randomDelay(500, 500))
            CropNuker.stop()
            sendCommand("setspawn")
            delay(randomDelay(250, 100))
            if (moonflowerMode) changeGardenTime(false)

            if (!swapLoadoutTo(mossyArmorSlot.toInt())) {
                modMessage("§cFailed to swap loadout to Mossy slot!")
                return@launch
            }

            delay(randomDelay(100, 50))
            val tpTarget = Plot.byId(event.plot)?.displayName?.ifBlank { null } ?: event.plot.toString()
            sendCommand("tptoplot $tpTarget")
            alert("§aKill Pest")
        }
    }

    @EventHandler
    private fun onPestKilled(event: GardenEvent.PestKilled) {
        if (!enabled || CropNuker.enabled || ignorePests) return
        val player = mc.player ?: return

        Hkim.scope.launch {
            holdKey(mc.options.keyShift, true)
            sendCommand("warp garden")
            delay(randomDelay(200, 100))
            holdKey(mc.options.keyShift, false)
            delay(randomDelay(50, 100))
            if (moonflowerMode) changeGardenTime(true)

            delay(randomDelay(200, 100))
            player.inventory.selectedSlot = if (lastHeldSlot == -1) 0 else lastHeldSlot
            delay(randomDelay(200, 100))
            CropNuker.start()
        }
    }

    @EventHandler
    private fun onGuestVisit(event: GardenEvent.GuestVisit) {
        if (enabled && autoKick) schedule(2) { sendCommand("sbkick ${event.player}") }
    }

    @EventHandler
    private fun onGuiOpen(event: GuiEvent.Open) {
        if (!enabled) return
        val chest = (event.screen as? AbstractContainerScreen<*>) ?: return
        val title = chest.title.cleanString

        if (title.contains("Configure Plot", ignoreCase = true)) {
            schedule(1) { mc.player?.containerMenu?.let { Plot.scanConfigurePlot(it) } }
            return
        }

        if (CropNuker.enabled || ignorePests || !moonflowerMode) return
        if (!title.containsOneOf("Desk", "Garden Time")) return
        containerId = mc.player?.containerMenu?.containerId ?: return
    }

    @EventHandler
    private fun onKey(event: InputEvent) {
        if (enabled && event.key.value == nukerKeybind) CropNuker.toggleNuker()
    }

    private inline val reachPosition: BlockPos?
        get() {
            val hitResult = mc.hitResult
            if (hitResult !is BlockHitResult) return null

            val blockPos = hitResult.blockPos
            val blockState = mc.level?.getBlockState(blockPos) ?: return null
            return if (blockState.isSolidRender) blockPos else null
        }

    suspend fun changeGardenTime(toNight: Boolean) {
        sendCommand("desk")
        delay(randomDelay(400, 100))
        mc.player?.clickInventorySlot(50, containerId)
        delay(randomDelay(400, 100))
        val slot = if (toNight) 13 else 11
        mc.player?.clickInventorySlot(slot, containerId)
        delay(randomDelay(200, 100))
        mc.player?.closeContainer()
        delay(randomDelay(200, 100))
    }
}