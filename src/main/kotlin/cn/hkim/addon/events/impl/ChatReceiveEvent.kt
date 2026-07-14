package cn.hkim.addon.events.impl

import cn.hkim.addon.events.Cancellable
import net.minecraft.network.chat.Component

class ChatReceiveEvent(var component: Component, val message: String) : Cancellable()