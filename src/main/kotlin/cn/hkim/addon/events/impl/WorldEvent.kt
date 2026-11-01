package cn.hkim.addon.events.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

class WorldEvent {
    class Load
    class Unload
    class BlockUpdate(val pos: BlockPos, val oldState: BlockState, val newState: BlockState)
}