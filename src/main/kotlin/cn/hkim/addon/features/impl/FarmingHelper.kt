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
import cn.hkim.addon.utils.HudUtils.getScoreboardLines
import cn.hkim.addon.utils.render.drawStyledBox
import cn.hkim.addon.utils.render.drawText
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.inventory.EquipmentUtils.swapEquipment
import cn.hkim.addon.utils.skyblock.inventory.LoadoutUtils.swapLoadoutTo
import cn.hkim.addon.utils.skyblock.inventory.WardrobeUtils.swapArmorTo
import cn.hkim.addon.utils.waypoints.FarmingWaypoints
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.BlockHitResult
import org.lwjgl.glfw.GLFW

@ModuleInfo("farming_helper", Category.SKYBLOCK)
object FarmingHelper : Module("Farming Helper", "Features for garden farming.") {
    private val allowEdits by BooleanSetting("Allow Edits", "Right-click blocks to add/remove waypoints.", false)
    private val renderWps by BooleanSetting("Render Waypoints", "Render waypoints.", true)
    private val renderOnFarming by BooleanSetting("Render on Farming", "Render waypoints when CropNuker is disabled.", false)

    private val armorDropdown by DropdownSetting("Armor")
    private val usingLoadout by BooleanSetting("Armor in Loadout", "Farming Armor in Loadout.", false)
    private val mossyArmorSlot by NumberSetting("Mossy Slot", "Mossy armor wardrobe slot.", 1f, 1f, 12f, 1f).depends { armorDropdown }
    private val mantidArmorSlot by NumberSetting("Mantid Slot", "Mantid armor wardrobe slot.", 2f, 1f, 12f, 1f).depends { armorDropdown }

    private val otherDropdown by DropdownSetting("Others")
    private val autoKick by BooleanSetting("Auto Kick", "Auto kick player who visiting your garden.", true).depends { otherDropdown }
    private val ignorePests by BooleanSetting("Ignore Pests", "CropNuker will not respond to pest ready/spawned/killed events.", false).depends { otherDropdown }
    private val equipZorro by BooleanSetting("Zorro's Cape", "Equip Zorro's Cape in farming contests.", false).depends { otherDropdown }
    private val killAtDisco by BooleanSetting("Pest Disco", "Kill pest around Disco.", false).depends { otherDropdown }
    private val maxVacuumTime by NumberSetting("Max Vacuum Time (ms)", "How long to hold right-click to use the vacuum.", 1000f, 500f, 2000f, 100f).depends { otherDropdown && killAtDisco }
    private val changeTimeOnPest by BooleanSetting("Change Time (Fireflies)", "Set garden time to day for Sunset's Overbloom bonus.", false).depends { otherDropdown }

    private val nukerKeybind by KeybindSetting("Nuker Keybind", "Keybind to toggle nuker.", GLFW.GLFW_KEY_X)

    val isJacobActive: Boolean
        get() = getScoreboardLines()?.lines?.any { it.contains("Jacob") } ?: false

    private val blossomIds: List<String>
        get() =
            if (isJacobActive && equipZorro) listOf("BLOSSOM_NECKLACE", "ZORROS_CAPE", "BLOSSOM_BELT", "BLOSSOM_BRACELET")
            else listOf("BLOSSOM_NECKLACE", "BLOSSOM_CLOAK", "BLOSSOM_BELT", "BLOSSOM_BRACELET")

    private val pestIds = listOf("PESTHUNTERS_NECKLACE", "PEST_VEST", "PESTHUNTERS_BELT", "PESTHUNTERS_GLOVES")
    val specialItemList = listOf("SQUEAKY_MOUSEMAT", "ASPECT_OF_THE_VOID", "INFINI_VACUUM_HOOVERIUS")

