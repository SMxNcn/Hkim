package cn.hkim.addon.utils.render.island.messages

import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.utils.render.island.ChatIslandContent
import cn.hkim.addon.utils.render.island.ChatIslandMessage

class PickaxeAbilityIslandMessage(isEnabled: () -> Boolean) : ChatIslandMessage(
    key = "pickaxe_ability",
    title = "Pickaxe Ability",
    durationMs = 5000L,
    isEnabled = isEnabled,
) {
    private val availableRegex = Regex("""^(.+?)\s+is now available!$""", RegexOption.IGNORE_CASE)

    override fun parse(event: ChatReceiveEvent): ChatIslandContent? {
        val message = event.message.trim()
        val match = availableRegex.find(message) ?: return null
        val ability = match.groupValues[1].trim()
        if (ability.isEmpty()) return null

        return ChatIslandContent(detail = "§6$ability §ais now available!")
    }
}
