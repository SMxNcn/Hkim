package cn.hkim.addon.gui

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.ModuleConfig
import cn.hkim.addon.features.ModuleManager
import cn.hkim.addon.hud.Bounds
import cn.hkim.addon.hud.HudAlignment
import cn.hkim.addon.hud.HudElement
import cn.hkim.addon.utils.HudUtils.drawHorizontalLine
import cn.hkim.addon.utils.HudUtils.drawVerticalLine
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class HudEditScreen(private val parent: Screen?) : Screen(Component.literal("HUD Editor")) {

    private var draggingElement: HudElement? = null
    private var dragStartMouseX = 0f
    private var dragStartMouseY = 0f
    private var dragStartAnchorX = 0f
    private var dragStartAnchorY = 0f

    private var lastHoveredElement: HudElement? = null

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val tick = object : DeltaTracker {
            override fun getGameTimeDeltaTicks(): Float = delta
            override fun getGameTimeDeltaPartialTick(ignoreFrozenGame: Boolean): Float = delta
            override fun getRealtimeDeltaTicks(): Float = delta
        }
        val mf = mouseX.toFloat(); val mfY = mouseY.toFloat()

        val halfW = width / 2f; val halfH = height / 2f
        graphics.drawHorizontalLine(0f, halfH, width.toFloat(), -1, 0.5f)
        graphics.drawVerticalLine(halfW, 0f, height.toFloat(), -1, 0.5f)

        for (module in ModuleManager.getAll()) {
            if (module.enabled) {
                module.render(graphics, tick)
            }
        }

        val elements = ModuleManager.getAll().flatMap { it.hudElements }
        var hovered: HudElement? = null
        for (i in elements.indices.reversed()) {
            val el = elements[i]
            if (!el.isVisible()) continue
            val bounds = el.getScreenBounds()
            if (bounds.contains(mf, mfY) && hovered == null) hovered = el
        }
        lastHoveredElement = hovered

        for (el in elements) {
            if (!el.isVisible()) continue
            renderElementOverlay(graphics, el.getScreenBounds(), el == hovered)
        }

        val target = hovered ?: draggingElement
        if (target != null) {
            val displayName = target.name.ifEmpty { target.owner?.name ?: "?" }
            val lines = listOfNotNull(
                "§f$displayName§7  §7(${target.owner?.javaClass?.simpleName ?: "?"}) §f${"%.2f".format(target.hudScale)}x",
                target.desc.takeIf { it.isNotEmpty() }?.let { "§f$it" }
            )
            var iy = 4f
            for (line in lines) {
                graphics.text(mc.font, line, 4, iy.toInt(), -1, false)
                iy += mc.font.lineHeight + 1f
            }
        }
    }

    private fun renderElementOverlay(graphics: GuiGraphicsExtractor, bounds: Bounds, hovered: Boolean = false) {
        val borderColor = -1
        val off = 1f
        val bw = if (hovered) 1.0f else 0.5f

        graphics.pose().pushMatrix()
        graphics.pose().translate(bounds.x - off, bounds.y - off)
        val w = bounds.w + 2 * off
        val h = bounds.h + 2 * off
        graphics.fill(0, 0, w.toInt(), h.toInt(), 0)
        graphics.drawHorizontalLine(0f, 0f, w, borderColor, bw)
        graphics.drawHorizontalLine(0f, h - bw, w, borderColor, bw)
        graphics.drawVerticalLine(0f, 0f, h, borderColor, bw)
        graphics.drawVerticalLine(w - bw, 0f, h, borderColor, bw)
        graphics.pose().popMatrix()
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick)
        val mx = event.x.toFloat(); val my = event.y.toFloat()
        val hit = ModuleManager.getAll().flatMap { it.hudElements }.asReversed().firstOrNull { el ->
            el.isVisible() && el.getScreenBounds().contains(mx, my)
        } ?: return super.mouseClicked(event, doubleClick)

        draggingElement = hit
        dragStartMouseX = mx
        dragStartMouseY = my
        dragStartAnchorX = hit.anchorX
        dragStartAnchorY = hit.anchorY
        return true
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        val el = draggingElement ?: return super.mouseDragged(event, dx, dy)
        el.anchorX = dragStartAnchorX + (event.x.toFloat() - dragStartMouseX)
        el.anchorY = dragStartAnchorY + (event.y.toFloat() - dragStartMouseY)
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (draggingElement != null) {
            val mx = event.x.toFloat(); val my = event.y.toFloat()
            if (mx != dragStartMouseX || my != dragStartMouseY) {
                autoSwitchAnchor(draggingElement!!)
            }
            draggingElement = null
            ModuleConfig.saveConfig()
            return true
        }
        return super.mouseReleased(event)
    }

    private fun autoSwitchAnchor(el: HudElement) {
        val bounds = el.getScreenBounds()
        val cx = bounds.x + bounds.w / 2f
        val cy = bounds.y + bounds.h / 2f
        val sw = width.toFloat()
        val sh = height.toFloat()

        val regionH = when {
            cx < sw * 0.35f -> "LEFT"
            cx > sw * 0.65f -> "RIGHT"
            else -> "MIDDLE"
        }
        val regionV = when {
            cy < sh * 0.35f -> "TOP"
            cy > sh * 0.65f -> "BOTTOM"
            else -> "MIDDLE"
        }

        val newAlign = when {
            regionV == "MIDDLE" && regionH == "MIDDLE" -> return
            regionV == "MIDDLE" -> if (regionH == "LEFT") HudAlignment.TOP_LEFT else HudAlignment.TOP_RIGHT
            regionH == "MIDDLE" -> if (regionV == "TOP") HudAlignment.TOP_MIDDLE else HudAlignment.BOTTOM_MIDDLE
            else -> HudAlignment.valueOf("${regionV}_${regionH}")
        }

        if (newAlign != el.hudAlignment) {
            el.switchAlignment(newAlign)
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (scrollY == 0.0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        val el = lastHoveredElement ?: return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        val delta = if (scrollY > 0) 0.1f else -0.1f
        val newScale = (el.hudScale + delta).coerceIn(0.2f, 4f)
        el.scaleCenter(newScale)
        ModuleConfig.saveConfig()
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val el = lastHoveredElement ?: return super.keyPressed(event)
        val step = if (event.modifiers() and GLFW.GLFW_MOD_SHIFT != 0) 10f else 1f
        when (event.key) {
            GLFW.GLFW_KEY_LEFT -> el.anchorX -= step
            GLFW.GLFW_KEY_RIGHT -> el.anchorX += step
            GLFW.GLFW_KEY_UP -> el.anchorY -= step
            GLFW.GLFW_KEY_DOWN -> el.anchorY += step
            else -> return super.keyPressed(event)
        }
        ModuleConfig.saveConfig()
        return true
    }

    override fun onClose() {
        ModuleConfig.saveConfig()
        mc.setScreen(parent)
    }
}