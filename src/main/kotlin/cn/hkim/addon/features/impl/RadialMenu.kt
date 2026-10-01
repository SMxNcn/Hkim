package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.ModuleConfig
import cn.hkim.addon.config.RadialEntry
import cn.hkim.addon.config.RadialLayout
import cn.hkim.addon.config.settings.ActionSetting
import cn.hkim.addon.config.settings.KeybindSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.config.settings.SelectorSetting
import cn.hkim.addon.events.impl.InputEvent
import cn.hkim.addon.events.impl.MouseButtonEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.gui.screen.RadialMenuEditorScreen
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.island.IslandQueue
import cn.hkim.addon.utils.render.island.IslandRenderer
import cn.hkim.addon.utils.render.skiko.SkikoDraw.edgeAngle
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawCircleWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoArc
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoCenteredText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoImage
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoParallelSector
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoBatch
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoTextHeight
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoTextWidth
import cn.hkim.addon.utils.sendCommand
import com.mojang.blaze3d.platform.InputConstants
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import org.lwjgl.glfw.GLFW
import kotlin.math.*

@ModuleInfo("radial_menu", Category.MISC, false)
object RadialMenu : Module("Radial Menu", "Easier to access menu/commands.") {
    val menuKeybind by KeybindSetting("Menu Keybind", "Radial Menu Keybind", GLFW.GLFW_MOUSE_BUTTON_4)
    private val profileSetting = SelectorSetting("Profile", "Layout used by the radial menu.", RadialLayout.PROFILE_NAMES, RadialLayout.PROFILE_NAMES.first())
    val profile by profileSetting
    private val openDuration by NumberSetting("Open Duration", "Radial menu open and close animation duration.", 100f, 50f, 500f, 10f, "ms")
    private val editLayout by ActionSetting("Edit Layout", "Open the layout editor of the selected profile.") {
        mc.setScreen(RadialMenuEditorScreen(mc.screen))
    }

    const val MENU_RADIUS = 120f
    const val CENTER_RADIUS = 30f

    private const val RING_THICKNESS = 2f
    internal const val ITEM_LABEL_RADIUS = (CENTER_RADIUS + MENU_RADIUS) / 2f + 8f
    private const val SECTOR_ANGLE = (2.0 * PI / RadialLayout.SLOT_COUNT).toFloat()
    private const val SECTOR_HALF = SECTOR_ANGLE / 2f
    private const val FIRST_ANGLE = (-PI / 2.0).toFloat()

    private const val MENU_HINT_KEY = "radial-menu"

    private val ALLOWED_KEYS = setOf(GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_ESCAPE)

    private const val MENU_BG = 0xC0262626.toInt()
    private const val RING_COLOR = 0xE0262626.toInt()
    private const val CENTER_COLOR = 0xD0161616.toInt()
    private const val CLOSE_ICON_COLOR = 0xFFE0E0E0.toInt()
    private const val INDICATOR_COLOR = 0xFFFFFFFF.toInt()
    private const val SLOT_HOVER_COLOR = 0x2EFFFFFF
    private const val SLOT_HOVER_FAINT_COLOR = 0x14FFFFFF
    private const val SLOT_LABEL_COLOR = 0xFF9E9E9E.toInt()
    private const val SLOT_LABEL_HOVER_COLOR = 0xFFFFFFFF.toInt()
    private const val SLOT_LABEL_DRAG_COLOR = 0xFF6E6E6E.toInt()

    private val indicatorHalfWidth = Math.toRadians(20.0).toFloat()

    private val openAnim = GuiAnimation.create(0f, 0f)
        .duration(openDuration.toLong())
        .easing(Easing.CUBIC_OUT)

    private var isMenuOpen = false
    private var closing = false

    private var closedUntilRelease = false

    private val entries: List<RadialEntry> get() = RadialLayout.entries(profile)

    private val centerX get() = mc.window.guiScaledWidth / 2f
    private val centerY get() = mc.window.guiScaledHeight / 2f

