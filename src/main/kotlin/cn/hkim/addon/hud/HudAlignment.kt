package cn.hkim.addon.hud

/**
 * Each anchor defines a reference point on the screen.
 * `anchorX`/`anchorY` in [HudElement] are offsets from this point referring to
 * the element's edge or center implied by the alignment.
 * Both are in GUI-scaled pixels.
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
        BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM_MIDDLE -> screenH.toFloat()
        MIDDLE_LEFT, MIDDLE_RIGHT -> screenH / 2f
        else -> 0f
    }

    companion object {
        fun forPosition(cx: Float, cy: Float, screenW: Float, screenH: Float): HudAlignment? {
            val regionH = when {
                cx < screenW * 0.35f -> "LEFT"
                cx > screenW * 0.65f -> "RIGHT"
                else -> "MIDDLE"
            }
            val regionV = when {
                cy < screenH * 0.35f -> "TOP"
                cy > screenH * 0.65f -> "BOTTOM"
                else -> "MIDDLE"
            }
            return when {
                regionV == "MIDDLE" && regionH == "MIDDLE" -> null
                regionV == "MIDDLE" -> valueOf("MIDDLE_${regionH}")
                regionH == "MIDDLE" -> if (regionV == "TOP") TOP_MIDDLE else BOTTOM_MIDDLE
                else -> valueOf("${regionV}_${regionH}")
            }
        }
    }
}
