package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.KeybindSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.config.settings.SelectorSetting
import cn.hkim.addon.events.impl.InputEvent
import cn.hkim.addon.events.impl.RenderEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.gui.screen.SimonSaysGuiScreen
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.render.drawWireFrameBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import meteordevelopment.orbit.EventHandler
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
//? if 26.2 {
//? } else {
//import net.minecraft.world.item.component.SwingAnimation
//? }
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import cn.hkim.addon.compat.input.InputCompat

@ModuleInfo("simon_says", Category.SKYBLOCK)
object SimonSays : Module("Simon Says", "Advanced simon says features.") {
    private val mode by SelectorSetting("Mode", "Simon Says mode.", listOf("Auto Click", "GUI"), "Auto Click")

    // ===== Auto Click 模式 =====
    private val autoClick by BooleanSetting("Auto Click", "Automatically aim and click the correct buttons.", false).depends { mode == 0 }
    private val aimSpeed by NumberSetting("Aim Speed", "Aim transition speed.", 0.25f, 0.05f, 1.0f, 0.05f).depends { mode == 0 && autoClick }
    private val clickDelay by NumberSetting("Click Delay", "Delay between aiming and clicking.", 150f, 50f, 500f, 10f, "ms").depends { mode == 0 && autoClick }

    // ===== GUI 模式 =====
    private val guiKeybind by KeybindSetting("GUI Keybind", "Key to open the Simon Says GUI.", InputCompat.KEY_NONE).depends { mode == 1 }
    private val guiAimSpeed by NumberSetting("GUI Aim Speed", "Aim transition speed while following the mouse in GUI mode.", 0.3f, 0.05f, 1.0f, 0.05f).depends { mode == 1 }
    private val guiClickDelay by NumberSetting("GUI Click Delay", "Delay after the view is aligned before clicking in GUI mode.", 120f, 0f, 500f, 10f, "ms").depends { mode == 1 }
    private val faceTolerance by NumberSetting("Face Tolerance", "Maximum angle to the start button required to open the GUI.", 45f, 10f, 90f, 5f, "°").depends { mode == 1 }

    private val startButton = BlockPos(110, 121, 91)
    private val clickSequence = mutableListOf<Int>()
    private var lastLanternTick = -1
    private var clickNeeded = 0
    private var firstPhase = true
    private var lastClickTime = 0L

    // ===== GUI 模式设备坐标 ====
    // TODO: 替换为实际设备坐标(与 SimonSaysGuiScreen 中的常量配套)
    private val stanceMin = BlockPos(107, 119, 93)
    private val stanceMax = BlockPos(108, 122, 94)

    // ===== GUI 模式状态 =====
    private var guiScreen: SimonSaysGuiScreen? = null
    private var pendingClick: PendingClick? = null
    private var mappingEnabled = false
    private var startTick = -1
    private var nextClickTime = 0L

    private class PendingClick(val target: Vec3, val pos: BlockPos, val time: Long)

    private val GRID = listOf(
        BlockPos(110, 123, 92), BlockPos(110, 123, 93), BlockPos(110, 123, 94), BlockPos(110, 123, 95),
        BlockPos(110, 122, 92), BlockPos(110, 122, 93), BlockPos(110, 122, 94), BlockPos(110, 122, 95),
        BlockPos(110, 121, 92), BlockPos(110, 121, 93), BlockPos(110, 121, 94), BlockPos(110, 121, 95),
        BlockPos(110, 120, 92), BlockPos(110, 120, 93), BlockPos(110, 120, 94), BlockPos(110, 120, 95),
    )

    @EventHandler
    private fun onWorldLoad(event: WorldEvent.Load) {
        if (!enabled) return
        reset()
        firstPhase = true
    }

    @EventHandler
    private fun onBlockUpdate(event: WorldEvent.BlockUpdate) {
        if (!enabled/* || P3Stages.getP3Stage() != P3Stages.S1*/) return
        if (event.pos == startButton && isButtonClicked(event.newState)) {
            reset()
            firstPhase = true
            return
        }

        if (event.pos.y !in 120..123 || event.pos.z !in 92..95) return

        when (event.pos.x) {
            111 -> {
                if (event.newState.block == Blocks.OBSIDIAN && event.oldState.block == Blocks.SEA_LANTERN) {
                    getIndex(event.pos.immutable())?.takeIf { it !in clickSequence }?.let { clickSequence.add(it) }
                    lastLanternTick = 0
                    if (!firstPhase) return

                    when (clickSequence.size) {
                        2 -> clickSequence.reverse()
                        3 -> clickSequence.removeAt(clickSequence.lastIndex - 1)
                    }
                }
            }
            110 -> {
                if (event.newState.block == Blocks.AIR) {
                    if (GRID.count { Hkim.mc.level?.getBlockState(it)?.isAir == true } > 8) reset()
                } else if (event.oldState.block == Blocks.STONE_BUTTON && event.newState.getValue(BlockStateProperties.POWERED)) {
                    val pressedIndex = getIndex(event.pos.east()) ?: return
                    clickNeeded = clickSequence.indexOf(pressedIndex) + 1
                    if (clickNeeded >= clickSequence.size) {
                        reset()
                        firstPhase = false
                    }
                }
            }
        }
    }

