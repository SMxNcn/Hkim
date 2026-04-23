package cn.hkim.addon.config.clickgui

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.ModuleConfig
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.config.settings.TextSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleManager
import cn.hkim.addon.features.impl.ClickGUI
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.HudUtils.drawHorizontalSeparator
import cn.hkim.addon.utils.HudUtils.drawRectWithBorder
import cn.hkim.addon.utils.playSoundAtPlayer
import com.mojang.blaze3d.platform.cursor.CursorType
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import kotlin.math.max
import kotlin.math.min

class ClickGUIScreen(private val parent: Screen?) : Screen(Component.literal("Click GUI")) {
    private var guiX = 0f
    private var guiY = 0f
    private val guiW = 480f
    private val guiH = 300f

    private val sidebarW = 36f
    private val headerH = 45f
    private val contentPadding = 12f

    private val sidebarLogo = Identifier.fromNamespaceAndPath("hkim", "textures/clickgui/sidebar/icon20x.png")
    private val editIcon = Identifier.fromNamespaceAndPath("hkim", "textures/clickgui/sidebar/edit.png")
    private val fileIcon = Identifier.fromNamespaceAndPath("hkim", "textures/clickgui/sidebar/file.png")

    private var selectedCategory: Category? = null
    private var searchQuery = ""
    private var contentScrollY = 0f

    private val themeColor = ClickGUI.getGuiColor()
    private val cardStates = mutableMapOf<String, ModuleCardState>()

    private var activeEditBox: EditBox? = null
    private var searchEditBox: EditBox? = null
    var activeEditBoxSetting: Setting<*>? = null

    override fun init() {
        guiX = (mc.window.guiScaledWidth - guiW) / 2f
        guiY = (mc.window.guiScaledHeight - guiH) / 2f

        cardStates.clear()
        for (module in ModuleManager.getAll()) {
            cardStates[module.id] = ModuleCardState(module)
        }

        activeEditBox = null
        searchEditBox = null
        activeEditBoxSetting = null
        super.init()
    }

    override fun tick() {
        cardStates.values.forEach { it.update(0.07f) }
        super.tick()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0x80000000.toInt())

        graphics.drawRectWithBorder(guiX, guiY, guiW, guiH, 0xFF1A1A1A.toInt(), 0xFF333333.toInt(), 2)

        renderSidebar(graphics, mouseX, mouseY, delta)
        renderHeader(graphics, mouseX, mouseY, delta)
        renderContentArea(graphics, mouseX, mouseY, delta)

