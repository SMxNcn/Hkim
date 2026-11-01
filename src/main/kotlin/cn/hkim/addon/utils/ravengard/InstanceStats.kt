package cn.hkim.addon.utils.ravengard

class InstanceStats {
    private val ratioRegex = Regex("(\\d+)/(\\d+)")
    private val timerRegex = Regex("\uE002(\\d{2}):(\\d{2})")
    private val statsRegex = Regex("\uE003(\\d+)\\s+\uE004(\\d+)\\s+\uE005(\\d+)\\s+\uE00E(\\d+)")

    fun parse(lines: List<String>): InstanceState {
        var health: Pair<Int, Int>? = null
        var stamina: Pair<Int, Int>? = null
        var elapsedSeconds: Int? = null

        var kills = 0
        var alive = 0
        var team = 0
        var portals = 0

        var firstRatioFound = false

        for (line in lines) {
            ratioRegex.findAll(line).forEach { match ->
                val current = match.groupValues[1].toIntOrNull() ?: return@forEach
                val max = match.groupValues[2].toIntOrNull() ?: return@forEach

                if (!firstRatioFound) {
                    health = current to max
                    firstRatioFound = true
                } else if (stamina == null) {
                    stamina = current to max
                }
            }

            timerRegex.find(line)?.let { match ->
                val min = match.groups["min"]?.value?.toIntOrNull() ?: 0
                val sec = match.groups["sec"]?.value?.toIntOrNull() ?: 0
                elapsedSeconds = min * 60 + sec
            }

            statsRegex.find(line)?.let { match ->
                kills = match.groups["kills"]?.value?.toIntOrNull() ?: 0
                alive = match.groups["alive"]?.value?.toIntOrNull() ?: 0
                team = match.groups["team"]?.value?.toIntOrNull() ?: 0
                portals = match.groups["portals"]?.value?.toIntOrNull() ?: 0
            }
        }

        return InstanceState(
            health = health,
            stamina = stamina,
            elapsedSeconds = elapsedSeconds,
            kills = kills,
            alivePlayers = alive,
            teamSize = team,
            portals = portals,
            isValid = health != null || elapsedSeconds != null
        )
    }
}