package cn.hkim.addon.config

import cn.hkim.addon.Hkim
import cn.hkim.addon.config.settings.*
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleManager
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.math.roundToInt

object ModuleConfig {
    private val configDir = File(FabricLoader.getInstance().configDir.toFile(), "hkim")
    private val configFile = File(configDir, "hkim-config.json")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun loadConfig() {
        if (!configFile.exists()) {
            Hkim.logger.info("Config file not found, using defaults.")
            return
        }

        try {
            val jsonContent = Files.readString(configFile.toPath(), StandardCharsets.UTF_8)
            val rootJson = gson.fromJson(jsonContent, JsonObject::class.java) ?: return

            for (module in ModuleManager.getAll()) {
                val moduleJson = rootJson.getAsJsonObject(module.id) ?: continue
                if (moduleJson.has("enabled")) {
                    module.enabled = moduleJson.get("enabled").asBoolean
                }
                applySettingsToModule(module, moduleJson)
                module.hudElements.forEach { it.loadFrom(moduleJson) }
            }
        } catch (e: Exception) {
            Hkim.logger.error("Failed to load config: ${e.message}")
            e.printStackTrace()
        }
    }

    fun saveConfig() {
        configDir.mkdirs()
        try {
            val rootJson = JsonObject()

            for (module in ModuleManager.getAll()) {
                val moduleJson = JsonObject()
                moduleJson.addProperty("enabled", module.enabled)
                serializeModuleSettings(module, moduleJson)
                module.hudElements.forEach { it.saveTo(moduleJson) }

                if (!moduleJson.entrySet().isEmpty()) {
                    rootJson.add(module.id, moduleJson)
                }
            }

            Files.writeString(configFile.toPath(), gson.toJson(rootJson), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Hkim.logger.error("Failed to save config: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun applySettingsToModule(module: Module, json: JsonObject) {
        for (setting in module.settings) {
            if (!setting.shouldSave) continue

            val key = setting.configKey
            if (!json.has(key)) continue

            try {
                val element = json.get(key)
                when (setting) {
                    is BooleanSetting -> setting.set(element.asBoolean)
                    is ColorSetting -> setting.set(ColorSetting.fromHexString(element.asString))
                    is NumberSetting -> setting.set(snapNumber(element.asDouble, setting.min, setting.max, setting.step))
                    is KeybindSetting -> {
                        val strVal = element.asString
                        setting.set(KeybindSetting.glfwNameToKeyCode(strVal))
                    }
                    is TextSetting -> setting.set(element.asString)
                    is SelectorSetting -> {
                        val strVal = element.asString
                        if (strVal in setting.options) {
                            setting.select(strVal)
                        }
                    }
                }
            } catch (e: Exception) {
                Hkim.logger.warn("Failed to load setting '${setting.name}' in module '${module.id}': ${e.message}")
            }
        }
    }

    private fun serializeModuleSettings(module: Module, json: JsonObject) {
        for (setting in module.settings) {
            if (!setting.shouldSave) continue

            val key = setting.configKey
            val value = setting.get()

            when (setting) {
                is BooleanSetting -> json.addProperty(key, value as Boolean)
                is ColorSetting -> json.addProperty(key, ColorSetting.toHexString(value as Int))
                is NumberSetting -> json.addProperty(key, (cleanDoubleForJson(value as Number)))
                is KeybindSetting -> json.addProperty(key, KeybindSetting.keyCodeToGlfwName(value as Int))
                is TextSetting -> json.addProperty(key, value as String)
                is SelectorSetting -> json.addProperty(key, setting.getSelected())
            }
        }
    }

    private fun cleanDoubleForJson(number: Number) = (number.toDouble() * 10000.0).roundToInt() / 10000.0

    private fun snapNumber(value: Double, min: Float, max: Float, step: Float): Float {
        val stepVal = step.toDouble()
        if (stepVal <= 0) return value.toFloat()

        val steps = ((value - min.toDouble()) / stepVal).roundToInt()
        var snapped = min.toDouble() + steps * stepVal
        snapped = snapped.coerceIn(min.toDouble(), max.toDouble())

        return snapped.toFloat()
    }
}