package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.TextSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.mcVersion
import cn.hkim.addon.utils.skyblock.DungeonUtils
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils

@ModuleInfo("title", Category.MISC, true)
object TitleManager : Module("Title Manager", "Manage game title.") {
    @JvmStatic val titleText by TextSetting("Title Text", "Main title.", "愿「开拓」的结局如我们所书♪")
    @JvmStatic val showPlayer by BooleanSetting("Player", "Current player username", false)
    @JvmStatic val showLocation by BooleanSetting("Location", "Current SkyBlock location.", false)
    @JvmStatic val customIcon by BooleanSetting("Custom Icon", "Restart the game to apply icon changes.", false)

    fun buildTitle(): String {
        val sb = StringBuilder(titleText.ifBlank { "Minecraft $mcVersion" })

        if (showPlayer) mc.player?.name?.string?.let { sb.append(" | ").append(it) }
        if (showLocation) {
            val locationText = when (val area = LocationUtils.currentArea) {
                Island.Unknown -> ""
                Island.Dungeon -> "Catacombs ${DungeonUtils.floor?.name.orEmpty()}"
                Island.Kuudra -> "Kuudra's Hollow ${LocationUtils.kuudraTier}"
                else -> area.displayName
            }
            if (locationText.isNotEmpty()) sb.append(" | ").append(locationText)
        }

        return sb.toString()
    }
}