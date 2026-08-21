package cn.hkim.addon.commands

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.features.impl.CropNuker
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.waypoints.FarmingWaypoints
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

val hwpCommand = Commodore("hwp") {
    literal("list").runs {
        val files = FarmingWaypoints.listFiles()
        if (files.isEmpty()) {
            modMessage("§cNo waypoint files found in config/hkim/waypoints")
            return@runs
        }
        val active = FarmingWaypoints.activeFile
        modMessage("§7Available waypoint files:")
        files.forEach { file ->
            val prefix = if (file == active) " §a■" else " §8■"
            val msg = Component.literal("$prefix §7$file.json").withStyle {
                it.withHoverEvent(HoverEvent.ShowText(Component.literal("§7Click to load waypoint!")))
                    .withClickEvent(ClickEvent.RunCommand("/nwp load $file"))
            }
            mc.execute { mc.gui.hud.chat.addClientSystemMessage(msg) }
        }
    }

    literal("load").runs { file: GreedyString? ->
        val fileName = file?.string?.trim()?.takeIf { it.isNotBlank() }
            ?: return@runs modMessage("§7Usage: §7/hwp load <filename>")
        FarmingWaypoints.load(fileName)
    }

    literal("reload").runs {
        FarmingWaypoints.reload()
    }

    literal("unload").runs {
        FarmingWaypoints.unload()
    }

    literal("setIndex").runs { index: Int ->
        if (index < 1) {
            modMessage("§cIndex must be >= 1.")
            return@runs
        }
        CropNuker.setCurrentActionId(index)
        modMessage("§6Crop Nuker §7target set to waypoint §b#$index.")
    }
}