        activeEditBox?.let { editBox ->
            if (editBox.isFocused) graphics.requestCursor(CursorType.DEFAULT)
            editBox.extractWidgetRenderState(graphics, mouseX, mouseY, delta)
        }
        searchEditBox?.let { box ->
            if (box.isFocused) {
                graphics.requestCursor(CursorType.DEFAULT)
            }
            box.extractWidgetRenderState(graphics, mouseX, mouseY, delta)
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mouseX = event.x.toFloat()
        val mouseY = event.y.toFloat()
        val button = event.button()

        if (searchEditBox != null && !isPointInSearchBox(mouseX, mouseY)) {
            deactivateSearchBox()
        }
        if (activeEditBox != null && !isPointInEditBox(mouseX, mouseY)) {
            saveActiveEditBoxValue()
            activeEditBox?.isFocused = false
            activeEditBox = null
            activeEditBoxSetting = null
        }

        if (handleSidebarClick(mouseX, mouseY, button)) return true
        if (handleHeaderClick(mouseX, mouseY, button)) return true
        if (handleContentClick(mouseX, mouseY, button)) return true

        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val contentX = guiX + sidebarW
        val contentY = guiY + headerH
        val contentW = guiW - sidebarW
        val contentH = guiH - headerH

        if (HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), contentX, contentY, contentW, contentH)) {
            contentScrollY = (contentScrollY + scrollY * 14f).coerceAtMost(0.0).toFloat()
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val mouseX = event.x.toFloat()
        val mouseY = event.y.toFloat()
        val button = event.button()

        var y = guiY + headerH + contentPadding + contentScrollY
        val x = guiX + sidebarW + contentPadding
        val w = guiW - sidebarW - contentPadding * 2

        for (module in getFilteredModules()) {
            val state = cardStates[module.id] ?: continue
            if (state.handleRelease(mouseX, mouseY, button, x, y, w)) return true
            y += state.totalHeight + 4f
        }
        return super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        val mouseX = event.x.toFloat()
        val mouseY = event.y.toFloat()
        val button = 0

        var y = guiY + headerH + contentPadding + contentScrollY
        val x = guiX + sidebarW + contentPadding
        val w = guiW - sidebarW - contentPadding * 2

        for (module in getFilteredModules()) {
            val state = cardStates[module.id] ?: continue
            if (state.handleDrag(mouseX, mouseY, button, x, y, w)) return true
            y += state.totalHeight + 4f
        }

        return super.mouseDragged(event, dx, dy)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (searchEditBox?.isFocused == true && searchEditBox!!.keyPressed(event)) return true
        if (activeEditBox?.isFocused == true && activeEditBox!!.keyPressed(event)) return true
        if (event.isEscape) {
            if (searchEditBox != null) {
                deactivateSearchBox()
                return true
            }
            if (activeEditBox != null) {
                saveActiveEditBoxValue()
                activeEditBox?.isFocused = false
                activeEditBox = null
                activeEditBoxSetting = null
                return true
            }
            onClose()
            return true
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (searchEditBox?.isFocused == true && searchEditBox!!.charTyped(event)) return true
        if (activeEditBox?.isFocused == true && activeEditBox!!.charTyped(event)) return true
        return super.charTyped(event)
    }

    override fun extractMenuBackground(graphics: GuiGraphicsExtractor) {}

    override fun onClose() {
        deactivateSearchBox()

        if (activeEditBox != null) {
            saveActiveEditBoxValue()
            activeEditBox = null
            activeEditBoxSetting = null
        }

        ModuleConfig.saveConfig()
        mc.setScreen(parent)
    }

    private fun renderSidebar(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val x = guiX
        val y = guiY
        val w = sidebarW
        val h = guiH

        graphics.fill(x.toInt(), y.toInt(), (x + w).toInt(), (y + h).toInt(), 0xFF141414.toInt())

        val logoX = x + (w - 20) / 2f
        val logoY = y + 12f
        graphics.blit(RenderPipelines.GUI_TEXTURED, sidebarLogo, logoX.toInt(), logoY.toInt(), 0f, 0f, 20, 20, 20, 20)

        val iconSize = 20f
        val iconPadding = 14f
        val iconX = x + (w - iconSize) / 2f
        var iconY = y + 50f

        for (category in Category.entries) {
            val isSelected = selectedCategory == category

            if (isSelected) {
                graphics.fill(
                    (x + 4).toInt(), (iconY - 4).toInt(),
                    (x + w).toInt(), (iconY + iconSize + 4).toInt(),
                    0xFF1C1C1C.toInt()
                )
            }

            val iconTex = Identifier.fromNamespaceAndPath("hkim", "textures/clickgui/sidebar/${category.name.lowercase()}.png")
            graphics.blit(RenderPipelines.GUI_TEXTURED, iconTex, iconX.toInt(), iconY.toInt(), 0f, 0f, 20, 20, 20, 20)

            if (HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), iconX, iconY, iconSize, iconSize)) {
                graphics.setTooltipForNextFrame(mc.font, Component.literal(category.name.lowercase().replaceFirstChar { it.uppercase() }), mouseX, mouseY)
            }

            iconY += iconSize + iconPadding
        }

        val bottomY = y + h - 70f
        val isVerHovered = HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), x + 1f, bottomY + 58f, sidebarW - 6f, 10f)
        val versionText = if (isVerHovered) Component.literal("v${Hkim.VERSION}").withStyle(ChatFormatting.UNDERLINE) else Component.literal("v${Hkim.VERSION}")
        graphics.text(mc.font, versionText, (guiX + 4).toInt(), (guiY + guiH - mc.font.lineHeight - 2).toInt(), 0xFF888888.toInt(), false)

        graphics.blit(RenderPipelines.GUI_TEXTURED, editIcon, iconX.toInt(), (bottomY + 30).toInt(), 0f, 0f, 20, 20, 20, 20)
        if (HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), x + 10f, bottomY + 30f, 20f, 20f)) {
            graphics.setTooltipForNextFrame(mc.font, Component.literal("Edit HUD"), mouseX, mouseY)
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, fileIcon, iconX.toInt(), bottomY.toInt(), 0f, 0f, 20, 20, 20, 20)
        if (HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), x + 10f, bottomY, 20f, 20f)) {
            graphics.setTooltipForNextFrame(mc.font, Component.literal("Open Config Folder"), mouseX, mouseY)
        }
    }

    private fun renderHeader(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val x = guiX + sidebarW
        val y = guiY
        val w = guiW - sidebarW
        val h = headerH

        graphics.fill(x.toInt(), y.toInt(), (x + w).toInt(), (y + h).toInt(), 0xFF1E1E1E.toInt())

        graphics.drawHorizontalSeparator(x, y + h - 1, w, 0xFF333333.toInt())

        val titleText = when {
            searchQuery.isNotEmpty() -> "Search: \"$searchQuery\""
            selectedCategory != null -> "${selectedCategory!!.name.lowercase().replaceFirstChar { it.uppercase() }} Modules"
            else -> "All Modules"
        }
        graphics.text(mc.font, titleText, x.toInt() + 16, y.toInt() + 18, 0xFFFFFFFF.toInt(), false)

        val sbX = x + w - 180f
        val sbY = y + 12f
        val sbW = 140f
        val sbH = 20f
        val isSearchActive = searchEditBox != null

        graphics.drawRectWithBorder(sbX, sbY, sbW, sbH, 0xFF2A2A2A.toInt(), 0xFF555555.toInt())

        val displayText = searchQuery.ifEmpty { "Search modules..." }
        val textColor = if (searchQuery.isEmpty()) 0xFF666666.toInt() else 0xFFFFFFFF.toInt()
        if (!isSearchActive) {
            graphics.text(mc.font, displayText, sbX.toInt() + 6, sbY.toInt() + 6, textColor, false)
        }

        val closeX = x + w - 32f
        graphics.drawRectWithBorder(closeX, sbY, 20f, 20f, 0xFF3A2A2A.toInt(), 0xFFAA4444.toInt())
        graphics.text(mc.font, "×", closeX.toInt() + 8, sbY.toInt() + 6, 0xFFFFFFFF.toInt(), false)

        if (HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), closeX, sbY, 20f, 20f)) {
            graphics.setTooltipForNextFrame(mc.font, Component.literal("Close"), mouseX, mouseY)
        }
    }

    private fun renderContentArea(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val baseX = guiX + sidebarW + contentPadding
        var baseY = guiY + headerH + contentPadding
        val availW = guiW - sidebarW - contentPadding * 2
        val availH = guiH - headerH - contentPadding * 2

        graphics.fill(
            (baseX - contentPadding).toInt(), (baseY - contentPadding).toInt(),
            (baseX - contentPadding + availW + contentPadding * 2).toInt(),
            (baseY - contentPadding + availH + contentPadding * 2).toInt(),
            0xFF1C1C1C.toInt()
        )

        val modules = getFilteredModules()

        val totalH = modules.sumOf { (cardStates[it.id]?.totalHeight ?: 44f).toInt() } + modules.size * 4f
        val maxScroll = max(0f, totalH - availH)
        contentScrollY = contentScrollY.coerceIn(-maxScroll, 0f)

        graphics.enableScissor((baseX - contentPadding).toInt(), (baseY - contentPadding).toInt(),
            (baseX + availW + contentPadding).toInt(), (baseY + availH + contentPadding).toInt())

        val scrollOffset = contentScrollY

        for (module in modules) {
            val state = cardStates[module.id] ?: continue
            val currentModuleY = baseY + scrollOffset

            state.render(graphics, baseX, currentModuleY, availW, mouseX.toFloat(), mouseY.toFloat(), themeColor)
            baseY += state.totalHeight + 4f
        }

        graphics.disableScissor()
    }

    private fun getFilteredModules(): List<Module> {
        val all = ModuleManager.getAll()

        val searched = if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase()
            all.filter { it.name.lowercase().contains(query) || it.description.lowercase().contains(query) }
        } else all

        return if (searchQuery.isEmpty() && selectedCategory != null) {
            searched.filter { it.category == selectedCategory }
        } else searched
    }

    private fun handleSidebarClick(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false

        val x = guiX
        val y = guiY
        val w = sidebarW

        val iconSize = 20f
        val iconPadding = 14f
        val iconX = x + (w - iconSize) / 2f
        var iconY = y + 50f

        for (category in Category.entries) {
            if (HudUtils.isPointInRect(mouseX, mouseY, iconX, iconY, iconSize, iconSize)) {
                playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
                selectedCategory = if (selectedCategory == category) null else category
                searchQuery = ""
                return true
            }
            iconY += iconSize + iconPadding
        }

        val bottomY = y + guiH - 70f
        if (HudUtils.isPointInRect(mouseX, mouseY, iconX, bottomY, 20f, 20f)) {
            Hkim.logger.info("[HKM] FILE clicked")
            return true
        }
        if (HudUtils.isPointInRect(mouseX, mouseY, iconX, bottomY + 30f, 20f, 20f)) {
            Hkim.logger.info("[HKM] EDIT clicked")
            return true
        }
        if (HudUtils.isPointInRect(mouseX, mouseY, x + 1f, bottomY + 58f, sidebarW - 6f, 10f)) {
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value())
            HudUtils.openUrl("https://github.com/SMxNcn/Hkim")
            return true
        }

        return false
    }

    private fun handleHeaderClick(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != 0) return false

        val x = guiX + sidebarW
        val y = guiY
        val w = guiW - sidebarW

        val closeX = x + w - 36f
        val closeY = y + 14f
        if (HudUtils.isPointInRect(mouseX, mouseY, closeX, closeY, 20f, 20f)) {
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            onClose()
            return true
        }

        val sbX = x + w - 180f
        val sbY = y + 12f
        val sbW = 140f
        val sbH = 20f

        if (HudUtils.isPointInRect(mouseX, mouseY, sbX, sbY, sbW, sbH)) {
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            activateSearchBox(sbX.toInt() + 6, sbY.toInt() + 6, sbW.toInt(), sbH.toInt())
            return true
        }

        return false
    }

    private fun handleContentClick(mouseX: Float, mouseY: Float, button: Int): Boolean {
        var y = guiY + headerH + contentPadding + contentScrollY
        val x = guiX + sidebarW + contentPadding
        val w = guiW - sidebarW - contentPadding * 2

        for (module in getFilteredModules()) {
            val state = cardStates[module.id] ?: continue
            if (state.handleClick(mouseX, mouseY, button, x, y, w)) return true
            y += state.totalHeight + 4f
        }
        return false
    }

    fun activateEditBox(setting: Setting<*>, x: Int, y: Int, width: Int, height: Int, initialValue: String) {
        if (activeEditBoxSetting == setting) return

        saveActiveEditBoxValue()

        activeEditBoxSetting = setting
        activeEditBox = EditBox(mc.font, x, y, width, height, Component.literal(setting.name)).apply {
            value = initialValue
            isBordered = false
            setTextColor(0xFFFFFFFF.toInt())
            setResponder { newValue ->
                when (setting) {
                    is TextSetting -> setting.set(newValue)
                    is ColorSetting -> parseColorHex(newValue)?.let { setting.set(it) }
                }
            }
            isFocused = true
            moveCursorToEnd(false)
        }
    }

    private fun activateSearchBox(x: Int, y: Int, width: Int, height: Int) {
        searchEditBox?.let {
            it.isFocused = true
            return
        }

        searchEditBox = EditBox(mc.font, x, y, width, height, Component.literal("Search")).apply {
            value = searchQuery
            isBordered = false
            setTextColor(0xFFFFFFFF.toInt())
            setResponder { newValue -> searchQuery = newValue }
            isFocused = true
            moveCursorToEnd(false)
        }
    }

    private fun deactivateSearchBox() {
        searchEditBox?.let {
            searchQuery = it.value
            it.isFocused = false
            searchEditBox = null
        }
    }

    private fun isPointInEditBox(mouseX: Float, mouseY: Float): Boolean {
        return activeEditBox?.let {
            mouseX in it.x.toFloat()..(it.x + it.width).toFloat() && mouseY in it.y.toFloat()..(it.y + it.height).toFloat()
        } ?: false
    }

    private fun isPointInSearchBox(mouseX: Float, mouseY: Float): Boolean {
        val x = guiX + sidebarW
        val y = guiY
        val w = guiW - sidebarW
        val sbX = x + w - 180f
        val sbY = y + 12f
        return HudUtils.isPointInRect(mouseX, mouseY, sbX, sbY, 180f, 24f)
    }

    private fun saveActiveEditBoxValue() {
        val setting = activeEditBoxSetting ?: return
        val value = activeEditBox?.value ?: return
        when (setting) {
            is TextSetting -> {
                setting.set(value)
                ModuleConfig.saveConfig ()
            }
            is ColorSetting -> {
                parseColorHex(value)?.let { setting.set(it) }
                ModuleConfig.saveConfig()
            }
        }
    }

    private fun parseColorHex(hex: String): Int? {
        return try {
            val clean = hex.replace("#", "").uppercase()
            if (clean.length == 6) (clean.toLong(16).toInt() or 0xFF000000.toInt()) else null
        } catch (_: Exception) { null }
    }
}

