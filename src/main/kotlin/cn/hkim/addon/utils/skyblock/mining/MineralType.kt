package cn.hkim.addon.utils.skyblock.mining

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

enum class MineralCategory {
    DWARVEN_METALS,
    GEMSTONES,
    ORE
}

enum class MineralType(
    val displayName: String,
    val category: MineralCategory,
    val blocks: List<Block>,
    val priorityBlocks: Set<Block> = emptySet()
) {
    MITHRIL(
        "Mithril", MineralCategory.DWARVEN_METALS, listOf(
            Blocks.WOOL.lightBlue(),
            Blocks.PRISMARINE,
            Blocks.PRISMARINE_BRICKS,
            Blocks.DARK_PRISMARINE,
            Blocks.WOOL.gray(),
            Blocks.DYED_TERRACOTTA.cyan()
        ),
        priorityBlocks = setOf(Blocks.WOOL.lightBlue(), Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE)
    ),
    TITANIUM(
        "Titanium", MineralCategory.DWARVEN_METALS, listOf(
            Blocks.POLISHED_DIORITE
        )
    ),
    UMBER(
        "Umber", MineralCategory.DWARVEN_METALS, listOf(
            Blocks.SMOOTH_RED_SANDSTONE,
            Blocks.DYED_TERRACOTTA.brown(),
            Blocks.TERRACOTTA
        ),
        priorityBlocks = setOf(Blocks.SMOOTH_RED_SANDSTONE)
    ),
    TUNGSTEN(
        "Tungsten", MineralCategory.DWARVEN_METALS, listOf(
            Blocks.CLAY,
            Blocks.COBBLESTONE_SLAB,
            Blocks.COBBLESTONE_STAIRS,
            Blocks.COBBLESTONE
        ),
        priorityBlocks = setOf(Blocks.CLAY)
    ),
    GLACITE(
        "Glacite", MineralCategory.DWARVEN_METALS, listOf(
            Blocks.PACKED_ICE
        )
    ),

    RUBY(
        "Ruby", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.red(),
            Blocks.STAINED_GLASS_PANE.red()
        )
    ),
    AMBER(
        "Amber", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.orange(),
            Blocks.STAINED_GLASS_PANE.orange()
        )
    ),
    SAPPHIRE(
        "Sapphire", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.lightBlue(),
            Blocks.STAINED_GLASS_PANE.lightBlue()
        )
    ),
    JADE(
        "Jade", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.lime(),
            Blocks.STAINED_GLASS_PANE.lime()
        )
    ),
    AMETHYST(
        "Amethyst", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.purple(),
            Blocks.STAINED_GLASS_PANE.purple()
        )
    ),
    TOPAZ(
        "Topaz", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.yellow(),
            Blocks.STAINED_GLASS_PANE.yellow()
        )
    ),
    JASPER(
        "Jasper", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.magenta(),
            Blocks.STAINED_GLASS_PANE.magenta()
        )
    ),
    OPAL(
        "Opal", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.white(),
            Blocks.STAINED_GLASS_PANE.white()
        )
    ),
    ONYX(
        "Onyx", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.black(),
            Blocks.STAINED_GLASS_PANE.black()
        )
    ),
    AQUAMARINE(
        "Aquamarine", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.blue(),
            Blocks.STAINED_GLASS_PANE.blue()
        )
    ),
    CITRINE(
        "Citrine", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.brown(),
            Blocks.STAINED_GLASS_PANE.brown()
        )
    ),
    PERIDOT(
        "Peridot", MineralCategory.GEMSTONES, listOf(
            Blocks.STAINED_GLASS.green(),
            Blocks.STAINED_GLASS_PANE.green()
        )
    ),

    GOLD(
        "Gold", MineralCategory.ORE, listOf(
            Blocks.GOLD_BLOCK
        )
    );

    companion object {
        private val blockLookup: Map<Block, MineralType> by lazy {
            entries.flatMap { type -> type.blocks.map { it to type } }.toMap()
        }

        fun fromBlock(block: Block): MineralType? = blockLookup[block]

        fun MineralType.isHighPriorityBlock(block: Block): Boolean = block in priorityBlocks

        fun byCategory(category: MineralCategory): List<MineralType> =
            entries.filter { it.category == category }
    }
}
