package cn.hkim.addon.utils.render.island.messages

import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.utils.render.island.ChatIslandContent
import cn.hkim.addon.utils.render.island.ChatIslandMessage

class SackMessage(isEnabled: () -> Boolean) : ChatIslandMessage(
    key = "sack",
    title = "Get from Sacks",
    durationMs = 2000L,
    isEnabled = isEnabled,
) {
    private val movedRegex = Regex("""Moved ([\d,]+) (.+?) from your Sacks to your inventory\.$""")

    override fun parse(event: ChatReceiveEvent): ChatIslandContent? {
        val match = movedRegex.find(event.message.trim()) ?: return null
        val amount = match.groupValues[1]
        val item = match.groupValues[2].trim()
        if (item.isEmpty()) return null

        return ChatIslandContent(detail = "§a$item §7x$amount")
    }
}
