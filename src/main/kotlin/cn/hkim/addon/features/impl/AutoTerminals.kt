package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.*
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.cleanString
import cn.hkim.addon.utils.randomDelay
import cn.hkim.addon.utils.skyblock.LocationUtils.inDungeons
import cn.hkim.addon.utils.skyblock.M7Phases
import cn.hkim.addon.utils.skyblock.dungeon.*
import cn.hkim.addon.utils.skyblock.getF7Phase
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot

@ModuleInfo("auto_terminals", Category.SKYBLOCK, false)
object AutoTerminals : Module("Auto Terminals", "Automatically solve terminals in Floor 7.") {
    private val firstClickDelay by NumberSetting("First Click Delay", "Delay before the first click after opening a terminal.", 500f, 0f, 1000f, 10f, "ms")
    private val clickDelay by NumberSetting("Click Delay", "Delay between each click.", 200f, 150f, 500f, 10f, "ms")
    private val melodySkip by BooleanSetting("Melody Skip", "Skip melody at edge.", true).depends { melody }
    private val melodySkipDelay by NumberSetting("Melody Skip Delay", "Delay for melody skip.", 100f, 100f, 150f, 5f, "ms").depends { melody && melodySkip }

    private val terminals by DropdownSetting("Terminals", "Select which terminal types to auto-complete.")
    private val panes by BooleanSetting("Panes", "Solve Panes terminal.", true).depends { terminals }
    private val rubix by BooleanSetting("Rubix", "Solve Rubix terminal.", false).depends { terminals }
    private val numbers by BooleanSetting("Numbers", "Solve Numbers terminal.", true).depends { terminals }
    private val startsWith by BooleanSetting("Starts With", "Solve Starts With terminal.", false).depends { terminals }
    private val select by BooleanSetting("Select All", "Solve Select All terminal.", false).depends { terminals }
    private val melody by BooleanSetting("Melody", "Solve Melody terminal.", false).depends { terminals }

    private var handler: TerminalHandler? = null
    private var terminalTitle: String? = null
    private var lastClickTime = 0L
    private var melodySkipClick = -1

    private const val SOLVE_DELAY = 250L
    private const val RESOLVE_TIMEOUT = 600L

    @EventHandler
    private fun onPacket(event: PacketReceiveEvent) {
        if (!enabled || !inDungeons || getF7Phase() != M7Phases.P3) return
        when (val packet = event.packet) {
            is ClientboundOpenScreenPacket -> {
                val title = packet.title.cleanString
                val type = TerminalEnums.entries.firstOrNull { it.regex.matches(title) } ?: return
                if (!isEnabled(type)) return
                handler = createHandler(type, title) ?: return
                terminalTitle = title
                lastClickTime = 0L
            }

            is ClientboundContainerClosePacket -> reset()
        }
    }

    @EventHandler
    private fun onPacketSend(event: PacketSendEvent) {
        if (!enabled || getF7Phase() != M7Phases.P3) return
        if (event.packet is ServerboundContainerClosePacket) reset()
    }

    @EventHandler
    private fun onGuiClose(event: GuiEvent.Close) {
        if (!enabled || getF7Phase() != M7Phases.P3 || handler == null) return
        if (event.screen.title.cleanString == terminalTitle) reset()
    }

    @EventHandler
    private fun onSlotUpdate(event: GuiEvent.SlotUpdate) {
        val handler = handler ?: return
        if (event.screen.title.cleanString != terminalTitle) return
        handler.updateSlots(containerSlots(event.menu.slots))
        handler.resetClicked()
    }

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        if (!inDungeons || getF7Phase() != M7Phases.P3) {
            if (handler != null) reset()
            return
        }
        val handler = handler ?: return
        val screen = mc.screen as? AbstractContainerScreen<*> ?: return
        if (screen.title.cleanString != terminalTitle) return

        val now = System.currentTimeMillis()
        val slots = containerSlots(screen.menu.slots)

        if (!handler.hasSolved && now - handler.timeOpened >= SOLVE_DELAY) {
            handler.updateSlots(slots)
        }

        if (now - handler.timeOpened < firstClickDelay) return

        handleClickTimeout(handler, slots, now)

        val interval = if (melodySkip && melodySkipClick != -1) melodySkipDelay.toLong() else randomDelay(clickDelay.toInt(), 50)
        if (now - lastClickTime < interval) return

        attemptClick(handler, screen, slots, now)
    }

    private fun handleClickTimeout(handler: TerminalHandler, slots: List<Slot>, now: Long) {
        if (!handler.isClicked || now - lastClickTime < RESOLVE_TIMEOUT || handler.type == TerminalEnums.MELODY) return
        handler.resolveExpired(RESOLVE_TIMEOUT)
        handler.updateSlots(slots)
        handler.resetClicked()
    }

    private fun attemptClick(handler: TerminalHandler, screen: AbstractContainerScreen<*>, slots: List<Slot>, now: Long) {
        val player = mc.player ?: return
        val slotId = if (melodySkipClick != -1) melodySkipClick else handler.nextClick() ?: return
        melodySkipClick = -1
        if (slotId !in slots.indices) return

        val clickedItem = slots.getOrNull(slotId)?.item
        mc.gameMode?.handleContainerInput(screen.menu.containerId, slotId, handler.clickButton(slotId), ContainerInput.CLONE, player)
        handler.markClicked(slotId, clickedItem)
        lastClickTime = now

        if (melodySkip && handler.type == TerminalEnums.MELODY && (handler as? MelodyHandler)?.isEdgeAligned == true && slotId / 9 < 3) {
            melodySkipClick = slotId + 9
        }
    }

    @EventHandler
    private fun onWorldUnload(event: WorldEvent.Unload) = reset()

    private fun containerSlots(slots: List<Slot>): List<Slot> =
        slots.subList(0, (slots.size - 36).coerceAtLeast(0))

    private fun isEnabled(type: TerminalEnums): Boolean = when (type) {
        TerminalEnums.PANES -> panes
        TerminalEnums.RUBIX -> rubix
        TerminalEnums.NUMBERS -> numbers
        TerminalEnums.START_WITH -> startsWith
        TerminalEnums.SELECT -> select
        TerminalEnums.MELODY -> melody
    }

    private fun createHandler(type: TerminalEnums, title: String): TerminalHandler? = when (type) {
        TerminalEnums.PANES -> PanesHandler()
        TerminalEnums.NUMBERS -> NumbersHandler()
        TerminalEnums.RUBIX -> RubixHandler()
        TerminalEnums.MELODY -> MelodyHandler()
        TerminalEnums.START_WITH -> {
            val letter = TerminalEnums.START_WITH.regex.find(title)?.groupValues?.get(1) ?: return null
            StartsWithHandler(letter)
        }
        TerminalEnums.SELECT -> {
            val color = TerminalEnums.SELECT.regex.find(title)?.groupValues?.get(1) ?: return null
            SelectAllHandler(color)
        }
    }

    private fun reset() {
        handler = null
        terminalTitle = null
        lastClickTime = 0L
        melodySkipClick = -1
    }

    override fun onDisable() = reset()
}
