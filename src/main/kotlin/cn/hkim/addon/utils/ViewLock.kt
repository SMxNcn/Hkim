package cn.hkim.addon.utils

import java.util.*

object ViewLock {
    private val owners = IdentityHashMap<Any, Boolean>()

    @JvmStatic
    val isLocked: Boolean get() = owners.isNotEmpty()

    @JvmStatic
    fun lock(owner: Any) {
        owners[owner] = true
    }

    @JvmStatic
    fun unlock(owner: Any) {
        owners.remove(owner)
    }
}