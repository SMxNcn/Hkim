package cn.hkim.addon.events.impl

import cn.hkim.addon.events.Cancellable
import cn.hkim.addon.utils.KeyAction

class MouseButtonEvent private constructor(
    var button: Int,
    var action: KeyAction
) : Cancellable() {
    companion object {
        private val INSTANCE = MouseButtonEvent(0, KeyAction.Press)

        @JvmStatic
        fun get(button: Int, action: KeyAction): MouseButtonEvent {
            INSTANCE.isCancelled = false
            INSTANCE.button = button
            INSTANCE.action = action
            return INSTANCE
        }
    }
}