package cn.hkim.addon.utils.render.island

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.hud.Bounds
import cn.hkim.addon.utils.notification.NOTIFICATION_DETAIL_COLOR
import cn.hkim.addon.utils.notification.NOTIFICATION_TITLE_COLOR
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRect
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoImage
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoBatch
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoTextHeight
import cn.hkim.addon.utils.render.skiko.SkikoDraw.skikoTextWidth
import net.minecraft.client.gui.GuiGraphicsExtractor

object IslandRenderer {
    private const val FALLBACK_TOP_OFFSET = 4f
    private const val BOSS_BAR_GAP = 4f

    private const val PADDING_X = 7f
    private const val PADDING_Y = 4f
    private const val ICON_SIZE = 13f

    private const val COLLAPSED_WIDTH = 28f
    private const val COLLAPSED_HEIGHT = 2f

    private var animationDurationMs = 150L

    private val widthTween = Tween(Easing.CUBIC_OUT) { animationDurationMs }
    private val heightTween = Tween(Easing.CUBIC_OUT) { animationDurationMs }
    private val visibleTween = Tween(Easing.CUBIC_OUT) { animationDurationMs }
    private val anchorYTween = Tween(Easing.CUBIC_OUT) { animationDurationMs }

    private var lastContent: IslandContent? = null

    private val titleLine = IslandLine(Tween(Easing.QUAD_OUT) { animationDurationMs })
    private val detailLine = IslandLine(Tween(Easing.QUAD_OUT) { animationDurationMs })
    private val iconLine = IslandLine(Tween(Easing.QUAD_OUT) { animationDurationMs })

    private var lastTickMs = 0L
    private var radius = 8f
    private var background = 0

    init {
        anchorYTween.snap(FALLBACK_TOP_OFFSET)
    }

    fun showMessage(title: String, detail: String, centered: Boolean = false, durationMs: Long = 500L, priority: Int = IslandQueue.PRIORITY_DEFAULT, key: Any? = null) {
        IslandQueue.show(
            IslandContent(
                key = key ?: MessageKey(title, detail, centered),
                priority = priority,
                title = title,
                detail = detail,
                centered = centered,
                durationMs = durationMs,
            )
        )
    }

    fun clearMessage(key: Any) = IslandQueue.remove(key)

    private data class MessageKey(val title: String, val detail: String, val centered: Boolean)

    fun anchor(now: Long = System.currentTimeMillis()): IslandAnchor {
        val geometry = geometry(now, visibleTween.get(now))
        return IslandAnchor(Bounds(geometry.x, geometry.y, geometry.width, geometry.height), geometry.radius, background)
    }

    fun render(graphics: GuiGraphicsExtractor, radius: Float, background: Int, animationDurationMs: Long) {
        this.radius = radius
        this.background = background
        this.animationDurationMs = animationDurationMs

        val now = System.currentTimeMillis()
        val delta = if (lastTickMs == 0L) 0L else (now - lastTickMs).coerceIn(0L, 250L)
        lastTickMs = now

        if (IslandHost.takenOver) {
            collapse()
            return
        }
        if (mc.gui.hud.isHidden) return

        val content = IslandQueue.tick(delta)
        if (content != null) lastContent = content

        visibleTween.target(if (content != null) 1f else 0f, now)
        val visible = visibleTween.get(now)
        if (content == null && visible <= 0.001f) {
            collapse()
            return
        }

        if (content != null) {
            val hasIcon = content.icon.isNotEmpty() && !content.centered
            val (targetWidth, targetHeight) = measureMessage(content.title, content.detail, hasIcon)
            if (visible >= 0.999f) {
                widthTween.target(targetWidth, now)
                heightTween.target(targetHeight, now)
            } else {
                widthTween.snap(targetWidth)
                heightTween.snap(targetHeight)
            }
        }

        val shown = content ?: lastContent ?: return
        val geometry = geometry(now, visible)
        if (geometry.fade <= 0.001f) return

        val alphas = contentAlphas(now, shown)
        val reveal = geometry.reveal

        drawBox(
            graphics, shown,
            geometry.x, geometry.y, geometry.width, geometry.height,
            geometry.radius, withAlpha(background, geometry.fade),
            centeredX(geometry.cardWidth),
            alphas.title * reveal, alphas.detail * reveal, alphas.icon * reveal,
        )
    }

    private fun collapse() {
        titleLine.reset()
        detailLine.reset()
        iconLine.reset()
        widthTween.snap(COLLAPSED_WIDTH)
        heightTween.snap(COLLAPSED_HEIGHT)
        visibleTween.snap(0f)
        lastContent = null
    }

    private class Geometry(
        val x: Float, val y: Float,
        val width: Float, val height: Float,
        val radius: Float,
        val fade: Float,
        val reveal: Float,
        val cardWidth: Float,
    )

