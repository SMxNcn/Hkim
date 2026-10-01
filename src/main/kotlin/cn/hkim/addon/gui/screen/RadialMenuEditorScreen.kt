package cn.hkim.addon.gui.screen

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.RadialLayout
import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.features.impl.RadialMenu
import cn.hkim.addon.gui.SkikoEditBox
import cn.hkim.addon.gui.SkikoSuggestionList
import cn.hkim.addon.utils.HudUtils.isPointInRect
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoCenteredText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoTextClipped
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoBatch
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoTextHeight
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class RadialMenuEditorScreen(private val parent: Screen? = null) : Screen(Component.literal("Radial Menu Editor")) {
    private val fields = listOf(
        Field("Display Item", "minecraft:dirt", "Icon item. Type to filter; Tab or click to complete."),
        Field("Display Name", "A name here", "Label shown when no icon is set."),
        Field("Description", "Some description here", "Detail shown when this slot is selected."),
        Field("Run Command", "give @s diamond", "Command without the leading slash.")
    )

    private var selectedSlot = 0
    private var activeField = -1
    private var unknownItem = false
    private var dirty = false

    private var applyingSuggestion = false

    private val editBox = SkikoEditBox(maxLength = 256)
    private val suggestions = SkikoSuggestionList()

    private var pressedSlot = -1
    private var pressX = 0f
    private var pressY = 0f

    private var draggingSlot = -1
    private var dropSlot = -1

    private val ringCenterY: Float get() = height / 2f

    private val ringScale: Float get() = minOf(
        1f,
        (width / 2f - CONTENT_GAP - SIDE_MARGIN) / (RadialMenu.MENU_RADIUS * 2f),
        (height - 72f) / (RadialMenu.MENU_RADIUS * 2f),
    ).coerceIn(0.1f, 1f)

    private val ringCenterX: Float get() = width / 2f - CONTENT_GAP - RadialMenu.MENU_RADIUS * ringScale

    private val panelW: Float get() = (width / 2f - CONTENT_GAP - SIDE_MARGIN).coerceAtMost(340f)
    private val panelX: Float get() = width / 2f + CONTENT_GAP
    private val panelY: Float get() = ((height - PANEL_HEIGHT) / 2f).coerceAtLeast(34f)

    private class Field(val label: String, val hint: String, val desc: String)

    init {
        editBox.responder = { text -> setFieldValue(activeField, text) }
        editBox.onConfirm = { deactivateField() }
        editBox.showClearButton = true
        editBox.textInsetX = 5f
        editBox.textInsetY = 3.5f
        editBox.backgroundColor = BOX_BG
        editBox.borderColor = BOX_BORDER
        editBox.focusedBorderColor = ACCENT
        editBox.hintColor = 0xFF7A7A7A.toInt()
        suggestions.backgroundColor = BOX_BG
        suggestions.borderColor = BOX_BORDER
        suggestions.textColor = 0xFFCFCFCF.toInt()
        suggestions.textActiveColor = 0xFFFFFFFF.toInt()
        suggestions.rowHoverColor = 0x14FFFFFF
        suggestions.scrollBarColor = 0xFF6E6E6E.toInt()
    }

    private fun entries() = RadialLayout.entries(RadialMenu.profile)

    private fun fieldValue(index: Int): String {
        val entry = RadialLayout.entry(RadialMenu.profile, selectedSlot)
        return when (index) {
            ITEM_FIELD -> entry.item
            NAME_FIELD -> entry.name
            2 -> entry.description
            else -> entry.command
        }
    }

    private fun setFieldValue(index: Int, text: String) {
        if (index !in fields.indices) return
        val entry = RadialLayout.entry(RadialMenu.profile, selectedSlot)
        when (index) {
            ITEM_FIELD -> entry.item = text
            NAME_FIELD -> entry.name = text
            2 -> entry.description = text
            else -> entry.command = text
        }
        unknownItem = index == ITEM_FIELD && text.isNotBlank() && !RadialLayout.isKnownItem(text)
        dirty = true
        if (!applyingSuggestion) refreshSuggestions()
    }

    private val nameOptional: Boolean
        get() {
            val item = RadialLayout.entry(RadialMenu.profile, selectedSlot).item
            return item.isNotBlank() && RadialLayout.isKnownItem(item)
        }

    private fun fieldHint(index: Int): String =
        if (index == NAME_FIELD && nameOptional) "Optional when an item is set" else fields[index].hint

    private fun fieldDesc(index: Int): String =
        if (index == NAME_FIELD && nameOptional) "Optional: the slot draws the item's icon instead of this name."
        else fields[index].desc

    private fun refreshUnknownItem() {
        val item = RadialLayout.entry(RadialMenu.profile, selectedSlot).item
        unknownItem = item.isNotBlank() && !RadialLayout.isKnownItem(item)
    }

    private fun boxX() = panelX + LABEL_WIDTH

    private fun boxY(index: Int) = panelY + ROW_TOP + index * ROW_HEIGHT

    private fun boxW() = panelW - LABEL_WIDTH

    private fun refreshSuggestions() {
        if (activeField != ITEM_FIELD) {
            suggestions.close()
            return
        }
        suggestions.setEntries(RadialLayout.suggestItems(editBox.text))
        val index = suggestions.entries.indexOf(RadialLayout.displayId(editBox.text))
        if (index >= 0) suggestions.highlight(index)
    }

    private fun applySuggestion(entry: String) {
        if (editBox.text == entry) return
        applyingSuggestion = true
        editBox.setText(entry)
        applyingSuggestion = false
    }

    private fun cycleSuggestion(step: Int) {
        val entry = suggestions.highlightedEntry ?: return
        if (editBox.text != entry) {
            applySuggestion(entry)
            return
        }
        suggestions.moveHighlight(step)
        suggestions.highlightedEntry?.let { applySuggestion(it) }
    }

    private fun suggestionY(): Float {
        val listHeight = suggestions.height(SUGGESTION_ROW)
        val below = boxY(ITEM_FIELD) + BOX_HEIGHT + 2f
        return if (below + listHeight + 4f <= height) below
        else (boxY(ITEM_FIELD) - listHeight - 2f).coerceAtLeast(4f)
    }

    private fun drawSuggestions(graphics: GuiGraphicsExtractor, mx: Float, my: Float) {
        if (!suggestions.isOpen) return
        suggestions.render(graphics, boxX(), suggestionY(), boxW(), SUGGESTION_ROW, mx, my, ACCENT)
    }

    private fun drawDragGhost(graphics: GuiGraphicsExtractor, mouseX: Float, mouseY: Float) {
        val entry = entries().getOrNull(draggingSlot) ?: return
        val angle = atan2(mouseY - ringCenterY, mouseX - ringCenterX)
        val radius = RadialMenu.ITEM_LABEL_RADIUS * ringScale
        val valid = dropSlot >= 0

        RadialMenu.drawSlotBackground(
            graphics, ringCenterX, ringCenterY, ringScale, angle,
            background = if (valid) GHOST_BACKGROUND else GHOST_BACKGROUND_INVALID,
            outline = GHOST_OUTLINE,
            growth = GHOST_GROWTH * ringScale,
        )
        RadialMenu.drawSlotContent(
            graphics, entry,
            ringCenterX + cos(angle) * radius, ringCenterY + sin(angle) * radius,
            ringScale * GHOST_SCALE, GHOST_LABEL_COLOR, bold = true,
        )
    }

    private fun isInCenter(mx: Float, my: Float): Boolean {
        val dx = mx - ringCenterX
        val dy = my - ringCenterY
        val radius = RadialMenu.CENTER_RADIUS * ringScale
        return dx * dx + dy * dy <= radius * radius
    }

    private fun switchProfile(backwards: Boolean) {
        deactivateField()
        RadialMenu.switchProfile(if (backwards) -1 else 1)
        refreshUnknownItem()
        playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
    }

    private fun drawProfileButton(graphics: GuiGraphicsExtractor, mx: Float, my: Float) {
        val hovered = draggingSlot < 0 && isInCenter(mx, my)
        val size = (RadialMenu.CENTER_RADIUS * 0.5f * ringScale).coerceIn(5f, 9f)
        graphics.drawSkikoCenteredText(
            RadialLayout.PROFILE_NAMES[RadialMenu.profile],
            ringCenterX, ringCenterY - size / 2f, size,
            if (hovered) Theme.controlTextActive else Theme.textMuted,
            bold = hovered,
        )
        if (hovered) graphics.requestCursor(CursorTypes.POINTING_HAND)
    }

    private fun slotAt(mx: Float, my: Float): Int = RadialMenu.slotAt(
        mx, my, ringCenterX, ringCenterY,
        RadialMenu.CENTER_RADIUS * ringScale, RadialMenu.MENU_RADIUS * ringScale,
    )

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()

        graphics.fill(0, 0, width, height, 0x99000000.toInt())
        graphics.drawSkikoCenteredText(
            "Radial Menu Editor",
            width / 2f, TITLE_Y, TITLE_SIZE, 0xFFFFFFFF.toInt(), bold = true,
        )

        val dragging = draggingSlot >= 0
        val hovered = slotAt(mx, my)
        RadialMenu.drawRing(
            graphics, entries(), ringCenterX, ringCenterY, ringScale, selectedSlot,
            hover = if (dragging) -1 else hovered,
            dragging = draggingSlot,
            batchContent = { drawProfileButton(graphics, mx, my) },
            extraPad = PROFILE_TEXT_PAD,
        )
        drawDragGhost(graphics, mx, my)

        val hint = when {
            draggingSlot >= 0 && dropSlot >= 0 -> "Release to swap slot ${draggingSlot + 1} with ${dropSlot + 1}"
            draggingSlot >= 0 -> "Release outside to cancel"
            isInCenter(mx, my) -> "Click or scroll to switch profile"
            hovered >= 0 -> "Click to edit, drag to reorder slot ${hovered + 1}"
            else -> "Click a slot to edit"
        }
        graphics.drawSkikoText(hint, 6f, height - 26f, Theme.CARD_FONT_SIZE + 1f, 0xFFC8C8C8.toInt())
        graphics.drawSkikoText("Esc to close, edits are saved on exit", 6f, height - 14f, Theme.CARD_FONT_SIZE + 1f, 0xFF9E9E9E.toInt())

        drawPanel(graphics, mx, my)
        drawSuggestions(graphics, mx, my)

        if (draggingSlot >= 0) {
            graphics.requestCursor(if (dropSlot >= 0) CursorTypes.POINTING_HAND else CursorTypes.NOT_ALLOWED)
        } else if (hovered >= 0) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    private fun drawPanel(graphics: GuiGraphicsExtractor, mx: Float, my: Float) {
        val textSize = Theme.CARD_FONT_SIZE
        val labelY = (BOX_HEIGHT - skikoTextHeight(textSize)) / 2f
        val panelTop = boxY(0)
        val panelBottom = boxY(fields.size - 1) + BOX_HEIGHT + 12f + skikoTextHeight(textSize) + 4f

        graphics.skikoBatch(panelX, panelTop, panelW, panelBottom - panelTop) {
            for ((index, field) in fields.withIndex()) {
                val x = boxX()
                val y = boxY(index)
                val w = boxW()
                val warn = index == ITEM_FIELD && unknownItem
                graphics.drawSkikoText(
                    field.label, panelX, y + labelY, textSize,
                    if (warn) WARN_COLOR else 0xFFCFCFCF.toInt(),
                )

                if (index == activeField) {
                    editBox.render(graphics, x, y, w, BOX_HEIGHT, mx, my, ACCENT, textSize)
                    continue
                }

                val hovered = draggingSlot < 0 && isPointInRect(mx, my, x, y, w, BOX_HEIGHT)
                val border = when {
                    warn -> WARN_COLOR
                    hovered -> 0xFF6E6E6E.toInt()
                    else -> BOX_BORDER
                }
                graphics.drawRoundedRectWithBorder(x, y, w, BOX_HEIGHT, BOX_BG, border, 1f, 3f)

                val value = fieldValue(index)
                if (value.isEmpty()) {
                    graphics.drawSkikoText(fieldHint(index), x + 5f, y + 3.5f, textSize, 0xFF7A7A7A.toInt())
                } else {
                    graphics.drawSkikoTextClipped(value, x + 5f, y + 3.5f, textSize, 0xFFFFFFFF.toInt(), x + 1f, y, w - 2f, BOX_HEIGHT)
                }

                if (hovered) graphics.requestCursor(CursorTypes.IBEAM)
            }

            val desc = if (activeField in fields.indices) fieldDesc(activeField) else "Click a field to edit it."
            graphics.drawSkikoText(desc, panelX, boxY(fields.size - 1) + BOX_HEIGHT + 12f, textSize, Theme.textMuted)
        }
    }

    private fun activateField(index: Int) {
        if (index == activeField) return
        deactivateField()
        activeField = index
        editBox.setText(fieldValue(index))
        editBox.hint = fieldHint(index)
        editBox.focus()
        unknownItem = index == ITEM_FIELD && editBox.text.isNotBlank() && !RadialLayout.isKnownItem(editBox.text)
        refreshSuggestions()
    }

    private fun deactivateField() {
        save()
        suggestions.close()
        if (activeField < 0) return
        activeField = -1
        editBox.defocus()
    }

    private fun save() {
        if (!dirty) return
        dirty = false
        RadialLayout.save()
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mx = event.x.toFloat()
        val my = event.y.toFloat()

        if (pressedSlot >= 0) return true
        if ((event.button() == 0 || event.button() == 1) && isInCenter(mx, my)) {
            switchProfile(event.button() == 1)
            return true
        }

        if (event.button() != 0) return super.mouseClicked(event, doubleClick)

        if (suggestions.isOpen) {
            if (suggestions.contains(mx, my, boxX(), suggestionY(), boxW(), SUGGESTION_ROW)) {
                val index = suggestions.indexAt(mx, my, boxX(), suggestionY(), boxW(), SUGGESTION_ROW)
                if (index >= 0) {
                    suggestions.highlight(index)
                    applySuggestion(suggestions.entries[index])
                }
                return true
            }
            suggestions.close()
        }

        if (activeField >= 0 && editBox.handleMouseClicked(mx, my, boxX(), boxY(activeField), boxW(), BOX_HEIGHT, doubleClick)) {
            refreshSuggestions()
            return true
        }

        for (index in fields.indices) {
            if (isPointInRect(mx, my, panelX, boxY(index), panelW, BOX_HEIGHT)) {
                activateField(index)
                playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
                return true
            }
        }

        val slot = slotAt(mx, my)
        if (slot >= 0) {
            deactivateField()
            pressedSlot = slot
            pressX = mx
            pressY = my
            return true
        }

        deactivateField()
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        if (suggestions.isOpen) {
            val index = suggestions.indexAt(mouseX.toFloat(), mouseY.toFloat(), boxX(), suggestionY(), boxW(), SUGGESTION_ROW)
            if (index >= 0) suggestions.highlight(index)
        }
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (suggestions.isOpen && suggestions.contains(mouseX.toFloat(), mouseY.toFloat(), boxX(), suggestionY(), boxW(), SUGGESTION_ROW)) {
            suggestions.scrollBy(if (scrollY < 0) 1 else -1)
            return true
        }
        if (scrollY != 0.0 && draggingSlot < 0 && isInCenter(mouseX.toFloat(), mouseY.toFloat())) {
            switchProfile(scrollY > 0)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (editBox.handleMouseDragged(event.x.toFloat(), event.y.toFloat())) return true

        if (pressedSlot >= 0) {
            val mx = event.x.toFloat()
            val my = event.y.toFloat()
            if (draggingSlot < 0 && hypot(mx - pressX, my - pressY) >= DRAG_THRESHOLD) {
                draggingSlot = pressedSlot
            }
            if (draggingSlot >= 0) {
                dropSlot = slotAt(mx, my)
                return true
            }
        }
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (editBox.handleMouseReleased()) return true

        val pressed = pressedSlot
        if (pressed >= 0) {
            val dragged = draggingSlot
            pressedSlot = -1
            draggingSlot = -1

            if (dragged >= 0) {
                val target = dropSlot
                dropSlot = -1
                if (target >= 0) {
                    RadialLayout.swap(RadialMenu.profile, pressed, target)
                    selectedSlot = target
                    refreshUnknownItem()
                    playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
                }
                return true
            }

            dropSlot = -1
            if (pressed != selectedSlot) {
                selectedSlot = pressed
                refreshUnknownItem()
                playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            }
            return true
        }
        return super.mouseReleased(event)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (suggestions.isOpen) {
            when {
                event.key() == GLFW.GLFW_KEY_TAB -> {
                    cycleSuggestion(if (event.hasShiftDown()) -1 else 1)
                    return true
                }
                event.isUp -> { suggestions.moveHighlight(-1); return true }
                event.isDown -> { suggestions.moveHighlight(1); return true }
                event.isConfirmation -> {
                    suggestions.highlightedEntry?.let { applySuggestion(it) }
                    deactivateField()
                    return true
                }
                event.isEscape -> { suggestions.close(); return true }
            }
        }

        if (editBox.handleKeyPressed(event)) return true

        if (event.isEscape) {
            if (activeField >= 0) deactivateField() else close()
            return true
        }

        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (editBox.handleCharTyped(event)) return true
        return super.charTyped(event)
    }

    override fun preeditUpdated(event: PreeditEvent?): Boolean {
        if (event == null) {
            editBox.clearPreedit()
            return true
        }
        if (editBox.handlePreedit(event)) return true
        return super.preeditUpdated(event)
    }

    private fun close() {
        save()
        onClose()
        mc.gui.setScreen(parent)
    }

    override fun removed() {
        deactivateField()
        pressedSlot = -1
        draggingSlot = -1
        dropSlot = -1
        super.removed()
    }

    override fun extractMenuBackground(graphics: GuiGraphicsExtractor) {}
    override fun extractTransparentBackground(graphics: GuiGraphicsExtractor) {}
    override fun isPauseScreen() = false

    private companion object {
        const val TITLE_Y = 12f
        const val TITLE_SIZE = 10f
        const val CONTENT_GAP = 40f
        const val SIDE_MARGIN = 40f
        const val LABEL_WIDTH = 60f

        const val PROFILE_TEXT_PAD = 24f

        const val ROW_HEIGHT = 26f
        const val ROW_TOP = 8f
        const val BOX_HEIGHT = 18f

        const val ITEM_FIELD = 0
        const val NAME_FIELD = 1
        const val SUGGESTION_ROW = 12f

        const val DRAG_THRESHOLD = 4f
        const val GHOST_SCALE = 1.15f
        const val GHOST_GROWTH = 2f
        const val GHOST_BACKGROUND = 0xE03C3C3C.toInt()
        const val GHOST_BACKGROUND_INVALID = 0x90282828.toInt()
        const val GHOST_OUTLINE = 0xFF909090.toInt()
        const val GHOST_LABEL_COLOR = 0xFFFFFFFF.toInt()

        const val PANEL_HEIGHT = ROW_TOP + 3 * ROW_HEIGHT + BOX_HEIGHT + 24f
        const val WARN_COLOR = 0xFFFF5555.toInt()

        const val ACCENT = 0xFFE0E0E0.toInt()
        const val BOX_BG = 0xFF2B2B2B.toInt()
        const val BOX_BORDER = 0xFF4A4A4A.toInt()
    }
}
