package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.TextSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.skyblock.DungeonUtils
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import net.minecraft.SharedConstants

@ModuleInfo("title", Category.MISC, true)
object TitleManager : Module("Title Manager", "Manage game title.") {
    @JvmStatic val titleText by TextSetting("Title Text", "Main title.", "愿「开拓」的结局如我们所书♪")
    @JvmStatic val showPlayer by BooleanSetting("Player", "Current player username", false)
    @JvmStatic val showLocation by BooleanSetting("Location", "Current SkyBlock location.", false)
    @JvmStatic val customIcon by BooleanSetting("Custom Icon", "Restart the game to apply icon changes.", false)

    fun buildTitle(): String {
        val sb = StringBuilder(titleText.ifEmpty { "Minecraft ${SharedConstants.getCurrentVersion().name()}" })
        val playerName = mc.player?.name?.string
        val locationText = LocationUtils.currentArea.let { area ->
            area.displayName + if (area == Island.Dungeon) { " ${DungeonUtils.floor?.name.orEmpty()}" } else ""
        }

        if (showPlayer && playerName != null) sb.append(" | ").append(playerName)
        if (showLocation && !locationText.contains("Unknown")) sb.append(" | ").append(locationText)

        return sb.toString()
    }
}