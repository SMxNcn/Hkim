package cn.hkim.addon.utils.ravengard

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.utils.loreString
import meteordevelopment.orbit.EventHandler
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack

val valueRegex = Regex("""([\d.,]+) Crown""")
val defRegex = Regex("""([\d.,]+) Defense""")
val damageRegex = Regex("""([\d.,]+) ?(?:Ranged Attack )?Damage""")
val attackSpeedRegex = Regex("""([\d.,]+) ?(?:Ranged Attack )?Attack Speed""")
val percentRegex = Regex("""([\d.,]+)%""")
val percentOnlyRegex = Regex("""^\+?([\d.,]+)%$""")

const val ABILITY_DAMAGE_SHARE = 0.5

inline val ItemStack.value: Int
    get() = loreString.firstNotNullOfOrNull { line ->
        valueRegex.find(line)?.groups?.get(1)?.value
            ?.replace(",", "")?.toDoubleOrNull()
            ?.toInt()
    } ?: 0

inline val ItemStack.defence: Int
    get() = loreString.firstNotNullOfOrNull { line ->
        defRegex.find(line)?.groups?.get(1)?.value
            ?.replace(",", "")?.toDoubleOrNull()
            ?.toInt()
    } ?: 0

/** Raw weapon damage. Takes the highest "N [Ranged Attack ]Damage" line. */
inline val ItemStack.damage: Double
    get() = loreString.mapNotNull { line ->
        damageRegex.find(line)?.groups?.get(1)?.value
            ?.replace(",", "")?.toDoubleOrNull()
    }.maxOrNull() ?: 0.0

/** "N [Ranged Attack ]Attack Speed", defaulting to 1.0 when the item has no such line. */
inline val ItemStack.attackSpeed: Double
    get() = loreString.mapNotNull { line ->
        attackSpeedRegex.find(line)?.groups?.get(1)?.value
            ?.replace(",", "")?.toDoubleOrNull()
    }.maxOrNull() ?: 1.0

/**
 * Ability Damage Boost in percent.
 * Supports both "10% Ability Damage Boost" on one line and the wrapped form
 * where "+10%" sits on the previous line.
 */
inline val ItemStack.abilityDamageBoost: Double
    get() {
        val lines = loreString
        for (i in lines.indices) {
            if (!lines[i].contains("Ability Damage Boost")) continue

            val raw = percentRegex.find(lines[i])?.groups?.get(1)?.value
                ?: lines.getOrNull(i - 1)?.trim()?.let { percentOnlyRegex.find(it)?.groups?.get(1)?.value }
                ?: continue
            return raw.replace(",", "").toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

inline val ItemStack.weaponScore: Double
    get() = damage * ((1 - ABILITY_DAMAGE_SHARE) * attackSpeed +
            ABILITY_DAMAGE_SHARE * (1 + abilityDamageBoost / 100.0))

object ArmorDefence {
    private val slots = arrayOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
    private val cached = MutableList(slots.size) { 0 }
    private val lastStacks = Array(slots.size) { ItemStack.EMPTY }

    val values: List<Int> get() = cached

    fun defenceOf(slot: EquipmentSlot): Int {
        val index = slots.indexOf(slot)
        return if (index >= 0) cached[index] else 0
    }

    @EventHandler
    private fun onTick(event: TickEvent.Start) {
        val player = mc.player
        for (i in slots.indices) {
            val stack = player?.getItemBySlot(slots[i]) ?: ItemStack.EMPTY
            if (ItemStack.matches(stack, lastStacks[i])) continue

            lastStacks[i] = stack.copy()
            cached[i] = if (stack.isEmpty) 0 else stack.defence
        }
    }
}