    @EventHandler
    private fun onServerTick(event: TickEvent.End) {
        if (!enabled/* || P3Stages.getP3Stage() != P3Stages.S1*/) return
        if (mode == 0) onAutoClickTick() else onGuiTick()
    }

    private fun onAutoClickTick() {
        val displayStopped = lastLanternTick++ > 10
        if (firstPhase && displayStopped && GRID.count { Hkim.mc.level?.getBlockState(it)?.block == Blocks.STONE_BUTTON } > 8) {
            firstPhase = false
        }

        if (!autoClick || !displayStopped) return
        if (mc.gui.screen() != null) return
        if (clickSequence.isEmpty() || clickNeeded >= clickSequence.size) return

        val nextIndex = clickSequence[clickNeeded]
        val nextPos = GRID.getOrNull(nextIndex) ?: return
        val target = getButtonVec(nextIndex) ?: return
        if (!RotationUtils.isAligned(target, 1.5f)) return

        val now = System.currentTimeMillis()
        if (now - lastClickTime < clickDelay) return
        lastClickTime = now

        Hkim.scope.launch {
            delay(randomDelay(clickDelay.toInt(), 80))
            rightClick()
        }
    }

    private fun onGuiTick() {
        val screen = guiScreen
        if (screen == null || mc.gui.screen() !== screen) {
            if (screen != null) onGuiClosed() // 兜底清理(GUI 被其它界面替换等场景)
            else stopAimingIfNeeded()
            return
        }

        // 点 Start 后延迟 2 tick 启用鼠标 -> 视角映射
        if (startTick >= 0 && (mc.level?.gameTime?.toInt() ?: 0) >= startTick) {
            startTick = -1
            mappingEnabled = true
        }

        // 连点开始按钮:按住 Shift 期间以 6-8 CPS 持续点击,松开即停
        if (screen.clicking) {
            if (screen.shiftHeld) {
                val now = System.currentTimeMillis()
                if (now >= nextClickTime) {
                    clickBlockPacket(startButton, buttonCenter(startButton), Direction.UP)
                    nextClickTime = now + randomDelay(146, 21)
                }
            } else {
                screen.clicking = false
            }
        }

        // 同步序列数据给 GUI 显示
        screen.sequence = clickSequence.toList()
        screen.clickNeeded = clickNeeded
        screen.firstPhase = firstPhase
    }

    /** GUI 模式瞄准(每帧驱动,避免 tick 驱动导致的卡顿) */
    private fun onGuiRender() {
        val screen = guiScreen
        if (screen == null || mc.gui.screen() !== screen) {
            if (screen != null) onGuiClosed()
            else stopAimingIfNeeded()
            return
        }

        val pending = pendingClick
        if (pending != null) {
            if (System.currentTimeMillis() - pending.time > 3000) {
                pendingClick = null
                screen.pendingIndex = -1
                return
            }

            val target = pending.target
            if (RotationUtils.isAligned(target, 2f) && System.currentTimeMillis() - pending.time >= guiClickDelay) {
                clickBlockPacket(pending.pos, target, Direction.EAST)
                pendingClick = null
                screen.pendingIndex = -1
                return
            }
            RotationUtils.aimSilent(target, guiAimSpeed, owner = this)
            return
        }

        if (mappingEnabled) {
            val dx = (screen.mouseX - screen.baseMouseX) / screen.pxPerBlock
            val dy = (screen.mouseY - screen.baseMouseY) / screen.pxPerBlock
            val target = screen.p0.add(0.0, -dy.toDouble(), dx.toDouble())
            RotationUtils.aimSilent(target, guiAimSpeed, owner = this)
        } else {
            stopAimingIfNeeded()
        }
    }

    @EventHandler
    private fun onRender(event: RenderEvent.Extract) {
        if (!enabled) {
            stopAimingIfNeeded()
            return
        }
        if (mode == 1) {
            onGuiRender()
            return
        }

        if (autoClick/* && lastLanternTick > 10*/ && clickSequence.isNotEmpty() && clickNeeded < clickSequence.size) {
            getButtonVec(clickSequence[clickNeeded])?.let { RotationUtils.aimSilent(it, aimSpeed / 10f, owner = this/*, 1.2f*/) }
        } else {
            stopAimingIfNeeded()
        }

        for (index in clickNeeded until clickSequence.size) {
            val gridIndex = clickSequence[index]
            val color = when (index) {
                clickNeeded -> Colors.MINECRAFT_GREEN
                clickNeeded + 1 -> Colors.MINECRAFT_GOLD
                else -> Colors.MINECRAFT_RED
            }

            event.drawWireFrameBox(getButtonAABB(gridIndex)!!, color, 2f)
        }
    }

