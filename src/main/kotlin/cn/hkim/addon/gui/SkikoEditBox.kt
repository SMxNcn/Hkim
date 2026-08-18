package cn.hkim.addon.gui

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoRectClipped
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoTextClipped
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoTextWidth
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.PreeditEvent
import org.lwjgl.glfw.GLFW
import kotlin.math.max
import kotlin.math.min

class SkikoEditBox(
    initialText: String = "",
    var maxLength: Int = 64,
    var responder: (String) -> Unit = {}
) {
    var text: String = initialText
        private set
    var cursor: Int = initialText.length
        private set
    var selectionAnchor: Int = -1
        private set
    var isFocused: Boolean = false
        private set

    var onConfirm: (() -> Unit)? = null
    var filter: ((String) -> String)? = null

    var hint: String? = null
    var hintColor: Int = 0xFF666666.toInt()
    var borderColor: Int? = null
    var focusedBorderColor: Int? = null
    var backgroundColor: Int? = null

    var textInsetX: Float = 6f
    var textInsetY: Float = 3f

    private var textSize = 9f
    private var scrollOffset = 0f
    private var dragging = false
    private var dragWordMode = false
    private var preeditText = ""
    private var preeditCaret = 0

    var lastX = 0f; var lastY = 0f; var lastW = 0f; var lastH = 0f
        private set

    private val hasSelection: Boolean get() = selectionAnchor >= 0 && selectionAnchor != cursor

    fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, w: Float, h: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int,
        fontSize: Float
    ) {
        lastX = x; lastY = y; lastW = w; lastH = h
        textSize = fontSize

        if (isFocused) {
            mc.textInputManager().setTextInputArea(x.toInt(), y.toInt(), (x + w).toInt(), (y + h).toInt())
        }

        val border = if (isFocused) focusedBorderColor ?: borderColor ?: themeColor
                     else borderColor ?: Theme.controlBorder
        graphics.drawRoundedRectWithBorder(x, y, w, h, backgroundColor ?: Theme.controlBg, border, 1f, 3f)

        if (HudUtils.isPointInRect(mouseX, mouseY, x, y, w, h)) {
            graphics.requestCursor(CursorTypes.IBEAM)
        }

        val innerX = x + textInsetX
        val textTop = y + textInsetY
        val clipX = x + 2f
        val clipY = y + 2f
        val clipW = w - 4f
        val clipH = h - 4f

        if (text.isEmpty() && preeditText.isEmpty()) {
            val hintText = hint
            if (hintText != null) {
                graphics.drawSkikoText(hintText, x + textInsetX, y + textInsetY, textSize, hintColor)
            }
            if (isFocused) {
                drawCaret(graphics, innerX - scrollOffset + prefixWidth(cursor), y, h, clipX, clipY, clipW, clipH)
            }
            return
        }

        if (isFocused) ensureCursorVisible()

        if (isFocused && hasSelection) {
            val selStart = min(cursor, selectionAnchor)
            val selEnd = max(cursor, selectionAnchor)
            val sx = innerX - scrollOffset + prefixWidth(selStart)
            val ex = innerX - scrollOffset + prefixWidth(selEnd)
            val selColor = (0x40 shl 24) or (themeColor and 0x00FFFFFF)
            graphics.drawSkikoRectClipped(sx, y + 1.5f, (ex - sx).coerceAtLeast(0.5f), h - 3f, selColor, clipX, clipY, clipW, clipH)
        }

        if (text.isNotEmpty()) {
            graphics.drawSkikoTextClipped(text, innerX - scrollOffset, textTop, textSize, Theme.controlTextActive, clipX, clipY, clipW, clipH)
        }

        if (preeditText.isNotEmpty()) {
            val preeditX = innerX - scrollOffset + prefixWidth(cursor)
            graphics.drawSkikoTextClipped(preeditText, preeditX, textTop, textSize, Theme.controlTextActive, clipX, clipY, clipW, clipH)
            graphics.drawSkikoRectClipped(preeditX, textTop + textSize + 1f, textWidthOf(preeditText), 1f, themeColor, clipX, clipY, clipW, clipH)
            if (isFocused) {
                val caretX = preeditX + textWidthOf(preeditText.substring(0, min(preeditCaret, preeditText.length)))
                drawCaret(graphics, caretX, y, h, clipX, clipY, clipW, clipH)
            }
        } else if (isFocused) {
            val caretX = innerX - scrollOffset + prefixWidth(cursor)
            drawCaret(graphics, caretX, y, h, clipX, clipY, clipW, clipH)
        }
    }

    private fun drawCaret(
        graphics: GuiGraphicsExtractor,
        caretX: Float, boxY: Float, boxH: Float,
        clipX: Float, clipY: Float, clipW: Float, clipH: Float
    ) {
        val blinkVisible = dragging || (System.currentTimeMillis() % 1000L) < 500L
        if (!blinkVisible) return
        val caretH = textSize + 2f
        graphics.drawSkikoRectClipped(caretX, boxY + (boxH - caretH) / 2f, 1f, caretH, Theme.controlTextActive, clipX, clipY, clipW, clipH)
    }

    private fun ensureCursorVisible() {
        if (textFitsInBox()) {
            scrollOffset = 0f
            return
        }
        val visibleW = lastW - 8f
        if (visibleW <= 0f) return

        val caretX = if (preeditText.isNotEmpty()) {
            prefixWidth(cursor) + textWidthOf(preeditText.substring(0, min(preeditCaret, preeditText.length)))
        } else {
            prefixWidth(cursor)
        }

        if (caretX - scrollOffset < 4f) scrollOffset = caretX - 4f
        else if (caretX - scrollOffset > visibleW - 4f) scrollOffset = caretX - (visibleW - 4f)
        if (scrollOffset < 0f) scrollOffset = 0f
    }

    private fun textFitsInBox(): Boolean {
        if (lastW <= 0f) return false
        val visibleW = lastW - 8f
        if (visibleW <= 0f) return false
        val contentW = textWidthOf(text) + if (preeditText.isNotEmpty()) textWidthOf(preeditText) else 0f
        return contentW <= visibleW
    }

    private fun prefixWidth(index: Int): Float {
        val idx = index.coerceIn(0, text.length)
        if (idx <= 0) return 0f
        return skikoTextWidth(text.substring(0, idx), textSize)
    }

    private fun textWidthOf(s: String): Float = skikoTextWidth(s, textSize)

    fun handleKeyPressed(event: KeyEvent): Boolean {
        if (!isFocused) return false
        if (event.isEscape) return false
        if (event.isConfirmation) {
            onConfirm?.invoke()
            defocus()
            return true
        }

        val ctrl = event.hasControlDownWithQuirk()
        val shift = event.hasShiftDown()

        when {
            event.isSelectAll -> { selectAll(); return true }
            event.isCopy -> { copySelection(); return true }
            event.isCut -> { cutSelection(); return true }
            event.isPaste -> { paste(); return true }
            event.isLeft -> { moveCursorTo(if (ctrl) wordBoundary(cursor, forward = false) else moveLeftChar(), shift); return true }
            event.isRight -> { moveCursorTo(if (ctrl) wordBoundary(cursor, forward = true) else moveRightChar(), shift); return true }
            event.isUp || event.isDown -> return true
            event.key() == GLFW.GLFW_KEY_HOME -> { moveCursorTo(0, shift); return true }
            event.key() == GLFW.GLFW_KEY_END -> { moveCursorTo(text.length, shift); return true }
            event.key() == GLFW.GLFW_KEY_BACKSPACE -> { if (ctrl) deleteWordBackward() else deleteBackward(); return true }
            event.key() == GLFW.GLFW_KEY_DELETE -> { if (ctrl) deleteWordForward() else deleteForward(); return true }
            else -> return false
        }
    }

    fun handleCharTyped(event: CharacterEvent): Boolean {
        if (!isFocused) return false
        if (!event.isAllowedChatCharacter) return false

        val cp = event.codepoint()
        if (cp == 167) return false // 排除 §

        insert(Character.toString(cp))
        return true
    }

    fun handlePreedit(event: PreeditEvent): Boolean {
        if (!isFocused) return false
        preeditText = event.fullText()
        preeditCaret = event.caretPosition().coerceIn(0, preeditText.length)
        ensureCursorVisible()
        return true
    }

    fun clearPreedit() {
        if (preeditText.isEmpty() && preeditCaret == 0) return
        preeditText = ""
        preeditCaret = 0
        ensureCursorVisible()
    }

    fun handleMouseClicked(mouseX: Float, mouseY: Float, x: Float, y: Float, w: Float, h: Float, doubleClick: Boolean): Boolean {
        if (!HudUtils.isPointInRect(mouseX, mouseY, x, y, w, h)) return false

        focus()
        dragging = true
        dragWordMode = false
        cursor = indexAtX(mouseX, x)
        selectionAnchor = cursor

        if (doubleClick) {
            val s = wordStartAt(cursor)
            val e = wordEndAt(cursor)
            if (s != e) {
                selectionAnchor = s
                cursor = e
                dragWordMode = true
            }
        }
        ensureCursorVisible()
        return true
    }

    fun handleMouseDragged(mouseX: Float, mouseY: Float): Boolean {
        if (!dragging) return false
        if (dragWordMode) {
            val idx = indexAtX(mouseX, lastX)
            selectionAnchor = wordStartAt(idx)
            cursor = wordEndAt(idx)
        } else {
            cursor = indexAtX(mouseX, lastX)
        }
        ensureCursorVisible()
        return true
    }

    fun handleMouseReleased(): Boolean {
        if (!dragging) return false
        dragging = false
        dragWordMode = false
        return true
    }

    fun isPointIn(mouseX: Float, mouseY: Float): Boolean =
        HudUtils.isPointInRect(mouseX, mouseY, lastX, lastY, lastW, lastH)

    fun setText(newText: String) {
        if (text == newText) return
        text = newText
        cursor = newText.length
        selectionAnchor = -1
        scrollOffset = 0f
        onChanged()
    }

    fun focus() {
        if (isFocused) return
        isFocused = true
        mc.textInputManager().onTextInputFocusChange(true)
    }

    fun defocus() {
        if (!isFocused) return
        isFocused = false
        dragging = false
        dragWordMode = false
        selectionAnchor = -1
        preeditText = ""
        preeditCaret = 0
        mc.textInputManager().onTextInputFocusChange(false)
    }

    private fun onChanged() {
        responder(text)
    }

    private fun moveCursorTo(pos: Int, extend: Boolean) {
        val newPos = pos.coerceIn(0, text.length)
        if (extend) {
            if (selectionAnchor < 0) selectionAnchor = cursor
        } else {
            selectionAnchor = -1
        }
        cursor = newPos
        ensureCursorVisible()
    }

    private fun selectAll() {
        selectionAnchor = 0
        cursor = text.length
        ensureCursorVisible()
    }

    private fun copySelection() {
        if (!hasSelection) return
        val s = min(cursor, selectionAnchor)
        val e = max(cursor, selectionAnchor)
        GLFW.glfwSetClipboardString(mc.window.handle(), text.substring(s, e))
    }

    private fun cutSelection() {
        if (!hasSelection) return
        copySelection()
        deleteSelection()
    }

    private fun paste() {
        val clipboard = GLFW.glfwGetClipboardString(mc.window.handle()) ?: return
        insert(sanitizeClipboard(clipboard))
    }

    private fun sanitizeClipboard(s: String): String =
        s.filter { it.code >= 32 && it != 167.toChar() }

    private fun insert(str: String) {
        val filtered = filter?.invoke(str) ?: str
        if (filtered.isEmpty()) return

        val selStart = if (hasSelection) min(cursor, selectionAnchor) else cursor
        val selEnd = if (hasSelection) max(cursor, selectionAnchor) else cursor
        val available = maxLength - (text.length - (selEnd - selStart))
        if (available <= 0) return

        val insertStr = if (filtered.length > available) capToMaxLength(filtered, available) else filtered
        if (insertStr.isEmpty()) return

        text = text.substring(0, selStart) + insertStr + text.substring(selEnd)
        cursor = selStart + insertStr.length
        selectionAnchor = -1
        ensureCursorVisible()
        onChanged()
    }

    private fun capToMaxLength(s: String, available: Int): String {
        if (available <= 0) return ""
        if (s.length <= available) return s
        var len = available
        while (len > 0 && (Character.isLowSurrogate(s[len - 1]) || Character.isHighSurrogate(s[len - 1]))) {
            len--
        }
        return s.substring(0, len)
    }

    private fun deleteBackward() {
        if (hasSelection) { deleteSelection(); return }
        if (cursor <= 0) return
        val start = if (cursor >= 2 && text[cursor - 1].isLowSurrogate() && text[cursor - 2].isHighSurrogate()) cursor - 2 else cursor - 1
        text = text.substring(0, start) + text.substring(cursor)
        cursor = start
        ensureCursorVisible()
        onChanged()
    }

    private fun deleteForward() {
        if (hasSelection) { deleteSelection(); return }
        if (cursor >= text.length) return
        val end = if (cursor + 1 < text.length && text[cursor].isHighSurrogate() && text[cursor + 1].isLowSurrogate()) cursor + 2 else cursor + 1
        text = text.substring(0, cursor) + text.substring(end)
        ensureCursorVisible()
        onChanged()
    }

    private fun deleteWordBackward() {
        if (hasSelection) { deleteSelection(); return }
        val start = wordBoundary(cursor, forward = false)
        if (start == cursor) return
        text = text.substring(0, start) + text.substring(cursor)
        cursor = start
        ensureCursorVisible()
        onChanged()
    }

    private fun deleteWordForward() {
        if (hasSelection) { deleteSelection(); return }
        val end = wordBoundary(cursor, forward = true)
        if (end == cursor) return
        text = text.substring(0, cursor) + text.substring(end)
        ensureCursorVisible()
        onChanged()
    }

    private fun deleteSelection() {
        if (!hasSelection) return
        val s = min(cursor, selectionAnchor)
        val e = max(cursor, selectionAnchor)
        text = text.substring(0, s) + text.substring(e)
        cursor = s
        selectionAnchor = -1
        ensureCursorVisible()
        onChanged()
    }

    private fun indexAtX(mouseX: Float, boxX: Float): Int {
        if (text.isEmpty()) return 0

        val offset = if (textFitsInBox()) 0f else scrollOffset
        val target = mouseX - boxX - textInsetX + offset
        var lo = 0
        var hi = text.length
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (prefixWidth(mid) < target) lo = mid + 1 else hi = mid
        }

        val before = if (lo > 0) prefixWidth(lo - 1) else 0f
        val after = prefixWidth(lo)
        val idx = if (target - before < after - target) lo - 1 else lo

        return fixSurrogate(idx)
    }

    private fun fixSurrogate(idx: Int): Int {
        var i = idx.coerceIn(0, text.length)
        if (i > 0 && i < text.length && text[i - 1].isHighSurrogate() && text[i].isLowSurrogate()) {
            i--
        }
        return i
    }

    private fun moveLeftChar(): Int {
        val p = cursor - 1
        if (p >= 1 && text[p].isLowSurrogate() && text[p - 1].isHighSurrogate()) return p - 1
        return p
    }

    private fun moveRightChar(): Int {
        if (cursor < text.length && text[cursor].isHighSurrogate() && cursor + 1 < text.length && text[cursor + 1].isLowSurrogate()) return cursor + 2
        return cursor + 1
    }

    private fun wordStartAt(from: Int): Int {
        var i = from.coerceIn(0, text.length)
        while (i > 0 && !isWordChar(text[i - 1])) i--
        while (i > 0 && isWordChar(text[i - 1])) i--
        return fixSurrogate(i)
    }

    private fun wordEndAt(from: Int): Int {
        var i = from.coerceIn(0, text.length)
        while (i < text.length && !isWordChar(text[i])) i++
        while (i < text.length && isWordChar(text[i])) i++
        return fixSurrogate(i)
    }

    private fun wordBoundary(from: Int, forward: Boolean): Int =
        if (forward) wordEndAt(from) else wordStartAt(from)

    private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'
}
