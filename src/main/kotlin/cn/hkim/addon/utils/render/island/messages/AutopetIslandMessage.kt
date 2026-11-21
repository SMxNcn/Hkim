package cn.hkim.addon.utils.render.island.messages

import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.utils.colorCodeBefore
import cn.hkim.addon.utils.legacy
import cn.hkim.addon.utils.render.island.ChatIslandContent
import cn.hkim.addon.utils.render.island.ChatIslandMessage

class AutopetIslandMessage(isEnabled: () -> Boolean) : ChatIslandMessage(
    key = "autopet",
    title = "Autopet",
    durationMs = 1000L,
    isEnabled = isEnabled,
) {
    private val autopetRegex = Regex(
        "§cAutopet §eequipped your §7\\[Lvl (?<level>\\d+)] §(?<rarity>.)" +
            "(?:\\[(?:§.)*\\d+(?:§.)*(?<altskin>✦)(?:§.)*] (?:§.)*)?" +
            "(?<pet>[^§!]+?)(?<skin>§. ✦)?§e! §a§lVIEW RULE(?: \\(\\d+\\))?"
    )

    override fun parse(event: ChatReceiveEvent): ChatIslandContent? {
        if (!event.message.contains("Autopet")) return null

        val legacy = event.component.legacy
        val match = autopetRegex.find(legacy) ?: return null
        val level = match.groups["level"]?.value ?: return null
        val petGroup = match.groups["pet"] ?: return null
        val pet = petGroup.value.trimEnd()
        if (pet.isEmpty()) return null

        val petColor = colorCodeBefore(legacy, petGroup.range.first)
            ?: match.groups["rarity"]?.value?.let { "§$it" }
            ?: "§f"
        val skin = match.groups["skin"]?.value ?: ""

        return ChatIslandContent(detail = "§7[Lvl $level] $petColor$pet$skin")
    }
}
