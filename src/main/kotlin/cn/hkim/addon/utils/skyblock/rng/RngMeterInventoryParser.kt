package cn.hkim.addon.utils.skyblock.rng

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.legacy
import cn.hkim.addon.utils.lore
import cn.hkim.addon.utils.skyblock.Floor
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.slayer.SlayerType
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu

/**
 * Reads the unified RNG meter lore block:
 *
 * Selected Drop:
 * {item}
 *
 * Progress: 12.3%
 * -------------------------- {current}/{needed}
 */
object RngMeterInventoryParser {
    private val progressRegex = Regex("""^Progress:\s*[\d.]+\s*%$""", RegexOption.IGNORE_CASE)
    private val pairRegex = Regex("""([\d,.]+[kKmM]?)\s*/\s*([\d,.]+[kKmM]?)""")
    private val floorRegex = Regex("""Catacombs \((F\d|M\d)\)""", RegexOption.IGNORE_CASE)

    fun handle(title: String, menu: AbstractContainerMenu) {
        if (!LocationUtils.inSkyBlock) return
        if (!title.contains("RNG Meter", ignoreCase = true)) return

        val playerInventory = mc.player?.inventory

        for (slot in menu.slots) {
            if (playerInventory != null && slot.container === playerInventory) continue

            val stack = slot.item
            if (stack.isEmpty) continue

            val lore = stack.lore
            if (lore.isEmpty()) continue

            val cleanLore = lore.map { it.string.clean.trim() }
            val block = parseMeterBlock(cleanLore, lore) ?: continue

            val itemName = stack.hoverName.string.clean.trim()
            val key = resolveKey(title, itemName, cleanLore) ?: continue

            RngMeterStorage.setScore(key, block.current)

            val item = block.item
            if (item != null) {
                RngMeterStorage.setItem(key, item)
                val tableNeeded = RngMeterStorage.getNeeded(key, item)
                (tableNeeded ?: block.needed)?.let { RngMeterStorage.setNeeded(key, it) }
            } else {
                block.needed?.let { RngMeterStorage.setNeeded(key, it) }
            }
        }
    }

    private data class MeterBlock(
        val item: String?,
        val current: Long,
        val needed: Long?,
    )

    private fun parseMeterBlock(cleanLore: List<String>, lore: List<Component>): MeterBlock? {
        val selectedDropIndex = cleanLore.indexOfFirst {
            it.removeSuffix(":").equals("Selected Drop", ignoreCase = true)
        }
        if (selectedDropIndex == -1) return null

        val itemComponent = lore.drop(selectedDropIndex + 1)
            .takeWhile { !progressRegex.matches(it.string.clean.trim()) }
            .firstOrNull { it.string.isNotBlank() }
        val item = itemComponent?.let { component ->
            val plain = component.string.clean.trim()
            if (plain.isEmpty() || plain.equals("None", ignoreCase = true)) null
            else component.legacy.trim()
        }

        var current: Long? = null
        var needed: Long? = null
        for (i in selectedDropIndex + 2 until cleanLore.size) {
            val match = pairRegex.findAll(cleanLore[i]).lastOrNull() ?: continue
            current = RngUtils.parseAmount(match.groupValues[1])
            needed = RngUtils.parseAmount(match.groupValues[2])
            break
        }

        val score = current ?: return null
        return MeterBlock(item, score, needed)
    }

    private fun resolveKey(title: String, itemName: String, cleanLore: List<String>): String? {
        if (itemName.equals("Frozen Corpses RNG Meter", ignoreCase = true)) return RngUtils.GLACITE_KEY

        floorKey(itemName)?.let { return it }
        SlayerType.fromText(itemName)?.let { return it.meterKey }

        for (line in cleanLore) {
            floorKey(line)?.let { return it }
        }
        for (line in cleanLore) {
            SlayerType.fromText(line)?.let { return it.meterKey }
        }

        floorKey(title)?.let { return it }
        SlayerType.fromText(title)?.let { return it.meterKey }

        return null
    }

    private fun floorKey(text: String): String? {
        val raw = floorRegex.find(text)?.groupValues?.get(1) ?: return null
        val key = raw.uppercase()
        return Floor.entries.firstOrNull { it.name == key }?.name
    }
}
