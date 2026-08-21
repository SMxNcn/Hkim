package cn.hkim.addon.events.impl

import cn.hkim.addon.events.Cancellable
import com.mojang.blaze3d.platform.InputConstants

class InputEvent(val key: InputConstants.Key) : Cancellable()