    private fun geometry(now: Long, visible: Float): Geometry {
        val cardWidth = maxOf(widthTween.get(now), COLLAPSED_WIDTH)
        val cardHeight = maxOf(heightTween.get(now), COLLAPSED_HEIGHT)

        val progress = ease(visible)
        val width = COLLAPSED_WIDTH + (cardWidth - COLLAPSED_WIDTH) * progress
        val height = COLLAPSED_HEIGHT + (cardHeight - COLLAPSED_HEIGHT) * progress
        val textSpace = (cardHeight - PADDING_Y * 2f).coerceAtLeast(1f)

        return Geometry(
            centeredX(width),
            anchorY(now),
            width,
            height,
            radius.coerceIn(0f, minOf(width, height) / 2f),
            (visible / 0.2f).coerceIn(0f, 1f),
            ((height - PADDING_Y * 2f) / textSpace).coerceIn(0f, 1f),
            cardWidth,
        )
    }

    private fun anchorY(now: Long): Float {
        val bossBarBottom = BossBarLayout.bottomY()
        anchorYTween.target(if (bossBarBottom > 0f) bossBarBottom + BOSS_BAR_GAP else FALLBACK_TOP_OFFSET, now)
        return anchorYTween.get(now)
    }

    private fun ease(progress: Float) = GuiAnimation.applyEasing(progress, Easing.CUBIC_OUT)

    private class Alphas(val title: Float, val detail: Float, val icon: Float)

    private fun contentAlphas(now: Long, content: IslandContent) = Alphas(
        titleLine.alpha(now, content.title),
        detailLine.alpha(now, content.detail),
        iconLine.alpha(now, content.icon to content.iconColor),
    )

    private fun drawBox(
        graphics: GuiGraphicsExtractor,
        content: IslandContent?,
        x: Float, y: Float, width: Float, height: Float,
        radius: Float, background: Int,
        contentX: Float,
        titleAlpha: Float, detailAlpha: Float, iconAlpha: Float,
    ) {
        val clip = Bounds(x, y, width, height)
        graphics.skikoBatch(x, y, width, height, clipRadius = radius) {
            graphics.drawRoundedRect(x, y, width, height, background, radius)

            if (content != null) {
                val titleColor = content.titleColor ?: NOTIFICATION_TITLE_COLOR
                val detailColor = content.detailColor ?: NOTIFICATION_DETAIL_COLOR
                drawMessage(graphics, content.title, content.detail, content.icon, content.iconColor, titleColor, detailColor, content.centered, contentX, y, iconAlpha, titleAlpha, detailAlpha, clip)
            }
        }
    }

    private fun measureMessage(title: String, detail: String, hasIcon: Boolean): Pair<Float, Float> {
        val detailSize = mc.font.lineHeight.toFloat()
        val titleSize = detailSize - 1f
        val textWidth = maxOf(
            skikoTextWidth(title, titleSize, bold = true),
            skikoTextWidth(detail, detailSize),
        )
        val iconWidth = if (hasIcon) ICON_SIZE + 5f else 0f
        val height = PADDING_Y * 2f + skikoTextHeight(titleSize) + 1f + skikoTextHeight(detailSize)
        return Pair(PADDING_X * 2f + iconWidth + textWidth, height)
    }

    private fun drawMessage(
        graphics: GuiGraphicsExtractor,
        title: String, detail: String,
        icon: String, iconColor: Int,
        titleColor: Int, detailColor: Int,
        centered: Boolean,
        x: Float, y: Float,
        iconAlpha: Float, titleAlpha: Float, detailAlpha: Float,
        clip: Bounds,
    ) {
        if (titleAlpha <= 0.01f && detailAlpha <= 0.01f && iconAlpha <= 0.01f) return

        val detailSize = mc.font.lineHeight.toFloat()
        val titleSize = detailSize - 1f
        val titleHeight = skikoTextHeight(titleSize)
        val hasIcon = icon.isNotEmpty() && !centered

        if (hasIcon && iconAlpha > 0.01f && clip.h >= ICON_SIZE) {
            graphics.drawSkikoImage(icon, x + PADDING_X, y + (clip.h - ICON_SIZE) / 2f, ICON_SIZE, ICON_SIZE, 0f, tintColor = withAlpha(iconColor, iconAlpha),)
        }

        val centerX = clip.x + clip.w / 2f
        val left = if (hasIcon) x + PADDING_X + ICON_SIZE + 5f else x + PADDING_X
        val titleX = if (centered) centerX - skikoTextWidth(title, titleSize, bold = true) / 2f else left
        val detailX = if (centered) centerX - skikoTextWidth(detail, detailSize) / 2f else left

        graphics.drawSkikoText(title, titleX, y + PADDING_Y, titleSize, withAlpha(titleColor, titleAlpha), bold = true)
        graphics.drawSkikoText(
            detail,
            detailX, y + PADDING_Y + titleHeight + 1f,
            detailSize, withAlpha(detailColor, detailAlpha),
        )
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        (color and 0x00FFFFFF) or ((((color ushr 24) * alpha).toInt().coerceIn(0, 255)) shl 24)

    private fun centeredX(width: Float) = (mc.window.guiScaledWidth - width) / 2f
}

private class IslandLine(private val tween: Tween) {
    private var key: Any? = null
    private var hasKey = false

    fun alpha(now: Long, contentKey: Any?): Float {
        if (!hasKey || contentKey != key) {
            hasKey = true
            key = contentKey
            tween.snap(0f)
        }
        tween.target(1f, now)
        return tween.get(now)
    }

    fun reset() = tween.snap(0f)
}
