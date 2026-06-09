package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim
import cn.hkim.addon.config.settings.ActionSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import net.fabricmc.loader.api.FabricLoader
import java.io.File

@ModuleInfo("main_menu", Category.MISC, true)
object MainMenuModule : Module("Main Menu", "Custom main menu.") {
    val switchInterval by NumberSetting("Switch Interval (s)", "Background image switching interval.", 10, 3, 60, 1)

    private val openBackgroundsFolder by ActionSetting("Open Backgrounds Folder", "Open the background images folder.") {
        val bgDir = File(FabricLoader.getInstance().configDir.toFile(), "hkim/backgrounds")
        if (!bgDir.exists()) bgDir.mkdirs()
        try {
            Runtime.getRuntime().exec(arrayOf("explorer.exe", bgDir.absolutePath))
        } catch (e: Exception) {
            Hkim.logger.error("Failed to open backgrounds folder: ${e.message}")
        }
    }
}
