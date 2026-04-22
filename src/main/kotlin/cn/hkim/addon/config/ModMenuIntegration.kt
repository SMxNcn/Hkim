package cn.hkim.addon.config

import cn.hkim.addon.config.clickgui.ClickGUIScreen
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent -> ClickGUIScreen(parent) }
    }
}