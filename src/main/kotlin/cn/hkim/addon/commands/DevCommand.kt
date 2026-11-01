package cn.hkim.addon.commands

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.GardenEvent
import cn.hkim.addon.mixins.accessors.BossHealthOverlayAccessor
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.ravengard.inRavengardInstance
import com.github.stivais.commodore.Commodore
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.*

val devCommand = Commodore("hdev") {
    literal("plotName").runs {
        val plot = HudUtils.getScoreboard().firstNotNullOfOrNull { line ->
            Regex("Plot - (.+)").find(line.clean)?.groupValues?.get(1)
        }
        modMessage(plot)
    }

    literal("getScoreboard").runs { formatted: Boolean ->
        val data = HudUtils.getScoreboardLines(formatted = true) ?: return@runs modMessage("§cNo scoreboard found.")
        val lines = if (formatted) {
            listOf(data.title) + data.lines
        } else {
            listOf(data.title.clean) + data.lines.map { it.clean }
        }
        val text = lines.joinToString("\n")
        mc.keyboardHandler.clipboard = text
        modMessage("§aScoreboard (formatted=$formatted, ${lines.size - 1} lines) copied to clipboard.")
    }

    literal("getBossBar").runs {
        val overlay = mc.gui.hud.bossOverlay as? BossHealthOverlayAccessor
            ?: return@runs modMessage("§cNo boss bar overlay.")
        val events = overlay.events
        if (events.isEmpty()) return@runs modMessage("§cNo boss bars found.")

        for ((i, event) in events.values.withIndex()) {
            modMessage("§7[$i] §fname=§r${event.name.string}")
            modMessage("§7[$i] §flegacy=§r${event.name.legacy}")
        }
    }

    literal("getMobName").runs {
        val level = mc.level ?: return@runs
        val armorStands = level.entitiesForRendering().filterIsInstance<ArmorStand>().filter { it.isAlive }

        val lvRegex = Regex("^\\[Lv\\d+]")
        val lvStands = armorStands.filter { lvRegex.containsMatchIn(it.displayName.string.clean) }

        if (lvStands.isEmpty()) return@runs modMessage("§cNo armor stand starting with [Lv?] found.")

        for (lvStand in lvStands) {
            val nearby = armorStands
                .filter { it.distanceToSqr(lvStand) <= 4.0 } // within 2 blocks (includes itself)
                .map { it.displayName.string.clean }
            modMessage("§6${lvStand.displayName.string.clean} §7→ §f${nearby.joinToString(" §8| §f")}")
        }
    }

    literal("getTex").runs { name: String ->
        val level = mc.level ?: return@runs modMessage("§cNo world.")

        val armorStands = level.entitiesForRendering().filterIsInstance<ArmorStand>().filter { it.isAlive }

        val target = armorStands.firstOrNull { it.displayName.string.contains(name, ignoreCase = true) }
            ?: return@runs modMessage("§cNo armor stand named \"$name\" found.")

        val nearest = armorStands
            .filter { it != target && !it.getItemBySlot(EquipmentSlot.HEAD).isEmpty }
            .minByOrNull { it.distanceToSqr(target.position()) } ?: return@runs modMessage("§cNo other armor stand with head item near \"$name\".")

        val headItem = nearest.getItemBySlot(EquipmentSlot.HEAD)
        val profile = headItem.get(DataComponents.PROFILE) ?: return@runs modMessage("§cNo head profile.")
        val gameProfile = profile.partialProfile()
        val texturesProperty = gameProfile.properties()["textures"].firstOrNull() ?: return@runs modMessage("§cNo texture property.")

        val decoded = try {
            String(Base64.getDecoder().decode(texturesProperty.value()))
        } catch (e: Exception) {
            modMessage("§cFailed to decode base64: ${e.message}")
            return@runs
        }

        val hash = Regex("texture/([a-f0-9]+)").find(decoded)?.groupValues?.get(1)
            ?: decoded.take(64)

        mc.keyboardHandler.clipboard = hash
        modMessage("§a[$name] §7$hash")
        modMessage("§7Copied to clipboard.")
    }

    literal("getTexDelay").runs {
        val container = mc.player?.containerMenu ?: return@runs modMessage("§cNo container open.")
        if (container.slots.size < 36) return@runs modMessage("§cContainer has fewer than 4 rows.")

        modMessage("§7Scanning chest rows 3-4 in 3s...")
        schedule(60) {
            val container2 = mc.player?.containerMenu ?: return@schedule
            val results = mutableListOf<String>()
            for (slot in 18 until 36.coerceAtMost(container2.slots.size)) {
                val item = container2.getSlot(slot).item
                if (item.isEmpty) continue

                val profile = item.get(DataComponents.PROFILE) ?: continue
                val gameProfile = profile.partialProfile()
                val texturesProperty = gameProfile.properties()["textures"].firstOrNull() ?: continue

                val decoded = try {
                    String(Base64.getDecoder().decode(texturesProperty.value()))
                } catch (e: Exception) {
                    continue
                }

                val hash = Regex("texture/([a-f0-9]+)").find(decoded)?.groupValues?.get(1)
                    ?: decoded.take(64)
                results.add("${item.hoverName.string}: $hash")
            }

            if (results.isNotEmpty()) {
                val text = results.joinToString("\n")
                mc.keyboardHandler.clipboard = text
                modMessage("§a${results.size} head(s) copied to clipboard.")
            } else {
                modMessage("§cNo skull items found in rows 3-4.")
            }
        }
    }

    literal("nbt").runs {
        val player = mc.player ?: return@runs modMessage("§cNo player.")
        val stack = player.mainHandItem
        if (stack.isEmpty) return@runs modMessage("§cNo item in main hand.")

        val tag = stack.customData
        if (tag.isEmpty) return@runs modMessage("§cItem has no custom data.")

        modMessage("§7Item: §f${stack.hoverName.legacy}")
        modMessage("\n" + tag.toString())
    }

    literal("lore").runs {
        val player = mc.player ?: return@runs
        val stack = player.mainHandItem
        if (stack.isEmpty) return@runs modMessage("§cNo item in main hand.")
        val lore = stack.lore
        if (lore.isEmpty()) return@runs modMessage("§cItem has no lore.")
        for ((i, line) in lore.withIndex()) {
            modMessage("§7[$i] §fstring=§r${line.string}")
            modMessage("§7[$i] §flegacy=§r${line.legacy}")
        }
    }

    literal("inInstance").runs {
        modMessage("In Ravengard instance: $inRavengardInstance")
    }

    literal("postPest") {
        literal("ready").runs {
            Hkim.EVENT_BUS.post(GardenEvent.PestReady())
            modMessage("§7Posted §bGardenEvent.PestReady§7.")
        }

        literal("spawned").runs { plot: Int ->
            if (plot !in 0..24) {
                modMessage("§cPlot id must be between 0 and 24.")
                return@runs
            }
            Hkim.EVENT_BUS.post(GardenEvent.PestSpawned(plot))
            modMessage("§7Posted §bGardenEvent.PestSpawned§7 plot=§a$plot§7.")
        }

        literal("killed").runs {
            Hkim.EVENT_BUS.post(GardenEvent.PestKilled())
            modMessage("§7Posted §bGardenEvent.PestKilled§7.")
        }
    }
}
