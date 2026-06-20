package cn.hkim.addon.utils.skyblock

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.features.impl.LeapMenu
import cn.hkim.addon.utils.Colors
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.isPlayerInArea
import cn.hkim.addon.utils.romanToInt
import meteordevelopment.orbit.EventHandler
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import java.awt.Color
import kotlin.jvm.optionals.getOrNull

data class DungeonPlayer(
    val name: String,
    val clazz: DungeonClass,
    val clazzLvl: Int,
    val locationSkin: Identifier?,
    var entity: Player? = null,
    var isDead: Boolean = false,
    var deaths: Int = 0,
)

enum class DungeonClass(
    val color: Color,
    val colorCode: Char,
    val defaultQuadrant: Int,
    var priority: Int,
) {
    Archer(Colors.MINECRAFT_RED, 'c', 0, 2),
    Berserk(Colors.MINECRAFT_GOLD, '6', 1, 0),
    Healer(Colors.MINECRAFT_LIGHT_PURPLE, 'd', 2, 2),
    Mage(Colors.MINECRAFT_AQUA, 'b', 3, 2),
    Tank(Colors.MINECRAFT_DARK_GREEN, '2', 3, 1),
    Unknown(Colors.MINECRAFT_GRAY, '7', 0, 0)
}

enum class Floor {
    E, F1, F2, F3, F4, F5, F6, F7,
    M1, M2, M3, M4, M5, M6, M7;

    inline val floorNumber: Int
        get() {
            return when (this) {
                E -> 0
                F1, M1 -> 1
                F2, M2 -> 2
                F3, M3 -> 3
                F4, M4 -> 4
                F5, M5 -> 5
                F6, M6 -> 6
                F7, M7 -> 7
            }
        }

    inline val isMM: Boolean
        get() {
            return when (this) {
                E, F1, F2, F3, F4, F5, F6, F7 -> false
                M1, M2, M3, M4, M5, M6, M7 -> true
            }
        }
}

enum class M7Phases(val displayName: String) {
    P1("P1"), P2("P2"), P3("P3"), P4("P4"), P5("P5"), Unknown("Unknown");
}

fun isFloor(vararg options: Int): Boolean {
    return DungeonUtils.floor?.floorNumber?.let { it in options } ?: false
}

fun getF7Phase(): M7Phases {
    if ((!isFloor(7) || !DungeonUtils.inBoss) && !LocationUtils.isCurrentArea(Island.SinglePlayer)) return M7Phases.Unknown

    with(mc.player ?: return M7Phases.Unknown) {
        return when {
            y > 210 -> M7Phases.P1
            y > 155 -> M7Phases.P2
            y > 100 -> M7Phases.P3
            y > 45 -> M7Phases.P4
            else -> M7Phases.P5
        }
    }
}

enum class P3Stages(val corner1: BlockPos, val corner2: BlockPos) {
    Unknown(BlockPos(-7, 160, -7), BlockPos(7, 160, 7)),
    Tunnel(BlockPos(39, 160, 54), BlockPos(69, 112, 118)),
    S1(BlockPos(89, 153, 51), BlockPos(111, 105, 121)),
    S2(BlockPos(89, 153, 121), BlockPos(19, 105, 143)),
    S3(BlockPos(19, 153, 121), BlockPos(-3, 105, 51)),
    S4(BlockPos(19, 153, 51), BlockPos(89, 105, 29));

    companion object {
        fun getP3Stage(): P3Stages {
            if (getF7Phase() != M7Phases.P3 || mc.player == null)
                return Unknown

            val player = mc.player!!
            val playerPos = BlockPos(player.x.toInt(), player.y.toInt(), player.z.toInt())

            for (stage in entries) {
                if (stage == Unknown) continue

                if (isPlayerInArea(stage.corner1, stage.corner2, playerPos)) {
                    return stage
                }
            }

            return Unknown
        }
    }
}

object DungeonUtils {
    var dungeonTeammates: ArrayList<DungeonPlayer> = ArrayList(5)
    var dungeonTeammatesNoSelf: List<DungeonPlayer> = ArrayList(4)
    var leapTeammates: List<DungeonPlayer> = ArrayList(4)
    inline val currentDungeonPlayer: DungeonPlayer
        get() = dungeonTeammates.find { it.name == mc.player?.name?.string } ?:
        DungeonPlayer(mc.player?.name?.string ?: "Unknown", DungeonClass.Unknown, 0, null)

    var floor: Floor? = null
    var inBoss = false
    var doorOpener: String = "Unknown"