private class ModuleCardState(val module: Module) {
    private var targetExpanded = false

    private var targetEnabled = module.enabled
    private var lerpEnabled = if (targetEnabled) 1f else 0f

    private val settingHeight = 20f
    private val settingGap = 3f

    private var draggingSetting: Setting<*>? = null
    private var draggingSettingY: Float = 0f

    private val visibleSettings: List<Setting<*>>
        get() = module.settings.filter { it.isVisible() }

    val totalHeight: Float
        get() = when {
            !targetExpanded -> 44f
            else -> {
                val settingsH = calculateCurrentVisibleHeight()
                if (settingsH > 0f) 44f + settingsH + 8f else 44f
            }
        }

    fun update(deltaTime: Float) {
        val factor = min(1f, deltaTime * 10f)

        if (targetEnabled != module.enabled) targetEnabled = module.enabled
        lerpEnabled = HudUtils.lerp(lerpEnabled, if (targetEnabled) 1f else 0f, factor)
    }

    fun render(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float, themeColor: Int): Float {
        val cardH = 44f
        val isHovered = HudUtils.isPointInRect(mouseX, mouseY, x, y, width, 44f)

        val borderColor = HudUtils.lerpColor(0xFF333333.toInt(), themeColor, lerpEnabled)
        val nameColor = HudUtils.lerpColor(0xFFFFFFFF.toInt(), themeColor, lerpEnabled)
        val bgColor = if (isHovered) 0x10FFFFFF else 0xFF222222.toInt()

        graphics.drawRectWithBorder(x, y, width, cardH, bgColor, borderColor, 1)

        graphics.text(mc.font, Component.literal(module.name).withStyle(ChatFormatting.BOLD), x.toInt() + 14, y.toInt() + 12, nameColor, false)
        graphics.text(mc.font, module.description, x.toInt() + 14, y.toInt() + 28, 0xFF888888.toInt(), false)

        if (targetExpanded) {
            var sy = y + cardH + 6f
            for (setting in visibleSettings) {
                if (!setting.isVisible()) continue
                val indent = 24f
                val renderedHeight = setting.render(graphics, x + indent, sy, width - indent * 2, mouseX, mouseY, themeColor)
                sy += renderedHeight + settingGap
            }
        }

        return totalHeight
    }

