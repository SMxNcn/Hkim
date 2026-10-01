package cn.hkim.addon.compat.tab

import cn.hkim.addon.hud.Bounds
import net.minecraft.client.gui.navigation.ScreenRectangle
import kotlin.math.max
import kotlin.math.min

class TabBounds {
    private var left = Int.MAX_VALUE
    private var top = Int.MAX_VALUE
    private var right = Int.MIN_VALUE
    private var bottom = Int.MIN_VALUE

    fun include(bounds: ScreenRectangle?) = bounds?.let { include(it.left(), it.top(), it.right(), it.bottom()) }

    fun include(x1: Int, y1: Int, x2: Int, y2: Int) {
        if (x2 <= x1 || y2 <= y1) return
        left = min(left, x1)
        top = min(top, y1)
        right = max(right, x2)
        bottom = max(bottom, y2)
    }

    val result: Bounds?
        get() = if (left > right || top > bottom) null
        else Bounds(left.toFloat(), top.toFloat(), (right - left).toFloat(), (bottom - top).toFloat())
}
