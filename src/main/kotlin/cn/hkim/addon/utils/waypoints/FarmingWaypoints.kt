package cn.hkim.addon.utils.waypoints

import cn.hkim.addon.utils.modMessage
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object FarmingWaypoints {
    private val gson = GsonBuilder().setPrettyPrinting().registerTypeAdapter(Action::class.java, ActionAdapter()).create()

    internal var currentWaypoints: MutableList<WaypointData> = mutableListOf()
        private set

    var activeFile: String = ""
        private set

    data class WaypointData(
        val id: Int,
        val x: Int,
        val y: Int,
        val z: Int,
        val action: Action
    ) {
        val blockPos: BlockPos get() = BlockPos(x, y, z)
    }

    data class Action(
        val forward: Boolean = false,
        val back: Boolean = false,
        val left: Boolean = false,
        val right: Boolean = false,
        val leftClick: Boolean = false
    )

    private val waypointsDir = File(FabricLoader.getInstance().configDir.toFile(), "hkim/waypoints")

    fun listFiles(): List<String> {
        if (!waypointsDir.exists()) return emptyList()
        return waypointsDir.listFiles { _, name -> name.endsWith(".json") }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    fun load(filename: String) {
        activeFile = filename
        currentWaypoints.clear()
        val file = File(waypointsDir, "$filename.json")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
            modMessage("§7Created & loaded empty waypoint file: §b$filename.json")
            return
        }

        try {
            val type = object : TypeToken<List<WaypointData>>() {}.type
            val loaded: List<WaypointData> = gson.fromJson(FileReader(file), type)
            currentWaypoints.clear()
            currentWaypoints.addAll(loaded)
            reindexWaypoints()
            modMessage("§7Loaded ${currentWaypoints.size} waypoints from §b$filename.json")
        } catch (e: Exception) {
            modMessage("§cFailed to load waypoints: ${e.message}")
            e.printStackTrace()
        }
    }

    fun unload() {
        currentWaypoints.clear()
        activeFile = ""
        modMessage("§cWaypoints unloaded.")
    }

    fun addAt(pos: BlockPos, action: Action = Action()) {
        val nextId = if (currentWaypoints.isEmpty()) 1 else currentWaypoints.maxOfOrNull { it.id }?.plus(1) ?: 1
        val wp = WaypointData(nextId, pos.x, pos.y, pos.z, action)
        currentWaypoints.add(wp)
        saveAsync()
        modMessage("§7Added waypoint #${wp.id} at §a${pos.x}, ${pos.y}, ${pos.z}§7 with $action.")
    }

    fun removeAt(pos: BlockPos): Boolean {
        val index = currentWaypoints.indexOfFirst { it.blockPos == pos }
        return if (index != -1) {
            currentWaypoints.removeAt(index)
            reindexWaypoints()
            saveAsync()
            modMessage("§7Removed waypoint at §c${pos.x}, ${pos.y}, ${pos.z}§7.")
            true
        } else false
    }

    fun updateAt(pos: BlockPos, action: Action) {
        val index = currentWaypoints.indexOfFirst { it.blockPos == pos }

        if (index != -1) {
            val oldWp = currentWaypoints[index]
            val newWp = WaypointData(oldWp.id, oldWp.x, oldWp.y, oldWp.z, action)
            currentWaypoints[index] = newWp
            saveAsync()
            modMessage("§7Updated waypoint at §b${pos.x}, ${pos.y}, ${pos.z} §7with $action.")
        } else addAt(pos, action)
    }

    fun reload() {
        if (activeFile.isBlank()) {
            modMessage("§cNo active waypoint file.")
            return
        }
        currentWaypoints.clear()
        val file = File(waypointsDir, "$activeFile.json")
        if (!file.exists()) {
            modMessage("§c$activeFile.json not found.")
            return
        }

        try {
            val type = object : TypeToken<List<WaypointData>>() {}.type
            val loaded: List<WaypointData> = gson.fromJson(FileReader(file), type)

            val newWaypoints = loaded.toMutableList()
            currentWaypoints.addAll(newWaypoints)
            reindexWaypoints()

            modMessage("§7Reloaded ${currentWaypoints.size} waypoints. §8[$activeFile]§r")
        } catch (e: Exception) {
            modMessage("§cFailed to reload waypoints. (${e.message})")
        }
    }

    private fun reindexWaypoints() {
        val reindex = currentWaypoints.mapIndexed { index, wp ->
            wp.copy(id = index + 1)
        }.toMutableList()

        currentWaypoints.clear()
        currentWaypoints.addAll(reindex)
    }

    private fun saveAsync() {
        if (activeFile.isBlank()) return
        val file = File(waypointsDir, "$activeFile.json")
        file.parentFile.mkdirs()
        try {
            FileWriter(file).use { gson.toJson(currentWaypoints, it) }
        } catch (e: Exception) {
            modMessage("§cFailed to save waypoints: ${e.message}")
        }
    }
}
