package cn.hkim.addon.utils.render.island

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.mixins.accessors.BossHealthOverlayAccessor
import kotlin.math.ceil

object BossBarLayout {
    private const val FIRST_BAR_Y = 12f
    private const val BAR_STEP = 19f
    private const val BAR_HEIGHT = 5f

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
