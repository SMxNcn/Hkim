package cn.hkim.addon.gui.screen

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.gui.ClientButton
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoCenteredText
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import cn.hkim.addon.compat.input.InputCompat

/**
 * Simon Says GUI: 4x4 button grid + a start button.
 *
 * The module reads [p0] / [pxPerBlock] / [baseMouseX] / [baseMouseY] / [mouseX] / [mouseY]
 * every tick to convert the mouse position into a world target point on the button plane.
 */
class SimonSaysGuiScreen(
    private val onStart: () -> Unit,
    private val onGridClick: (Int) -> Unit,
    private val closeCallback: () -> Unit,
) : Screen(Component.literal("Simon Says")) {

    companion object {
        private const val GRID_SIZE = 4
        private const val CELL_SIZE = 40
        private const val GAP = 6
        private const val START_W = 96
        private const val START_H = 36

        // ==== 设备坐标 ====
        // TODO: 替换为实际设备坐标(与玩家站位区域配套)
        private const val GRID_X = 110           // 按钮方块所在 x
        private const val GRID_Y_MAX = 123      // 网格最高行
        private const val GRID_Z_MIN = 92       // 网格最小列(z)
        private const val BUTTON_FACE_X = 110.95 // 按钮贴在东面的点击中心 x
    }

    /** 当前鼠标位置(每帧由 extractRenderState 更新,供模块 tick 读取) */
    var mouseX = 0
        private set
    var mouseY = 0
        private set

    /** 鼠标映射基准:点击 Start 时记录的鼠标位置 */
    var baseMouseX = 0
    var baseMouseY = 0

    /** 是否正在连点开始按钮 */
    var clicking = false
        internal set

    /** 是否按住 Shift(按住期间以 6-8 CPS 连点开始按钮,每帧由 GLFW 状态更新) */
    var shiftHeld = false
        private set

    /** 模块当前正在瞄准的格子索引(用于高亮),-1 表示无 */
    var pendingIndex = -1

    /** 记录到的序列(按钮索引 0-15)与点击进度,由模块每 tick 同步 */
    var sequence: List<Int> = emptyList()
    var clickNeeded = 0
    var firstPhase = true

    /** 映射参数:初始视角射线与按钮平面的交点 */
    var p0: Vec3 = Vec3.ZERO
        private set

    /** GUI 内每格对应的世界像素比例(px/格) */
    val pxPerBlock: Float = (CELL_SIZE + GAP).toFloat()

    private var gridX = 0
    private var gridY = 0
    private var gridW = 0
    private var gridH = 0

    private val buttonVecs: Array<Vec3?> = arrayOfNulls(GRID_SIZE * GRID_SIZE)

    override fun init() {
        gridW = GRID_SIZE * CELL_SIZE + (GRID_SIZE - 1) * GAP
        gridH = gridW
        gridX = (width - gridW) / 2
        gridY = (height - gridH) / 2 - 24

        addRenderableWidget(
            ClientButton(
                gridX - START_W - 24,
                gridY + gridH / 2 - START_H / 2,
                START_W, START_H,
                Component.literal("Start"),
                onPress = { if (!clicking) onStart() },
            )
        )

        // 预计算 16 个按钮的世界坐标(GUI 顶行 = 世界最高行 y=123)
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                buttonVecs[r * GRID_SIZE + c] = Vec3(
                    BUTTON_FACE_X,
                    (GRID_Y_MAX - r) + 0.5,
                    (GRID_Z_MIN + c) + 0.5,
                )
            }
        }

        // 初始交点:打开 GUI 时玩家视角射线与按钮平面 x = BUTTON_FACE_X 的交点。
        // 鼠标位于基准位置(baseMouse)时视角保持为打开 GUI 时的视角。
        val player = mc.player
        if (player != null) {
            val eye = player.eyePosition
            val dir = rotationToVec3(player.yRot, player.xRot)
            val t = if (abs(dir.x) > 0.05) (BUTTON_FACE_X - eye.x) / dir.x else 4.0
            p0 = eye.add(dir.scale(t))
        } else {
            p0 = Vec3(BUTTON_FACE_X, (GRID_Y_MAX + GRID_Y_MAX - 3 + 1) / 2.0, (GRID_Z_MIN + GRID_Z_MIN + 3 + 1) / 2.0)
        }

        super.init()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        this.mouseX = mouseX
        this.mouseY = mouseY
        shiftHeld = InputCompat.isShiftDown()

        val title = "Simon Says"
        val tw = mc.font.width(title)
        graphics.text(mc.font, title, width / 2 - tw / 2, gridY - 32, 0xFFFFFFFF.toInt(), false)

        // 状态文本
        val status = when {
            clicking -> "Clicking start button..."
            pendingIndex >= 0 -> "Aiming..."
            firstPhase && sequence.isNotEmpty() -> "Recording sequence..."
            sequence.isNotEmpty() && clickNeeded < sequence.size -> "Click in order: green -> gold -> red"
            sequence.isNotEmpty() -> "Round complete!"
            else -> "Press Start to begin."
        }
        val sw = mc.font.width(status)
        graphics.text(mc.font, status, width / 2 - sw / 2, gridY + gridH + 14, 0xFFAAAAAA.toInt(), false)

        // 4x4 网格(纯色圆角矩形)
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                val index = r * GRID_SIZE + c
                val x = gridX + c * (CELL_SIZE + GAP)
                val y = gridY + r * (CELL_SIZE + GAP)
                val hovered = mouseX in x until x + CELL_SIZE && mouseY in y until y + CELL_SIZE
                val active = index == pendingIndex

                val fill = when {
                    active -> 0xA02E7D32.toInt()
                    hovered -> 0xA0444444.toInt()
                    else -> 0xA0333333.toInt()
                }
                val border = if (active) 0xFF66BB6A.toInt() else 0xFF888888.toInt()
                graphics.drawRoundedRectWithBorder(
                    x.toFloat(), y.toFloat(),
                    CELL_SIZE.toFloat(), CELL_SIZE.toFloat(),
                    fill, border, 1f, 4f,
                )

                // 序列高亮:直接显示在网格上(下一个绿、下下个金、其余红,已点过的置灰)
                val highlight = sequenceHighlight(index)
                if (highlight != null) {
                    graphics.drawRoundedRectWithBorder(
                        x.toFloat(), y.toFloat(),
                        CELL_SIZE.toFloat(), CELL_SIZE.toFloat(),
                        highlight.first, highlight.second, 2f, 2f,
                    )
                }

                // 按钮序号
                graphics.drawSkikoCenteredText(
                    "${index + 1}",
                    x + CELL_SIZE / 2f,
                    y + CELL_SIZE / 2f - mc.font.lineHeight / 2f,
                    mc.font.lineHeight.toFloat(),
                    0xFFE0E0E0.toInt(),
                )

                // 悬停平面边框 / 瞄准目标高亮
                if (active) {
                    graphics.drawRoundedRectWithBorder(
                        x.toFloat(), y.toFloat(),
                        CELL_SIZE.toFloat(), CELL_SIZE.toFloat(),
                        0x00000000, 0xFFFFFFFF.toInt(), 2.5f, 2f,
                    )
                } else if (hovered) {
                    graphics.drawRoundedRectWithBorder(
                        x.toFloat(), y.toFloat(),
                        CELL_SIZE.toFloat(), CELL_SIZE.toFloat(),
                        0x00000000, 0xFFFFFFFF.toInt(), 1.5f, 2f,
                    )
                }
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    private fun sequenceHighlight(index: Int): Pair<Int, Int>? {
        val nextPos = sequence.indices.firstOrNull { sequence[it] == index && it >= clickNeeded }
        return when {
            nextPos != null -> when (nextPos) {
                clickNeeded -> 0x4055FF55 to 0xFF55FF55.toInt()
                clickNeeded + 1 -> 0x40FFAA00 to 0xFFFFAA00.toInt()
                else -> 0x40FF5555 to 0xFFFF5555.toInt()
            }
            index in sequence -> 0x40666666 to 0xFF888888.toInt()
            else -> null
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        // 左右键均触发
        if (event.button() == InputCompat.MOUSE_LEFT || event.button() == InputCompat.MOUSE_RIGHT) {
            val mx = event.x.toInt()
            val my = event.y.toInt()

            for (r in 0 until GRID_SIZE) {
                for (c in 0 until GRID_SIZE) {
                    val x = gridX + c * (CELL_SIZE + GAP)
                    val y = gridY + r * (CELL_SIZE + GAP)
                    if (mx >= x && mx < x + CELL_SIZE && my >= y && my < y + CELL_SIZE) {
                        onGridClick(r * GRID_SIZE + c)
                        return true
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun onClose() {
        closeCallback()
        super.onClose()
    }

    fun getButtonVec(index: Int): Vec3? = buttonVecs.getOrNull(index)

    fun getButtonPos(index: Int): BlockPos? {
        if (index !in 0 until GRID_SIZE * GRID_SIZE) return null
        val r = index / GRID_SIZE
        val c = index % GRID_SIZE
        return BlockPos(GRID_X, GRID_Y_MAX - r, GRID_Z_MIN + c)
    }

    private fun rotationToVec3(yaw: Float, pitch: Float): Vec3 {
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())
        val dx = -sin(yawRad) * cos(pitchRad)
        val dy = -sin(pitchRad)
        val dz = cos(yawRad) * cos(pitchRad)
        return Vec3(dx, dy, dz)
    }
}
