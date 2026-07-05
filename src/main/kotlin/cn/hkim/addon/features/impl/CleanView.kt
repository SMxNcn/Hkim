package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.cleanString
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.item.Items

// The name CleanView may not accurately represent the GUI features and could be changed later.
@ModuleInfo("clean_view", Category.RENDER)
object CleanView : Module("Clean View", "Hides unwanted renderings.") {
    private val entity by DropdownSetting("Entity", defaultExpanded = true)
    private val hideFallingBlock by BooleanSetting("Hide Falling Block", "Stop rendering falling blocks.", false).depends { entity }
    private val hideExperienceOrbs by BooleanSetting("Hide Experience Orbs", "Hide experience orbs.", false).depends { entity }
    private val hideLightning by BooleanSetting("Hide Lightning", "Hide lightning bolts.", false).depends { entity }

    private val particle by DropdownSetting("Particle", defaultExpanded = true)
    private val hideWitherImpact by BooleanSetting("Hide Wither Impact", "Hide explosion particles.", false).depends { particle }

    private val effect by DropdownSetting("Others", defaultExpanded = true)
    private val hideBlindness by BooleanSetting("Hide Blindness", "Remove blindness ans darkness effect.", false).depends { effect }
    private val hideFireOverlay by BooleanSetting("Hide Fire Overlay", "Hide fire overlay on screen.", false).depends { effect }
    private val hideEntityFire by BooleanSetting("Hide Entity Fire", "Hide fire overlay on other entities.", false).depends { effect && hideFireOverlay }
    private val hideBlockStuck by BooleanSetting("See Through Blocks", "Removes the suffocation overlay when stuck in blocks.", false).depends { effect }
    private val disableLavaFog by BooleanSetting("Disable Lava Fog", "Removes fog while in lava.", false).depends { effect }

    private val gui by DropdownSetting("GUI")
    private val cleanGui by BooleanSetting("Clean Gui View", "Stop clicking on blank name items in SkyBlock menus.", true).depends { gui }
    private val blockHoveringClose by BooleanSetting("Block Hovering Close", "Prevent clicking 'Close' button with item on cursor.", false).depends { gui }

    @EventHandler
    private fun onGuiClick(event: GuiEvent.SlotClick) {
        if (!enabled || !LocationUtils.inSkyBlock) return
        val chest = mc.player?.containerMenu as? ChestMenu ?: return
        val slot = chest.slots.getOrNull(event.slotId) ?: return

        if (cleanGui && slot.hasItem() && slot.item.displayName.cleanString.isBlank()) {
            event.cancel()
            return
        }

        if (blockHoveringClose && slot.hasItem() && slot.item.item == Items.BARRIER && slot.item.displayName.cleanString == "Close" && !chest.carried.isEmpty) {
            event.cancel()
        }
    }

    @JvmStatic
    fun shouldHideFallingBlocks(): Boolean = enabled && hideFallingBlock

    @JvmStatic
    fun shouldHideExperienceOrbs(): Boolean = enabled && hideExperienceOrbs

    @JvmStatic
    fun shouldHideLightning(): Boolean = enabled && hideLightning

    @JvmStatic
    fun shouldHideWitherImpact(): Boolean = enabled && hideWitherImpact

    @JvmStatic
    fun shouldHideBlindness(): Boolean = enabled && hideBlindness

    @JvmStatic
    fun shouldHideFireOverlay(): Boolean = enabled && hideFireOverlay

    @JvmStatic
    fun shouldHideEntityFire(): Boolean = enabled && hideFireOverlay && hideEntityFire

    @JvmStatic
    fun shouldSeeThroughBlocks(): Boolean = enabled && hideBlockStuck

    @JvmStatic
    fun shouldDisableLavaFog(): Boolean = enabled && disableLavaFog
}