    private val floorRegex = Regex("The Catacombs \\((\\w+)\\)$")
    private val doorOpenRegex = Regex("^(?:\\[\\w+] )?(\\w+) opened a (?:WITHER|Blood) door!")
    private val deathRegex = Regex("☠ (\\w{1,16}) .* and became a ghost\\.")
    private val tabListRegex = Regex("^\\[(\\d+)] (?:\\[\\w+] )*(\\w+) .*?\\((\\w+)(?: (\\w+))*\\)$")

    private fun getBoss(): Boolean = with(mc.player) {
        if (this == null || floor?.floorNumber == null) return false
        when (floor?.floorNumber) {
            1 -> x > -71 && z > -39
            in 2..4 -> x > -39 && z > -39
            in 5..6 -> x > -39 && z > -7
            7 -> x > -7 && z > -7
            else -> false
        }
    }

    @EventHandler
    private fun onTickEnd(event: TickEvent.End) {
        if (LocationUtils.inDungeons) inBoss = getBoss()
    }

    @EventHandler
    private fun onWorldLoad(event: WorldEvent.Load) {
        dungeonTeammates.clear()
        dungeonTeammatesNoSelf = emptyList()
        leapTeammates = emptyList()
        inBoss = false
        floor = null
    }

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        doorOpenRegex.find(event.message)?.let { doorOpener = it.groupValues[1] }
        deathRegex.find(event.message)?.let { match ->
            dungeonTeammates.find { teammate ->
                teammate.name == (match.groupValues[1].takeUnless { it == "You" } ?: mc.player?.name?.string)
            }?.deaths?.inc()
        }
    }

    @EventHandler
    private fun onPacket(event: PacketReceiveEvent) {
        when (event.packet) {
            is ClientboundPlayerInfoUpdatePacket -> {
                val tabListEntries = event.packet.entries().mapNotNull { it.displayName?.string }.ifEmpty { return }
                updateDungeonTeammates(tabListEntries)
            }

            is ClientboundSetPlayerTeamPacket -> {
                val text = event.packet.parameters.getOrNull()?.let { it.playerPrefix.string.plus(it.playerSuffix.string).clean }.toString()
                floorRegex.find(text)?.groupValues?.get(1)?.let { floor = Floor.valueOf(it) }
            }

            is ClientboundAddEntityPacket -> {
                if (event.packet.type == EntityType.PLAYER) {
                    dungeonTeammates.find { it.entity == null && it.name == mc.level?.getEntity(event.packet.id)?.name?.string }?.entity =
                        mc.level?.getEntity(event.packet.id) as? Player
                }
            }

            is ClientboundRemoveEntitiesPacket -> {
                dungeonTeammates.forEach {
                    val id = it.entity?.id ?: return@forEach
                    if (event.packet.entityIds.contains(id)) it.entity = null
                }
            }
        }
    }

    private fun updateDungeonTeammates(tabList: List<String>) = mc.execute {
        dungeonTeammates = getDungeonTeammates(dungeonTeammates, tabList)
        dungeonTeammatesNoSelf = dungeonTeammates.filter { it.name != mc.player?.name?.string }

        leapTeammates = when (LeapMenu.type) {
            0 -> LeapMenu.defaultSorting(dungeonTeammatesNoSelf.sortedBy { it.clazz.priority }).toList()
            1 -> dungeonTeammatesNoSelf.sortedWith(compareBy ({ it.clazz.ordinal }, { it.name }))
            2 -> dungeonTeammatesNoSelf.sortedBy { it.name }
            else -> dungeonTeammatesNoSelf
        }
    }

    private fun getDungeonTeammates(previousTeammates: ArrayList<DungeonPlayer>, tabList: List<String>): ArrayList<DungeonPlayer> {
        for (line in tabList) {
            val (_, name, clazz, clazzLevel) = tabListRegex.find(line)?.destructured ?: continue

            previousTeammates.find { it.name == name }?.let { player -> player.isDead = clazz == "DEAD" }
                ?: run {
                    val player = mc.connection?.getPlayerInfo(name) ?: continue
                    previousTeammates.add(
                        DungeonPlayer(
                            name, DungeonClass.entries.find { it.name == clazz } ?: continue,
                            romanToInt(clazzLevel), mc.player?.skin?.body?.id(),
                            player.profile.id?.let { mc.level?.getPlayerByUUID(it) }
                        )
                    )
                }
        }
        return previousTeammates
    }
}