    fun handleClick(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (HudUtils.isPointInRect(mouseX, mouseY, x, y, width, 44f)) {
            when (button) {
                0 -> { module.toggle(); return true }
                1 -> { targetExpanded = !targetExpanded; return true }
            }
        }

        if (targetExpanded) {
            var sy = y + 44f + 6f
            for (setting in visibleSettings) {
                if (!setting.isVisible()) continue
                val indent = 24f
                if (setting.mouseClicked(mouseX, mouseY, button, x + indent, sy, width - indent * 2)) {
                    draggingSetting = setting
                    draggingSettingY = sy
                    return true
                }
                sy += settingHeight + settingGap
            }
        }

        return false
    }

    fun handleDrag(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (draggingSetting != null) {
            val indent = 24f
            return draggingSetting!!.mouseDragged(
                mouseX, mouseY, button, 0f, 0f,
                x + indent, draggingSettingY, width - indent * 2
            )
        }
        return false
    }

    fun handleRelease(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (draggingSetting != null) {
            val indent = 24f
            draggingSetting!!.mouseReleased(
                mouseX, mouseY, button,
                x + indent, draggingSettingY, width - indent * 2
            )
            draggingSetting = null
            return true
        }
        return false
    }

    private fun calculateCurrentVisibleHeight(): Float {
        return visibleSettings.sumOf { (settingHeight + settingGap).toDouble() }.toFloat()
    }
}