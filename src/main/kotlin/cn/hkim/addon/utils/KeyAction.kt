package cn.hkim.addon.utils

import cn.hkim.addon.compat.input.InputCompat

enum class KeyAction {
    Press,
    Repeat,
    Release;

    companion object {
        @JvmStatic
        fun get(action: Int): KeyAction {
            return when (action) {
                InputCompat.PRESS -> Press
                InputCompat.RELEASE -> Release
                else -> Repeat
            }
        }
    }
}
