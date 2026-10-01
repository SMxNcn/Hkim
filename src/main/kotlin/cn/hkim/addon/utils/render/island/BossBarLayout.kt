package cn.hkim.addon.utils.render.island

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.mixins.accessors.BossHealthOverlayAccessor
import kotlin.math.ceil

/**
 * Mirrors the vanilla boss bar layout so HUD elements can be placed below it.
 *
 * Vanilla draws the first bar at y = 12, advances 19 pixels per bar and stops
 * once the next bar would pass a third of the screen height.
 */
object BossBarLayout {
    private const val FIRST_BAR_Y = 12f
    private const val BAR_STEP = 19f
    private const val BAR_HEIGHT = 5f

    /**
     * Bottom edge of the last boss bar that is currently on screen,
     * or `0` when no bar is displayed.
     */
    fun bottomY(): Float {
        val overlay = mc.gui.hud.bossOverlay as? BossHealthOverlayAccessor ?: return 0f
        val bars = overlay.events?.size ?: return 0f
        if (bars <= 0) return 0f

        val fitsOnScreen = ceil((mc.window.guiScaledHeight / 3f - FIRST_BAR_Y) / BAR_STEP).toInt()
        val drawn = bars.coerceAtMost(fitsOnScreen)
        if (drawn <= 0) return 0f

        return FIRST_BAR_Y + BAR_STEP * (drawn - 1) + BAR_HEIGHT
    }
}
