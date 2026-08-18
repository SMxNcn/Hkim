package cn.hkim.addon.config.clickgui

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.ModuleConfig
import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.config.settings.KeybindSetting
import cn.hkim.addon.config.settings.SelectorSetting
import cn.hkim.addon.config.settings.TextSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleManager
import cn.hkim.addon.features.impl.ClickGUI
import cn.hkim.addon.gui.HudEditScreen
import cn.hkim.addon.gui.SkikoEditBox
import cn.hkim.addon.gui.SkikoTooltip.drawTooltip
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawGradientRectMulti
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithShadow
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoEdgeRoundedRect
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoImage
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import cn.hkim.addon.utils.render.skiko.SkikoGradient
import cn.hkim.addon.utils.render.skiko.SkikoRoundEdge
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.PreeditEvent
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import org.lwjgl.glfw.GLFW
import kotlin.math.max

class ClickGUIScreen(private val parent: Screen?) : Screen(Component.literal("Click GUI")) {
    private var guiX = 0f
    private var guiY = 0f
    private val guiW = 520f
    private val guiH = 300f

    private val sidebarW = 36f
    private val headerH = 45f
    private val headerW get() = guiW - sidebarW
    private val contentPadding = 12f

    private var selectedCategory: Category? = null
    private var searchQuery = ""
    private var contentScrollY = 0f

    private val searchBoxH = 20f
    private val searchBoxW = 140f
    private val searchX get() = guiX + sidebarW + headerW - 180f
    private val searchY get() = guiY + 12f

    private val cardsTop get() = guiY + headerH + contentPadding

    private val themeColor get() = ClickGUI.getGuiColor()
    private val cardStates = mutableMapOf<String, ModuleCardState>()

    private val highlightAlphaAnim = GuiAnimation.create(0f, 0f)
        .duration(150L)
        .easing(Easing.CUBIC_OUT)

    private val highlightYAnim = GuiAnimation.create(0f, 0f)
        .duration(200L)
        .easing(Easing.CUBIC_OUT)

    var activeSkikoEditBox: SkikoEditBox? = null
    var activeEditBoxSetting: Setting<*>? = null
    private var searchSkikoBox: SkikoEditBox? = null

    companion object {
        private var lastSelectedCategory: Category? = null
        private const val CARD_GAP = 8f
    }

    private class CardLayout(val module: Module, val x: Float, val w: Float) {
        var y = 0f
        var h = 0f
    }

    private fun buildCardLayouts(modules: List<Module>): List<CardLayout> {
        val baseX = guiX + sidebarW + contentPadding
        val availW = guiW - sidebarW - contentPadding * 2
        val cardW = (availW - CARD_GAP) / 2f
        val rightX = baseX + cardW + CARD_GAP
        val layouts = modules.mapIndexed { i, module ->
            CardLayout(module, if (i % 2 == 0) baseX else rightX, cardW)
        }
        refreshLayoutY(layouts)
        return layouts
    }

    private fun refreshLayoutY(layouts: List<CardLayout>) {
        var leftY = 0f
        var rightY = 0f
        for ((i, layout) in layouts.withIndex()) {
            val h = cardStates[layout.module.id]?.totalHeight ?: 44f
            if (i % 2 == 0) {
                layout.y = leftY
                leftY += h + 4f
            } else {
                layout.y = rightY
                rightY += h + 4f
            }
            layout.h = h
        }
    }

    private var cachedLayouts: List<CardLayout> = emptyList()
    private var layoutKey: Pair<String, Category?>? = null

    private fun getCardLayouts(): List<CardLayout> {
        val key = searchQuery to selectedCategory
        if (layoutKey != key) {
            layoutKey = key
            cachedLayouts = buildCardLayouts(getFilteredModules())
        } else {
            refreshLayoutY(cachedLayouts)
        }
        return cachedLayouts
    }

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
        layoutKey = null

        restoreCategory()

