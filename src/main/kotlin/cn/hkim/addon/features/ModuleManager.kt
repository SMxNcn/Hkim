package cn.hkim.addon.features

import cn.hkim.addon.Hkim
import cn.hkim.addon.utils.containsOneOf
import java.lang.invoke.MethodHandles

object ModuleManager {
    private val modules = mutableListOf<Module>()

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
    }

    fun getAll(): List<Module> = modules.toList()
    fun getByCategory(cat: Category): List<Module> = modules.filter { it.category == cat }
    fun getEnabled(): List<Module> = modules.filter { it.enabled }
    fun getById(id: String): Module? = modules.find { it.id == id }
    fun getEnabledToName(): List<String> = modules.filter { it.enabled && !it.name.containsOneOf("Test", "Click GUI") }.map { it.name }
}