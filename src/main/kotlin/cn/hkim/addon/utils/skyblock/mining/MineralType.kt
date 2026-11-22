package cn.hkim.addon.utils.skyblock.mining

import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

enum class MineralCategory {
    DWARVEN_METALS,
    GEMSTONES,
    ORE,
    RIFT
}

enum class MineralType(
    val displayName: String,
    val category: MineralCategory,
    val blocks: List<Block>,
    val strength: Int,  // https://hypixelskyblock.minecraft.wiki/w/Block_Strength (Ref: AHN 2026-11-03)
    val strengthOverrides: Map<Block, Int> = emptyMap(),
    val instantMultiplier: Int = 60,
    val priorityBlocks: Set<Block> = emptySet()
) {
    MITHRIL(
        "Mithril", MineralCategory.DWARVEN_METALS, listOf(
            Blocks.WOOL.lightBlue,
            Blocks.PRISMARINE,
            Blocks.PRISMARINE_BRICKS,
            Blocks.DARK_PRISMARINE,
            Blocks.WOOL.gray,
            Blocks.DYED_TERRACOTTA.cyan,
            Blocks.POLISHED_DIORITE
        ),
        strength = 500,
        strengthOverrides = mapOf(
            Blocks.PRISMARINE to 800,
            Blocks.PRISMARINE_BRICKS to 800,
            Blocks.DARK_PRISMARINE to 800,
            Blocks.WOOL.lightBlue to 1500,
            Blocks.POLISHED_DIORITE to 2000
        ),
        priorityBlocks = setOf(Blocks.POLISHED_DIORITE, Blocks.WOOL.lightBlue, Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE)
    ),
    UMBER(
        "Umber", MineralCategory.DWARVEN_METALS, listOf(
            Blocks.SMOOTH_RED_SANDSTONE,
            Blocks.DYED_TERRACOTTA.brown,
            Blocks.TERRACOTTA
        ),
        strength = 5600,
        priorityBlocks = setOf(Blocks.SMOOTH_RED_SANDSTONE)
    ),
    TUNGSTEN(
        "Tungsten", MineralCategory.DWARVEN_METALS, listOf(
            Blocks.CLAY,
            Blocks.COBBLESTONE_SLAB,
            Blocks.COBBLESTONE_STAIRS,
            Blocks.COBBLESTONE
        ),
        strength = 5600,
        priorityBlocks = setOf(Blocks.CLAY)
    ),
    GLACITE(
        "Glacite", MineralCategory.DWARVEN_METALS, listOf(
            Blocks.PACKED_ICE
        ),
        strength = 6000
    ),

    RUBY(
        "Ruby", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.red,
            Blocks.STAINED_GLASS_PANE.red
        ),
        strength = 2500
    ),
    AMBER(
        "Amber", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.orange,
            Blocks.STAINED_GLASS_PANE.orange
        ),
        strength = 3000
    ),
    SAPPHIRE(
        "Sapphire", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.lightBlue,
            Blocks.STAINED_GLASS_PANE.lightBlue
        ),
        strength = 3000
    ),
    JADE(
        "Jade", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.lime,
            Blocks.STAINED_GLASS_PANE.lime
        ),
        strength = 3000
    ),
    AMETHYST(
        "Amethyst", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.purple,
            Blocks.STAINED_GLASS_PANE.purple
        ),
        strength = 3000
    ),
    TOPAZ(
        "Topaz", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.yellow,
            Blocks.STAINED_GLASS_PANE.yellow
        ),
        strength = 3500
    ),
    JASPER(
        "Jasper", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.magenta,
            Blocks.STAINED_GLASS_PANE.magenta
        ),
        strength = 4000
    ),
    OPAL(
        "Opal", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.white,
            Blocks.STAINED_GLASS_PANE.white
        ),
        strength = 3000
    ),
    ONYX(
        "Onyx", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.black,
            Blocks.STAINED_GLASS_PANE.black
        ),
        strength = 4000
    ),
    AQUAMARINE(
        "Aquamarine", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.blue,
            Blocks.STAINED_GLASS_PANE.blue
        ),
        strength = 4000
    ),
    CITRINE(
        "Citrine", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.brown,
            Blocks.STAINED_GLASS_PANE.brown
        ),
        strength = 4000
    ),
    PERIDOT(
        "Peridot", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.green,
            Blocks.STAINED_GLASS_PANE.green
        ),
        strength = 4000
    ),

    GOLD(
        "Gold", MineralCategory.ORE, listOf(
            Blocks.GOLD_BLOCK
        ),
        strength = 600
    ),

    YOUNGITE(
        "Youngite", MineralCategory.RIFT, listOf(
            Blocks.STAINED_GLASS_PANE.lightBlue
        ),
        strength = 0
    ),
    TIMITE(
        "Timite", MineralCategory.RIFT, listOf(
            Blocks.STAINED_GLASS_PANE.blue
        ),
        strength = 0
    ),
    OBSOLITE(
        "Obsolite", MineralCategory.RIFT, listOf(
            Blocks.STAINED_GLASS_PANE.purple
        ),
        strength = 0
    );

    fun strengthOf(block: Block): Int = strengthOverrides[block] ?: strength

    companion object {
        private val RIFT_VARIANTS: Map<Block, MineralType> = mapOf(
            Blocks.STAINED_GLASS_PANE.lightBlue to YOUNGITE,
            Blocks.STAINED_GLASS_PANE.blue to TIMITE,
            Blocks.STAINED_GLASS_PANE.purple to OBSOLITE,
        )

        private val blockLookup: Map<Block, MineralType> by lazy {
            val pairs = entries.flatMap { type -> type.blocks.map { it to type } }
                .filterNot { (block, type) -> type.category == MineralCategory.RIFT && block in RIFT_VARIANTS }
            val conflicts = pairs.groupBy { it.first }.filterValues { it.size > 1 }.keys
            check(conflicts.isEmpty()) { "Ambiguous mineral blocks: $conflicts" }
            pairs.toMap()
        }

        fun fromBlock(block: Block): MineralType? {
            if (LocationUtils.currentArea == Island.Rift) RIFT_VARIANTS[block]?.let { return it }
            return blockLookup[block]
        }

        fun MineralType.isHighPriorityBlock(block: Block): Boolean = block in priorityBlocks

        fun byCategory(category: MineralCategory): List<MineralType> =
            entries.filter { it.category == category }
    }
}
