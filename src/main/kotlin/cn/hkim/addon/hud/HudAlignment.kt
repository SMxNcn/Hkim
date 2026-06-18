package cn.hkim.addon.hud

/**
 * Each anchor defines a reference point on the screen.
 * `anchorX`/`anchorY` in [HudElement] are offsets from this anchor point,
 * in GUI-scaled pixels.
 */
enum class HudAlignment {
    TOP_LEFT, TOP_MIDDLE, TOP_RIGHT,
    MIDDLE_LEFT, MIDDLE_RIGHT,
    BOTTOM_LEFT, BOTTOM_MIDDLE, BOTTOM_RIGHT,
    NONE;

    fun baseX(screenW: Int): Float = when (this) {
        TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> screenW.toFloat()
        TOP_MIDDLE, BOTTOM_MIDDLE -> screenW / 2f
        else -> 0f
    }

    fun baseY(screenH: Int): Float = when (this) {
        BOTTOM_LEFT, BOTTOM_MIDDLE, BOTTOM_RIGHT -> screenH.toFloat()
        MIDDLE_LEFT, MIDDLE_RIGHT -> screenH / 2f
        else -> 0f
    }
}
