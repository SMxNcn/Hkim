package cn.hkim.addon.runtime

import cn.hkim.addon.Hkim
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

object BridgeLoader {
    const val SKIKO_VERSION = "0.151.0"
    const val SKIKO_AWT_JAR = "skiko-awt-$SKIKO_VERSION.jar"
    const val SKIKO_NATIVE_JAR = "skiko-awt-runtime-windows-x64-$SKIKO_VERSION.jar"

    fun load(): SkikoRuntime? {
        val awtJar = locateSkikoAwtJar() ?: run {
            Hkim.logger.error("skiko-awt jar not found in ${AssetInstaller.libDir()} or dev classpath")
            return null
        }

        val urls = mutableListOf<URL>()
        FabricLoader.getInstance().getModContainer("hkim").get().origin.paths
            .filter { Files.exists(it) }
            .forEach { urls += it.toUri().toURL() }
        urls += awtJar.toUri().toURL()

        return runCatching {
            val loader = ChildFirstClassLoader(urls.toTypedArray(), BridgeLoader::class.java.classLoader)
            val cls = Class.forName("cn.hkim.addon.bridge.SkikoRuntimeImpl", true, loader)
            cls.getDeclaredConstructor().newInstance() as SkikoRuntime
        }.onFailure { Hkim.logger.error("Failed to load Skiko bridge", it) }
            .getOrNull()
    }

    private fun locateSkikoAwtJar(): Path? {
        val cached = AssetInstaller.libDir().resolve(SKIKO_AWT_JAR)
        if (Files.isRegularFile(cached)) return cached
        return System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .map { Path.of(it) }
            .firstOrNull { name ->
                val n = name.fileName.toString()
                n.contains("skiko-awt") && !n.contains("runtime")
            }
    }
}
