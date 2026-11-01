package cn.hkim.addon.utils.render.skiko

import java.nio.file.Files
import java.nio.file.Path

data class SkikoFont(
    val location: String? = null,
    val family: String? = null,
    val bold: Boolean = false
) {
    val bytes: ByteArray by lazy {
        val loc = location ?: throw IllegalStateException("SkikoFont has no file location.")
        val file = Path.of(loc)
        if (Files.isRegularFile(file)) {
            Files.readAllBytes(file)
        }
        else {
            val stream = Skiko::class.java.classLoader.getResourceAsStream(loc)
                ?: throw IllegalStateException("Font resource not found: $loc")
            stream.use { it.readBytes() }
        }
    }

    companion object {
        fun system(bold: Boolean = false) = SkikoFont(bold = bold)
        fun family(name: String, bold: Boolean = false) = SkikoFont(family = name, bold = bold)
        fun file(location: String, bold: Boolean = false) = SkikoFont(location = location, bold = bold)
    }
}
