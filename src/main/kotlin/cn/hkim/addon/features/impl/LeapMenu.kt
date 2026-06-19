package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.KeybindSetting
import cn.hkim.addon.config.settings.SelectorSetting
import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.HudUtils.getQuadrant
import cn.hkim.addon.utils.HudUtils.scaledText
import cn.hkim.addon.utils.render.nvg.NVGPIPRenderer
import cn.hkim.addon.utils.render.nvg.NVGRenderer
import cn.hkim.addon.utils.skyblock.DungeonClass
import cn.hkim.addon.utils.skyblock.DungeonPlayer
import cn.hkim.addon.utils.skyblock.DungeonUtils.leapTeammates
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import java.awt.Color

@ModuleInfo("leap_menu", Category.SKYBLOCK)
object LeapMenu : Module("Leap Menu", "Custom leap menu.") {
    val type by SelectorSetting("Sorting", "How to sort the leap menu.", arrayListOf("Default", "A-Z Class", "A-Z Name", "No Sorting"), "Default")
    private val leapAnnounce by BooleanSetting("Leap Announce", "Announces when you leap to a player.", false)
    val keybindType by SelectorSetting("Mode", "How the keybinds should function.", arrayListOf("Corners", "Class"), "Normal")

    private val topLeftKeybind by KeybindSetting("Top Left", "Used to click on the first person in the leap menu.", GLFW.GLFW_KEY_UNKNOWN).depends { keybindType == 0 }
    private val topRightKeybind by KeybindSetting("Top Right", "Used to click on the second person in the leap menu.", GLFW.GLFW_KEY_UNKNOWN).depends { keybindType == 0 }
    private val bottomLeftKeybind by KeybindSetting("Bottom Left", "Used to click on the third person in the leap menu.", GLFW.GLFW_KEY_UNKNOWN).depends { keybindType == 0 }
    private val bottomRightKeybind by KeybindSetting("Bottom Right", "Used to click on the fourth person in the leap menu.", GLFW.GLFW_KEY_UNKNOWN).depends { keybindType == 0 }

    private val archerKeybind by KeybindSetting("Archer", "Used to leap to the Archer in the leap menu.", GLFW.GLFW_KEY_UNKNOWN).depends { keybindType == 1 }
    private val berserkKeybind by KeybindSetting("Berserker", "Used to leap to the Berserker in the leap menu.", GLFW.GLFW_KEY_UNKNOWN).depends { keybindType == 1 }
    private val healerKeybind by KeybindSetting("Healer", "Used to leap to the Healer in the leap menu.", GLFW.GLFW_KEY_UNKNOWN).depends { keybindType == 1 }
    private val mageKeybind by KeybindSetting("Mage", "Used to leap to the Mage in the leap menu.", GLFW.GLFW_KEY_UNKNOWN).depends { keybindType == 1 }
    private val tankKeybind by KeybindSetting("Tank", "Used to leap to the Tank in the leap menu.", GLFW.GLFW_KEY_UNKNOWN).depends { keybindType == 1 }

    private val EMPTY = DungeonPlayer("Empty", DungeonClass.Unknown, 0, Identifier.withDefaultNamespace("textures/entity/steve.png"))
    private val leapedRegex = Regex("You have teleported to (\\w{1,16})!")

    private const val CARD_WIDTH = 200
    private const val CARD_HEIGHT = 75
    private const val AVATAR_SIZE = 48
    private const val TEXT_PADDING = 8

    @EventHandler
    fun onGuiDraw(event: GuiEvent.Draw) {
        if (!(isLeapMenu(event.screen) && enabled)) return

        val graphics = event.graphics
        val guiW = mc.window.guiScaledWidth.toFloat()
        val guiH = mc.window.guiScaledHeight.toFloat()

        val gridWidth = 2 * CARD_WIDTH + 12
        val gridHeight = 2 * CARD_HEIGHT + 12
        val startX = (guiW - gridWidth) / 2
        val startY = (guiH - gridHeight) / 2

        for (row in 0..1) {
            for (col in 0..1) {
                val index = row * 2 + col
                if (index >= leapTeammates.size) break

                val player = leapTeammates[index]
                if (player == EMPTY) continue

                val cardX = startX + col * (CARD_WIDTH + 12)
                val cardY = startY + row * (CARD_HEIGHT + 12)

                renderCard(graphics, player, cardX.toInt(), cardY.toInt())
            }
        }

        event.cancel()
    }

    @EventHandler
    fun onGuiBackground(event: GuiEvent.DrawBackground) {
        if (!isLeapMenu(event.screen)) return
        event.cancel()
    }

