package cn.hkim.addon.gui

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.HudUtils.getChromaColor
import cn.hkim.addon.utils.coloredChar
import com.terraformersmc.modmenu.gui.ModsScreen
import net.minecraft.SharedConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import java.awt.Color

class MainMenu : Screen(Component.literal("Main Menu")) {
    private val logoTex = Identifier.fromNamespaceAndPath("hkim", "icon.png")

    private var currentParallaxX = 0f
    private var currentParallaxY = 0f
    private var targetParallaxX = 0f
    private var targetParallaxY = 0f

    override fun init() {
        initButtons()
        super.init()
    }

    private fun updateParallax(mouseX: Double, mouseY: Double) {
        val centerX = this.width / 2.0
        val centerY = this.height / 2.0
        val parallaxStrength = 0.06f

        val normX = (mouseX - centerX) / this.width
        val normY = (mouseY - centerY) / this.height

        targetParallaxX = (normX * this.width * parallaxStrength).toFloat()
        targetParallaxY = (normY * this.height * parallaxStrength).toFloat()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val s1 = "Minecraft ${SharedConstants.getCurrentVersion().id()}"
        val s2 = "Hkim v${Hkim.VERSION}"
        val s3 = "Cheaters get banned!"
        val chromaColor = getChromaColor(Color(142, 221, 255), Color(166, 166, 166), 1, 4, 5).rgb

        Background.update()
        this.updateParallax(mouseX.toDouble(), mouseY.toDouble())
        this.extractBackground(graphics, mouseX, mouseY, partialTick)
        graphics.text(mc.font, s1, 2, height - 10, 0xFFFFFFFF.toInt())
        graphics.text(mc.font, s2, 2, height - 20, chromaColor)
        graphics.text(mc.font, s3, width - mc.font.width(s3) - 2, height - 10, 0xFFFFFFFF.toInt())
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }

    private fun initButtons() {
        val centerX = width / 2
        val centerY = height / 2
        val btnW = 180
        val btnH = 20

        addRenderableWidget(ClientButton(centerX - 90, centerY + 1, btnW, btnH, Component.translatable("menu.singleplayer")) {
            mc.setScreen(SelectWorldScreen(this))
        })

        addRenderableWidget(ClientButton(centerX - 90, centerY + 23, btnW, btnH, Component.translatable("menu.multiplayer")) {
            mc.setScreen(JoinMultiplayerScreen(this))
        })

        addRenderableWidget(ClientButton(centerX - 90, centerY + 45, btnW, btnH, Component.translatable("modmenu.title")) {
            mc.setScreen(ModsScreen(this))
        })

        addRenderableWidget(ClientButton(centerX - 90, centerY + 67, 88, btnH, Component.translatable("menu.options")) {
            mc.setScreen(OptionsScreen(this, mc.options, false))
        })

        addRenderableWidget(ClientButton(centerX + 2, centerY + 67, 88, btnH, Component.translatable("menu.quit")) {
            mc.stop()
        })

        addRenderableWidget(ClientButton(width - 55, 5, 50, btnH, coloredChar("Vanilla", 0xFF4CAF50.toInt())) {
            mc.setScreen(TitleScreen())
        })
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        currentParallaxX += (targetParallaxX - currentParallaxX) * 0.15f
        currentParallaxY += (targetParallaxY - currentParallaxY) * 0.15f
        val centerX = this.width / 2
        val centerY = this.height / 2

        Background.renderBackground(this, graphics, currentParallaxX, currentParallaxY)

        val logoSize = 64
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            logoTex,
            centerX - logoSize / 2, centerY - 76,
            0F, 0F,
            logoSize, logoSize,
            logoSize, logoSize
        )
    }

    override fun shouldCloseOnEsc(): Boolean = false
}