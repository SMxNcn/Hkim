package cn.hkim.addon.utils.skyblock.inventory

import cn.hkim.addon.utils.Colors
import net.minecraft.resources.Identifier
import java.awt.Color

enum class ItemRarity(
    val baseColor: String,
    val color: Color
) {
    COMMON("§f", Colors.WHITE),
    UNCOMMON("§a", Colors.MINECRAFT_GREEN),
    RARE("§9", Colors.MINECRAFT_BLUE),
    EPIC("§5", Colors.MINECRAFT_DARK_PURPLE),
    LEGENDARY("§6", Colors.MINECRAFT_GOLD),
    MYTHIC("§d", Colors.MINECRAFT_LIGHT_PURPLE),
    DIVINE("§b", Colors.MINECRAFT_AQUA),
    SPECIAL("§c", Colors.MINECRAFT_RED),
    VERY_SPECIAL("§c", Colors.MINECRAFT_RED),
    ULTIMATE("§4", Colors.MINECRAFT_DARK_RED);

    val loreName: String = name.replace('_', ' ')
    val stylePath: String get() = name.lowercase()
    val tooltipStyle: Identifier get() = Identifier.withDefaultNamespace(stylePath)

    companion object {
        val RARITY_PATTERN by lazy {
            Regex("(?:§[\\da-f]§l§ka§r )?(?<rarity>${
                entries.joinToString("|") { "(?:${it.baseColor}§l)+(?:SHINY )?${it.loreName}" }
            })")
        }

        @JvmStatic
        fun fromStylePath(path: String): ItemRarity? = entries.find { it.stylePath == path }
    }
}
