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

@ModuleInfo("auto_leap", Category.SKYBLOCK, false)
object AutoLeap : Module("Auto Leap", "Auto leap to players based on predefined rules.") {
    private val forceMageCore by BooleanSetting("Force Mage Core", "Always treat mage as core in P3 S3.", true)

    private var inLeapGui = false
    private var shouldAutoLeap = false
    private var targetClass: DungeonClass? = null
//    private val leapedRegex = Regex("You have teleported to (\\w{1,16})!")

    @EventHandler
    fun onMouseClick(event: MouseButtonEvent) {
        if (event.button != 0 || !LocationUtils.inDungeons) return

        if (mc.player?.mainHandItem?.itemId.equalsOneOf("INFINITE_SPIRIT_LEAP", "SPIRIT_LEAP")) {
            targetClass = selectLeapTarget()
            shouldAutoLeap = true
            event.cancel()
            rightClick()
        }
    }

    @EventHandler
    fun onGuiOpen(event: GuiEvent.Open) {
        val chest = (event.screen as? AbstractContainerScreen<*>) ?: return
        inLeapGui = chest.title.string.equalsOneOf("Spirit Leap", "Teleport to Player")
        if (!LocationUtils.inDungeons || !inLeapGui || !shouldAutoLeap) return
        schedule(4) { performAutoLeap(chest) }
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
            leapTo(targetPlayer.name, screen)
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

        val isCoreNormal = p3Stage == P3Stages.S3
        val isCore = if (forceMageCore && p3Stage == P3Stages.S3) true else isCoreNormal

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

    private fun queryRule(sourceClass: DungeonClass, phase: M7Phases, p3Stage: P3Stages, isCore: Boolean = false): DungeonClass? {
        return when (sourceClass) {
            DungeonClass.Archer -> when (phase) {
                M7Phases.P1 -> DungeonClass.Berserk
                M7Phases.P2 -> DungeonClass.Healer
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> null
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
                M7Phases.P1 -> null
                M7Phases.P2 -> DungeonClass.Healer
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> DungeonClass.Archer
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
                M7Phases.P5 -> null
                else -> null
            }

            DungeonClass.Healer -> when (phase) {
                M7Phases.P1 -> null
                M7Phases.P2 -> DungeonClass.Archer
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> DungeonClass.Archer
                    P3Stages.S2 -> null
                    P3Stages.S3 -> {
                        if (isCore) DungeonClass.Mage
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
                M7Phases.P2 -> DungeonClass.Healer
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> DungeonClass.Archer
                    P3Stages.S2 -> DungeonClass.Healer
                    P3Stages.S3 -> null
                    P3Stages.S4 -> null
                    P3Stages.Tunnel -> DungeonClass.Healer
                    else -> null
                }
                M7Phases.P4 -> DungeonClass.Healer
                M7Phases.P5 -> DungeonClass.Berserk
                else -> null
            }

            DungeonClass.Tank -> when (phase) {
                M7Phases.P1 -> DungeonClass.Berserk
                M7Phases.P2 -> DungeonClass.Healer
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> DungeonClass.Archer
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
}