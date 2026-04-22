package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.events.impl.RenderEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.render.drawText
import cn.hkim.addon.utils.skyblock.DungeonUtils
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

@ModuleInfo("nametag", Category.RENDER, false)
object Nametags : Module("Nametags", "Render a nametag above players.") {
    private val dropdown by DropdownSetting("Show Settings", "")
    private val renderDistance by BooleanSetting("Distance", "Render distance string.", true).depends { dropdown }
    private val teammateESP by BooleanSetting("Teammate ESP", "Show ESP box for dungeon teammates.", false).depends { dropdown }
    private val removeGlowing by BooleanSetting("Disable Glowing effect", "Removes glowing effect by Hypixel.", true).depends { teammateESP }
    private val forceSkyBlock by BooleanSetting("Force SkyBlock", "Force render other players.", false)

    @EventHandler
    fun onRender(event: RenderEvent.Extract) {
        if (!canDisplayNametags()) return
        val level = mc.level ?: return
        val player = mc.player ?: return

        for (entity in level.players()) {
            if (entity == player) continue
            if (!isValidSkyBlockPlayer(entity)) continue
            if (entity.isRemoved || !entity.isAlive) continue

            val distance = player.distanceTo(entity)
            val nametagText = buildNametagText(entity, distance.toInt())
            val scale = calculateScale(distance)
            val yOffset = if (entity.isCrouching) 0.6 + scale / 5f else 0.8 + scale / 5f
            val renderPos = Vec3(entity.renderX, entity.renderY + entity.eyeHeight + yOffset, entity.renderZ)

            event.drawText(nametagText, renderPos, scale, false)
        }
    }

    private fun calculateScale(distance: Float): Float {
        var size = distance / 10.0f
        if (size < 1.1f) size = 1.1f
        return size * 2f / 1.6f
    }

    private fun buildNametagText(entity: Player, distance: Int): String {
        val playerName = entity.name.string

        return if (LocationUtils.isCurrentArea(Island.Dungeon)) {
            val dungeonPlayer = DungeonUtils.dungeonTeammates.find { it.name == playerName }

            if (dungeonPlayer != null) {
                val clazz = dungeonPlayer.clazz
                val classInitial = clazz.name.first().uppercase()

                "${clazz.colorCode}[$classInitial] $playerName"
            } else {
                "§7[?] $playerName"
            }
        } else {
            if (renderDistance) "${entity.displayName.legacy} §7${distance}m" else entity.displayName.legacy
        }
    }

    private fun isValidSkyBlockPlayer(entity: Player): Boolean {
        val name = entity.displayName.string.clean
        return if (forceSkyBlock) !name.contains("[NPC]") && !name.contains("CIT-")
        else name.matches(Regex("^\\[\\d{1,3}]\\s[a-zA-Z0-9_]{1,16}.*"))
    }

    fun shouldRemoveGlowing() = enabled && teammateESP && removeGlowing && LocationUtils.inDungeons

    fun canDisplayNametags() = enabled && (forceSkyBlock || LocationUtils.inSkyBlock)
}