package cn.hkim.addon.runtime

import net.fabricmc.loader.api.FabricLoader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration

object AssetInstaller {
    private const val BASE_URL = "https://gitee.com/mixturedg/hkim-repo/releases/download/0.0.1-ski"

    const val SKIKO_AWT_URL = "$BASE_URL/${BridgeLoader.SKIKO_AWT_JAR}"
    const val SKIKO_RUNTIME_URL = "$BASE_URL/${BridgeLoader.SKIKO_NATIVE_JAR}"
    const val FONT_URL = "$BASE_URL/Regular.ttf"
    const val FONT_BOLD_URL = "$BASE_URL/Bold.ttf"

    fun libDir(): Path = FabricLoader.getInstance().gameDir.resolve("skiko")
    fun fontsDir(): Path = libDir().resolve("font")

    fun install(targetDir: Path, urls: List<String>, onMessage: (String) -> Unit): List<String> {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        val failed = mutableListOf<String>()
        for (url in urls) {
            val name = url.substringAfterLast('/')
            val target = targetDir.resolve(name)
            if (Files.isRegularFile(target)) {
                if (verify(target, name, onMessage)) {
                    onMessage("$name already exists")
                    continue
                }
                Files.delete(target)
            }
            onMessage("Downloading $name ...")
            val response = runCatching {
                client.send(HttpRequest.newBuilder(URI(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray())
            }.getOrElse {
                onMessage("Download failed: ${it.message}")
                failed += name
                continue
            }
            if (response.statusCode() != 200) {
                onMessage("Download failed: HTTP ${response.statusCode()} — $url")
                failed += name
                continue
            }
            runCatching { Files.createDirectories(targetDir) }
            Files.write(target, response.body())
            if (!verify(target, name, onMessage)) {
                Files.delete(target)
                failed += name
                continue
            }
            onMessage("Done: $name (${response.body().size / 1024} KB)")
        }
        return failed
    }

    private fun verify(file: Path, name: String, onMessage: (String) -> Unit): Boolean {
        val expected = expectedHashes[name] ?: return true
        val actual = sha256(file)
        if (actual != expected) {
            onMessage("SHA256 mismatch: $name (expected $expected, got $actual)")
            return false
        }
        return true
    }

    fun sha256(file: Path): String = sha256(Files.readAllBytes(file))

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    val expectedHashes: Map<String, String> by lazy {
        val stream = AssetInstaller::class.java.classLoader.getResourceAsStream("sha256.json")
            ?: return@lazy emptyMap()
        val json = String(stream.use { it.readAllBytes() })
        """([^"]+)"\s*:\s*"([a-f0-9]{64})""".toRegex()
            .findAll(json)
            .associate { it.groupValues[1] to it.groupValues[2] }
    }
}
