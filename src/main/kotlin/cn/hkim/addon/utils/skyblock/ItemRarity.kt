package cn.hkim.addon.utils.skyblock

import cn.hkim.addon.utils.Colors
import java.awt.Color

enum class ItemRarity(
    val loreName: String,
    val colorCode: String,
    val color: Color
) {
    COMMON("COMMON", "§f", Colors.WHITE),
    UNCOMMON("UNCOMMON", "§2", Colors.MINECRAFT_GREEN),
    RARE("RARE", "§9", Colors.MINECRAFT_BLUE),
    EPIC("EPIC", "§5", Colors.MINECRAFT_DARK_PURPLE),
    LEGENDARY("LEGENDARY", "§6", Colors.MINECRAFT_GOLD),
    MYTHIC("MYTHIC", "§d", Colors.MINECRAFT_LIGHT_PURPLE),
    DIVINE("DIVINE", "§b", Colors.MINECRAFT_AQUA),
    SPECIAL("SPECIAL", "§c", Colors.MINECRAFT_RED),
    VERY_SPECIAL("VERY SPECIAL", "§c", Colors.MINECRAFT_RED),
    ULTIMATE("ULTIMATE", "§4", Colors.MINECRAFT_DARK_RED);
}

val rarityRegex = Regex("(${ItemRarity.entries.joinToString("|") { it.loreName }}) ?([A-Z ]+)?")