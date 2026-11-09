package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.gui.screen.HudEditScreen
import cn.hkim.addon.hud.HudAlignment
import cn.hkim.addon.hud.HudElement
import cn.hkim.addon.utils.skyblock.rng.*
import cn.hkim.addon.utils.skyblock.rng.RngMeterStorage.MeterData
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

@ModuleInfo("rng_meter", Category.SKYBLOCK)
object RngMeter : Module("RNG Meter", "Shows the current RNG Meter progress.") {
    private val showBackground by BooleanSetting("Background", "Draw a HUD background.", true)
    private val backgroundColor by ColorSetting("Background Color", "HUD background color.", 0x80000000.toInt()).depends { showBackground }
    private val daemonLevel by NumberSetting("Daemon Level", "Your 'Daemon' Shard level.", 0f, 0f, 10f, 1f)
    private val compactNumber by BooleanSetting("Compact Number", "Compact values above 10M.", false)

    private val daemonMultiplier: Double get() = 1.0 + daemonLevel * 0.01

    private val hud by HudElement("RNG Meter", "Shows the current dungeon or slayer RNG Meter progress.",
        x = 10f, y = 100f, alignment = HudAlignment.TOP_LEFT
    ) { graphics ->
        renderHud(graphics)
    }

    private val editorMeter = MeterData(
        score = 42_069L,
        needed = 231_599L,
        item = "§6Necron's Handle",
    )

    private var scanCountdown = -1

    private fun renderHud(graphics: GuiGraphicsExtractor): Pair<Float, Float> {
        val key: String
        val data: MeterData

        if (mc.gui.screen() is HudEditScreen) {
            key = "M7"
            data = editorMeter
        } else {
            key = RngUtils.currentMeterKey() ?: return 0f to 0f
            data = RngMeterStorage.get(key)
        }

        val lines = buildLines(key, data)

        val padding = 2
        val lineHeight = mc.font.lineHeight
        val width = lines.maxOf { mc.font.width(it) } + padding * 2
        val height = lines.size * lineHeight + padding * 2

        if (showBackground) {
            graphics.fill(0, 0, width, height, backgroundColor)
        }

        var y = padding
        for (line in lines) {
            graphics.text(mc.font, line, padding, y, 0xFFFFFFFF.toInt(), false)
            y += lineHeight
        }

        return width.toFloat() to height.toFloat()
    }

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        if (!enabled) return
        RngMeterChatParser.handle(event, daemonMultiplier)
    }

    @EventHandler
    private fun onWorldUnload(event: WorldEvent.Unload) {
        RngMeterChatParser.reset()
    }

    @EventHandler
    private fun onGuiOpen(event: GuiEvent.Open) {
        if (!enabled) return
        val title = event.screen.title.string
        scanCountdown = if (title.contains("RNG Meter", ignoreCase = true)) 3 else -1
    }

    @EventHandler
    private fun onGuiClose(event: GuiEvent.Close) {
        scanCountdown = -1
    }

    @EventHandler
    private fun onSlotUpdate(event: GuiEvent.SlotUpdate) {
        if (!enabled) return
        val title = event.screen.title.string
        if (title.contains("RNG Meter", ignoreCase = true)) {
            RngMeterInventoryParser.handle(title, event.menu)
        }
    }

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        if (!enabled || scanCountdown < 0) return

        val screen = mc.gui.screen() as? AbstractContainerScreen<*> ?: run {
            scanCountdown = -1
            return
        }

        if (scanCountdown-- > 0) return
        scanCountdown = -1

        val title = screen.title.string
        if (title.contains("RNG Meter", ignoreCase = true)) {
            RngMeterInventoryParser.handle(title, screen.menu)
        }
    }

    private fun buildLines(key: String, data: MeterData): List<String> {
        val keyColor = when {
            key.startsWith("M") -> "§c"
            key == RngUtils.GLACITE_KEY -> "§b"
            else -> "§a"
        }
        val percentage = RngMeterFormatter.formatPercentage(data.percentage)
        val format: (Long) -> String = if (compactNumber) {
            { RngMeterFormatter.formatCompactNumber(it) }
        } else {
            { RngMeterFormatter.formatNumber(it) }
        }

        val lines = mutableListOf("§dRNG Meter §8- $keyColor$key §8- §d$percentage%")

        when {
            data.score > 0 && data.needed <= 0 -> {
                lines += "§7Stored Score: §d${format(data.score)}"
            }

            data.needed > 0 -> {
                lines += "§7Item: ${data.item ?: "§cNone"}"
                lines += RngMeterFormatter.formatBar(data.score, data.needed, compactNumber)
            }

            else -> lines += "§cNo RNG Meter Data!"
        }

        return lines
    }
}
