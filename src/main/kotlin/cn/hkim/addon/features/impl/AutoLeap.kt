package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.events.impl.MouseButtonEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.skyblock.*
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import cn.hkim.addon.compat.input.InputCompat
import cn.hkim.addon.config.settings.NumberSetting

@ModuleInfo("auto_leap", Category.SKYBLOCK)
object AutoLeap : Module("Auto Leap", "Auto leap to players in dungeons.") {
    private val ee2Mage by BooleanSetting("Mage EE2", "Set EE2 leap target to Mage.", false)
    private val leapTicks by NumberSetting("Leap Ticks", "Ticks to leap.", 2f, 1f, 4f, 1f, "t")

    private var inLeapGui = false
    private var shouldAutoLeap = false
    private var targetClass: DungeonClass? = null

    @EventHandler
    private fun onMouseClick(event: MouseButtonEvent) {
        if (!enabled || event.button != InputCompat.MOUSE_LEFT || !LocationUtils.inDungeons) return
        val player = mc.player ?: return

        if (player.mainHandItem.itemId.equalsOneOf("INFINITE_SPIRIT_LEAP", "SPIRIT_LEAP")) {
            targetClass = selectLeapTarget()
            shouldAutoLeap = true
            event.cancel()
            rightClick()
        }
    }

    @EventHandler
    private fun onGuiOpen(event: GuiEvent.Open) {
        if (!enabled) return
        val chest = (event.screen as? AbstractContainerScreen<*>) ?: return
        inLeapGui = chest.title.string.equalsOneOf("Spirit Leap", "Teleport to Player")
        if (!LocationUtils.inDungeons || !inLeapGui || !shouldAutoLeap) return
        schedule(leapTicks.toInt()) { performAutoLeap(chest) }
    }

    private fun performAutoLeap(screen: AbstractContainerScreen<*>) {
        if (targetClass == null) {
            shouldAutoLeap = false
            return
        }

        val targetPlayer = DungeonUtils.dungeonTeammates.find {
            it.clazz == targetClass && !it.isDead
        }

        if (targetPlayer == null) {
            shouldAutoLeap = false
            return
        }

        try {
            leapTo(targetPlayer.name, screen, false)
            modMessage("Teleporting to ${targetPlayer.name} (${targetClass})")
        } catch (_: Exception) {} finally {
            inLeapGui = false
            shouldAutoLeap = false
        }
    }

    private fun selectLeapTarget(): DungeonClass? {
        if (!LocationUtils.inDungeons) return null

        return if (DungeonUtils.inBoss) {
            handleM7Phase()
        } else {
            handlePreBossPhase()
        }
    }

    private fun handleM7Phase(): DungeonClass? {
        val floor = DungeonUtils.floor ?: return null
        if (floor.floorNumber != 7) return null

        val myClass = DungeonUtils.currentDungeonPlayer.clazz
        val phase = getF7Phase()
        val p3Stage = P3Stages.getP3Stage()

        if (phase == M7Phases.Unknown) return null

        val isCore = p3Stage == P3Stages.S3

        return queryRule(myClass, phase, p3Stage, isCore)
    }

    private fun handlePreBossPhase(): DungeonClass? {
        val myPlayer = DungeonUtils.currentDungeonPlayer
        val myClass = myPlayer.clazz

        if (myClass != DungeonClass.Mage && myClass != DungeonClass.Archer) return null

        val doorOpenerName = DungeonUtils.doorOpener
        if (doorOpenerName.isNotBlank()) {
            val doorOpener = DungeonUtils.dungeonTeammates.find { it.name == doorOpenerName }
            if (doorOpener != null && !doorOpener.name.equals(mc.player?.name)) {
                return doorOpener.clazz
            }
        }

        val teammateToLeap = when (myClass) {
            DungeonClass.Archer -> DungeonUtils.dungeonTeammates.find { it.clazz == DungeonClass.Mage }
            DungeonClass.Mage -> DungeonUtils.dungeonTeammates.find { it.clazz == DungeonClass.Archer }
        }

        return teammateToLeap?.clazz
    }

