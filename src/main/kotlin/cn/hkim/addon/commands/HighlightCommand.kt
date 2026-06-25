package cn.hkim.addon.commands

import cn.hkim.addon.features.impl.CustomHighlight
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.skyblock.Island
import com.github.stivais.commodore.Commodore
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

val highlightCommand = Commodore("highlight", "hl") {
    literal("add").executable {
        param("island") {
            suggests { Island.entries.filter { i -> i != Island.Unknown && i != Island.SinglePlayer }.map { i -> i.name } }
        }
        runs { entityName: String, island: String? ->
            if (entityName.isBlank()) return@runs modMessage("§cUsage: /highlight add <entityName> [island]")

            if (island != null) {
                val validIsland = Island.entries.any { i -> i.name.equals(island, true) || i.displayName.equals(island, true) }
                if (!validIsland) return@runs modMessage("§cInvalid island: §e$island")
            }

            val added = CustomHighlight.addEntry(entityName.trim(), island?.trim())
            if (added) {
                val suffix = if (island != null) " §7on §b$island" else ""
                modMessage("§aAdded §7highlight for §e$entityName$suffix§7.")
            } else {
                modMessage("§e$entityName §cis already in the highlight list§7.")
            }
        }
    }

    literal("list").runs {
        val entries = CustomHighlight.getEntries()
        if (entries.isEmpty()) {
            modMessage("§cNo custom highlight entries. Add one with §7/highlight add <name> [island]")
            return@runs
        }

        modMessage("§7Custom highlight entries (§b${entries.size}§7):")
        val mc = cn.hkim.addon.Hkim.mc
        entries.forEach { entry ->
            val islandSuffix = if (entry.islandId != null) " §8(${entry.islandId})" else ""
            val msg = Component.literal(" §8■ §f${entry.entityName}$islandSuffix").withStyle {
                it.withHoverEvent(HoverEvent.ShowText(Component.literal("§7Click to prepare removal")))
                    .withClickEvent(ClickEvent.SuggestCommand("/highlight remove ${entry.entityName}"))
            }
            mc.execute { mc.gui.chat.addClientSystemMessage(msg) }
        }
    }

    literal("remove").executable {
        param("entityName") {
            suggests { CustomHighlight.getEntries().map { e -> e.entityName } }
        }
        runs { entityName: String ->
            if (entityName.isBlank()) return@runs modMessage("§cUsage: /highlight remove <entityName>")

            val removed = CustomHighlight.removeEntry(entityName.trim())
            if (removed) modMessage("§cRemoved §7highlight for §e$entityName§7.")
            else modMessage("§e$entityName §cnot found in highlight list§7.")
        }
    }
}