    private val mouseX get() = HudUtils.mouseX * mc.window.guiScaledWidth / mc.window.screenWidth
    private val mouseY get() = HudUtils.mouseY * mc.window.guiScaledHeight / mc.window.screenHeight

    @EventHandler
    fun onTick(event: TickEvent.End) {
        if (!enabled || mc.player == null) return

        if (isMenuOpen) {
            if (mc.screen != null || closing && openAnim.getValue() <= 0.001f) {
                finishClose()
                return
            }
        }

        val holding = isKeyPressed(menuKeybind)
        if (holding) {
            if (!isMenuOpen && !closedUntilRelease) openMenu()
        } else {
            closedUntilRelease = false
            if (isMenuOpen && !closing) {
                val hovered = hoveredSlot()
                if (hovered >= 0) activate(hovered) else closeMenu()
            }
        }
    }

    @EventHandler
    fun onInput(event: InputEvent) {
        if (!isMenuOpen || closing) return
        if (event.key.type != InputConstants.Type.KEYSYM) {
            event.cancel()
            return
        }
        if (event.key.value in ALLOWED_KEYS) return
        event.cancel()
    }

    @EventHandler
    fun onMouseClick(event: MouseButtonEvent) {
        if (!isMenuOpen || closing) return

        event.cancel()

        closedUntilRelease = true
        val hovered = hoveredSlot()
        if (hovered >= 0) activate(hovered) else closeMenu()
    }

    @JvmStatic
    fun onScroll(scrollY: Double): Boolean {
        if (!isMenuOpen) return false
        if (!closing && scrollY != 0.0 && hoveredSlot() < 0) {
            switchProfile(if (scrollY < 0) 1 else -1)
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
        }
        return true
    }

    override fun render(graphics: GuiGraphicsExtractor, tickTracker: DeltaTracker) {
        if (!isMenuOpen || mc.options.hideGui) return
        super.render(graphics, tickTracker)

        val progress = openAnim.getValue()
        if (progress <= 0f && !closing) return

        val discRadius = MENU_RADIUS * progress
        if (discRadius < 1f) return

        val slots = entries
        val hovered = if (closing) -1 else hoveredSlot()

        if (!closing) {
            val detail = slots.getOrNull(hovered)?.description?.takeIf { it.isNotBlank() } ?: "Grab mouse to select buttons"
            IslandRenderer.showMessage(
                "Radial Menu · ${RadialLayout.PROFILE_NAMES[profile]}",
                detail,
                centered = true,
                priority = IslandQueue.PRIORITY_MENU_HINT,
                key = MENU_HINT_KEY,
            )
        }

        drawRing(graphics, slots, centerX, centerY, progress, hovered, batchContent = {
            val iconSize = 12f * progress
            graphics.drawSkikoImage(
                "assets/hkim/textures/clickgui/close.svg",
                centerX - iconSize / 2f, centerY - iconSize / 2f,
                iconSize, iconSize, 0f,
                tintColor = CLOSE_ICON_COLOR,
            )

            val dx = mouseX - centerX
            val dy = mouseY - centerY
            val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (distance > CENTER_RADIUS * progress) {
                val angle = atan2(dy, dx)
                graphics.drawSkikoArc(
                    centerX, centerY, discRadius,
                    angle - indicatorHalfWidth, indicatorHalfWidth * 2f,
                    2f * progress, INDICATOR_COLOR,
                )
            }
        })
    }

