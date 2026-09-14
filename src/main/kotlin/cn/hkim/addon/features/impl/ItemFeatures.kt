package cn.hkim.addon.features.impl

import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.config.settings.SelectorSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.HudUtils.multiplyShade
import cn.hkim.addon.utils.getItemRarity
import cn.hkim.addon.utils.isSkyBlockItem
import cn.hkim.addon.utils.skyblock.LocationUtils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack

@ModuleInfo("item_features", Category.SKYBLOCK, true)
object ItemFeatures : Module("Item Features", "Item related features for SkyBlock items.") {

    private val starDisplay by BooleanSetting("Star Display", "Show item upgrade level as stack size.", true)
    private val rarityTooltip by BooleanSetting("Rarity Tooltip", "Draw a rarity tooltip frame for SkyBlock items.", false)

    private val rarityBackground by BooleanSetting("Rarity Background", "Fill inventory/hotbar slots with the item rarity color.", false)
    private val rarityBgMode by SelectorSetting("Background Type", "Rendering mode.", listOf("Outline", "Filled", "Filled Outline"), "Filled Outline").depends { rarityBackground }
    private val rarityBgAlpha by NumberSetting("Background Opacity", "", 50f, 0f, 100f, 1f, "%").depends { rarityBackground }
    private val rarityBgShade by NumberSetting("Background Darkness", "", 10f, 0f, 100f, 1f, "%").depends { rarityBackground }

    val isStarDisplayEnabled: Boolean get() = enabled && starDisplay
    val isRarityTooltipEnabled: Boolean get() = enabled && rarityTooltip
    val isRarityBackgroundEnabled: Boolean get() = enabled && rarityBackground

    @JvmStatic
    fun drawRarityBackground(graphics: GuiGraphicsExtractor, x: Int, y: Int, stack: ItemStack) {
        if (!isRarityBackgroundEnabled || !LocationUtils.inSkyBlock) return
        if (stack.isEmpty || !isSkyBlockItem(stack)) return
        val rarity = getItemRarity(stack) ?: return
        if (rarityBgAlpha.toInt() <= 0) return

        val base = rarity.color.multiplyShade(rarityBgShade / 100f)
        val fillColor = (rarityBgAlpha.toInt() * 255 / 100 shl 24) or (base.red shl 16) or (base.green shl 8) or base.blue
        val borderColor = (0xFF shl 24) or (base.red shl 16) or (base.green shl 8) or base.blue

        when (rarityBgMode) {
            0 -> graphics.outline(x, y, 16, 16, borderColor)
            1 -> graphics.fill(x, y, x + 16, y + 16, fillColor)
            2 -> {
                graphics.fill(x, y, x + 16, y + 16, fillColor)
                graphics.outline(x, y, 16, 16, borderColor)
            }
        }
    }
}
