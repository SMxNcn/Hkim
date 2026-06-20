package cn.hkim.addon.events.impl

sealed class GardenEvent {
    class PestReady : GardenEvent()

    class PestSpawned(val plot: Int) : GardenEvent()

    class PestKilled : GardenEvent()

    class GuestVisit(val player: String) : GardenEvent()

    class FailSafe(val reason: String) : GardenEvent()
}
