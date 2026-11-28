package cn.hkim.addon.hud

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.features.Module
import com.google.gson.JsonObject
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.round
import kotlin.properties.PropertyDelegateProvider
import kotlin.reflect.KProperty

data class Bounds(val x: Float, val y: Float, val w: Float, val h: Float) {
    fun contains(px: Float, py: Float) = px in x..x + w && py in y..y + h
}

class HudElement(
    val name: String = "",
    val desc: String = "",
    x: Float = 10f,
    y: Float = 10f,
    scale: Float = 1f,
    alignment: HudAlignment = HudAlignment.TOP_LEFT,
    val renderContent: (GuiGraphicsExtractor) -> Pair<Float, Float>
) : PropertyDelegateProvider<Module, HudElement> {

    var configKey: String = "hud"

    var anchorX: Float = x
    var anchorY: Float = y
    var hudScale: Float = scale
    var hudAlignment: HudAlignment = alignment

    var loadedFromConfig: Boolean = false
    var enabled: Boolean = true
    var contentWidth: Float = 50f
    var contentHeight: Float = 20f

    var owner: Module? = null
        internal set

    var onFirstRender: ((HudElement) -> Unit)? = null
        private set

    private var hasRendered = false
    private var dependsCondition: (() -> Boolean)? = null

    fun onFirstRender(action: (HudElement) -> Unit): HudElement {
        onFirstRender = action
        return this
    }

    fun depends(condition: () -> Boolean): HudElement {
        dependsCondition = condition
        return this
    }

    fun isVisible(): Boolean = enabled && (dependsCondition?.invoke() ?: true)

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): HudElement {
        thisRef.registerHudElement(this)
        this.owner = thisRef
        return this
    }

    operator fun getValue(thisRef: Module, property: KProperty<*>): HudElement = this

    fun originX(width: Float = contentWidth * hudScale): Float = when (hudAlignment) {
        HudAlignment.TOP_RIGHT, HudAlignment.MIDDLE_RIGHT, HudAlignment.BOTTOM_RIGHT -> width
        HudAlignment.TOP_MIDDLE, HudAlignment.BOTTOM_MIDDLE -> width / 2f
        else -> 0f
    }

    fun originY(height: Float = contentHeight * hudScale): Float = when (hudAlignment) {
        HudAlignment.BOTTOM_LEFT, HudAlignment.BOTTOM_MIDDLE, HudAlignment.BOTTOM_RIGHT -> height
        HudAlignment.MIDDLE_LEFT, HudAlignment.MIDDLE_RIGHT -> height / 2f
        else -> 0f
    }

    fun actualX(): Float {
        return hudAlignment.baseX(mc.window.guiScaledWidth) + anchorX - originX()
    }

    fun actualY(): Float {
        return hudAlignment.baseY(mc.window.guiScaledHeight) + anchorY - originY()
    }

    fun setScreenX(x: Float, width: Float = contentWidth * hudScale) {
        anchorX = x + originX(width) - hudAlignment.baseX(mc.window.guiScaledWidth)
    }

    fun setScreenY(y: Float, height: Float = contentHeight * hudScale) {
        anchorY = y + originY(height) - hudAlignment.baseY(mc.window.guiScaledHeight)
    }

    fun syncAlignmentToPosition() {
        if (contentWidth <= 0f || contentHeight <= 0f) return
        val bounds = getScreenBounds()
        val newAlign = HudAlignment.forPosition(
            bounds.x + bounds.w / 2f,
            bounds.y + bounds.h / 2f,
            mc.window.guiScaledWidth.toFloat(),
            mc.window.guiScaledHeight.toFloat(),
        ) ?: return
        if (newAlign != hudAlignment) switchAlignment(newAlign)
    }

    fun switchAlignment(newAlign: HudAlignment) {
        val ax = actualX()
        val ay = actualY()
        hudAlignment = newAlign
        anchorX = ax + originX() - newAlign.baseX(mc.window.guiScaledWidth)
        anchorY = ay + originY() - newAlign.baseY(mc.window.guiScaledHeight)
    }

    fun scaleCenter(newScale: Float) {
        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight
        val oldW = contentWidth * hudScale
        val oldH = contentHeight * hudScale
        val cx = actualX() + oldW / 2f
        val cy = actualY() + oldH / 2f

        hudScale = newScale

        val newW = contentWidth * newScale
        val newH = contentHeight * newScale
        anchorX = (cx - newW / 2f) + originX(newW) - hudAlignment.baseX(screenW)
        anchorY = (cy - newH / 2f) + originY(newH) - hudAlignment.baseY(screenH)
    }

    fun saveTo(json: JsonObject) {
        val obj = JsonObject()
        obj.addProperty("enabled", enabled)
        obj.addProperty("x", anchorX.toInt())
        obj.addProperty("y", anchorY.toInt())
        obj.addProperty("scale", round(hudScale * 10f) / 10f)
        obj.addProperty("alignment", hudAlignment.name)
        json.add(configKey, obj)
    }

    fun loadFrom(json: JsonObject) {
        val k = configKey
        if (json.has(k) && json.get(k).isJsonObject) {
            val obj = json.getAsJsonObject(k)
            if (obj.has("enabled")) enabled = obj.get("enabled").asBoolean
            if (obj.has("x")) anchorX = obj.get("x").asFloat
            if (obj.has("y")) anchorY = obj.get("y").asFloat
            if (obj.has("scale")) hudScale = obj.get("scale").asFloat
            if (obj.has("alignment")) {
                hudAlignment = try { HudAlignment.valueOf(obj.get("alignment").asString.uppercase()) } catch (_: Exception) { HudAlignment.TOP_LEFT }
            }
            loadedFromConfig = true
        }
    }

    fun render(graphics: GuiGraphicsExtractor, tickTracker: DeltaTracker) {
        if (!isVisible()) return

        if (!hasRendered) {
            if (!loadedFromConfig) {
                onFirstRender?.invoke(this)
            }
            hasRendered = true
        }

        graphics.pose().pushMatrix()
        graphics.pose().translate(actualX(), actualY())
        graphics.pose().scale(hudScale, hudScale)
        val (w, h) = renderContent(graphics)
        contentWidth = w
        contentHeight = h
        graphics.pose().popMatrix()
    }

    fun getScreenBounds(): Bounds {
        return Bounds(actualX(), actualY(), contentWidth * hudScale, contentHeight * hudScale)
    }
}
