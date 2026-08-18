package cn.hkim.prelaunch

import cn.hkim.addon.runtime.AssetInstaller
import cn.hkim.addon.runtime.BridgeLoader
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

object HkimPreLaunch : PreLaunchEntrypoint {
    val logger: Logger = LogManager.getLogger("Hkim/PreLaunch")

    private const val SKIKO_DLL = "skiko-windows-x64.dll"
    private const val ICU_DAT = "icudtl.dat"

    override fun onPreLaunch() {
        val libDir = AssetInstaller.libDir()
        val dllFile = libDir.resolve(SKIKO_DLL)
        val icuFile = libDir.resolve(ICU_DAT)

        runCatching { Files.createDirectories(libDir) }
        runCatching { Files.createDirectories(AssetInstaller.fontsDir()) }

        val failed = mutableListOf<String>()
        failed += AssetInstaller.install(libDir, listOf(AssetInstaller.SKIKO_AWT_URL)) { logger.info(it) }

        if (!Files.isRegularFile(dllFile) || !Files.isRegularFile(icuFile)) {
            val runtimeJar = libDir.resolve(BridgeLoader.SKIKO_NATIVE_JAR)
            val runtimeFailed = AssetInstaller.install(libDir, listOf(AssetInstaller.SKIKO_RUNTIME_URL)) { logger.info(it) }
            if (runtimeFailed.isEmpty()) {
                failed += ensureRuntimeFiles(runtimeJar, libDir)
                runCatching { Files.deleteIfExists(runtimeJar) }
            }
            else {
                failed += runtimeFailed
            }
        }

        failed += AssetInstaller.install(
            AssetInstaller.fontsDir(),
            listOf(AssetInstaller.FONT_URL, AssetInstaller.FONT_BOLD_URL)
        ) { logger.info(it) }

        if (failed.isNotEmpty()) {
            throw RuntimeException(
                "Required runtime assets failed to install: ${failed.joinToString(", ")} — check network and restart"
            )
        }

        setLibPath(libDir)
    }

    private fun ensureRuntimeFiles(runtimeJar: Path, libDir: Path): List<String> {
        val failed = mutableListOf<String>()
        for (name in listOf(SKIKO_DLL, ICU_DAT)) {
            val target = libDir.resolve(name)
            if (Files.isRegularFile(target)) continue
            val ok = runCatching {
                ZipFile(runtimeJar.toFile()).use { zip ->
                    val entry = zip.getEntry(name) ?: return@use false
                    zip.getInputStream(entry).use { Files.copy(it, target) }
                    true
                }
            }.getOrDefault(false)
            if (ok) {
                logger.info("Extracted: {}", target)
            }
            else {
                logger.warn("Failed to extract {} from {}", name, runtimeJar)
                failed += name
            }
        }
        return failed
    }

    private fun setLibPath(libDir: Path) {
        System.setProperty("skiko.library.path", libDir.toString())
    }
}
