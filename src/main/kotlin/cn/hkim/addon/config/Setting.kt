package cn.hkim.addon.config

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.features.Module
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class Setting<T>(
    val name: String,
    val desc: String
) : ReadWriteProperty<Module, T>, PropertyDelegateProvider<Module, Setting<T>> {
    abstract val default: T
    open var value: T = default
    var configKey: String = name.replace(" ", "_").lowercase()
    var dependsCondition: (() -> Boolean)? = null
    var shouldSave: Boolean = true

    override operator fun provideDelegate(thisRef: Module, property: KProperty<*>): Setting<T> {
        thisRef.registerSetting(this)
        return this
    }

    override operator fun getValue(thisRef: Module, property: KProperty<*>) = value

    override operator fun setValue(thisRef: Module, property: KProperty<*>, value: T) {
        this.value = value
    }

    fun get(): T = value
    open fun set(newValue: T) { value = newValue }
    fun depends(condition: () -> Boolean): Setting<T> {
        this.dependsCondition = condition
        return this
    }

    fun key(key: String): Setting<T> {
        this.configKey = key
        return this
    }

    fun noSave(): Setting<T> {
        this.shouldSave = false
        return this
    }

    fun isVisible(): Boolean = dependsCondition?.invoke() ?: true

    protected fun settingsChanged() {
        ModuleConfig.saveConfig()
    }

    open fun reset() {
        value = default
    }

    abstract fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int
    ): Float

    protected fun renderDescriptionTooltip(
        graphics: GuiGraphicsExtractor,
        isHovered: Boolean,
        mouseX: Float,
        mouseY: Float
    ) {
        if (isHovered && desc.isNotEmpty()) {
            graphics.setTooltipForNextFrame(mc.font, Component.literal(desc), mouseX.toInt(), mouseY.toInt())
        }
    }

    open fun mouseClicked(
        mouseX: Float, mouseY: Float, button: Int,
        x: Float, y: Float, width: Float
    ): Boolean = false

    open fun mouseReleased(
        mouseX: Float, mouseY: Float, button: Int,
        x: Float, y: Float, width: Float
    ): Boolean = false

    open fun mouseDragged(
        mouseX: Float, mouseY: Float, button: Int,
        deltaX: Float, deltaY: Float,
        x: Float, y: Float, width: Float
    ): Boolean = false
}
