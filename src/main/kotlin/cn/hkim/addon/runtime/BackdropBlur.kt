package cn.hkim.addon.runtime

data class BackdropBlur(
    val radius: Float,
    val cornerRadius: Float? = null,
    val tint: Int = 0,
    val downsample: Int = 4,
    val maskKey: Any? = null,
) {
    val enabled: Boolean get() = radius > 0f
}
