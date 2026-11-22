package cn.hkim.addon.utils.skyblock.mining

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.loreString
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

object MiningStatsTracker {
    private val speedRegex = Regex("""Mining Speed:\s*[\uE000-\uF8FF]*\s*([\d,]+)""")
    private val msbChatRegex = Regex("""you used your mining speed boost pickaxe ability""", RegexOption.IGNORE_CASE)
    private val cooldownRegex = Regex("""([0-9.]+)\s*s""")
    private val multiplierRegex = Regex("""\+([0-9.]+)%""")

    @Volatile
    private var baseSpeed = 0

    @Volatile
    private var msbMultiplier = 1f

    private var msbActiveUntilTick = -1
    private var msbCooldownTicks = 0
    private var lastToolCheckTick = -200

    @Volatile
    private var refreshPending = false

    val hasSpeed: Boolean get() = baseSpeed > 0

    @EventHandler
    private fun onPacket(event: PacketReceiveEvent) {
        if (event.packet is ClientboundPlayerInfoUpdatePacket) refreshFromTab()
    }

    @EventHandler
    private fun onWorldLoad(event: WorldEvent.Load) = reset()

    @EventHandler
    private fun onWorldUnload(event: WorldEvent.Unload) = reset()

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        if (msbCooldownTicks <= 0 || !msbChatRegex.containsMatchIn(event.message)) return
        val player = mc.player ?: return
        msbActiveUntilTick = player.tickCount + msbCooldownTicks
    }

    @EventHandler
    private fun onTick(event: TickEvent.Start) {
        val player = mc.player ?: return
        if (baseSpeed <= 0 && player.tickCount % 20 == 0) refreshFromTab()

        if (player.tickCount - lastToolCheckTick < 20) return
        lastToolCheckTick = player.tickCount
        updateMsbTool(player.mainHandItem)
    }

    fun currentSpeed(player: Player): Int {
        val base = baseSpeed
        if (base <= 0) return 0
        return if (player.tickCount < msbActiveUntilTick) (base * msbMultiplier).toInt() else base
    }

    private fun refreshFromTab() {
        if (refreshPending) return
        refreshPending = true
        mc.execute {
            refreshPending = false
            val lines = mc.connection?.listedOnlinePlayers
                ?.mapNotNull { it.tabListDisplayName?.string }
                ?: return@execute

            for (line in lines) {
                val match = speedRegex.find(line.clean) ?: continue
                val speed = match.groupValues[1].replace(",", "").toIntOrNull() ?: continue
                if (speed > 0) baseSpeed = speed
                return@execute
            }
        }
    }

    private fun updateMsbTool(stack: ItemStack) {
        if (stack.isEmpty) {
            msbCooldownTicks = 0
            msbMultiplier = 1f
            return
        }

        val lore = stack.loreString
        val abilityIndex = lore.indexOfFirst { it.contains("Ability: Mining Speed") }
        if (abilityIndex < 0) {
            msbCooldownTicks = 0
            msbMultiplier = 1f
            return
        }

        val abilityLore = lore.subList(abilityIndex + 1, lore.size)
        msbCooldownTicks = abilityLore.firstNotNullOfOrNull {
            cooldownRegex.find(it.clean)?.groupValues?.get(1)?.toFloatOrNull()
        }?.let { (it * 20).toInt() } ?: 0
        msbMultiplier = abilityLore.firstNotNullOfOrNull {
            multiplierRegex.find(it.clean)?.groupValues?.get(1)?.toFloatOrNull()
        }?.let { 1f + it / 100f } ?: 1f
    }

    private fun reset() {
        baseSpeed = 0
        msbMultiplier = 1f
        msbActiveUntilTick = -1
        msbCooldownTicks = 0
        lastToolCheckTick = -200
    }
}