    fun drawRing(
        graphics: GuiGraphicsExtractor,
        entries: List<RadialEntry>,
        centerX: Float, centerY: Float,
        scale: Float,
        highlight: Int,
        hover: Int = -1,
        dragging: Int = -1,
        batchContent: (() -> Unit)? = null,
        extraPad: Float = 0f,
    ) {
        val radius = MENU_RADIUS * scale
        val innerRadius = (CENTER_RADIUS + 2f) * scale
        val gap = 2f * scale
        val labelRadius = ITEM_LABEL_RADIUS * scale
        val contentScale = if (dragging >= 0) scale * 0.9f else scale

        var labelPad = 0f
        entries.forEachIndexed { index, entry ->
            if (index == dragging || RadialLayout.icon(entry.item) != null || entry.name.isBlank()) return@forEachIndexed
            val size = 9f * contentScale
            val half = maxOf(skikoTextWidth(entry.name, size) / 2f, skikoTextHeight(size) / 2f)
            if (half > labelPad) labelPad = half
        }
        val bound = maxOf(
            radius + RING_THICKNESS * scale + 2f,
            labelRadius + labelPad + 2f,
            CENTER_RADIUS * scale + 2f,
            extraPad,
        )

        graphics.skikoBatch(centerX - bound, centerY - bound, bound * 2f, bound * 2f) {
            entries.forEachIndexed { index, _ ->
                if (index == dragging) return@forEachIndexed
                drawSlotBackground(graphics, centerX, centerY, scale, slotAngle(index))
            }

            if (hover in entries.indices && hover != highlight && hover != dragging) {
                graphics.drawSkikoParallelSector(centerX, centerY, innerRadius, radius, sectorStart(hover), sectorEnd(hover), gap, SLOT_HOVER_FAINT_COLOR)
            }

            if (highlight in entries.indices && highlight != dragging) {
                graphics.drawSkikoParallelSector(centerX, centerY, innerRadius, radius, sectorStart(highlight), sectorEnd(highlight), gap, SLOT_HOVER_COLOR)
            }

            graphics.drawCircleWithBorder(
                centerX, centerY,
                CENTER_COLOR, RING_COLOR, RING_THICKNESS * scale, CENTER_RADIUS * scale,
            )

            entries.forEachIndexed { index, entry ->
                if (index == dragging) return@forEachIndexed
                val selected = index == highlight
                val angle = slotAngle(index)
                drawSlotLabel(
                    graphics, entry,
                    centerX + cos(angle) * labelRadius,
                    centerY + sin(angle) * labelRadius,
                    contentScale,
                    when {
                        selected -> SLOT_LABEL_HOVER_COLOR
                        dragging >= 0 -> SLOT_LABEL_DRAG_COLOR
                        else -> SLOT_LABEL_COLOR
                    },
                    bold = selected,
                )
            }

            batchContent?.invoke()
        }

        entries.forEachIndexed { index, entry ->
            if (index == dragging) return@forEachIndexed
            val angle = slotAngle(index)
            drawSlotIcon(
                graphics, entry,
                centerX + cos(angle) * labelRadius,
                centerY + sin(angle) * labelRadius,
                contentScale,
            )
        }
    }

    fun drawSlotBackground(
        graphics: GuiGraphicsExtractor,
        centerX: Float, centerY: Float,
        scale: Float,
        angle: Float,
        background: Int = MENU_BG,
        outline: Int = RING_COLOR,
        growth: Float = 0f,
    ) {
        val radius = MENU_RADIUS * scale + growth
        val innerRadius = (CENTER_RADIUS + 2f) * scale - growth
        val gap = 2f * scale
        val start = angle - SECTOR_HALF
        val end = angle + SECTOR_HALF

        graphics.drawSkikoParallelSector(centerX, centerY, innerRadius, radius, start, end, gap, background)

        val outlineStart = edgeAngle(start, gap / 2f, radius)
        val outlineEnd = edgeAngle(end, -gap / 2f, radius)
        graphics.drawSkikoArc(
            centerX, centerY, radius,
            outlineStart, outlineEnd - outlineStart,
            RING_THICKNESS * scale, outline,
        )
    }

    fun drawSlotContent(graphics: GuiGraphicsExtractor, entry: RadialEntry, x: Float, y: Float, scale: Float, color: Int, bold: Boolean = false, ) {
        if (RadialLayout.icon(entry.item) != null) {
            drawSlotIcon(graphics, entry, x, y, scale)
        } else {
            drawSlotLabel(graphics, entry, x, y, scale, color, bold)
        }
    }

