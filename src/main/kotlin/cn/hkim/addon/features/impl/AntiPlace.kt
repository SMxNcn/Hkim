package cn.hkim.addon.features.impl

import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.isBlockCarrier
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack

@ModuleInfo("anti_place", Category.MISC)
object AntiPlace : Module("Anti Place", "Prevent placing block-based weapons and items on the ground.") {

    @JvmStatic
    fun shouldCancelPlacement(stack: ItemStack): Boolean {
        if (!enabled) return false
        if (!LocationUtils.inSkyBlock || LocationUtils.isCurrentArea(Island.PrivateIsland)) return false
        return stack.item is BlockItem && isBlockCarrier(stack)
    }
}