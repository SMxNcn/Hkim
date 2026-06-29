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
            Blocks.LIGHT_BLUE_WOOL,
            Blocks.PRISMARINE,
            Blocks.PRISMARINE_BRICKS,
            Blocks.DARK_PRISMARINE,
            Blocks.GRAY_WOOL,
            Blocks.CYAN_TERRACOTTA
        ),
        priorityBlocks = setOf(Blocks.LIGHT_BLUE_WOOL, Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE)
    ),
    TITANIUM(
        "Titanium", MineralCategory.DWARVEN_METALS, listOf(
            Blocks.POLISHED_DIORITE
        )
    ),
    UMBER(
        "Umber", MineralCategory.DWARVEN_METALS, listOf(
            Blocks.SMOOTH_RED_SANDSTONE,
            Blocks.BROWN_TERRACOTTA,
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
            Blocks.RED_STAINED_GLASS,
            Blocks.RED_STAINED_GLASS_PANE
        )
    ),
    AMBER(
        "Amber", MineralCategory.GEMSTONES, listOf(
            Blocks.ORANGE_STAINED_GLASS,
            Blocks.ORANGE_STAINED_GLASS_PANE
        )
    ),
    SAPPHIRE(
        "Sapphire", MineralCategory.GEMSTONES, listOf(
            Blocks.LIGHT_BLUE_STAINED_GLASS,
            Blocks.LIGHT_BLUE_STAINED_GLASS_PANE
        )
    ),
    JADE(
        "Jade", MineralCategory.GEMSTONES, listOf(
            Blocks.LIME_STAINED_GLASS,
            Blocks.LIME_STAINED_GLASS_PANE
        )
    ),
    AMETHYST(
        "Amethyst", MineralCategory.GEMSTONES, listOf(
            Blocks.PURPLE_STAINED_GLASS,
            Blocks.PURPLE_STAINED_GLASS_PANE
        )
    ),
    TOPAZ(
        "Topaz", MineralCategory.GEMSTONES, listOf(
            Blocks.YELLOW_STAINED_GLASS,
            Blocks.YELLOW_STAINED_GLASS_PANE
        )
    ),
    JASPER(
        "Jasper", MineralCategory.GEMSTONES, listOf(
            Blocks.MAGENTA_STAINED_GLASS,
            Blocks.MAGENTA_STAINED_GLASS_PANE
        )
    ),
    OPAL(
        "Opal", MineralCategory.GEMSTONES, listOf(
            Blocks.WHITE_STAINED_GLASS,
            Blocks.WHITE_STAINED_GLASS_PANE
        )
    ),
    ONYX(
        "Onyx", MineralCategory.GEMSTONES, listOf(
            Blocks.BLACK_STAINED_GLASS,
            Blocks.BLACK_STAINED_GLASS_PANE
        )
    ),
    AQUAMARINE(
        "Aquamarine", MineralCategory.GEMSTONES, listOf(
            Blocks.BLUE_STAINED_GLASS,
            Blocks.BLUE_STAINED_GLASS_PANE
        )
    ),
    CITRINE(
        "Citrine", MineralCategory.GEMSTONES, listOf(
            Blocks.BROWN_STAINED_GLASS,
            Blocks.BROWN_STAINED_GLASS_PANE
        )
    ),
    PERIDOT(
        "Peridot", MineralCategory.GEMSTONES, listOf(
            Blocks.GREEN_STAINED_GLASS,
            Blocks.GREEN_STAINED_GLASS_PANE
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
