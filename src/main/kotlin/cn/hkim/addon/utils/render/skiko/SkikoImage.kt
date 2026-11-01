package cn.hkim.addon.utils.render.skiko

data class SkikoImage(
    val location: String,
    val isSvg: Boolean = location.endsWith(".svg", ignoreCase = true),
) {
    val bytes: ByteArray by lazy {
        val stream = Skiko::class.java.classLoader.getResourceAsStream(location)
            ?: throw IllegalStateException("Image resource not found: $location")
        stream.use { it.readBytes() }
    }
}
