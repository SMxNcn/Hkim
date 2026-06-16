package cn.hkim.addon.hud

/**
 * Each anchor defines a reference point on the screen.
 * `anchorX`/`anchorY` in [HudElement] are offsets from this anchor point,
 * in GUI-scaled pixels.
 */
enum class HudAlignment {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP_MIDDLE, BOTTOM_MIDDLE, NONE;

    fun baseX(screenW: Int): Float = when (this) {
        TOP_RIGHT, BOTTOM_RIGHT -> screenW.toFloat()
        TOP_MIDDLE, BOTTOM_MIDDLE -> screenW / 2f
        else -> 0f
    }

    fun baseY(screenH: Int): Float = when (this) {
        BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM_MIDDLE -> screenH.toFloat()
        else -> 0f
    }
}
