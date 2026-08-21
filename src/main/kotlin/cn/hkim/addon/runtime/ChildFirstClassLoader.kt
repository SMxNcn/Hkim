package cn.hkim.addon.runtime

import java.net.URL
import java.net.URLClassLoader

class ChildFirstClassLoader(urls: Array<URL>, parent: ClassLoader) : URLClassLoader(urls, parent) {

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (!isChildFirst(name)) return super.loadClass(name, resolve)

        synchronized(getClassLoadingLock(name)) {
            var c = findLoadedClass(name)
            if (c == null) {
                c = try {
                    findClass(name)
                }
                catch (e: ClassNotFoundException) {
                    super.loadClass(name, false)
                }
            }
            if (resolve) resolveClass(c)
            return c
        }
    }

    private fun isChildFirst(name: String): Boolean {
        if (name == "cn.hkim.addon.utils.render.skiko.SkikoGradient" ||
            name == "cn.hkim.addon.utils.render.skiko.SkikoRoundEdge") {
            return false
        }
        return name.startsWith("cn.hkim.addon.bridge.") ||
            name.startsWith("cn.hkim.addon.utils.render.skiko.") ||
            name.startsWith("cn.hkim.addon.utils.render.pip.") ||
            name.startsWith("org.jetbrains.skia")
    }
}
