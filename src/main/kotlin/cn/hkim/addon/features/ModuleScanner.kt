package cn.hkim.addon.features

import java.io.File
import java.net.JarURLConnection

object ModuleScanner {
    private const val TARGET_PACKAGE = "cn.hkim.addon.features.impl"
    private val targetPath = TARGET_PACKAGE.replace('.', '/')

    fun discoverModules(): List<Module> {
        val classLoader = ModuleScanner::class.java.classLoader
        val resources = classLoader.getResources(targetPath)
        val modules = mutableListOf<Module>()

        while (resources.hasMoreElements()) {
            val resource = resources.nextElement()
            when (resource.protocol) {
                "file" -> scanDirectory(File(resource.toURI()), modules, classLoader)
                "jar" -> scanJar(resource, modules, classLoader)
            }
        }
        return modules
    }

    private fun scanDirectory(dir: File, modules: MutableList<Module>, classLoader: ClassLoader) {
        if (!dir.exists() || !dir.isDirectory) return

        dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".class") }
            ?.forEach { file ->
                val className = "$TARGET_PACKAGE.${file.name.removeSuffix(".class")}"
                loadAndCheckModule(className, modules, classLoader)
            }
    }

    private fun scanJar(resource: java.net.URL, modules: MutableList<Module>, classLoader: ClassLoader) {
        val connection = resource.openConnection() as? JarURLConnection ?: return
        val prefix = "$targetPath/"

        connection.jarFile.use { jar ->
            jar.entries().asSequence()
                .filter { it.name.startsWith(prefix) && it.name.endsWith(".class") }
                .filter { it.name.substring(prefix.length).indexOf('/') == -1 }
                .forEach { entry ->
                    val className = entry.name.replace('/', '.').removeSuffix(".class")
                    loadAndCheckModule(className, modules, classLoader)
                }
        }
    }

    private fun loadAndCheckModule(className: String, modules: MutableList<Module>, classLoader: ClassLoader) {
        try {
            val clazz = Class.forName(className, false, classLoader)

            if (!Module::class.java.isAssignableFrom(clazz) || clazz == Module::class.java) return
            if (!clazz.isAnnotationPresent(ModuleInfo::class.java)) return
            val instance = clazz.kotlin.objectInstance as? Module ?: return

            modules.add(instance)
        } catch (_: Throwable) { }
    }
}