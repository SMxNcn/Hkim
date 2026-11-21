package cn.hkim.addon.utils.render.island

import cn.hkim.addon.events.impl.ChatReceiveEvent

data class ChatIslandContent(
    val detail: String,
    val cancelChat: Boolean = true,
    val centered: Boolean = false,
    val icon: String = "",
    val iconColor: Int = 0,
    val titleColor: Int? = null,
    val detailColor: Int? = null,
)

abstract class ChatIslandMessage(
    val key: String,
    val title: String,
    val durationMs: Long,
    val priority: Int = IslandQueue.PRIORITY_MODULE,
    val isEnabled: () -> Boolean,
) {
    protected abstract fun parse(event: ChatReceiveEvent): ChatIslandContent?

    fun handle(event: ChatReceiveEvent): Boolean {
        if (!isEnabled()) return false
        val content = parse(event) ?: return false
        if (content.cancelChat) event.cancel()
        IslandQueue.show(
            IslandContent(
                key = key,
                priority = priority,
                title = title,
                detail = content.detail,
                icon = content.icon,
                iconColor = content.iconColor,
                centered = content.centered,
                titleColor = content.titleColor,
                detailColor = content.detailColor,
                durationMs = durationMs,
            )
        )
        return true
    }
}
