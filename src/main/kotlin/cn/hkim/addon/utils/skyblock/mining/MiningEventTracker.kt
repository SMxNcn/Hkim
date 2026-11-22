package cn.hkim.addon.utils.skyblock.mining

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.BossBarRenderEvent
import cn.hkim.addon.mixins.accessors.BossHealthOverlayAccessor
import cn.hkim.addon.utils.LegacyTextRun
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.legacy
import cn.hkim.addon.utils.parseLegacyRuns
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.chat.Component
import net.minecraft.world.BossEvent

object MiningEventTracker {
    private val DEFAULT_COLOR = 0xFFFFFFFF.toInt()

    data class MiningEvent(
        val name: String,
        val time: String,
        val area: String?,
        val passive: Boolean,
        val nameColor: Int = DEFAULT_COLOR,
    )

    private val activeRegex = Regex("""^\s*EVENT\s+(.+?)\s+ACTIVE IN\s+(.+?)\s+for\s+(\S+)\s*$""")
    private val passiveRegex = Regex("""^\s*PASSIVE EVENT\s+(.+?)\s+RUNNING FOR\s+(\S+)\s*$""")

    @JvmField
    var hideBossBar = false

    @Volatile
    var current: MiningEvent? = null
        private set

    fun update() {
        val overlay = mc.gui.hud.bossOverlay as? BossHealthOverlayAccessor
        val events = overlay?.events
        if (events.isNullOrEmpty()) {
            current = null
            return
        }

        var found: MiningEvent? = null
        for (event in events.values) {
            val parsed = parse(event.name) ?: continue
            found = parsed
            break
        }
        current = found
    }

    @EventHandler
    private fun onBossBarRender(event: BossBarRenderEvent) {
        if (!hideBossBar) return
        if (isEventBar(event.bar)) event.cancel()
    }

    @JvmStatic
    fun isEventBar(event: BossEvent): Boolean = parse(event.name) != null

    private fun parse(component: Component): MiningEvent? {
        val runs = parseLegacyRuns(component.legacy, DEFAULT_COLOR)
        parseParsed(runs)?.let { return it }

        val plain = component.string.clean
        return parseParsed(listOf(LegacyTextRun(plain, DEFAULT_COLOR, false)))
    }

    private fun parseParsed(runs: List<LegacyTextRun>): MiningEvent? {
        val plain = buildString { runs.forEach { append(it.text) } }

        activeRegex.find(plain)?.let { match ->
            val nameStart = match.groups[1]?.range?.first ?: 0
            return MiningEvent(
                name = match.groupValues[1].trim(),
                time = match.groupValues[3].trim(),
                area = match.groupValues[2].trim(),
                passive = false,
                nameColor = colorAt(runs, nameStart),
            )
        }
        passiveRegex.find(plain)?.let { match ->
            val nameStart = match.groups[1]?.range?.first ?: 0
            return MiningEvent(
                name = match.groupValues[1].trim(),
                time = match.groupValues[2].trim(),
                area = null,
                passive = true,
                nameColor = colorAt(runs, nameStart),
            )
        }
        return null
    }

    private fun colorAt(runs: List<LegacyTextRun>, index: Int): Int {
        var remaining = index
        for (run in runs) {
            if (remaining < run.text.length) return run.color
            remaining -= run.text.length
        }
        return DEFAULT_COLOR
    }
}
