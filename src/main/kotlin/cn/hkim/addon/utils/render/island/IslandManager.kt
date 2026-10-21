package cn.hkim.addon.utils.render.island

import cn.hkim.addon.hud.Bounds
import cn.hkim.addon.utils.HudUtils
import java.util.*

data class IslandAnchor(
    val bounds: Bounds,
    val radius: Float,
    val background: Int,
)

fun IslandAnchor.towards(bounds: Bounds, radius: Float, background: Int, t: Float): IslandAnchor = IslandAnchor(
    Bounds(
        this.bounds.x + (bounds.x - this.bounds.x) * t,
        this.bounds.y + (bounds.y - this.bounds.y) * t,
        this.bounds.w + (bounds.w - this.bounds.w) * t,
        this.bounds.h + (bounds.h - this.bounds.h) * t,
    ),
    this.radius + (radius - this.radius) * t,
    HudUtils.lerpColor(this.background, background, t),
)

data class IslandContent(
    val key: Any,
    val priority: Int,
    val title: String,
    val detail: String,
    val icon: String = "",
    val iconColor: Int = 0,
    val centered: Boolean = false,
    val titleColor: Int? = null,
    val detailColor: Int? = null,
    val durationMs: Long = 500L,
)

enum class IslandUser { TAB, CLICK_GUI }

object IslandHost {
    private val users: MutableSet<IslandUser> = EnumSet.noneOf(IslandUser::class.java)

    private var shapeOwner: IslandUser? = null
    private var shape: IslandAnchor? = null

    var listShowing: Boolean = false
        internal set

    val takenOver: Boolean get() = users.isNotEmpty()

    fun hasOther(user: IslandUser): Boolean = users.any { it != user }

    fun acquire(user: IslandUser) {
        users += user
        IslandQueue.dropTransient()
    }

    fun release(user: IslandUser) {
        users -= user
        clearShape(user)
    }

    fun currentAnchor(): IslandAnchor = shape ?: IslandRenderer.anchor()

    fun publishShape(user: IslandUser, anchor: IslandAnchor) {
        shapeOwner = user
        shape = anchor
    }

    fun clearShape(user: IslandUser) {
        if (shapeOwner == user) {
            shapeOwner = null
            shape = null
        }
    }
}

object IslandQueue {
    const val PRIORITY_FAIL_SAFE = 1000
    const val PRIORITY_MODULE = 300
    const val PRIORITY_SWAP_INFO = 200
    const val PRIORITY_MENU_HINT = 100
    const val PRIORITY_DEFAULT = 0

    private class Entry(val content: IslandContent, val appearedAt: Long, var remainingMs: Long)

    private val lock = Any()
    private val entries = ArrayList<Entry>()

    fun show(content: IslandContent) {
        synchronized(lock) {
            if (IslandHost.takenOver && content.priority <= 300) return
            val index = entries.indexOfFirst { it.content.key == content.key }
            if (index >= 0) {
                entries[index] = Entry(content, entries[index].appearedAt, content.durationMs)
            } else {
                entries += Entry(content, System.currentTimeMillis(), content.durationMs)
            }
        }
    }

    fun dropTransient() = synchronized(lock) {
        entries.removeAll { it.content.priority <= 300 }
    }

    fun remove(key: Any) = synchronized(lock) { entries.removeAll { it.content.key == key } }

    fun clear() = synchronized(lock) { entries.clear() }

    fun tick(deltaMs: Long): IslandContent? = synchronized(lock) {
        val current = select() ?: return null
        current.remainingMs -= deltaMs
        if (current.remainingMs <= 0L) {
            entries.remove(current)
            return select()?.content
        }
        current.content
    }

    private fun select(): Entry? = entries.minWithOrNull(
        compareByDescending<Entry> { it.content.priority }.thenBy { it.appearedAt }
    )
}
