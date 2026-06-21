package cn.hkim.addon.features.impl

import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo

@ModuleInfo("clean_view", Category.RENDER)
object CleanView : Module("Clean View", "Hide unnecessary renderings.") {
    private val entity by DropdownSetting("Entity", defaultExpanded = true)
    private val hideFallingBlock by BooleanSetting("Hide Falling Block", "Stop rendering falling blocks.", false).depends { entity }
    private val hideExperienceOrbs by BooleanSetting("Hide Experience Orbs", "Hide experience orbs.", false).depends { entity }
    private val hideLightning by BooleanSetting("Hide Lightning", "Hide lightning bolts.", false).depends { entity }

    private val particle by DropdownSetting("Particle", defaultExpanded = true)
    private val hideWitherImpact by BooleanSetting("Hide Wither Impact", "Hide explosion particles.", false).depends { particle }

    private val effect by DropdownSetting("Others", defaultExpanded = true)
    private val hideBlindness by BooleanSetting("Hide Blindness", "Remove blindness effect.", false).depends { effect }
    private val hideFireOverlay by BooleanSetting("Hide Fire Overlay", "Hide fire overlay on screen.", false).depends { effect }
    private val hideEntityFire by BooleanSetting("Hide Entity Fire", "Hide fire overlay on other entities.", false).depends { effect && hideFireOverlay }
    private val hideBlockStuck by BooleanSetting("See Through Blocks", "Makes blocks transparent.", false).depends { effect }

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
}