    private var lastHeldSlot: Int = -1
    private var containerId = -1

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        if (!enabled || LocationUtils.currentArea != Island.Garden || mc.gui.screen() != null) return
        CropNuker.onTick()
    }

    @EventHandler
    private fun onMouseClick(event: MouseButtonEvent) {
        if (!enabled || !allowEdits || event.button != 1 || mc.gui.screen() != null) return
        if (LocationUtils.currentArea != Island.Garden) return

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
        if (!renderWps || LocationUtils.currentArea != Island.Garden) return
        if (!enabled || !renderOnFarming && CropNuker.enabled) return

        for (wp in FarmingWaypoints.currentWaypoints) {
            event.drawStyledBox(AABB(wp.blockPos), Colors.MINECRAFT_GRAY, 1, false)
            event.drawText("#${wp.id}", Vec3.atCenterOf(wp.blockPos).add(0.0, 1.1, 0.0), 1.2f, false)
        }
    }

    @EventHandler
    private fun onPestReady(event: GardenEvent.PestReady) {
        if (!enabled || ignorePests) return
        val player = mc.player ?: return
        lastHeldSlot = player.inventory.selectedSlot

        if (!CropNuker.enabled) return

        Hkim.scope.launch {
            CropNuker.stop()

            val success = if (usingLoadout) {
                swapLoadoutTo(mantidArmorSlot.toInt())
            } else {
                val slot = mantidArmorSlot.toInt()
                if (slot !in 1..9) {
                    modMessage("Invalid index: $slot!")
                    false
                } else if (!swapArmorTo(slot)) {
                    modMessage("§cFailed to equip armor from Wardrobe #$slot!")
                    false
                } else {
                    delay(randomDelay(400, 100))
                    if (!swapEquipment(pestIds)) {
                        modMessage("§cMissing equipments!")
                        false
                    } else true
                }
            }
            if (!success) return@launch

            delay(randomDelay(100, 50))
            CropNuker.start()
        }
    }

    @EventHandler
    private fun onPestSpawned(event: GardenEvent.PestSpawned) {
        if (!enabled || !CropNuker.enabled || ignorePests) return
        val player = mc.player ?: return

        Hkim.scope.launch {
            delay(randomDelay(200, 100))
            CropNuker.stop()
            sendCommand("setspawn")
            delay(randomDelay(250, 50))
            if (changeTimeOnPest) changeGardenTime(false)

            val success = if (usingLoadout) {
                swapLoadoutTo(mossyArmorSlot.toInt())
            } else {
                val slot = mossyArmorSlot.toInt()
                if (slot !in 1..9) {
                    modMessage("Invalid index: $slot!")
                    false
                } else if (!swapArmorTo(slot)) {
                    modMessage("§cFailed to equip armor from Wardrobe #$slot!")
                    false
                } else {
                    delay(randomDelay(400, 100))
                    if (!swapEquipment(blossomIds)) {
                        modMessage("§cMissing equipments!")
                        false
                    } else true
                }
            }
            if (!success) return@launch

            delay(randomDelay(100, 50))
            sendCommand("tptoplot ${event.plot}")
            if (killAtDisco) {
                delay(randomDelay(1200, 400))
                val slot = findItemByID(specialItemList[2])
                if (slot != -1 && slot in 0..9) {
                    player.inventory.selectedSlot = slot
                    delay(randomDelay(100, 100))
                    holdKey(mc.options.keyUse, true)
                    delay(randomDelay(maxVacuumTime.toInt(), 100))
                    holdKey(mc.options.keyUse, false)
                    delay(randomDelay(500, 50))
                    Hkim.EVENT_BUS.post(GardenEvent.PestKilled())
                }
            }
        }
    }

    @EventHandler
    private fun onPestKilled(event: GardenEvent.PestKilled) {
        if (!enabled || CropNuker.enabled || ignorePests) return
        val player = mc.player ?: return

        Hkim.scope.launch {
            delay(100)
            holdKey(mc.options.keyShift, true)
            sendCommand("warp garden")
            delay(randomDelay(200, 100))
            holdKey(mc.options.keyShift, false)
            if (changeTimeOnPest) changeGardenTime(true)

            delay(randomDelay(200, 100))
            player.inventory.selectedSlot = if (lastHeldSlot == -1) 0 else lastHeldSlot
            delay(randomDelay(100, 50))
            CropNuker.start()
        }
    }

    @EventHandler
    private fun onGuestVisit(event: GardenEvent.GuestVisit) {
        if (enabled && autoKick) schedule(2) { sendCommand("sbkick ${event.player}") }
    }

    @EventHandler
    private fun onGuiOpen(event: GuiEvent.Open) {
        if (!enabled || CropNuker.enabled || ignorePests || !changeTimeOnPest) return
        val chest = (event.screen as? AbstractContainerScreen<*>) ?: return
        if (!chest.title.cleanString.containsOneOf("Desk", "Garden Time", "Pesthunter")) return
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
        delay(randomDelay(200, 100))
        sendCommand("desk")
        delay(randomDelay(600, 100))
        mc.player?.clickInventorySlot(50, containerId)
        delay(randomDelay(600, 100))
        val slot = if (toNight) 13 else 11
        mc.player?.clickInventorySlot(slot, containerId)
        delay(randomDelay(600, 100))
        mc.player?.clickInventorySlot(31, containerId)
        delay(randomDelay(250, 50))
    }
}
