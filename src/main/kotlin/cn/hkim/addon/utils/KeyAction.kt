package cn.hkim.addon.utils

import org.lwjgl.glfw.GLFW

enum class KeyAction {
    Press,
    Repeat,
    Release;

    companion object {
        @JvmStatic
        fun get(action: Int): KeyAction {
            return when (action) {
                GLFW.GLFW_PRESS -> Press
                GLFW.GLFW_RELEASE -> Release
                else -> Repeat
            }
        }
    }
}