    // Dynamic leap rules in the future.
    private fun queryRule(sourceClass: DungeonClass, phase: M7Phases, p3Stage: P3Stages, isCore: Boolean = false): DungeonClass? {
        val player = mc.player!!
        val playerPos = BlockPos(player.x.toInt(), player.y.toInt(), player.z.toInt())
        val area = LeapAreas.getAreas()

        return when (sourceClass) {
            DungeonClass.Archer -> when (phase) {
                M7Phases.P1 -> when (area) {
                    LeapAreas.LeftCrystalSpawn -> DungeonClass.Tank
                    LeapAreas.LeftCrystalPlace -> DungeonClass.Mage
                    else -> DungeonClass.Berserk
                }
                M7Phases.P2 -> when (area) {
                    LeapAreas.PurplePillar -> DungeonClass.Mage
                    LeapAreas.YellowPillar -> DungeonClass.Healer
                    else -> null
                }
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> {
                        if (ee2Mage) DungeonClass.Mage
                        else null
                    }
                    P3Stages.S2 -> DungeonClass.Healer
                    P3Stages.S3 -> {
                        if (isCore) DungeonClass.Mage
                        else null
                    }
                    P3Stages.S4 -> DungeonClass.Mage
                    P3Stages.Tunnel -> DungeonClass.Healer
                    else -> null
                }
                M7Phases.P4 -> DungeonClass.Healer
                M7Phases.P5 -> null
                else -> null
            }

            DungeonClass.Berserk -> when (phase) {
                M7Phases.P1 -> if (area == LeapAreas.RightCrystalSpawn) DungeonClass.Healer else null
                M7Phases.P2 -> DungeonClass.Tank
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> {
                        if (ee2Mage) DungeonClass.Mage
                        else DungeonClass.Archer
                    }
                    P3Stages.S2 -> {
                        if (isPositionInArea(BlockPos(57, 131, 137), BlockPos(63, 136, 142), playerPos)) {
                            DungeonClass.Tank
                        }
                        else DungeonClass.Healer
                    }
                    P3Stages.S3 -> {
                        if (isCore) DungeonClass.Mage
                        else DungeonClass.Archer
                    }
                    P3Stages.S4 -> {
                        if (isPositionInArea(BlockPos(62, 126, 34), BlockPos(64, 130, 36), playerPos)) {
                            DungeonClass.Tank
                        } else DungeonClass.Mage
                    }
                    P3Stages.Tunnel -> DungeonClass.Healer
                    else -> null
                }
                M7Phases.P4 -> DungeonClass.Healer
                M7Phases.P5 -> DungeonClass.Archer
                else -> null
            }

            DungeonClass.Healer -> when (phase) {
                M7Phases.P1 -> null
                M7Phases.P2 -> if (area == LeapAreas.PurplePad) DungeonClass.Berserk else null
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> {
                        if (ee2Mage) DungeonClass.Mage
                        else DungeonClass.Archer
                    }
                    P3Stages.S2 -> null
                    P3Stages.S3 -> {
                        if (isPositionInArea(BlockPos(-3, 125, 80), BlockPos(3, 118, 74), playerPos)) { // Arrow
                            DungeonClass.Mage
                        } else if (isCore) DungeonClass.Mage
                        else DungeonClass.Archer
                    }
                    P3Stages.S4 -> DungeonClass.Mage
                    P3Stages.Tunnel -> null
                    else -> null
                }
                M7Phases.P4 -> null
                M7Phases.P5 -> DungeonClass.Berserk
                else -> null
            }

            DungeonClass.Mage -> when (phase) {
                M7Phases.P1 -> DungeonClass.Berserk
                M7Phases.P2 -> when (area) {
                    LeapAreas.PurplePad -> DungeonClass.Healer
                    LeapAreas.PurpleStorm -> DungeonClass.Tank
                    LeapAreas.YellowPillar -> DungeonClass.Healer
                    else -> null
                }
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> {
                        if (ee2Mage) null
                        else DungeonClass.Archer
                    }
                    P3Stages.S2 -> DungeonClass.Healer
                    P3Stages.S3 -> null
                    P3Stages.S4 -> null
                    P3Stages.Tunnel -> DungeonClass.Healer
                    else -> null
                }
                M7Phases.P4 -> DungeonClass.Healer
                M7Phases.P5 -> {
                    if (hasRelicInHotbar()) DungeonClass.Berserk
                    else DungeonClass.Tank
                }
                else -> null
            }

            DungeonClass.Tank -> when (phase) {
                M7Phases.P1 -> when (area) {
                    LeapAreas.LeftCrystalPlace -> DungeonClass.Mage
                    else -> DungeonClass.Berserk
                }
                M7Phases.P2 -> if (area == LeapAreas.YellowPad) DungeonClass.Healer else null
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> {
                        if (ee2Mage) DungeonClass.Mage
                        else DungeonClass.Archer
                    }
                    P3Stages.S2 -> DungeonClass.Healer
                    P3Stages.S3 -> {
                        if (isCore) DungeonClass.Mage
                        else DungeonClass.Archer
                    }
                    P3Stages.S4 -> DungeonClass.Mage
                    P3Stages.Tunnel -> DungeonClass.Healer
                    else -> null
                }
                M7Phases.P4 -> DungeonClass.Healer
                M7Phases.P5 -> DungeonClass.Archer
                else -> null
            }

            DungeonClass.Unknown -> null
        }
    }

    private fun hasRelicInHotbar(): Boolean {
        val stack = mc.player?.inventory?.getItem(8) ?: return false
        return !stack.isEmpty && stack.itemId.contains("RELIC")
    }
}
