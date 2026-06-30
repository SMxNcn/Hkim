package cn.hkim.addon.gui

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.Colors
import cn.hkim.addon.utils.waypoints.FarmingWaypoints
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class ActionInputScreen(
    initialAction: FarmingWaypoints.Action,
    private val onSave: (FarmingWaypoints.Action) -> Unit
) : Screen(Component.literal("Movement Keys")) {

    private var forward = initialAction.forward
    private var back = initialAction.back
    private var left = initialAction.left
    private var right = initialAction.right
    private var leftClick = initialAction.leftClick

    private val btnSize = 25
    private val gap = 10
    private val padding = 10

    private val contentWidth = (btnSize * 5) + (gap * 4) + (padding * 2)
    private val contentHeight = btnSize + (padding * 2)

    private var dialogX = 0
    private var dialogY = 0

    override fun init() {
        dialogX = (width - contentWidth) / 2
        dialogY = (height - contentHeight) / 2 - 5

        addRenderableWidget(
            Button.builder(Component.literal("Done")) {
                onSave(FarmingWaypoints.Action(forward, back, left, right, leftClick))
                onClose()
            }
                .pos(dialogX + contentWidth / 2 - 85, dialogY + contentHeight + 20)
                .width(80)
                .build()
        )

        addRenderableWidget(
            Button.builder(Component.literal("Cancel")) { onClose() }
                .pos(dialogX + contentWidth / 2 + 5, dialogY + contentHeight + 20)
                .width(80)
                .build()
        )
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val titleWidth = font.width("Movement Actions Editor")
        val textX = dialogX + contentWidth / 2 - titleWidth / 2
        val textY = dialogY - 20

        graphics.text(mc.font, "Movement Actions Editor", textX, textY, 0xFFFFFFFF.toInt(), false)

        graphics.fill(dialogX, dialogY, dialogX + contentWidth, dialogY + contentHeight, 0xE0101010.toInt())

        graphics.fill(dialogX, dialogY, dialogX + contentWidth, dialogY + 1, Colors.MINECRAFT_DARK_GRAY.rgb)
        graphics.fill(dialogX, dialogY + contentHeight - 1, dialogX + contentWidth, dialogY + contentHeight, Colors.MINECRAFT_DARK_GRAY.rgb)
        graphics.fill(dialogX, dialogY, dialogX + 1, dialogY + contentHeight, Colors.MINECRAFT_DARK_GRAY.rgb)
        graphics.fill(dialogX + contentWidth - 1, dialogY, dialogX + contentWidth, dialogY + contentHeight, Colors.MINECRAFT_DARK_GRAY.rgb)

        val startX = dialogX + padding
        val startY = dialogY + padding

        drawKey(graphics, "A", startX, startY, left)
        drawKey(graphics, "W", startX + (btnSize + gap) * 1, startY, forward)
        drawKey(graphics, "S", startX + (btnSize + gap) * 2, startY, back)
        drawKey(graphics, "D", startX + (btnSize + gap) * 3, startY, right)
        drawKey(graphics, "L", startX + (btnSize + gap) * 4, startY, leftClick)

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    private fun drawKey(graphics: GuiGraphicsExtractor, label: String, x: Int, y: Int, active: Boolean) {
        val bgColor = if (active) java.awt.Color(0, 170, 0, 180).rgb else java.awt.Color(85, 85, 85, 120).rgb
        val borderColor = if (active) Colors.MINECRAFT_GREEN.rgb else Colors.MINECRAFT_DARK_GRAY.rgb

        graphics.fill(x, y, x + btnSize, y + btnSize, bgColor)

        graphics.fill(x, y, x + btnSize, y + 1, borderColor)
        graphics.fill(x, y + btnSize - 1, x + btnSize, y + btnSize, borderColor)
        graphics.fill(x, y, x + 1, y + btnSize, borderColor)
        graphics.fill(x + btnSize - 1, y, x + btnSize, y + btnSize, borderColor)

        val textWidth = font.width(label)
        val textX = x + (btnSize - textWidth) / 2
        val textY = y + (btnSize - font.lineHeight) / 2
        graphics.text(mc.font, label, textX + 1, textY + 1, Colors.WHITE.rgb, false)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val mx = mouseButtonEvent.x.toInt()
        val my = mouseButtonEvent.y.toInt()
        val inBox = mx in dialogX..(dialogX + contentWidth) && my in dialogY..(dialogY + contentHeight)

        if (mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && inBox) {
            leftClick = !leftClick
            return true
        }

        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        return when (keyEvent.key) {
            GLFW.GLFW_KEY_W -> { forward = !forward; true }
            GLFW.GLFW_KEY_A -> { left = !left; true }
            GLFW.GLFW_KEY_S -> { back = !back; true }
            GLFW.GLFW_KEY_D -> { right = !right; true }
            GLFW.GLFW_KEY_ESCAPE -> { onClose(); true }
            else -> super.keyPressed(keyEvent)
        }
    }

    override fun isPauseScreen(): Boolean = false
    override fun extractTransparentBackground(graphics: GuiGraphicsExtractor) {}
    override fun extractMenuBackground(graphics: GuiGraphicsExtractor) {}
}
