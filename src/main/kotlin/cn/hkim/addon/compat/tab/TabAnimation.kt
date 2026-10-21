package cn.hkim.addon.compat.tab

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.features.impl.DynamicIsland
import cn.hkim.addon.hud.Bounds
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.island.IslandAnchor
import cn.hkim.addon.utils.render.island.IslandHost
import cn.hkim.addon.utils.render.island.IslandUser
import cn.hkim.addon.utils.render.island.towards
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle

object TabAnimation {
    private const val CONTAINER_PADDING = 2f

    private val anim = GuiAnimation.create(0f, 0f)
        .duration(150L)
        .easing(Easing.CUBIC_OUT)

    private var target = 0f
    private var listRect: Bounds? = null
    private var morphFrom: IslandAnchor? = null

    private var measuring: TabBounds? = null
    private var clipping = false
    private var offsetY = 0f
    private var insideSpan = false
    private var alpha = 1f

    private var skyHanniUpAt = 0L
    private var externalDrawRequested = false
    private var externalUp = false
    var forcingListKey = false

    @JvmStatic
    fun requestExternalDraw(): Boolean {
        skyHanniUpAt = System.currentTimeMillis()
        if (!DynamicIsland.enabled || !DynamicIsland.canRenderTabOnIsland || insideSpan || !SkyHanniTab.available) return false
        externalDrawRequested = true
        setTarget(1f)
        return true
    }

    @JvmStatic
    fun begin(graphics: GuiGraphicsExtractor): Boolean {
        IslandHost.listShowing = listShowingNow()

        val drawnThisFrame = externalDrawRequested
        externalDrawRequested = false
        if (drawnThisFrame) externalUp = true

        clipping = false
        offsetY = 0f
        insideSpan = false
        alpha = 1f
        if (!DynamicIsland.enabled || !DynamicIsland.canRenderTabOnIsland) {
            externalUp = false
            listRect = null
            releaseTab()
            return false
        }

        if (IslandHost.hasOther(IslandUser.TAB)) {
            if (listRect != null) anim.snapTo(0f)
            listRect = null
            target = 0f
            releaseTab()
            return false
        }

        if (externalUp) {
            setTarget(if (drawnThisFrame) 1f else 0f)
            val progress = anim.getValue()
            if (!drawnThisFrame && progress <= 0f) {
                externalUp = false
            } else {
                openSpan(graphics, progress)
                SkyHanniTab.draw(graphics)
                return false
            }
        }

        val down = mc.options.keyPlayerList.isDown
        setTarget(if (down) 1f else 0f)

        val progress = anim.getValue()
        if (!down && progress <= 0f) {
            listRect = null
            releaseTab()
            return false
        }

        openSpan(graphics, progress)
        return !down
    }

    @JvmStatic
    fun trackElement(bounds: ScreenRectangle?) {
        measuring?.include(bounds)
    }

    @JvmStatic
    fun dropTabBackground(color: Int, bounds: ScreenRectangle?): Boolean {
        if (!insideSpan || color != 0x80000000.toInt()) return false
        measuring?.include(bounds)
        return true
    }

    @JvmStatic
    fun fadeTabColor(color: Int): Int {
        if (!insideSpan || alpha >= 1f) return color
        val faded = ((color ushr 24) * alpha).toInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (faded shl 24)
    }

    @JvmStatic
    fun end(graphics: GuiGraphicsExtractor) {
        insideSpan = false
        val offset = offsetY
        offsetY = 0f
        if (offset != 0f) graphics.pose().popMatrix()
        if (clipping) {
            graphics.disableScissor()
            clipping = false
        }

        val collector = measuring ?: return
        measuring = null
        val measured = collector.result
        if (measured == null) {
            listRect = null
            releaseTab()
            return
        }

        val rect = if (offset == 0f) measured else Bounds(measured.x, measured.y + offset, measured.w, measured.h)
        listRect = if (SkyHanniTab.hidesBackground()) union(rect, SkyHanniTab.listBounds()) else rect
    }

    private fun listShowingNow(): Boolean =
        mc.options.keyPlayerList.isDown ||
            (skyHanniUpAt != 0L && System.currentTimeMillis() - skyHanniUpAt < 120L)

    private fun openSpan(graphics: GuiGraphicsExtractor, progress: Float) {
        insideSpan = true
        alpha = ((progress - 0.1f) / (0.5f - 0.1f)).coerceIn(0f, 1f)

        if (listRect != null && morphFrom == null) morphFrom = IslandHost.currentAnchor()
        val baseline = morphFrom
        if (baseline != null) IslandHost.acquire(IslandUser.TAB)

        val container = listRect?.let {
            Bounds(
                it.x - CONTAINER_PADDING, it.y - CONTAINER_PADDING,
                it.w + CONTAINER_PADDING * 2f, it.h + CONTAINER_PADDING * 2f,
            )
        }
        val shape = if (baseline != null) container?.let { baseline.towards(it, 6f, 0x80000000.toInt(), progress) } else null

        if (shape != null) {
            val rect = shape.bounds
            if (!SkyHanniTab.hidesBackground()) graphics.drawRoundedRect(rect.x, rect.y, rect.w, rect.h, shape.background, shape.radius)
            IslandHost.publishShape(IslandUser.TAB, shape)

            if (progress < 1f) {
                val inset = shape.radius * 0.2929f
                val left = (rect.x + inset).toInt()
                val top = (rect.y + inset).toInt()
                val right = (rect.x + rect.w - inset).toInt().coerceAtLeast(left + 1)
                val bottom = (rect.y + rect.h - inset).toInt().coerceAtLeast(top + 1)
                graphics.enableScissor(left, top, right, bottom)
                clipping = true
            }
        } else {
            graphics.pose().pushMatrix()
            graphics.pose().translate(0f, -4096f)
            offsetY = 4096f
        }

        measuring = if (clipping) null else TabBounds()
    }

    private fun setTarget(value: Float) {
        if (target == value) return
        target = value
        anim.duration(DynamicIsland.animationDurationMs)
        anim.animateTo(value)
    }

    private fun releaseTab() {
        morphFrom = null
        IslandHost.clearShape(IslandUser.TAB)
        IslandHost.release(IslandUser.TAB)
    }

    private fun union(a: Bounds, b: Bounds?): Bounds {
        if (b == null) return a
        val left = minOf(a.x, b.x)
        val top = minOf(a.y, b.y)
        return Bounds(
            left, top,
            maxOf(a.x + a.w, b.x + b.w) - left,
            maxOf(a.y + a.h, b.y + b.h) - top,
        )
    }
}
