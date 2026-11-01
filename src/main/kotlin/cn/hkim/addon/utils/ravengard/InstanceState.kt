package cn.hkim.addon.utils.ravengard

data class InstanceState(
    val health: Pair<Int, Int>? = null,
    val stamina: Pair<Int, Int>? = null,
    val elapsedSeconds: Int? = null,
    val kills: Int = 0,
    val alivePlayers: Int = 0,
    val teamSize: Int = 0,
    val portals: Int = 0,
    val isValid: Boolean = false
) {
    companion object {
        val EMPTY = InstanceState(isValid = false)

        @Volatile
        var current: InstanceState = EMPTY
    }
}
