package cn.hkim.addon.utils.skyblock.farming

import cn.hkim.addon.Hkim
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.cleanString
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

data class Plot(val id: Int, val corner1: BlockPos, val corner2: BlockPos) {
    var itemStack: ItemStack = ItemStack.EMPTY
    var displayName: String = ""

    val minX: Int get() = corner1.x
    val minZ: Int get() = corner1.z
    val maxX: Int get() = corner2.x
    val maxZ: Int get() = corner2.z
    val minY: Int get() = corner1.y
    val maxY: Int get() = corner2.y
    val center: BlockPos get() = BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2)

    fun contains(pos: BlockPos): Boolean =
        pos.x in minX..maxX && pos.z in minZ..maxZ

    fun contains(x: Int, z: Int): Boolean =
        x in minX..maxX && z in minZ..maxZ

    companion object {
        const val PLOT_BARN = 0

        private const val MIN_Y = 67
        private const val MAX_Y = 77
        private const val SIZE = 96
        private const val GRID = 5
        private const val ORIGIN_X = -240
        private const val ORIGIN_Z = 239

        internal val PLOT_GRID = arrayOf(
            intArrayOf(23, 19, 12, 20, 24),
            intArrayOf(17,  7,  4,  8, 18),
            intArrayOf(10,  2,  0,  3, 11),
            intArrayOf(15,  5,  1,  6, 16),
            intArrayOf(21, 13,  9, 14, 22),
        )

        private val plotsById: Map<Int, Plot> = run {
            val map = mutableMapOf<Int, Plot>()
            for (row in 0 until GRID) {
                for (col in 0 until GRID) {
                    val id = PLOT_GRID[row][col]
                    val x1 = ORIGIN_X + col * SIZE
                    val x2 = x1 + SIZE - 1
                    val z1 = ORIGIN_Z - (row + 1) * SIZE + 1
                    val z2 = ORIGIN_Z - row * SIZE
                    map[id] = Plot(id, BlockPos(x1, MIN_Y, z1), BlockPos(x2, MAX_Y, z2))
                }
            }
            map.toMap()
        }

        private val dataFile = File(File(FabricLoader.getInstance().configDir.toFile(), "hkim/data"), "garden_plots.json")
        private val gson = GsonBuilder().setPrettyPrinting().create()
        private val plotNameRegex = Regex("^Plot - (.+)$")

        val all: Collection<Plot> get() = plotsById.values
        val unlockable: List<Plot> get() = plotsById.values.filter { it.id != PLOT_BARN }

        fun byId(id: Int): Plot? = plotsById[id]

        fun byName(name: String): Plot? =
            all.firstOrNull { it.displayName.clean == name.clean }

        fun resolveName(name: String): Int? =
            name.toIntOrNull()?.takeIf { it in 0..24 } ?: byName(name)?.id

        fun at(pos: BlockPos): Plot? {
            if (pos.x < ORIGIN_X || pos.x > ORIGIN_X + GRID * SIZE - 1) return null
            if (pos.z > ORIGIN_Z || pos.z < ORIGIN_Z - GRID * SIZE + 1) return null
            val col = (pos.x - ORIGIN_X) / SIZE
            val row = (ORIGIN_Z - pos.z) / SIZE
            if (row !in 0 until GRID || col !in 0 until GRID) return null
            return byId(PLOT_GRID[row][col])
        }

        fun averageGroundY(plot: Plot, getTopY: (x: Int, z: Int) -> Int?): Int? {
            val step = SIZE / 8
            var sum = 0
            var count = 0
            for (sx in 0 until 8) {
                for (sz in 0 until 8) {
                    val x = plot.minX + sx * step + step / 2
                    val z = plot.maxZ - sz * step - step / 2
                    val top = getTopY(x, z)
                    if (top != null) {
                        sum += top
                        count++
                    }
                }
            }
            return if (count > 0) sum / count else null
        }

        fun scanConfigurePlot(container: AbstractContainerMenu) {
            for (localRow in 0 until GRID) {
                for (localCol in 0 until GRID) {
                    val slotIndex = localRow * 9 + (localCol + 2)
                    val item = container.getSlot(slotIndex).item
                    val plotId = PLOT_GRID[GRID - 1 - localRow][localCol]
                    val plot = plotsById[plotId] ?: continue
                    plot.itemStack = item
                    val fullName = item.displayName.cleanString
                    plot.displayName = plotNameRegex.find(fullName)?.groupValues?.get(1) ?: fullName
                }
            }
            save()
        }

        @JvmStatic
        fun load() {
            if (!dataFile.exists()) return
            try {
                val json = gson.fromJson(Files.readString(dataFile.toPath()), JsonArray::class.java) ?: return
                for (i in 0 until json.size().coerceAtMost(25)) {
                    val obj = json.get(i).asJsonObject
                    val plot = byId(i) ?: continue
                    plot.displayName = if (obj.has("name")) obj.get("name").asString.clean else i.toString()
                    val itemIdStr = obj.get("item")?.asString
                    if (itemIdStr != null && itemIdStr.contains(":")) {
                        val parts = itemIdStr.split(":", limit = 2)
                        val itemId = Identifier.fromNamespaceAndPath(parts[0], parts[1])
                        BuiltInRegistries.ITEM.getOptional(itemId).ifPresent { item ->
                            plot.itemStack = ItemStack(item)
                        }
                    }
                }
            } catch (e: Exception) {
                Hkim.logger.warn("Failed to load garden plots data", e)
            }
        }

        private fun save() {
            try {
                dataFile.parentFile.mkdirs()
                val root = JsonArray()
                for (i in 0..24) {
                    val plot = byId(i) ?: continue
                    val obj = JsonObject()
                    obj.addProperty("item", BuiltInRegistries.ITEM.getKey(plot.itemStack.item).toString())
                    if (plot.displayName != i.toString()) {
                        obj.addProperty("name", plot.displayName)
                    }
                    root.add(obj)
                }
                Files.writeString(dataFile.toPath(), gson.toJson(root), StandardCharsets.UTF_8)
            } catch (e: Exception) {
                Hkim.logger.warn("Failed to save garden plots data", e)
            }
        }
    }
}
