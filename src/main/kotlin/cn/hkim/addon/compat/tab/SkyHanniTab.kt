package cn.hkim.addon.compat.tab

import cn.hkim.addon.Hkim
import cn.hkim.addon.hud.Bounds
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

object SkyHanniTab {
    private val rendererClass = loadClass("at.hannibal2.skyhanni.features.misc.compacttablist.TabListRenderer")
    private val renderer = rendererClass?.instance()
    private val drawMethod = rendererClass?.declaredMethod("drawTabList")

    private val contextClass = loadClass("at.hannibal2.skyhanni.utils.compat.DrawContextUtils")
    private val context = contextClass?.instance()
    private val setContext = contextClass?.publicMethod("setContext", GuiGraphicsExtractor::class.java)
    private val clearContext = contextClass?.publicMethod("clearContext")

    private val readerClass = loadClass("at.hannibal2.skyhanni.features.misc.compacttablist.TabListReader")
    private val reader = readerClass?.instance()
    private val renderColumnsMethod = readerClass?.publicMethod("getRenderColumns")

    private val columnClass = loadClass("at.hannibal2.skyhanni.features.misc.compacttablist.RenderColumn")
    private val columnSizeMethod = columnClass?.publicMethod("size")
    private val columnMaxWidthMethod = columnClass?.publicMethod("getMaxWidth")

    private val config: Any? by lazy { renderer?.let { rendererClass?.declaredMethod("getConfig")?.call(it) } }
    private val enabledMethod: Method? by lazy { config?.javaClass?.publicMethod("getEnabled") }
    private val enabledPropertyGetter: Method? by lazy {
        val property = enabledMethod?.call(config) ?: return@lazy null
        property.javaClass.publicMethod("get")
    }
    private val hideBackgroundMethod: Method? by lazy { config?.javaClass?.publicMethod("getHideTabBackground") }

    var available = renderer != null && drawMethod != null && context != null && setContext != null && clearContext != null
        private set

    private var failures = 0

    fun draw(graphics: GuiGraphicsExtractor) {
        if (!available) return
        try {
            setContext!!.invoke(context, graphics)
            try {
                drawMethod!!.invoke(renderer)
            } finally {
                clearContext!!.invoke(context)
            }
            failures = 0
        } catch (t: Throwable) {
            val cause = (t as? InvocationTargetException)?.targetException ?: t
            if (isVersionMismatch(cause)) {
                available = false
                Hkim.logger.warn("SkyHanni tab bridge disabled: incompatible API (${cause.javaClass.simpleName}: ${cause.message})")
            } else {
                failures++
                if (failures >= 3) {
                    available = false
                    Hkim.logger.error("SkyHanni tab bridge disabled after $failures failed draws", cause)
                } else {
                    Hkim.logger.warn("SkyHanni tab draw failed ($failures/3)", cause)
                }
            }
        }
    }

    fun hidesBackground(): Boolean {
        val config = config ?: return false
        if (!featureEnabled(config)) return false
        val method = hideBackgroundMethod ?: return false
        return method.call(config) as? Boolean == true
    }

    fun listBounds(): Bounds? {
        if (!available) return null
        val columns = renderColumnsMethod?.call(reader) as? List<*> ?: return null
        val size = columnSizeMethod ?: return null
        val maxWidth = columnMaxWidthMethod ?: return null
        if (columns.isEmpty()) return null

        var lines = 0
        var width = -6
        for (column in columns) {
            if (column == null) continue
            lines = maxOf(lines, size.call(column) as? Int ?: 0)
            width += (maxWidth.call(column) as? Int ?: 0) + 6
        }
        if (width <= 0) return null

        val centerX = Minecraft.getInstance().window.guiScaledWidth / 2f
        return Bounds(
            centerX - width / 2f - 6f, 7f,
            width + 12f, lines * 9f + 6f,
        )
    }

    private fun featureEnabled(config: Any): Boolean {
        val getter = enabledPropertyGetter ?: return true
        val property = enabledMethod?.call(config) ?: return true
        return getter.call(property) as? Boolean != false
    }

    private fun isVersionMismatch(t: Throwable): Boolean =
        t is LinkageError || t is ClassNotFoundException || t is NoSuchMethodException ||
            t is IllegalAccessException || t is NoSuchFieldException

    private fun loadClass(name: String) = runCatching { Class.forName(name) }.getOrNull()

    private fun Class<*>.instance(): Any? = runCatching { getField("INSTANCE").get(null) }.getOrNull()

    private fun Class<*>.declaredMethod(name: String): Method? =
        runCatching { getDeclaredMethod(name).apply { isAccessible = true } }.getOrNull()

    private fun Class<*>.publicMethod(name: String, vararg parameters: Class<*>): Method? =
        runCatching { getMethod(name, *parameters) }.getOrNull()

    private fun Method.call(receiver: Any?): Any? = runCatching { invoke(receiver) }.getOrNull()
}
