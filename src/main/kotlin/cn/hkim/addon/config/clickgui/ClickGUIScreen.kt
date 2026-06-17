package cn.hkim.addon.config.clickgui

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.ModuleConfig
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.config.settings.KeybindSetting
import cn.hkim.addon.config.settings.TextSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleManager
import cn.hkim.addon.features.impl.ClickGUI
import cn.hkim.addon.gui.HudEditScreen
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.HudUtils.drawHorizontalSeparator
import cn.hkim.addon.utils.HudUtils.drawVerticalSeparator
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.nvg.NVGPIPRenderer
import cn.hkim.addon.utils.render.nvg.NVGRenderer
import com.mojang.blaze3d.platform.cursor.CursorType
import com.mojang.blaze3d.platform.cursor.CursorTypes
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
import java.awt.Color
import kotlin.math.max

class ClickGUIScreen(private val parent: Screen?) : Screen(Component.literal("Click GUI")) {
    companion object {
        private var lastSelectedCategory: Category? = null
    }

    private var guiX = 0f
    private var guiY = 0f
    private val guiW = 480f
    private val guiH = 300f

    private val sidebarW = 36f
    private val headerH = 45f
    private val contentPadding = 12f

    private val sidebarLogo = Identifier.fromNamespaceAndPath("hkim", "textures/clickgui/sidebar/icon20x.png")
    private val editIcon = Identifier.fromNamespaceAndPath("hkim", "textures/clickgui/sidebar/edit.png")

    private var selectedCategory: Category? = null
    private var searchQuery = ""
    private var contentScrollY = 0f

    private val themeColor = ClickGUI.getGuiColor()
    private val cardStates = mutableMapOf<String, ModuleCardState>()

    private val highlightAlphaAnim = GuiAnimation.create(0f, 0f)
        .duration(150L)
        .easing(Easing.CUBIC_OUT)

    private val highlightYAnim = GuiAnimation.create(0f, 0f)
        .duration(200L)
        .easing(Easing.CUBIC_OUT)

    var activeEditBox: EditBox? = null
    private var searchEditBox: EditBox? = null
    var activeEditBoxSetting: Setting<*>? = null

    override fun init() {
        if (selectedCategory != null) {
            lastSelectedCategory = selectedCategory
        }

        guiX = (mc.window.guiScaledWidth - guiW) / 2f
        guiY = (mc.window.guiScaledHeight - guiH) / 2f

        cardStates.clear()
        for (module in ModuleManager.getAll()) {
            cardStates[module.id] = ModuleCardState(module)
        }

        restoreCategory()

        activeEditBox = null
        searchEditBox = null
        activeEditBoxSetting = null
        super.init()
    }

    override fun added() {
        super.added()
        restoreCategory()
    }

    override fun removed() {
        lastSelectedCategory = selectedCategory
        super.removed()
    }

    private fun restoreCategory() {
        selectedCategory = lastSelectedCategory
        if (selectedCategory != null) {
            highlightYAnim.reset()
            highlightYAnim.from(getIconHighlightY(selectedCategory!!))
            highlightAlphaAnim.animateTo(1f)
        } else {
            highlightAlphaAnim.animateTo(0f)
        }
    }

    private fun getIconHighlightY(category: Category): Float {
        val index = Category.entries.indexOf(category)
        return guiY + 50f + index * 34f - 4f
    }

    override fun tick() {
        cardStates.values.forEach { it.update(0.07f) }
        super.tick()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0x80000000.toInt())

        NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
            NVGRenderer.rect(guiX * 2, guiY * 2, guiW * 2, guiH * 2, Color(0x801A1A1A.toInt(), true), 16f)
            NVGRenderer.hollowRect(guiX * 2, guiY * 2, guiW * 2, guiH * 2, 3f, Color(0x444444), 16f)
        }

        renderSidebar(graphics, mouseX, mouseY, delta)
        renderHeader(graphics, mouseX, mouseY, delta)
        renderContentArea(graphics, mouseX, mouseY, delta)

        searchEditBox?.let { editBox ->
            if (editBox.isFocused) graphics.requestCursor(CursorType.DEFAULT)
            editBox.extractWidgetRenderState(graphics, mouseX, mouseY, delta)
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mouseX = event.x.toFloat()
        val mouseY = event.y.toFloat()
        val button = event.button()

        for (state in cardStates.values) {
            for (setting in state.module.settings) {
                if (setting is KeybindSetting && setting.isBinding) {
                    if (setting.handleMouseButton(button)) return true
                }
            }
        }

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

        for (state in cardStates.values) {
            for (setting in state.module.settings) {
                if (setting is KeybindSetting && setting.isBinding) {
                    if (setting.handleKey(event.key)) return true
                }
            }
        }

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
            mc.setScreen(parent)
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

    override fun extractTransparentBackground(graphics: GuiGraphicsExtractor) {}

    override fun onClose() {
        lastSelectedCategory = selectedCategory

        deactivateSearchBox()

        if (activeEditBox != null) {
            saveActiveEditBoxValue()
            activeEditBox = null
            activeEditBoxSetting = null
        }

        ModuleConfig.saveConfig()
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    private fun renderSidebar(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val x = guiX
        val y = guiY
        val w = sidebarW
        val h = guiH

        graphics.drawVerticalSeparator(x + w, y + 12f, h - 24f, 0xFF444444.toInt())

        val logoX = x + (w - 20) / 2f
        val logoY = y + 12f
        graphics.blit(RenderPipelines.GUI_TEXTURED, sidebarLogo, logoX.toInt(), logoY.toInt(), 0f, 0f, 20, 20, 20, 20)

        val hlAlpha = highlightAlphaAnim.getValue()
        if (hlAlpha > 0.001f) {
            val hlX = x + 4f
            val hlW = w - 8f
            val hlH = 28f
            val hlY = highlightYAnim.getValue()
            val alphaInt = (0x33 * hlAlpha).toInt().coerceIn(0, 0xFF)
            val hlColor = (alphaInt shl 24) or (this.themeColor and 0x00FFFFFF)

            NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
                NVGRenderer.rect(hlX * 2, hlY * 2, hlW * 2, hlH * 2, Color(hlColor, true), 8f)
            }
        }

        val iconSize = 20f
        val iconPadding = 14f
        val iconX = x + (w - iconSize) / 2f
        var iconY = y + 50f

        for (category in Category.entries) {
            val iconTex = Identifier.fromNamespaceAndPath("hkim", "textures/clickgui/sidebar/${category.name.lowercase()}.png")
            graphics.blit(RenderPipelines.GUI_TEXTURED, iconTex, iconX.toInt(), iconY.toInt(), 0f, 0f, 20, 20, 20, 20)

            if (HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), iconX, iconY, iconSize, iconSize)) {
                graphics.requestCursor(CursorTypes.POINTING_HAND)
                graphics.setTooltipForNextFrame(mc.font, Component.literal(category.name.lowercase().replaceFirstChar { it.uppercase() }), mouseX, mouseY)
            }

            iconY += iconSize + iconPadding
        }

        val bottomY = y + h - 70f
        val isVerHovered = HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), x + 1f, bottomY + 58f, sidebarW - 6f, 10f)
        val versionText = if (isVerHovered) Component.literal("v${Hkim.VERSION}").withStyle(ChatFormatting.UNDERLINE) else Component.literal("v${Hkim.VERSION}")
        if (isVerHovered) graphics.requestCursor(CursorTypes.POINTING_HAND)
        graphics.text(mc.font, versionText, (guiX + 4).toInt(), (guiY + guiH - mc.font.lineHeight - 2).toInt(), 0xFF888888.toInt(), false)

        graphics.blit(RenderPipelines.GUI_TEXTURED, editIcon, iconX.toInt(), (bottomY + 30).toInt(), 0f, 0f, 20, 20, 20, 20)
        if (HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), x + 10f, bottomY + 30f, 20f, 20f)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
            graphics.setTooltipForNextFrame(mc.font, Component.literal("Edit HUD"), mouseX, mouseY)
        }
    }

    private fun renderHeader(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val x = guiX + sidebarW
        val y = guiY
        val w = guiW - sidebarW
        val h = headerH

        graphics.drawHorizontalSeparator(x + 6, y + h - 1, w - 12, 0xFF444444.toInt())

        val titleText = when {
            searchQuery.isNotEmpty() -> "Search: \"$searchQuery\""
            selectedCategory != null -> "${selectedCategory!!.name.lowercase().replaceFirstChar { it.uppercase() }} Modules"
            else -> "All Modules"
        }
        graphics.text(mc.font, titleText, x.toInt() + 16, y.toInt() + 18, 0xFFFFFFFF.toInt(), false)

        val sbX = x + w - 180f
        val sbY = y + 13f
        val sbW = 140f
        val sbH = 20f
        val isSearchActive = searchEditBox != null

        val displayText = searchQuery.ifEmpty { "Search modules..." }
        val textColor = if (searchQuery.isEmpty()) 0xFF666666.toInt() else 0xFFFFFFFF.toInt()
        if (!isSearchActive) {
            graphics.text(mc.font, displayText, sbX.toInt() + 6, sbY.toInt() + 6, textColor, false)
        }

        val closeX = x + w - 32f

        NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
            NVGRenderer.hollowRect(sbX * 2, sbY * 2, sbW * 2, sbH * 2, 2f, Color(0x555555), 6f)

            NVGRenderer.rect(closeX * 2, sbY * 2, 40f, sbH * 2, Color(0x3A2A2A), 6f)
            NVGRenderer.hollowRect(closeX * 2, sbY * 2, 40f, sbH * 2, 2f, Color(0xAA4444), 6f)
        }

        graphics.text(mc.font, "×", closeX.toInt() + 8, sbY.toInt() + 7, 0xFFFFFFFF.toInt(), false)

        if (HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), closeX, sbY, 20f, 20f)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
            graphics.setTooltipForNextFrame(mc.font, Component.literal("Close"), mouseX, mouseY)
        }

        if (!isSearchActive && HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), sbX, sbY, sbW, sbH)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }
    }

    private fun renderContentArea(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val baseX = guiX + sidebarW + contentPadding
        var baseY = guiY + headerH + contentPadding
        val availW = guiW - sidebarW - contentPadding * 2
        val availH = guiH - headerH - contentPadding * 2

        val modules = getFilteredModules()

        val totalH = modules.sumOf { (cardStates[it.id]?.totalHeight ?: 44f).toInt() } + modules.size * 4f
        val maxScroll = max(0f, totalH - availH)
        contentScrollY = contentScrollY.coerceIn(-maxScroll, 0f)

        graphics.enableScissor((baseX - contentPadding).toInt(), (baseY - contentPadding).toInt(),
            (baseX + availW + contentPadding).toInt(), (baseY + availH + contentPadding - 1).toInt())

        val contentTop = baseY - contentPadding
        val contentBottom = baseY + availH + contentPadding

        val scrollOffset = contentScrollY

        for (module in modules) {
            val state = cardStates[module.id] ?: continue
            val currentModuleY = baseY + scrollOffset

            state.render(graphics, baseX, currentModuleY, availW, mouseX.toFloat(), mouseY.toFloat(), contentTop, contentBottom, themeColor, delta)
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

                val oldCategory = selectedCategory
                val newCategory = if (oldCategory == category) null else category
                selectedCategory = newCategory

                if (newCategory != null) {
                    val targetY = getIconHighlightY(newCategory)
                    if (oldCategory == null) {
                        highlightYAnim.reset()
                        highlightYAnim.from(targetY)
                    } else {
                        highlightYAnim.animateTo(targetY)
                    }
                    highlightAlphaAnim.animateTo(1f)
                } else {
                    highlightAlphaAnim.animateTo(0f)
                }

                searchQuery = ""
                return true
            }
            iconY += iconSize + iconPadding
        }

        val bottomY = y + guiH - 70f
        if (HudUtils.isPointInRect(mouseX, mouseY, iconX, bottomY + 30f, 20f, 20f)) {
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            mc.setScreen(HudEditScreen(this))
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
            mc.setScreen(parent)
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
        val visibleTop = guiY + headerH
        val visibleBottom = guiY + guiH

        for (module in getFilteredModules()) {
            val state = cardStates[module.id] ?: continue
            if (state.handleClick(mouseX, mouseY, button, x, y, w, visibleTop, visibleBottom)) return true
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
            ColorSetting.fromHexString(hex)
        } catch (_: Exception) { null }
    }
}