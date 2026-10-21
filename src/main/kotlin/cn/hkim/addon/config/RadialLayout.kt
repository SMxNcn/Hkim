package cn.hkim.addon.config

import cn.hkim.addon.Hkim
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.io.File
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class RadialEntry {
    var item: String = ""
    var name: String = ""
    var description: String = ""
    var command: String = ""
}

object RadialLayout {
    const val SLOT_COUNT = 8
    private const val NAMESPACE = "minecraft:"
    val PROFILE_NAMES: List<String> = List(5) { "Profile ${it + 1}" }

    private val configDir = File(FabricLoader.getInstance().configDir.toFile(), "hkim")
    private val configFile = File(configDir, "radial_menu.json")
    private val backupFile = File(configDir, "radial_menu.json.bak")
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .registerTypeAdapter(RadialEntry::class.java, EntryAdapter)
        .create()
    private val byNameType = object : TypeToken<Map<String, List<RadialEntry>>>() {}.type

    private object EntryAdapter : JsonSerializer<RadialEntry>, JsonDeserializer<RadialEntry> {
        override fun serialize(entry: RadialEntry, type: Type, context: JsonSerializationContext): JsonElement =
            JsonObject().apply {
                if (entry.item.isNotBlank()) addProperty("item", entry.item)
                if (entry.name.isNotBlank()) addProperty("name", entry.name)
                if (entry.description.isNotBlank()) addProperty("description", entry.description)
                if (entry.command.isNotBlank()) addProperty("command", entry.command)
            }

        override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): RadialEntry =
            RadialEntry().apply {
                val obj = json.takeIf { it.isJsonObject }?.asJsonObject ?: return@apply
                item = obj.string("item")
                name = obj.string("name")
                description = obj.string("description")
                command = obj.string("command")
            }

        private fun JsonObject.string(key: String): String =
            get(key)?.takeIf { it.isJsonPrimitive }?.asString ?: ""
    }

    private val profiles = LinkedHashMap<String, MutableList<RadialEntry>>()
    private val iconCache = HashMap<String, ItemStack?>()
    private var itemIdsCache: List<String>? = null

    init {
        load()
    }

    private fun slots(profile: Int): MutableList<RadialEntry> =
        profiles.getValue(PROFILE_NAMES[profile.coerceIn(PROFILE_NAMES.indices)])

    fun entries(profile: Int): List<RadialEntry> = slots(profile)

    fun entry(profile: Int, slot: Int): RadialEntry = slots(profile)[slot.coerceIn(0, SLOT_COUNT - 1)]

    fun swap(profile: Int, from: Int, to: Int) {
        val list = slots(profile)
        if (from !in list.indices || to !in list.indices || from == to) return
        val entry = list[from]
        list[from] = list[to]
        list[to] = entry
        save()
    }

    fun icon(itemId: String): ItemStack? {
        val id = itemId.trim()
        if (id.isEmpty()) return null
        iconCache[id]?.let { return it }

        val item = item(id) ?: return null
        if (!item.builtInRegistryHolder().areComponentsBound()) return null
        return ItemStack(item).also { iconCache[id] = it }
    }

    fun isKnownItem(itemId: String): Boolean = item(itemId.trim()) != null

    fun displayId(itemId: String): String {
        val id = itemId.trim()
        return if (id.startsWith(NAMESPACE)) id.removePrefix(NAMESPACE) else id
    }

    fun itemIds(): List<String> = itemIdsCache ?: BuiltInRegistries.ITEM.keySet()
        .filter { it.namespace == Identifier.DEFAULT_NAMESPACE }
        .map { it.path }
        .sorted()
        .also { itemIdsCache = it }

    fun suggestItems(input: String): List<String> {
        val query = displayId(input).lowercase()
        if (query.isEmpty()) return itemIds()
        return itemIds().filter { it.startsWith(query) }
    }

    private fun item(id: String): Item? {
        if (id.isEmpty()) return null
        val identifier = Identifier.tryParse(if (id.contains(':')) id else "minecraft:$id") ?: return null
        return BuiltInRegistries.ITEM.getOptional(identifier).orElse(null)
    }

    fun load() {
        profiles.clear()
        profiles.putAll(defaultProfiles())

        if (!configFile.exists()) {
            save()
            return
        }

        val text = runCatching { Files.readString(configFile.toPath(), StandardCharsets.UTF_8) }.getOrNull()
        val loaded = text?.let { runCatching { readProfiles(JsonParser.parseString(it)) }.getOrNull() }
        if (loaded == null) backup(text) else applyLoaded(loaded)
    }

    fun save() {
        try {
            configDir.mkdirs()
            Files.writeString(configFile.toPath(), gson.toJson(profiles), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Hkim.logger.error("Failed to save radial menu layout: ${e.message}")
        }
    }

    private fun readProfiles(root: JsonElement): List<List<RadialEntry>> {
        val byName: Map<String, List<RadialEntry>> = gson.fromJson(root, byNameType) ?: emptyMap()
        return PROFILE_NAMES.map { byName[it].orEmpty() }
    }

    private fun applyLoaded(loaded: List<List<RadialEntry>>) {
        loaded.forEachIndexed { profileIndex, entries ->
            if (profileIndex !in PROFILE_NAMES.indices) return@forEachIndexed
            val slots = profiles.getValue(PROFILE_NAMES[profileIndex])
            entries.take(SLOT_COUNT).forEachIndexed { index, entry ->
                if (entry != null) slots[index] = entry
            }
        }
    }

    private fun backup(text: String?) {
        if (text == null) return
        runCatching {
            Files.writeString(backupFile.toPath(), text, StandardCharsets.UTF_8)
            Hkim.logger.warn("Could not parse radial_menu.json, kept a copy as ${backupFile.name}")
        }
    }

    private fun defaultProfiles(): LinkedHashMap<String, MutableList<RadialEntry>> {
        val map = LinkedHashMap<String, MutableList<RadialEntry>>()
        for (name in PROFILE_NAMES) {
            map[name] = MutableList(SLOT_COUNT) { RadialEntry() }
        }

        val starter = listOf(
            RadialEntry().apply { item = "barrel"; name = "Loadouts"; description = "Loadouts"; command = "ld" },
            RadialEntry().apply { item = "diamond"; name = "BZ"; description = "Bazaar"; command = "bz" },
            RadialEntry().apply { item = "bone"; name = "Pets"; description = "Pets"; command = "pets" },
            RadialEntry().apply { item = "leather_chestplate"; name = "Wardrobe"; description = "Wardrobe"; command = "wd" },
            RadialEntry().apply { item = "white_harness"; name = "Equipment"; description = "Equipment Wardrobe"; command = "eq" },
            RadialEntry().apply { item = "ender_chest"; name = "Storage"; description = "Storage"; command = "storage" },
            RadialEntry().apply { item = "potion"; name = "Potion Bag"; description = "Potion Bag"; command = "pb" },
            RadialEntry().apply { item = "gold_block"; name = "AH"; description = "Auction House"; command = "ah" },
        )
        val firstProfile = map.getValue(PROFILE_NAMES.first())
        starter.forEachIndexed { index, entry -> firstProfile[index] = entry }

        return map
    }
}
