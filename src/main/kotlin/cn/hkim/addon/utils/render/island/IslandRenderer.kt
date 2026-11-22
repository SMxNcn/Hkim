package cn.hkim.addon.utils.render.island

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.hud.Bounds
import cn.hkim.addon.runtime.BackdropBlur
import cn.hkim.addon.utils.LegacyTextRun
import cn.hkim.addon.utils.notification.NOTIFICATION_DETAIL_COLOR
import cn.hkim.addon.utils.notification.NOTIFICATION_TITLE_COLOR
import cn.hkim.addon.utils.parseLegacyRuns
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawBackdropBlur
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithShadow
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
    private const val ICON_SIZE = 12f

    private const val INLINE_GAP = 6f
    private const val INLINE_LABEL_GAP = 2f
    private const val INLINE_PART_GAP = 4f

    private const val COLLAPSED_WIDTH = 28f
    private const val COLLAPSED_HEIGHT = 16f

    private const val SOFT_EDGE_FEATHER = 1f
    private const val SOFT_EDGE_PAD = SOFT_EDGE_FEATHER * 3f

    private var animationDurationMs = 150L

    private val widthTween = Tween(Easing.CUBIC_OUT) { animationDurationMs }
    private val heightTween = Tween(Easing.CUBIC_OUT) { animationDurationMs }
    private val visibleTween = Tween(Easing.CUBIC_OUT) { animationDurationMs }
    private val anchorYTween = Tween(Easing.CUBIC_OUT) { animationDurationMs }

    private var lastContent: IslandContent? = null

    private var restWidth = COLLAPSED_WIDTH
    private var restHeight = COLLAPSED_HEIGHT

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

    fun render(
        graphics: GuiGraphicsExtractor,
        radius: Float,
        background: Int,
        animationDurationMs: Long,
        homeContent: IslandContent? = null,
        blurRadius: Float = 0f,
    ) {
        this.radius = radius
        this.background = background
        this.animationDurationMs = animationDurationMs

        val now = System.currentTimeMillis()
        val delta = if (lastTickMs == 0L) 0L else (now - lastTickMs).coerceIn(0L, 250L)
        lastTickMs = now

        if (IslandHost.takenOver) {
            collapse(homeContent)
            return
        }
        if (mc.gui.hud.isHidden) return

        val content = IslandQueue.tick(delta) ?: homeContent
        if (content != null) lastContent = content

        visibleTween.target(if (content != null) 1f else 0f, now)
        val visible = visibleTween.get(now)
        if (content == null && visible <= 0.001f) {
            collapse()
            return
        }

        if (content != null) {
            if (visible <= 0.001f && homeContent != null) {
                val (restW, restH) = measureContent(homeContent)
                restWidth = restW
                restHeight = restH
            }
            val (targetWidth, targetHeight) = measureContent(content)
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
            blurRadius,
        )
    }

    private fun collapse(homeContent: IslandContent? = null) {
        titleLine.reset()
        detailLine.reset()
        iconLine.reset()
        if (homeContent != null) {
            val (width, height) = measureContent(homeContent)
            restWidth = width
            restHeight = height
        } else {
            restWidth = COLLAPSED_WIDTH
            restHeight = COLLAPSED_HEIGHT
        }
        widthTween.snap(restWidth)
        heightTween.snap(restHeight)
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
        val width = restWidth + (cardWidth - restWidth) * progress
        val height = restHeight + (cardHeight - restHeight) * progress
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
        blurRadius: Float,
    ) {
        if (blurRadius > 0f) {
            graphics.drawBackdropBlur(
                x, y, width, height,
                BackdropBlur(radius = blurRadius, cornerRadius = radius),
            )
        }
        val clip = Bounds(x, y, width, height)
        graphics.skikoBatch(
            x - SOFT_EDGE_PAD, y - SOFT_EDGE_PAD,
            width + SOFT_EDGE_PAD * 2f, height + SOFT_EDGE_PAD * 2f,
            clipRadius = radius + SOFT_EDGE_PAD,
        ) {
            graphics.drawRoundedRectWithShadow(
                x, y, width, height,
                background, 0, 0f, radius,
                background, SOFT_EDGE_FEATHER, 0f,
            )

            if (content != null) {
                val titleColor = content.titleColor ?: NOTIFICATION_TITLE_COLOR
                val detailColor = content.detailColor ?: NOTIFICATION_DETAIL_COLOR
                drawMessage(graphics, content.title, content.detail, content.icon, content.iconColor, titleColor, detailColor, content.centered, content.inline, contentX, y, iconAlpha, titleAlpha, detailAlpha, clip)
            }
        }
    }

    private fun measureContent(content: IslandContent): Pair<Float, Float> {
        val hasIcon = content.icon.isNotEmpty() && !content.centered
        return measureMessage(content.title, content.detail, hasIcon, content.inline)
    }

    private fun measureMessage(
        title: String, detail: String, hasIcon: Boolean,
        inline: List<IslandMetric> = emptyList(),
    ): Pair<Float, Float> {
        val detailSize = mc.font.lineHeight.toFloat()
        val titleSize = detailSize - 1f
        val titleWidth = skikoTextWidth(title, titleSize, bold = true) + inlineMetricsWidth(inline, titleSize)
        val textWidth = maxOf(
            titleWidth,
            runsWidth(parseLegacyRuns(detail, NOTIFICATION_DETAIL_COLOR), detailSize),
        )
        val iconWidth = if (hasIcon) ICON_SIZE + 5f else 0f
        val height = if (detail.isEmpty()) {
            COLLAPSED_HEIGHT
        } else {
            PADDING_Y * 2f + skikoTextHeight(titleSize) + 1f + skikoTextHeight(detailSize)
        }
        return Pair(PADDING_X * 2f + iconWidth + textWidth, height)
    }

    private fun inlineMetricsWidth(metrics: List<IslandMetric>, size: Float): Float {
        if (metrics.isEmpty()) return 0f
        var width = INLINE_GAP
        for (metric in metrics) {
            width += skikoTextWidth(metric.slot, size, metric.valueBold) + inlineLabelWidth(metric, size) + INLINE_PART_GAP
        }
        return width - INLINE_PART_GAP
    }

    private fun inlineLabelWidth(metric: IslandMetric, size: Float): Float =
        if (metric.label.isEmpty()) 0f
        else INLINE_LABEL_GAP + runsWidth(parseLegacyRuns(metric.label, metric.labelColor), size)

    private fun drawMessage(
        graphics: GuiGraphicsExtractor,
        title: String, detail: String,
        icon: String, iconColor: Int,
        titleColor: Int, detailColor: Int,
        centered: Boolean,
        inline: List<IslandMetric>,
        x: Float, y: Float,
        iconAlpha: Float, titleAlpha: Float, detailAlpha: Float,
        clip: Bounds,
    ) {
        if (titleAlpha <= 0.01f && detailAlpha <= 0.01f && iconAlpha <= 0.01f) return

        val detailSize = mc.font.lineHeight.toFloat()
        val titleSize = detailSize - 1f
        val titleHeight = skikoTextHeight(titleSize)
        val hasIcon = icon.isNotEmpty() && !centered
        val iconSize = ICON_SIZE

        if (hasIcon && iconAlpha > 0.01f && clip.h >= iconSize) {
            graphics.drawSkikoImage(icon, x + PADDING_X, y + (clip.h - iconSize) / 2f, iconSize, iconSize, 0f, tintColor = withAlpha(iconColor, iconAlpha),)
        }

        val centerX = clip.x + clip.w / 2f
        val left = if (hasIcon) x + PADDING_X + iconSize + 5f else x + PADDING_X
        val titleX = if (centered) centerX - skikoTextWidth(title, titleSize, bold = true) / 2f else left
        val detailRuns = parseLegacyRuns(detail, detailColor)
        val detailX = if (centered) centerX - runsWidth(detailRuns, detailSize) / 2f else left

        val titleY = if (detail.isEmpty()) y + (clip.h - titleHeight) / 2f else y + PADDING_Y
        graphics.drawSkikoText(title, titleX, titleY, titleSize, withAlpha(titleColor, titleAlpha), bold = true)

        if (!centered && inline.isNotEmpty()) {
            drawInlineMetrics(graphics, inline, titleX + skikoTextWidth(title, titleSize, bold = true) + INLINE_GAP, titleY, titleSize, titleAlpha)
        }

        var cursorX = detailX
        val detailY = y + PADDING_Y + titleHeight + 1f
        for (run in detailRuns) {
            if (run.text.isEmpty()) continue
            graphics.drawSkikoText(
                run.text,
                cursorX, detailY,
                detailSize, withAlpha(run.color, detailAlpha),
                bold = run.bold,
            )
            cursorX += skikoTextWidth(run.text, detailSize, run.bold)
        }
    }

    private fun drawInlineMetrics(
        graphics: GuiGraphicsExtractor,
        metrics: List<IslandMetric>,
        x: Float, y: Float,
        size: Float, alpha: Float,
    ) {
        if (alpha <= 0.01f) return
        var cursorX = x
        for (metric in metrics) {
            val slotWidth = skikoTextWidth(metric.slot, size, metric.valueBold)
            val valueWidth = skikoTextWidth(metric.value, size, metric.valueBold)
            graphics.drawSkikoText(
                metric.value, cursorX + slotWidth - valueWidth, y, size,
                withAlpha(metric.valueColor, alpha), bold = metric.valueBold,
            )
            if (metric.label.isNotEmpty()) {
                var labelX = cursorX + slotWidth + INLINE_LABEL_GAP
                for (run in parseLegacyRuns(metric.label, metric.labelColor)) {
                    if (run.text.isEmpty()) continue
                    graphics.drawSkikoText(
                        run.text, labelX, y, size,
                        withAlpha(run.color, alpha), bold = run.bold,
                    )
                    labelX += skikoTextWidth(run.text, size, run.bold)
                }
            }
            cursorX += slotWidth + inlineLabelWidth(metric, size) + INLINE_PART_GAP
        }
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        (color and 0x00FFFFFF) or ((((color ushr 24) * alpha).toInt().coerceIn(0, 255)) shl 24)

    private fun centeredX(width: Float) = (mc.window.guiScaledWidth - width) / 2f

    private fun runsWidth(runs: List<LegacyTextRun>, size: Float): Float {
        var width = 0f
        for (run in runs) width += skikoTextWidth(run.text, size, run.bold)
        return width
    }
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
