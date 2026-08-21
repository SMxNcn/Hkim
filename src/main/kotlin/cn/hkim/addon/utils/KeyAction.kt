package cn.hkim.addon.utils

import com.mojang.blaze3d.platform.InputConstants

enum class KeyAction {
    Press,
    Repeat,
    Release;

    companion object {
        @JvmStatic
        fun get(action: Int): KeyAction {
            return when (action) {
                InputConstants.PRESS -> Press
                InputConstants.RELEASE -> Release
                else -> Repeat
            }
        }
    }
}