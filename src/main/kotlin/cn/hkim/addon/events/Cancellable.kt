package cn.hkim.addon.events

import meteordevelopment.orbit.ICancellable

abstract class Cancellable: ICancellable {
    private var cancellable = false

    override fun setCancelled(cancelled: Boolean) {
        cancellable = cancelled
    }

    override fun isCancelled() = cancellable
}