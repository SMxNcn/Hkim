package cn.hkim.addon.events.impl

import cn.hkim.addon.events.Cancellable
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.world.inventory.Slot

sealed class GuiEvent(open val screen: Screen) : Cancellable() {

    class Open(screen: Screen) : GuiEvent(screen)

    class Close(screen: Screen) : GuiEvent(screen)

    class SlotClick(screen: Screen, val slotId: Int, val button: Int) : GuiEvent(screen)

    class MouseClick(screen: Screen, val click: MouseButtonEvent, val doubled: Boolean) : GuiEvent(screen)

    class KeyPress(screen: Screen, val input: KeyEvent) : GuiEvent(screen)

    class Draw(screen: Screen, val graphics: GuiGraphicsExtractor, val mouseX: Int, val mouseY: Int) : GuiEvent(screen)

    class DrawSlot(screen: Screen, val graphics: GuiGraphicsExtractor, val slot: Slot) : GuiEvent(screen)

    class DrawTooltip(screen: Screen, val graphics: GuiGraphicsExtractor, val mouseX: Int, val mouseY: Int) : GuiEvent(screen)
}