    @EventHandler
    private fun onKeyEvent(event: InputEvent) {
        if (!enabled || mode != 1) return
        if (mc.gui.screen() != null) return
        if (event.key.value != guiKeybind) return

        val player = mc.player ?: return
        if (!isPositionInArea(stanceMin, stanceMax, player.blockPosition())) {
            modMessage("You must stand in the Simon Says device area to open the GUI.")
            return
        }

        val startTarget = RotationUtils.vec3ToRotation(buttonCenter(startButton))
        if (Math.abs(RotationUtils.wrapAngleTo180(player.yRot - startTarget.yaw)) > faceTolerance ||
            Math.abs(RotationUtils.wrapAngleTo180(player.xRot - startTarget.pitch)) > faceTolerance) {
            modMessage("You must face the start button to open the GUI.")
            return
        }

        openGui()
    }

    private fun openGui() {
        if (guiScreen != null) return
        val screen = SimonSaysGuiScreen(
            onStart = { onStartPressed() },
            onGridClick = { onGridClick(it) },
            closeCallback = { onGuiClosed() },
        )
        guiScreen = screen
        mc.gui.setScreen(screen)
    }

    private fun onGuiClosed() {
        guiScreen = null
        pendingClick = null
        mappingEnabled = false
        startTick = -1
        nextClickTime = 0
        stopAimingInstantly()
    }

    private fun stopAimingInstantly() {
        if (!RotationUtils.isSilentAiming && !RotationUtils.isStoppingAiming) return
        var guard = 0
        while (guard++ < 20 && RotationUtils.tickStopAiming(0.6f, this)) { }
    }

    private fun onStartPressed() {
        val screen = guiScreen ?: return
        screen.baseMouseX = screen.mouseX
        screen.baseMouseY = screen.mouseY
        mappingEnabled = false
        startTick = (mc.level?.gameTime?.toInt() ?: 0) + 2

        if (screen.shiftHeld) {
            screen.clicking = true
            nextClickTime = System.currentTimeMillis()
        } else {
            clickBlockPacket(startButton, buttonCenter(startButton), Direction.UP)
        }
    }

    private fun onGridClick(index: Int) {
        val screen = guiScreen ?: return
        val target = screen.getButtonVec(index) ?: return
        val pos = screen.getButtonPos(index) ?: return
        pendingClick = PendingClick(target, pos, System.currentTimeMillis())
        screen.pendingIndex = index
    }

    private fun buttonCenter(pos: BlockPos): Vec3 =
        Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)

    private fun clickBlockPacket(pos: BlockPos, hit: Vec3, face: Direction) {
        val player = mc.player ?: return
        val hitResult = BlockHitResult(hit, face, pos, false)
        mc.gameMode?.useItemOn(player, InteractionHand.MAIN_HAND, hitResult)
        //? if 26.2 {
        player.swing(InteractionHand.MAIN_HAND)
        //? } else {
        //player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true)
        //? }
    }

    override fun onDisable() {
        if (guiScreen != null) {
            mc.gui.setScreen(null)
            onGuiClosed()
        }
    }

    private fun reset() {
        clickSequence.clear()
        clickNeeded = 0
        lastLanternTick = -1
    }

    private fun getIndex(pos: BlockPos): Int? {
        if (pos.x !in 110..111 || pos.y !in 120..123 || pos.z !in 92..95) return null
        val row = 123 - pos.y
        val col = pos.z - 92
        return row * 4 + col
    }

    private fun isButtonClicked(state: BlockState): Boolean {
        return state.block == Blocks.STONE_BUTTON && state.getValue(BlockStateProperties.POWERED)
    }

    private fun stopAimingIfNeeded() {
        if (RotationUtils.isSilentAiming || RotationUtils.isStoppingAiming) {
            RotationUtils.tickStopAiming(aimSpeed / 5f, this)
        }
    }

    private fun clickButton(pos: BlockPos) {
        val player = mc.player ?: return
        val hit = BlockHitResult(Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5), Direction.WEST, pos, false)
        rightClick()
    }

    private fun getButtonVec(index: Int): Vec3? {
        val pos = GRID.getOrNull(index) ?: return null
        return Vec3(pos.x + 0.95, pos.y + 0.5, pos.z + 0.5)
    }

    private fun getButtonAABB(index: Int): AABB? {
        val pos = GRID.getOrNull(index) ?: return null
        return AABB(pos.x + 1.00, pos.y + 0.37, pos.z + 0.3, pos.x + 0.85, pos.y + 0.63, pos.z + 0.7)
    }
}
