package cn.hkim.addon.gui

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import com.mojang.blaze3d.platform.NativeImage
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

object Background {
    private const val FADE_DURATION = 1000L
    private const val DISPLAY_DURATION = 10_000L

    private val backgrounds = mutableListOf<Identifier>()

    private var currentIndex = 0
    private var nextIndex = 0
    private var fadeStartTime = 0L
    private var isFading = false
    private var currentAlpha = 1f
    private var nextAlpha = 0f
    private var lastSwitchTime = 0L
    private var initialized = false

    fun getBackgrounds(): List<Identifier> {
        ensureInitialized()
        return backgrounds
    }

    fun getDefaultBackground(): Identifier = Identifier.fromNamespaceAndPath("hkim", "bg.png")

    @JvmStatic
    fun loadBackgrounds() {
        val bgDir = FabricLoader.getInstance().configDir.resolve("hkim/backgrounds").toFile()
        bgDir.mkdirs()

        val texManager = mc.textureManager

        bgDir.listFiles { it.name.lowercase().endsWith(".png") }?.forEach { file ->
            runCatching {
                ImageIO.read(file)?.let { image ->
                    val native = convertToNativeImage(image)
                    val texture = DynamicTexture({ "${file.name}" }, native)
                    val location = Identifier.fromNamespaceAndPath("hkim", "bg_${file.nameWithoutExtension}")
                    texManager.register(location, texture)
                    backgrounds.add(location)
                    initialized = true
                }
            }.onFailure { Hkim.logger.error("Failed: ${file.name} - ${it.message}") }
        }

        if (backgrounds.isEmpty()) {
            backgrounds.add(getDefaultBackground())
        }

        Hkim.logger.info("Loaded ${backgrounds.size} backgrounds")
    }

    private fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            loadBackgrounds()
        }
    }

    fun renderBackground(screen: Screen, graphics: GuiGraphicsExtractor, offsetX: Float, offsetY: Float) {
        val currentBg = backgrounds.getOrNull(currentIndex)
        if (currentBg != null && currentAlpha > 0) {
            renderBackgroundWithAlpha(screen, graphics, currentBg, offsetX, offsetY, currentAlpha)
        }

        if (isFading && nextAlpha > 0) {
            val nextBg = backgrounds.getOrNull(nextIndex)
            if (nextBg != null) {
                renderBackgroundWithAlpha(screen, graphics, nextBg, offsetX, offsetY, nextAlpha)
            }
        }
    }

    fun convertToNativeImage(bufferedImage: BufferedImage): NativeImage {
        val width = bufferedImage.width
        val height = bufferedImage.height
        val nativeImage = NativeImage(NativeImage.Format.RGBA, width, height, false)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val argb = bufferedImage.getRGB(x, y)
                nativeImage.setPixel(x, y, argb)
            }
        }
        return nativeImage
    }

    fun renderBackgroundWithAlpha(screen: Screen, graphics: GuiGraphicsExtractor, texture: Identifier, offsetX: Float, offsetY: Float, alpha: Float) {
        ensureInitialized()
        if (alpha <= 0f) return

        val extraSize = 30
        val drawWidth = screen.width + extraSize * 2
        val drawHeight = screen.height + extraSize * 2

        val startX = -extraSize + offsetX.toInt()
        val startY = -extraSize + offsetY.toInt()

        val alphaInt = (alpha * 255).toInt()
        val color = (alphaInt shl 24) or 0x00FFFFFF
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            startX, startY,
            0f, 0f,
            drawWidth, drawHeight,
            drawWidth, drawHeight,
            drawWidth, drawHeight,
            color
        )
    }

    fun update() {
        ensureInitialized()
        if (!isFading && backgrounds.size > 1) {
            val now = System.currentTimeMillis()
            if (now - lastSwitchTime >= DISPLAY_DURATION) {
                nextIndex = (currentIndex + 1) % backgrounds.size
                fadeStartTime = now
                isFading = true
                lastSwitchTime = now
            }
        }

        if (isFading) {
            val elapsed = System.currentTimeMillis() - fadeStartTime
            if (elapsed >= FADE_DURATION) {
                isFading = false
                currentIndex = nextIndex
                currentAlpha = 1f
                nextAlpha = 0f
            } else {
                val progress = elapsed.toFloat() / FADE_DURATION
                currentAlpha = 1f - progress
                nextAlpha = progress
            }
        }
    }
}