package cn.hkim.addon.commands

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.features.impl.CropNuker
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.waypoints.FarmingWaypoints
import com.github.stivais.commodore.Commodore
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
                    .withClickEvent(ClickEvent.RunCommand("/hwp load $file"))
            }
            mc.execute { mc.gui.chat.addClientSystemMessage(msg) }
        }
    }

    literal("load").executable {
        param("file") {
            suggests { FarmingWaypoints.listFiles() }
        }
        runs { file: String ->
            val fileName = file.trim().takeIf { it.isNotBlank() }
                ?: return@runs modMessage("§7Usage: §7/hwp load <filename>")
            FarmingWaypoints.load(fileName)
            CropNuker.resetRoute()
        }
    }

    literal("reload").runs {
        FarmingWaypoints.reload()
        CropNuker.resetRoute()
    }

    literal("unload").runs {
        FarmingWaypoints.unload()
        CropNuker.resetRoute()
    }

    literal("setIndex").runs { index: Int ->
        val size = FarmingWaypoints.currentWaypoints.size
        if (size == 0) {
            modMessage("§cWaypoints not loaded.")
            return@runs
        }
        if (index !in 1..size) {
            modMessage("§cIndex out of range (1~$size).")
            return@runs
        }
        CropNuker.setCurrentActionIndex(index)
    }
}
