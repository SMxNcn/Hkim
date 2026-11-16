package cn.hkim.addon.events.impl

import cn.hkim.addon.compat.input.InputCompat
import cn.hkim.addon.events.Cancellable
import cn.hkim.addon.utils.KeyAction
import com.mojang.blaze3d.platform.InputConstants

class InputEvent(val key: InputConstants.Key, val action: KeyAction) : Cancellable() {
    val code: Int = InputCompat.encode(key)

    fun matches(keybind: Int): Boolean = action == KeyAction.Press && isBoundTo(keybind)

    fun isBoundTo(keybind: Int): Boolean = keybind != InputCompat.KEY_NONE && keybind == code
}