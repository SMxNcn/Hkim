package cn.hkim.addon.utils.skyblock.rng

import cn.hkim.addon.Hkim
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Locale

object RngMeterStorage {
    data class MeterData(val score: Long = 0L, val needed: Long = 0L, val item: String? = null) {
        val percentage: Double
            get() = if (needed > 0L) {
                (score.toDouble() / needed * 100.0).coerceIn(0.0, 100.0)
            } else {
                0.0
            }
    }

    private const val RESOURCE_PATH = "assets/hkim/rng_meter_values.json"

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val colorCodeRegex = Regex("""§([0-9a-fA-F])""")
    private val dataFile = File(File(FabricLoader.getInstance().configDir.toFile(), "hkim/data"), "rng_meter.json")
    private val meters = mutableMapOf<String, MeterData>()

    @Volatile
    private var normalizedValues: Map<String, Map<String, Long>> = emptyMap()

    @Volatile
    private var compactValues: Map<String, Map<String, Long>> = emptyMap()

    @Volatile
    private var coloredValues: Map<String, Map<String, Long>> = emptyMap()

    init {
        loadValues()
        loadMeters()
    }

    private fun loadValues() {
        try {
            val stream = RngMeterStorage::class.java.classLoader.getResourceAsStream(RESOURCE_PATH)
                ?: error("Resource not found: $RESOURCE_PATH")
            val type = object : TypeToken<Map<String, Map<String, Long>>>() {}.type

            val raw: Map<String, Map<String, Long>> = stream.bufferedReader().use {
                gson.fromJson(it, type)
            } ?: emptyMap()

            normalizedValues = raw.mapValues { (_, items) ->
                items.entries.associate { (name, value) -> normalizeItemName(name) to value }
            }

            compactValues = raw.mapValues { (_, items) ->
                items.entries.associate { (name, value) -> compactItemName(name) to value }
            }

            coloredValues = raw.mapValues { (_, items) ->
                val entries = mutableMapOf<String, Long>()
                for ((name, value) in items) {
                    val color = colorCodeRegex.find(name)
                        ?.groupValues?.get(1)
                        ?.lowercase(Locale.US)
                        ?: continue
                    entries["$color:${normalizeItemName(name)}"] = value
                    entries["$color:${compactItemName(name)}"] = value
                }
                entries
            }

            Hkim.logger.info("Loaded ${normalizedValues.size} RNG meter value categories.")
        } catch (e: Exception) {
            Hkim.logger.error("Failed to load RNG meter values.", e)
        }
    }

    fun getNeeded(key: String, item: String): Long? {
        val target = normalizeItemName(item)
        if (target.isEmpty()) return null
        val compactTarget = compactItemName(item)

        val color = colorCodeRegex.find(item)
            ?.groupValues?.get(1)
            ?.lowercase(Locale.US)
        if (color != null) {
            coloredValues[key]?.let { items ->
                items["$color:$target"]?.let { return it }
                items["$color:$compactTarget"]?.let { return it }
            }
        }

        normalizedValues[key]?.get(target)?.let { return it }
        compactValues[key]?.get(compactTarget)?.let { return it }

        if (target.length < 4) return null
        val items = normalizedValues[key] ?: return null
        return items.entries.firstOrNull { (name, _) ->
            name.contains(target) || target.contains(name) ||
                name.replace(" ", "").contains(compactTarget) ||
                compactTarget.contains(name.replace(" ", ""))
        }?.value
    }

    private fun normalizeItemName(raw: String): String = raw
        .replace(Regex("""§."""), "")
        .replace(Regex("""^\d+\s*[xX]?\s*"""), "")
        .trim()
        .replace(Regex("""\s+"""), " ")
        .lowercase(Locale.US)

    private fun compactItemName(raw: String): String = normalizeItemName(raw).replace(" ", "")

    fun get(key: String): MeterData = meters[key] ?: MeterData()

    fun setScore(key: String, score: Long) {
        val data = get(key)
        val updated = data.copy(score = score.coerceAtLeast(0L))
        if (updated == data) return

        meters[key] = updated
        save()
    }

    fun addScore(key: String, delta: Long) {
        if (delta == 0L) return
        setScore(key, get(key).score + delta)
    }

    fun setItem(key: String, item: String?) {
        val data = get(key)
        val needed = item?.let { getNeeded(key, it) } ?: 0L
        val updated = data.copy(item = item, needed = needed)
        if (updated == data) return

        meters[key] = updated
        save()
    }

    fun setNeeded(key: String, needed: Long) {
        val data = get(key)
        val updated = data.copy(needed = needed.coerceAtLeast(0L))
        if (updated == data) return

        meters[key] = updated
        save()
    }

    private fun loadMeters() {
        if (!dataFile.exists()) return

        try {
            val root = gson.fromJson(Files.readString(dataFile.toPath(), StandardCharsets.UTF_8), JsonObject::class.java)
                ?: return

            for ((key, element) in root.entrySet()) {
                val obj = element.asJsonObject
                val itemElement = obj.get("item")
                val item = if (itemElement != null && !itemElement.isJsonNull) itemElement.asString else null
                val storedNeeded = obj.get("needed")?.asLong ?: 0L

                val needed = item?.let { getNeeded(key, it) } ?: storedNeeded
                val data = MeterData(
                    score = obj.get("score")?.asLong ?: 0L,
                    needed = needed,
                    item = item,
                )
                if (data.score == 0L && data.needed == 0L && data.item == null) continue
                meters[key] = data
            }
        } catch (e: Exception) {
            Hkim.logger.error("Failed to load RNG meter data.", e)
        }
    }

    private fun save() {
        try {
            dataFile.parentFile?.mkdirs()
            val root = JsonObject()

            for ((key, data) in meters) {
                if (data.score == 0L && data.needed == 0L && data.item == null) continue

                val obj = JsonObject()
                obj.addProperty("score", data.score)
                obj.addProperty("needed", data.needed)
                data.item?.let { obj.addProperty("item", it) }
                root.add(key, obj)
            }

            Files.writeString(dataFile.toPath(), gson.toJson(root), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Hkim.logger.error("Failed to save RNG meter data.", e)
        }
    }
}
