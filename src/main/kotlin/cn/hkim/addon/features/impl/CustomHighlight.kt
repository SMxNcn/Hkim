package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.events.impl.RenderEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.Colors
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.render.drawWireFrameBox
import cn.hkim.addon.utils.renderBoundingBox
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import meteordevelopment.orbit.EventHandler
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import java.awt.Color
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

@ModuleInfo("custom_highlight", Category.RENDER, false)
object CustomHighlight : Module("Custom Highlight", "Highlight custom entities in specified SkyBlock islands.") {
    private val highlightColor by ColorSetting("Highlight Color", "The color of the highlight.", Colors.WHITE.rgb)

    private val entries = mutableListOf<HighlightEntry>()
    private val trackedEntities = mutableSetOf<Entity>()
    private val PLAYER_REGEX = Regex("^\\[\\d{1,3}]\\s[a-zA-Z0-9_]{1,16}.*")

    private val configDir = File(FabricLoader.getInstance().configDir.toFile(), "hkim")
    private val dataFile = File(configDir, "custom_highlight.json")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    init {
        loadEntries()
    }

    data class HighlightEntry(
        val entityName: String,
        val islandId: String? = null
    ) {
        val island: Island?
            get() = islandId?.let { id ->
                Island.entries.firstOrNull { it.name.equals(id, true) || it.displayName.equals(id, true) }
            }
    }

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        val level = mc.level ?: return
        if (!enabled || entries.isEmpty()) {
            if (trackedEntities.isNotEmpty()) trackedEntities.clear()
            return
        }

        trackedEntities.clear()

        for (entry in entries) {
            if (entry.island != null && LocationUtils.currentArea != entry.island) continue
            if (!LocationUtils.inSkyBlock && entry.island == null) continue

            for (entity in level.entitiesForRendering()) {
                if (!entity.isAlive || entity !is ArmorStand) continue

                val name = entity.name.string.clean
                if (!name.contains(entry.entityName, ignoreCase = true)) continue

                val realEntity = level.getEntities(entity, entity.boundingBox.move(0.0, -1.0, 0.0)) {
                    isValidHighlightEntity(it)
                }.firstOrNull()

                if (realEntity != null) {
                    trackedEntities.add(realEntity)
                }
            }
        }
    }

    @EventHandler
    private fun onRender(event: RenderEvent.Extract) {
        if (!enabled || trackedEntities.isEmpty() || !LocationUtils.inSkyBlock || LocationUtils.inDungeons) return

        val color = Color(highlightColor)
        for (entity in trackedEntities) {
            if (!entity.isAlive) continue
            event.drawWireFrameBox(entity.renderBoundingBox, color, 2f, false)
        }
    }

    @EventHandler
    private fun onWorldLoad(event: WorldEvent.Load) {
        if (!enabled) return
        trackedEntities.clear()
    }

    fun getEntries(): List<HighlightEntry> = entries.toList()

    fun addEntry(entityName: String, islandId: String?): Boolean {
        val normalizedIsland = islandId?.ifBlank { null }
        if (entries.any { it.entityName.equals(entityName, true) && it.islandId == normalizedIsland }) return false
        entries.add(HighlightEntry(entityName, normalizedIsland))
        saveEntries()
        return true
    }

    fun removeEntry(entityName: String): Boolean {
        val removed = entries.removeAll { it.entityName.equals(entityName, true) }
        if (removed) {
            trackedEntities.clear()
            saveEntries()
        }
        return removed
    }

    private fun loadEntries() {
        if (!dataFile.exists()) return
        try {
            val json = Files.readString(dataFile.toPath(), StandardCharsets.UTF_8)
            val type = object : TypeToken<List<HighlightEntry>>() {}.type
            val loaded: List<HighlightEntry> = gson.fromJson(json, type) ?: return
            entries.clear()
            entries.addAll(loaded)
        } catch (e: Exception) {
            Hkim.logger.error("Failed to load entries: ${e.message}")
        }
    }

    private fun saveEntries() {
        configDir.mkdirs()
        try {
            Files.writeString(dataFile.toPath(), gson.toJson(entries), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Hkim.logger.error("Failed to save entries: ${e.message}")
        }
    }

    private fun isValidHighlightEntity(entity: Entity): Boolean = when (entity) {
        is ArmorStand -> false
        is WitherBoss -> false
        is Player -> {
            if (entity == mc.player) return false
            val name = entity.displayName.string.clean
            !name.matches(PLAYER_REGEX)
        }
        else -> true
    }
}
