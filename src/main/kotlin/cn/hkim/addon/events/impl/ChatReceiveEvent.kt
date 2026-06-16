package cn.hkim.addon.events.impl

import cn.hkim.addon.events.Cancellable
import cn.hkim.addon.utils.cleanString
import net.minecraft.network.chat.Component

class ChatReceiveEvent private constructor() : Cancellable() {
    var component: Component = Component.empty()
        private set
    val message: String
        get() = component.cleanString

    companion object {
        private val INSTANCE = ChatReceiveEvent()

        @JvmStatic
        fun get(message: Component): ChatReceiveEvent {
            INSTANCE.isCancelled = false
            INSTANCE.component = message
            return INSTANCE
        }
    }
}