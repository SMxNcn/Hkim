package cn.hkim.addon.features

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.gui.HudEditScreen
import cn.hkim.addon.utils.containsOneOf
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.resources.Identifier
import java.lang.invoke.MethodHandles

object ModuleManager {
    private val modules = mutableListOf<Module>()
    private var hudHookRegistered = false

    fun initOrbit() {
        Hkim.EVENT_BUS.registerLambdaFactory("cn.hkim.addon") { lookupInMethod, klass ->
            try {
                lookupInMethod.invoke(null, klass, MethodHandles.lookup()) as MethodHandles.Lookup
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    private fun register(module: Module) {
        if (module in modules) return
        modules.add(module)
        Hkim.EVENT_BUS.subscribe(module)
    }

    fun registerAll(vararg modules: Module) {
        if (this.modules.isEmpty()) {
            initOrbit()
        }
        modules.forEach { register(it) }
        initHudRenderHook()
    }

    private fun initHudRenderHook() {
        if (hudHookRegistered) return
        hudHookRegistered = true
        HudElementRegistry.attachElementBefore(VanillaHudElements.SLEEP,
            Identifier.fromNamespaceAndPath("hkim", "hud_elements")
        ) { graphics, tick ->
            if (mc.options.hideGui || mc.screen is HudEditScreen) return@attachElementBefore
            for (module in modules) {
                if (module.enabled) {
                    module.render(graphics, tick)
                }
            }
        }
        Hkim.logger.info("HUD element render hook registered")
    }

    fun getAll(): List<Module> = modules.toList()
    fun getByCategory(cat: Category): List<Module> = modules.filter { it.category == cat }
    fun getEnabled(): List<Module> = modules.filter { it.enabled }
    fun getById(id: String): Module? = modules.find { it.id == id }
    fun getEnabledToName(): List<String> = modules.filter { it.enabled && !it.name.containsOneOf("Test", "Click GUI") }.map { it.name }
}