    fun drawSlotLabel(graphics: GuiGraphicsExtractor, entry: RadialEntry, x: Float, y: Float, scale: Float, color: Int, bold: Boolean = false, ) {
        if (RadialLayout.icon(entry.item) != null || entry.name.isBlank()) return
        val labelSize = 9f * scale
        graphics.drawSkikoCenteredText(entry.name, x, y - skikoTextHeight(labelSize) / 2f, labelSize, color, bold)
    }

    fun drawSlotIcon(graphics: GuiGraphicsExtractor, entry: RadialEntry, x: Float, y: Float, scale: Float, ) {
        val icon = RadialLayout.icon(entry.item) ?: return
        val iconScale = scale * 1.5f
        graphics.pose().pushMatrix()
        graphics.pose().translate(x, y)
        graphics.pose().scale(iconScale, iconScale)
        graphics.item(icon, -8, -8)
        graphics.pose().popMatrix()
    }

    fun slotAt(
        mouseX: Float, mouseY: Float,
        centerX: Float, centerY: Float,
        centerRadius: Float = CENTER_RADIUS,
        outerRadius: Float = Float.POSITIVE_INFINITY,
    ): Int {
        val dx = mouseX - centerX
        val dy = mouseY - centerY
        val distance = sqrt((dx * dx + dy * dy).toDouble())
        if (distance <= centerRadius || distance > outerRadius) return -1

        val relative = atan2(dy, dx) - FIRST_ANGLE
        return floor((relative + SECTOR_HALF) / SECTOR_ANGLE).toInt().mod(RadialLayout.SLOT_COUNT)
    }

    override fun onDisable() {
        if (isMenuOpen) finishClose()
    }

    private fun openMenu() {
        if (isMenuOpen || mc.screen != null) return
        isMenuOpen = true
        closing = false
        openAnim.snapTo(0f)
        openAnim.duration(openDuration.toLong())
        openAnim.animateTo(1f)
        mc.mouseHandler.releaseMouse()
    }

    private fun closeMenu() {
        if (!isMenuOpen || closing) return
        closing = true
        IslandRenderer.clearMessage(MENU_HINT_KEY)
        if (mc.screen == null) mc.mouseHandler.grabMouse()
        openAnim.duration(openDuration.toLong())
        openAnim.animateTo(0f)
    }

    private fun finishClose() {
        isMenuOpen = false
        closing = false
        IslandRenderer.clearMessage(MENU_HINT_KEY)
        if (mc.screen == null) mc.mouseHandler.grabMouse()
    }

    private fun activate(slot: Int) {
        val command = entries.getOrNull(slot)?.command?.trim()?.removePrefix("/")?.trim().orEmpty()
        closeMenu()
        if (command.isNotEmpty()) sendCommand(command)
    }

    private fun hoveredSlot(): Int = slotAt(mouseX, mouseY, centerX, centerY)

    fun switchProfile(step: Int) {
        val names = RadialLayout.PROFILE_NAMES
        profileSetting.select(names[(profile + step).mod(names.size)])
        ModuleConfig.saveConfigImmediate()
    }

    private fun slotAngle(index: Int): Float = FIRST_ANGLE + index * SECTOR_ANGLE

    private fun sectorStart(index: Int): Float = slotAngle(index) - SECTOR_HALF

    private fun sectorEnd(index: Int): Float = slotAngle(index) + SECTOR_HALF

    private fun isKeyPressed(glfwCode: Int): Boolean {
        val window = mc.window.handle()
        return if (glfwCode in 0..7) GLFW.glfwGetMouseButton(window, glfwCode) == GLFW.GLFW_PRESS
        else GLFW.glfwGetKey(window, glfwCode) == GLFW.GLFW_PRESS
    }
}
