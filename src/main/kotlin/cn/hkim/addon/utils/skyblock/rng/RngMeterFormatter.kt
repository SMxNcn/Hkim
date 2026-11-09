package cn.hkim.addon.utils.skyblock.rng

import java.util.Locale

object RngMeterFormatter {
    fun formatNumber(value: Long): String = String.format(Locale.US, "%,d", value)

    fun formatCompactNumber(value: Long): String = when {
        value > 10_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
        else -> formatNumber(value)
    }

    fun formatPercentage(value: Double): String = String.format(Locale.US, "%.2f", value)

    fun formatBar(score: Long, needed: Long, compact: Boolean = false): String {
        val filled = if (needed <= 0L) {
            0
        } else {
            ((score.toDouble() / needed).coerceIn(0.0, 1.0) * 15).toInt()
        }

        val scoreText = if (compact) formatCompactNumber(score) else formatNumber(score)
        val neededText = if (compact) formatCompactNumber(needed) else formatNumber(needed)

        return "§d$scoreText " +
            "§a§m§l${" ".repeat(filled)}" +
            "§7§m§l${" ".repeat(15 - filled)}" +
            "§r §d$neededText"
    }
}