    @EventHandler
    fun onGuiKey(event: GuiEvent.KeyPress) {
        if (!isLeapMenu(event.screen)) return
        val screen = event.screen as AbstractContainerScreen<*>

        val keybindList = if (keybindType == 0) listOf(topLeftKeybind, topRightKeybind, bottomLeftKeybind, bottomRightKeybind)
            else listOf(archerKeybind, berserkKeybind, healerKeybind, mageKeybind, tankKeybind)
        val index = if (keybindType == 0) keybindList.indexOfFirst { it == event.input.key }
            else DungeonClass.entries.find { clazz -> clazz.ordinal == keybindList.indexOfFirst { it == event.input.key } }?.let { clazz -> leapTeammates.indexOfFirst { it.clazz == clazz } } ?: return

        if (index == -1) return
        val player = leapTeammates[index]
        if (player == EMPTY) return
        if (player.isDead) return modMessage("Can't leap dead player!")
        leapTo(player.name, screen)
    }

    @EventHandler
    fun onGuiClick(event: GuiEvent.MouseClick) {
        if (!isLeapMenu(event.screen)) return
        val screen = event.screen as AbstractContainerScreen<*>

        val centerX = mc.window.guiScaledWidth / 2
        val centerY = mc.window.guiScaledHeight / 2

        val leftBound = centerX - (CARD_WIDTH + 6)
        val rightBound = centerX + (CARD_WIDTH + 6)
        val topBound = centerY - (CARD_HEIGHT + 6)
        val bottomBound = centerY + (CARD_HEIGHT + 6)

        val mouseX = event.click.x.toInt()
        val mouseY = event.click.y.toInt()

        if (mouseX in leftBound..rightBound && mouseY in topBound..bottomBound) {
            val quadrant = getQuadrant(mouseX, mouseY)
            if ((type.equalsOneOf(1, 2, 3)) && leapTeammates.size < quadrant) return
            val player = leapTeammates[quadrant - 1]
            if (player == EMPTY) return
            if (player.isDead) return modMessage("Can't leap dead player!")

            leapTo(player.name, screen)
        }

        event.cancel()
    }

    @EventHandler
    fun onChat(event: ChatReceiveEvent) {
        if (!(leapAnnounce && LocationUtils.inDungeons)) return
        leapedRegex.find(event.message)?.groupValues?.get(1)?.let { sendCommand("pc Leaped to ${it}.") }
    }

    private fun renderCard(graphics: GuiGraphicsExtractor, player: DungeonPlayer, x: Int, y: Int) {
        NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
            NVGRenderer.rect(x * 2f, y * 2f, CARD_WIDTH * 2f, CARD_HEIGHT * 2f, Color(0xBF262626.toInt(), true), 12f)
        }

        val avatarX = x + 14
        val avatarY = (y + (CARD_HEIGHT - AVATAR_SIZE) / 2)

        val skinLoc = player.locationSkin ?: mc.player?.skin?.body?.id() ?: return
        graphics.blit(RenderPipelines.GUI_TEXTURED, skinLoc, avatarX, avatarY, 8f, 8f, AVATAR_SIZE, AVATAR_SIZE, 8, 8, 64, 64)
        graphics.blit(RenderPipelines.GUI_TEXTURED, skinLoc, avatarX - 2, avatarY - 2, 40f, 8f, AVATAR_SIZE + 4, AVATAR_SIZE + 4, 8, 8, 64, 64)

        val centerY = y + CARD_HEIGHT / 2
        val lineHeight = mc.font.lineHeight
        val totalTextHeight = lineHeight * 2 + 4
        val textStartX = avatarX + AVATAR_SIZE + TEXT_PADDING
        val textStartY = centerY - totalTextHeight / 2 + 2

        val playerName = player.name
        graphics.scaledText(mc.font, playerName, textStartX + 1, textStartY, 0xFFFFFFFF.toInt(), true, 1.5f)

        val className = if (player.isDead) "DEAD" else player.clazz.name
        val classColor = if (player.isDead) Colors.MINECRAFT_RED.rgb else player.clazz.color.rgb
        graphics.text(mc.font, className, textStartX, textStartY + lineHeight + 6, classColor, true)
    }

    fun defaultSorting(players: List<DungeonPlayer>): Array<DungeonPlayer> {
        val result = Array(4) { EMPTY }
        val secondRound = mutableListOf<DungeonPlayer>()

        for (player in players.sortedBy { it.clazz.priority }) {
            when {
                result[player.clazz.defaultQuadrant] == EMPTY -> result[player.clazz.defaultQuadrant] = player
                else -> secondRound.add(player)
            }
        }

        if (secondRound.isEmpty()) return result

        result.forEachIndexed { index, _ ->
            when {
                result[index] == EMPTY -> {
                    result[index] = secondRound.removeAt(0)
                    if (secondRound.isEmpty()) return result
                }
            }
        }
        return result
    }

    fun isLeapMenu(screen: Screen): Boolean {
        val chest = (screen as? AbstractContainerScreen<*>) ?: return false
        return chest.title.string.equalsOneOf("Spirit Leap", "Teleport to Player") && leapTeammates.isNotEmpty() && leapTeammates.all { it != EMPTY }
    }
}