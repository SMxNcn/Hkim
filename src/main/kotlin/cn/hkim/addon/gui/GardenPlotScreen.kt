package cn.hkim.addon.gui

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.features.impl.FarmingHelper
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.sendCommand
import cn.hkim.addon.utils.skyblock.farming.PestTracker
import cn.hkim.addon.utils.skyblock.farming.Plot
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

class GardenPlotScreen : Screen(Component.literal("Garden Plots")) {
    private val gridRows = 5
    private val gridCols = 5
    private val slotSize = 22
    private val slotPadding = 5
    private val itemOffset = (slotSize - 16) / 2

    private var gridX = 0
    private var gridY = 0
    private var gridWidth = 0
    private var gridHeight = 0

    override fun init() {
        gridWidth = gridCols * slotSize + (gridCols - 1) * slotPadding
        gridHeight = gridRows * slotSize + (gridRows - 1) * slotPadding
        gridX = (width - gridWidth) / 2
        gridY = (height - gridHeight) / 2 + 4
        super.init()
    }

    private fun slotToScreen(slotCoord: Float, center: Float): Float =
        (slotCoord - center) * FarmingHelper.plotScreenScale + center

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val scale = FarmingHelper.plotScreenScale
        graphics.fill(0, 0, width, height, 0x80000000.toInt())

        val title = "Garden Plots"
        val tw = mc.font.width(title)
        graphics.text(mc.font, title, width / 2 - tw / 2, 35, 0xFFFFFFFF.toInt(), false)

        graphics.pose().pushMatrix()
        graphics.pose().translate(width / 2f, height / 2f)
        graphics.pose().scale(scale, scale)
        graphics.pose().translate(-width / 2f, -height / 2f)

        for (row in 0 until gridRows) {
            for (col in 0 until gridCols) {
                val displayRow = gridRows - 1 - row
                val plotId = Plot.PLOT_GRID[displayRow][col]
                val plot = Plot.byId(plotId)
                val slotX = gridX + col * (slotSize + slotPadding)
                val slotY = gridY + row * (slotSize + slotPadding)

                val sSlotX = slotToScreen(slotX.toFloat(), width / 2f)
                val sSlotY = slotToScreen(slotY.toFloat(), height / 2f)
                val sSlotSize = slotSize * scale
                val hovered = mouseX >= sSlotX && mouseX < sSlotX + sSlotSize
                    && mouseY >= sSlotY && mouseY < sSlotY + sSlotSize

                val bgColor = if (hovered) 0xFF505050.toInt() else 0xFF333333.toInt()
                graphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, bgColor)

                val hasPest = PestTracker.aliveCount > 0
                    && PestTracker.pestPlots.isNotEmpty()
                    && plot?.displayName?.toIntOrNull() in PestTracker.pestPlots
                val borderColor = when {
                    hasPest && hovered -> 0xFFFF6666.toInt()
                    hasPest -> 0xFFFF4444.toInt()
                    hovered -> 0xFFAAAAAA.toInt()
                    else -> 0xFF555555.toInt()
                }
                graphics.outline(slotX, slotY, slotSize, slotSize, borderColor)

                val item = plot?.itemStack
                if (item != null && !item.isEmpty) {
                    graphics.item(item, slotX + itemOffset, slotY + itemOffset)
                    graphics.itemDecorations(mc.font, item, slotX + itemOffset, slotY + itemOffset)
                }

                if (hovered) {
                    graphics.requestCursor(CursorTypes.POINTING_HAND)
                    val displayName = if (plot != null && plot.displayName.isNotBlank())
                        "Plot - ${plot.displayName}"
                    else
                        "Plot - §7Unknown"
                    graphics.setTooltipForNextFrame(mc.font, Component.literal(displayName), mouseX, mouseY)
                }
            }
        }

        graphics.pose().popMatrix()
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mx = event.x.toInt()
        val my = event.y.toInt()

        for (row in 0 until gridRows) {
            for (col in 0 until gridCols) {
                val displayRow = gridRows - 1 - row
                val plotId = Plot.PLOT_GRID[displayRow][col]
                val slotX = gridX + col * (slotSize + slotPadding)
                val slotY = gridY + row * (slotSize + slotPadding)

                val sSlotX = slotToScreen(slotX.toFloat(), width / 2f)
                val sSlotY = slotToScreen(slotY.toFloat(), height / 2f)
                val sSlotSize = slotSize * FarmingHelper.plotScreenScale
                if (mx >= sSlotX && mx < sSlotX + sSlotSize
                    && my >= sSlotY && my < sSlotY + sSlotSize) {
                    playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
                    val plot = Plot.byId(plotId)
                    val name = plot?.displayName?.ifBlank { null } ?: plotId.toString()
                    val cmd = if (plotId == Plot.PLOT_BARN) "plottp barn" else "plottp $name"
                    sendCommand(cmd)
                    return true
                }
            }
        }

        return super.mouseClicked(event, doubleClick)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.isEscape) {
            onClose()
            mc.setScreen(null)
            return true
        }
        return super.keyPressed(event)
    }

    override fun extractMenuBackground(graphics: GuiGraphicsExtractor) {}
    override fun extractTransparentBackground(graphics: GuiGraphicsExtractor) {}
    override fun isPauseScreen() = false
}