        activeSkikoEditBox = null
        searchSkikoBox = null
        activeEditBoxSetting = null
        super.init()
    }

    override fun added() {
        super.added()
        restoreCategory()
    }

    override fun removed() {
        lastSelectedCategory = selectedCategory

        (Setting.activeModalPopup as? ColorSetting)?.closePopup()
        deactivateSearchBox()
        activeSkikoEditBox?.defocus()
        activeSkikoEditBox = null
        activeEditBoxSetting = null

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

        graphics.drawRoundedRectWithShadow(guiX, guiY, guiW, guiH, Theme.bg, 0, 0f, 10f, Theme.bgShadow, 6f, 1f)

        renderSidebar(graphics, mouseX, mouseY, delta)
        renderHeader(graphics, mouseX, mouseY, delta)
        renderContentArea(graphics, mouseX, mouseY, delta)

        val colorPopup = Setting.activeModalPopup as? ColorSetting
        colorPopup?.renderPopup(graphics, width.toFloat(), height.toFloat(), mouseX.toFloat(), mouseY.toFloat(), themeColor, Theme.CARD_FONT_SIZE)
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

        val colorPopup = Setting.activeModalPopup as? ColorSetting
        if (colorPopup != null) {
            if (colorPopup.isPointInPopup(mouseX, mouseY)) {
                if (!colorPopup.isPointInHex(mouseX, mouseY) && !colorPopup.isPointInAlpha(mouseX, mouseY)) {
                    saveActiveEditBoxValue()
                    activeSkikoEditBox?.defocus()
                    activeSkikoEditBox = null
                    activeEditBoxSetting = null
                }
                colorPopup.handlePopupClick(mouseX, mouseY, button, doubleClick)
            } else {
                saveActiveEditBoxValue()
                activeSkikoEditBox?.defocus()
                activeSkikoEditBox = null
                activeEditBoxSetting = null
                colorPopup.closePopup()
                ModuleConfig.saveConfig()
            }
            return true
        }

        if (searchSkikoBox != null && !searchSkikoBox!!.isPointIn(mouseX, mouseY)) {
            deactivateSearchBox()
        }
        if (activeSkikoEditBox != null && !activeSkikoEditBox!!.isPointIn(mouseX, mouseY)) {
            saveActiveEditBoxValue()
            activeSkikoEditBox?.defocus()
            activeSkikoEditBox = null
            activeEditBoxSetting = null
        }

        if (handleSidebarClick(mouseX, mouseY, button)) {
            SelectorSetting.scrollFocused = null
            return true
        }
        if (handleHeaderClick(mouseX, mouseY, button, doubleClick)) {
            SelectorSetting.scrollFocused = null
            return true
        }
        if (handleContentClick(mouseX, mouseY, button, doubleClick)) return true

        SelectorSetting.scrollFocused = null
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val mX = mouseX.toFloat()
        val mY = mouseY.toFloat()

        if (activeSkikoEditBox?.isPointIn(mX, mY) == true) return true
        if (searchSkikoBox?.isPointIn(mX, mY) == true) return true

        if (Setting.activeModalPopup is ColorSetting) return true

        val contentX = guiX + sidebarW
        val contentY = guiY + headerH
        val contentW = guiW - sidebarW
        val contentH = guiH - headerH

        if (HudUtils.isPointInRect(mX, mY, contentX, contentY, contentW, contentH)) {
            if (handleSelectorScroll(mX, mY, scrollX, scrollY)) return true

            val shiftHeld = GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                            GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS
            val multiplier = if (shiftHeld) 3.0 else 1.0
            contentScrollY = (contentScrollY + scrollY * 14f * multiplier).coerceAtMost(0.0).toFloat()
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val mouseX = event.x.toFloat()
        val mouseY = event.y.toFloat()
        val button = event.button()

        if (searchSkikoBox?.handleMouseReleased() == true) return true
        if (activeSkikoEditBox?.handleMouseReleased() == true) return true

        val colorPopup = Setting.activeModalPopup as? ColorSetting
        if (colorPopup != null) {
            colorPopup.handlePopupRelease()
            return true
        }

        for (layout in getCardLayouts()) {
            val cy = cardsTop + contentScrollY + layout.y
            if (cardStates[layout.module.id]?.handleRelease(mouseX, mouseY, button, layout.x, cy, layout.w) == true) return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        val mouseX = event.x.toFloat()
        val mouseY = event.y.toFloat()
        val button = 0

        if (searchSkikoBox?.handleMouseDragged(mouseX, mouseY) == true) return true
        if (activeSkikoEditBox?.handleMouseDragged(mouseX, mouseY) == true) return true

        val colorPopup = Setting.activeModalPopup as? ColorSetting
        if (colorPopup != null) {
            if (colorPopup.handlePopupDrag(mouseX, mouseY)) return true
            return true
        }

        for (layout in getCardLayouts()) {
            val cy = cardsTop + contentScrollY + layout.y
            if (cardStates[layout.module.id]?.handleDrag(mouseX, mouseY, button, layout.x, cy, layout.w) == true) return true
        }

        return super.mouseDragged(event, dx, dy)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (searchSkikoBox?.handleKeyPressed(event) == true) return true
        if (activeSkikoEditBox?.handleKeyPressed(event) == true) return true

        val popupKeyTarget = Setting.activeModalPopup as? ColorSetting
        if (popupKeyTarget != null && popupKeyTarget.popup.handleKeyPressed(event)) return true

        for (state in cardStates.values) {
            for (setting in state.module.settings) {
                if (setting is KeybindSetting && setting.isBinding) {
                    if (setting.handleKey(event.key)) return true
                }
            }
        }

        if (event.isEscape) {
            val colorPopup = Setting.activeModalPopup as? ColorSetting
            if (colorPopup != null) {
                saveActiveEditBoxValue()
                activeSkikoEditBox?.defocus()
                activeSkikoEditBox = null
                activeEditBoxSetting = null
                colorPopup.closePopup()
                ModuleConfig.saveConfig()
                return true
            }
            if (searchSkikoBox != null) {
                deactivateSearchBox()
                return true
            }
            if (activeSkikoEditBox != null) {
                saveActiveEditBoxValue()
                activeSkikoEditBox?.defocus()
                activeSkikoEditBox = null
                activeEditBoxSetting = null
                return true
            }
            onClose()
            return true
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (searchSkikoBox?.handleCharTyped(event) == true) return true
        if (activeSkikoEditBox?.handleCharTyped(event) == true) return true

        val popupCharTarget = Setting.activeModalPopup as? ColorSetting
        if (popupCharTarget != null && popupCharTarget.popup.handleCharTyped(event)) return true
        return super.charTyped(event)
    }

    override fun preeditUpdated(event: PreeditEvent?): Boolean {
        val popupPreeditTarget = Setting.activeModalPopup as? ColorSetting
        if (event == null) {
            searchSkikoBox?.clearPreedit()
            activeSkikoEditBox?.clearPreedit()
            popupPreeditTarget?.popup?.handlePreedit(null)
            return true
        }
        if (searchSkikoBox?.handlePreedit(event) == true) return true
        if (activeSkikoEditBox?.handlePreedit(event) == true) return true
        if (popupPreeditTarget != null && popupPreeditTarget.popup.handlePreedit(event)) return true
        return super.preeditUpdated(event)
    }

    override fun extractMenuBackground(graphics: GuiGraphicsExtractor) {}

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun onClose() {
        lastSelectedCategory = selectedCategory

        deactivateSearchBox()

        if (activeSkikoEditBox != null) {
            saveActiveEditBoxValue()
            activeSkikoEditBox = null
            activeEditBoxSetting = null
        }

        ModuleConfig.saveConfig()
        mc.gui.setScreen(parent)
    }

    private fun renderSidebar(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val x = guiX
        val y = guiY
        val w = sidebarW
        val h = guiH

        graphics.drawSkikoEdgeRoundedRect(x, y, w, h, Theme.sidebar, 10f, SkikoRoundEdge.LEFT)

        val logoSize = 16f
        val logoX = x + (w - logoSize) / 2f
        val logoY = y + 12f
        graphics.drawSkikoImage("assets/hkim/textures/clickgui/icon.svg", logoX, logoY, logoSize, logoSize, 0f, tintColor = Theme.iconPrimary)

        val hlAlpha = highlightAlphaAnim.getValue()
        if (hlAlpha > 0.001f) {
            val hlX = x + 4f
            val hlW = w - 8f
            val hlH = 28f
            val hlY = highlightYAnim.getValue()
            val alphaInt = (0xFF * Theme.categoryHighlightAlpha * hlAlpha).toInt().coerceIn(0, 0xFF)
            val hlColor = (alphaInt shl 24) or (this.themeColor and 0x00FFFFFF)
            graphics.drawRoundedRectWithBorder(hlX, hlY, hlW, hlH, hlColor, 0, 0f, 4f)
        }

        val iconSize = 20f
        val iconPadding = 14f
        val iconX = x + (w - iconSize) / 2f
        var iconY = y + 50f

        for (category in Category.entries) {
            graphics.drawSkikoImage("assets/hkim/textures/clickgui/${category.name.lowercase()}.svg", iconX, iconY, iconSize, iconSize, 0f, tintColor = Theme.iconPrimary)

            if (HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), iconX, iconY, iconSize, iconSize)) {
                graphics.requestCursor(CursorTypes.POINTING_HAND)
                graphics.drawTooltip(category.name.lowercase().replaceFirstChar { it.uppercase() }, mouseX.toFloat(), mouseY.toFloat(), delayTicks = 4)
            }

            iconY += iconSize + iconPadding
        }

        val bottomY = y + h - 70f
        graphics.drawSkikoText("v${Hkim.VERSION}", guiX + 5f, guiY + guiH - mc.font.lineHeight - 6f, mc.font.lineHeight.toFloat(), Theme.textMuted)

        graphics.drawSkikoImage("assets/hkim/textures/clickgui/edit.svg", iconX, bottomY + 30f, 20f, 20f, 0f, tintColor = Theme.iconMuted)
        if (HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), x + 10f, bottomY + 30f, 20f, 20f)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
            graphics.drawTooltip("Edit HUD", mouseX.toFloat(), mouseY.toFloat(), delayTicks = 4)
        }
    }

    private fun renderHeader(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val x = guiX + sidebarW
        val y = guiY
        val w = guiW - sidebarW

        val titleText = when {
            searchQuery.isNotEmpty() -> "Search: \"$searchQuery\""
            selectedCategory != null -> "${selectedCategory!!.name.lowercase().replaceFirstChar { it.uppercase() }} Modules"
            else -> "All Modules"
        }
        graphics.drawSkikoText(titleText, x + 16f, y + 15f, mc.font.lineHeight.toFloat(), Theme.controlTextActive)

        if (searchSkikoBox != null) {
            searchSkikoBox!!.render(graphics, searchX, searchY, searchBoxW, searchBoxH, mouseX.toFloat(), mouseY.toFloat(), themeColor, mc.font.lineHeight.toFloat())
        } else {
            graphics.drawRoundedRectWithBorder(searchX, searchY, searchBoxW, searchBoxH, 0, Theme.controlBorder, 1f, 3f)
            if (HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), searchX, searchY, searchBoxW, searchBoxH)) {
                graphics.requestCursor(CursorTypes.IBEAM)
            }
            val hasQuery = searchQuery.isNotEmpty()
            graphics.drawSkikoText(
                if (hasQuery) searchQuery else "Search modules...",
                searchX + 6f, searchY + 4f, mc.font.lineHeight.toFloat(),
                if (hasQuery) Theme.controlTextActive else Theme.controlTextMuted
            )
        }

        val closeX = x + w - 32f
        val closeY = y + 12f
        val hovering = HudUtils.isPointInRect(mouseX.toFloat(), mouseY.toFloat(), closeX, closeY, 20f, 20f)

        if (hovering) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
            graphics.drawTooltip("Close", mouseX.toFloat(), mouseY.toFloat(), delayTicks = 4)
        }
        val hover = if (hovering) Theme.dangerIcon else 0xFFFFFFFF.toInt()
        graphics.drawSkikoImage("assets/hkim/textures/clickgui/close.svg", closeX + 4f, closeY + 4f, 12f, 12f, 0f, hover)
    }

    private fun renderContentArea(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val baseX = guiX + sidebarW + contentPadding
        val baseY = cardsTop
        val availW = guiW - sidebarW - contentPadding * 2
        val availH = guiH - headerH - contentPadding * 2

        val layouts = getCardLayouts()
        val totalH = layouts.maxOfOrNull { it.y + it.h } ?: 0f
        val maxScroll = max(0f, totalH - availH)
        contentScrollY = contentScrollY.coerceIn(-maxScroll, 0f)

        graphics.enableScissor((baseX - contentPadding).toInt(), (baseY - contentPadding).toInt(),
            (baseX + availW + contentPadding).toInt(), (baseY + availH + contentPadding).toInt())

        val contentTop = baseY - contentPadding
        val contentBottom = baseY + availH + contentPadding

        val scrollOffset = contentScrollY

        for (layout in layouts) {
            val state = cardStates[layout.module.id] ?: continue
            val currentModuleY = baseY + scrollOffset + layout.y

            state.render(graphics, layout.x, currentModuleY, layout.w, mouseX.toFloat(), mouseY.toFloat(), contentTop, contentBottom, themeColor, delta)
        }

        graphics.drawGradientRectMulti(
            baseX - 1, baseY - contentPadding,
            availW + 1, 8f,
            listOf(Theme.bg, Theme.bg and 0x00FFFFFF),
            null, SkikoGradient.TOP_BOTTOM, 0f
        )

        graphics.drawGradientRectMulti(
            baseX - 1, baseY + availH + contentPadding - 8f,
            availW + 1, 8f,
            listOf(Theme.bg and 0x00FFFFFF, Theme.bg),
            null, SkikoGradient.TOP_BOTTOM, 0f
        )

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
            mc.gui.setScreen(HudEditScreen(this))
            return true
        }
        return false
    }

    private fun handleHeaderClick(mouseX: Float, mouseY: Float, button: Int, doubleClick: Boolean = false): Boolean {
        if (button != 0) return false

        val x = guiX + sidebarW
        val y = guiY
        val w = guiW - sidebarW

        if (HudUtils.isPointInRect(mouseX, mouseY, searchX, searchY, searchBoxW, searchBoxH)) {
            if (searchSkikoBox != null) {
                searchSkikoBox!!.handleMouseClicked(mouseX, mouseY, searchX, searchY, searchBoxW, searchBoxH, doubleClick)
            } else {
                playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
                activateSearchBox()
            }
            return true
        }

        val closeX = x + w - 32f
        val closeY = y + 12f
        if (HudUtils.isPointInRect(mouseX, mouseY, closeX, closeY, 20f, 20f)) {
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            onClose()
            return true
        }

        return false
    }

    private fun handleContentClick(mouseX: Float, mouseY: Float, button: Int, doubleClick: Boolean): Boolean {
        val visibleTop = guiY + headerH
        val visibleBottom = guiY + guiH

        for (layout in getCardLayouts()) {
            val cy = cardsTop + contentScrollY + layout.y
            if (cardStates[layout.module.id]?.handleClick(mouseX, mouseY, button, layout.x, cy, layout.w, visibleTop, visibleBottom, doubleClick) == true) return true
        }
        return false
    }

    private fun handleSelectorScroll(mouseX: Float, mouseY: Float, scrollX: Double, scrollY: Double): Boolean {
        val visibleTop = guiY + headerH
        val visibleBottom = guiY + guiH

        for (layout in getCardLayouts()) {
            val cy = cardsTop + contentScrollY + layout.y
            if (cardStates[layout.module.id]?.handleScroll(mouseX, mouseY, scrollX, scrollY, layout.x, cy, layout.w, visibleTop, visibleBottom) == true) return true
        }
        return false
    }

    fun activateSkikoEditBox(setting: Setting<*>, initialValue: String, maxLength: Int = 64, insetY: Float = 2.5f) {
        if (activeEditBoxSetting == setting) return

        saveActiveEditBoxValue()

        activeEditBoxSetting = setting
        val box = SkikoEditBox(initialValue, maxLength).apply {
            textInsetX = 5f
            textInsetY = insetY
            responder = { newValue ->
                when (setting) {
                    is TextSetting -> setting.set(newValue)
                    is ColorSetting -> applyColorEditValue(setting, newValue)
                }
            }
            onConfirm = {
                saveActiveEditBoxValue()
                activeSkikoEditBox?.defocus()
                activeSkikoEditBox = null
                activeEditBoxSetting = null
            }
        }
        box.focus()
        activeSkikoEditBox = box
    }

    private fun activateSearchBox() {
        if (searchSkikoBox != null) return

        val box = SkikoEditBox(searchQuery, maxLength = 64).apply {
            borderColor = Theme.controlBorder
            focusedBorderColor = Theme.controlBorder
            backgroundColor = 0
            textInsetY = 4f
            responder = { newValue -> searchQuery = newValue }
        }
        box.focus()
        searchSkikoBox = box
    }

    private fun deactivateSearchBox() {
        searchSkikoBox?.let {
            searchQuery = it.text
            it.defocus()
            searchSkikoBox = null
        }
    }

    fun deactivateEditBox() {
        saveActiveEditBoxValue()
        activeSkikoEditBox?.defocus()
        activeSkikoEditBox = null
        activeEditBoxSetting = null
    }

    private fun saveActiveEditBoxValue() {
        val setting = activeEditBoxSetting ?: return
        val box = activeSkikoEditBox ?: return
        when (setting) {
            is TextSetting -> {
                setting.set(box.text)
                ModuleConfig.saveConfig()
            }
            is ColorSetting -> {
                applyColorEditValue(setting, box.text)
                ModuleConfig.saveConfig()
            }
        }
    }

    private fun applyColorEditValue(setting: ColorSetting, text: String) {
        val parsed = HudUtils.fromHexString(text) ?: return
        val value = if (text.replace("#", "").trim().length == 8) {
            parsed
        } else {
            (setting.value and 0xFF000000.toInt()) or (parsed and 0x00FFFFFF)
        }
        setting.set(value)
    }
}