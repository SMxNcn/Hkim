package cn.hkim.addon.features

import cn.hkim.addon.config.ModuleConfig
import cn.hkim.addon.config.Setting
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class Module(
    val name: String,
    val description: String
) {
    private val _settings: MutableList<Setting<*>> = mutableListOf()
    val settings: List<Setting<*>> get() = _settings

    var id: String private set
    var category: Category private set
    var default: Boolean = false
        private set

    init {
        val info = this.javaClass.getAnnotation(ModuleInfo::class.java)
            ?: throw IllegalStateException("Module ${this.javaClass.simpleName} must be annotated with @ModuleInfo")
        id = info.id
        category = info.category
        default = info.default
    }

    internal fun registerSetting(setting: Setting<*>) {
        _settings.add(setting)
    }

    var enabled: Boolean = default

    open fun onEnable() {}
    open fun onDisable() {}
    open fun render(graphics: GuiGraphicsExtractor, tickTracker: DeltaTracker) {}

    fun toggle() {
        if (enabled) disable() else enable()
        ModuleConfig.saveConfig()
    }

    fun enable() {
        if (!enabled) {
            enabled = true
            onEnable()
        }
    }

    fun disable() {
        if (enabled) {
            enabled = false
            onDisable()
        }
    }
}