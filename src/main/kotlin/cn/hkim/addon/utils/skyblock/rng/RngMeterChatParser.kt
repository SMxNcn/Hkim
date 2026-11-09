package cn.hkim.addon.utils.skyblock.rng

import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.utils.legacy
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.slayer.SlayerUtils

object RngMeterChatParser {
    private val storedXpRegex = Regex("""RNG Meter\s*-\s*([\d,.]+[kKmM]?)\s*Stored XP""", RegexOption.IGNORE_CASE)
    private val teamScoreRegex = Regex("""Team Score:\s*(\d+)\s*\((S\+?)\)""")
    private val corpseLootRegex = Regex("""^ {2}(LAPIS|UMBER|TUNGSTEN|VANGUARD) CORPSE LOOT! *$""", RegexOption.IGNORE_CASE)
    private val resetRegex = Regex("""RNG METER!\s*Reselected the (.+?) for (.+?)!""", RegexOption.IGNORE_CASE)
    private val deselectRegex = Regex("""You reset your selected drop for your (.+?) RNG Meter!""", RegexOption.IGNORE_CASE)
    private val setRegex = Regex("""You set your (.+?) RNG Meter to drop (.+?)!""", RegexOption.IGNORE_CASE)
    private val legacyItemRegex = Regex("""to drop (.+?)!""")

    private val corpseLootScores = mapOf(
        "LAPIS" to 500L,
        "UMBER" to 2_500L,
        "TUNGSTEN" to 2_500L,
        "VANGUARD" to 25_000L,
    )

    private var scoreAdded = false

    fun reset() {
        scoreAdded = false
    }

    fun handle(event: ChatReceiveEvent, daemonMultiplier: Double = 1.0) {
        if (!LocationUtils.inSkyBlock) return

        val message = event.message
        val legacyMessage = event.component.legacy

        corpseLootRegex.find(message)?.let { match ->
            if (!legacyMessage.contains("§l")) return@let
            val base = corpseLootScores[match.groupValues[1].uppercase()] ?: return@let
            val gained = (base * daemonMultiplier).toLong()
            RngMeterStorage.addScore(RngUtils.GLACITE_KEY, gained)
            return
        }

        storedXpRegex.find(message)?.let { match ->
            val newScore = RngUtils.parseAmount(match.groupValues[1]) ?: return@let
            val slayer = SlayerUtils.currentType() ?: return@let
            val key = slayer.meterKey

            val data = RngMeterStorage.get(key)
            val oldScore = data.score
            RngMeterStorage.setScore(key, newScore)

            when {
                oldScore <= 0L -> Unit
                newScore > oldScore -> sendGain(newScore - oldScore, newScore)
                newScore < oldScore -> {
                    val percentage = if (data.needed > 0L) {
                        oldScore.toDouble() / data.needed * 100.0
                    } else {
                        0.0
                    }
                    sendReset(oldScore, percentage)
                }
            }
            return
        }

        if (!scoreAdded) {
            teamScoreRegex.find(message)?.let { match ->
                val key = RngUtils.currentDungeonKey() ?: return@let
                val rank = match.groupValues[2]
                if (rank != "S" && rank != "S+") return@let

                var gained = match.groupValues[1].toLongOrNull() ?: return@let
                if (rank == "S") gained = (gained * 0.7).toLong()
                gained = (gained * daemonMultiplier).toLong()

                val data = RngMeterStorage.get(key)
                // Bloom: reaching the goal makes the item drop on the next run.
                if (data.needed > 0L && data.score >= data.needed && !data.item.isNullOrEmpty()) {
                    val percentage = data.score.toDouble() / data.needed * 100.0
                    sendReset(data.score, percentage)
                    RngMeterStorage.setScore(key, data.score - data.needed)
                    RngMeterStorage.setItem(key, null)
                }

                RngMeterStorage.addScore(key, gained)
                scoreAdded = true
                return
            }
        }

        resetRegex.find(message)?.let { match ->
            val key = RngUtils.resolveMeterKey(match.groupValues[2]) ?: return@let
            val data = RngMeterStorage.get(key)

            if (data.score > 0L) {
                val percentage = if (data.needed > 0L) {
                    data.score.toDouble() / data.needed * 100.0
                } else {
                    0.0
                }
                sendReset(data.score, percentage)
            }

            RngMeterStorage.setScore(key, 0L)
            return
        }

        deselectRegex.find(message)?.let { match ->
            val key = RngUtils.resolveMeterKey(match.groupValues[1]) ?: return@let
            RngMeterStorage.setItem(key, null)
            return
        }

        setRegex.find(message)?.let { match ->
            val key = RngUtils.resolveMeterKey(match.groupValues[1]) ?: return@let
            val item = legacyItemRegex.find(legacyMessage)?.groupValues?.get(1)
                ?.trimTrailingFormatting()
                ?.takeIf { it.isNotEmpty() }
                ?: match.groupValues[2].trim()

            RngMeterStorage.setItem(key, item)
            return
        }
    }

    private fun String.trimTrailingFormatting(): String {
        var result = trim()
        while (result.endsWith("§a") || result.endsWith("§r")) {
            result = result.dropLast(2).trim()
        }
        if (result.startsWith("§r")) result = result.drop(2).trim()
        return result
    }

    private fun sendGain(gained: Long, currentScore: Long) {
        modMessage(
            "§dRNG Meter §7gained §6${RngMeterFormatter.formatNumber(gained)} §7XP! " +
                "(§6${RngMeterFormatter.formatNumber(currentScore)} §bScore§7)",
        )
    }

    private fun sendReset(dropScore: Long, percentage: Double) {
        modMessage(
            "§dRng Item §7reset! (§6${RngMeterFormatter.formatNumber(dropScore)} §bScore, " +
                "§6${RngMeterFormatter.formatPercentage(percentage)}§b%§7)",
        )
    }
}
