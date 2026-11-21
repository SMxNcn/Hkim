package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.Colors
import cn.hkim.addon.utils.ServerUtils
import cn.hkim.addon.utils.render.island.ChatIslandMessage
import cn.hkim.addon.utils.render.island.IslandContent
import cn.hkim.addon.utils.render.island.IslandHost
import cn.hkim.addon.utils.render.island.IslandMetric
import cn.hkim.addon.utils.render.island.IslandQueue
import cn.hkim.addon.utils.render.island.IslandRenderer
import cn.hkim.addon.utils.render.island.messages.AutopetIslandMessage
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor

@ModuleInfo("dynamic_island", Category.MISC)
object DynamicIsland : Module("Dynamic Island", "Apple-style dynamic island HUD element.") {
    private val backgroundColor by ColorSetting("Background Color", "", 0xB3000000.toInt())
    private val animationDuration by NumberSetting("Animation Duration", "", 150f, 50f, 500f, 10f, "ms")
    private val tabOnIsland by BooleanSetting("Island Tab List", "Morph the tab list.", true)

    private val messages by DropdownSetting("Messages", "Messages shown on the island.")
    private val autopetMessage by BooleanSetting("Autopet Message", "Show Autopet info.", false).depends { messages }

    private val performance by DropdownSetting("Performance", "Performance info on island.")
    private val showTPS by BooleanSetting("Show TPS", "Shows the TPS.", true).depends { performance }
    private val showFPS by BooleanSetting("Show FPS", "Shows the FPS.", true).depends { performance }
    private val showPing by BooleanSetting("Show Ping", "Shows current ping.", true).depends { performance }
    private val lagThreshold by NumberSetting("Lag Threshold", "Shows lag warning when TPS drops below this value.", 10f, 5f, 15f, 1f,).depends { performance }

    private val autopetRule = AutopetIslandMessage { autopetMessage }
    private val chatMessages: List<ChatIslandMessage> = listOf(autopetRule)

    private const val RADIUS = 8f
    private const val LAG_WARNING = "⚠ Lag Detected"

    private val homeContent: IslandContent
        get() = IslandContent(
            key = "island_home",
            priority = Int.MIN_VALUE,
            title = "Hkim",
            detail = "",
            icon = "assets/hkim/textures/clickgui/icon.svg",
            iconColor = 0xFFFFFFFF.toInt(),
            titleColor = 0xFFFFFFFF.toInt(),
            inline = islandMetrics(),
        )

    private fun islandMetrics(): List<IslandMetric> {
        val labelColor = ClickGUI.getGuiColor()
        val metrics = ArrayList<IslandMetric>(3)

        if (showTPS) metrics += tpsMetric(labelColor)
        if (ServerUtils.averageTps < lagThreshold) {
            metrics += IslandMetric(LAG_WARNING, "", LAG_WARNING, Colors.MINECRAFT_RED.rgb, labelColor)
        } else {
            if (showFPS) metrics += intMetric(mc.fps, "FPS", labelColor)
            if (showPing && !mc.hasSingleplayerServer()) metrics += intMetric(ServerUtils.averagePing, "ms", labelColor)
        }
        return metrics
    }

    private fun tpsMetric(labelColor: Int): IslandMetric {
        val text = "%.1f".format(ServerUtils.averageTps)
        val slot = "9".repeat(text.substringBefore('.').length) + ".9"
        return IslandMetric(text, "TPS", slot, Colors.WHITE.rgb, labelColor)
    }

    private fun intMetric(value: Int, label: String, labelColor: Int): IslandMetric {
        val text = "$value"
        return IslandMetric(text, label, "9".repeat(text.length), Colors.WHITE.rgb, labelColor)
    }

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        if (!enabled) return
        for (rule in chatMessages) {
            if (rule.handle(event)) return
        }
    }

    override fun render(graphics: GuiGraphicsExtractor, tickTracker: DeltaTracker) {
        if (!enabled) return
        if (!canRenderTabOnIsland && IslandHost.listShowing) return

        IslandRenderer.render(graphics, RADIUS, backgroundColor, animationDurationMs, homeContent)
        super.render(graphics, tickTracker)
    }

    fun renderStandalone(graphics: GuiGraphicsExtractor) {
        if (enabled || IslandHost.listShowing) return
        IslandRenderer.render(graphics, RADIUS, backgroundColor, animationDurationMs)
    }

    val animationDurationMs: Long get() = animationDuration.toLong()

    val canRenderTabOnIsland: Boolean get() = enabled && tabOnIsland

    override fun onDisable() = IslandQueue.clear()
}
