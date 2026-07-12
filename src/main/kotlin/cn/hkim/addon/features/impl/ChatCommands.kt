package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.cleanString
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.sendCommand
import cn.hkim.addon.utils.skyblock.InfoUtils
import cn.hkim.addon.utils.skyblock.RollType
import meteordevelopment.orbit.EventHandler

@ModuleInfo("chat_commands", Category.MISC, true)
object ChatCommands : Module("Chat Commands", "Responds to chat commands in party and co-op channels.") {
    private val partyChat by BooleanSetting("Party", "Enables party chat commands.", true)
    private val coopChat by BooleanSetting("Co-op", "Enables co-op chat commands.", true)

    private val commands by DropdownSetting("Commands", "")
    private val loc by BooleanSetting("Loc", "", false).depends { commands }
    private val lore by BooleanSetting("Lore", "", false).depends { commands }
    private val roll by BooleanSetting("Roll", "", false).depends { commands }

    private val messageRegex = Regex("""^(Party|Co-op) > (?:\[[^]]*])? ?(\w{1,16}):? ?!(.+)$""")

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        if (!enabled || mc.player == null) return

        val raw = event.component.cleanString
        val match = messageRegex.matchEntire(raw) ?: return

        val prefix = when (match.groupValues[1]) {
            "Party" -> if (!partyChat) return else "pchat"
            "Co-op" -> if (!coopChat) return else "cchat"
            else -> return
        }

        val name = match.groupValues[2]
        val parts = match.groupValues[3].split(" ")
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        schedule(4) {
            execute(prefix, name, cmd, args)
        }
    }

    private fun execute(prefix: String, name: String, cmd: String, args: List<String>) {
        when (cmd) {
            "help", "h" -> sendCommand("$prefix Commands: loc, lore, roll")
            "loc" -> if (loc) sendCommand("$prefix ${InfoUtils.formatLocation()}")
            "lore" -> if (lore) sendCommand("$prefix ${InfoUtils.randomTip()}")
            "roll" -> if (roll) {
                val first = args.firstOrNull()
                val matchedType = first?.let { t ->
                    RollType.entries.firstOrNull { it.name.equals(t, true) }
                }
                val type = matchedType ?: RollType.CATA
                val times = if (matchedType != null) {
                    args.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 100) ?: 10
                } else {
                    first?.toIntOrNull()?.coerceIn(1, 100) ?: 10
                }
                val items = InfoUtils.roll(type, times)
                val result = InfoUtils.formatRollResult(type, items)
                sendCommand("$prefix $name rolled ${type.displayName} $times times -> ${result ?: "Nothing :<"}")
            }
        }
    }
}
