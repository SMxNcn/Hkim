package cn.hkim.addon.commands

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.features.impl.AutoSell
import cn.hkim.addon.features.impl.AutoSell.sellList
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.modMessage
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString

val autoSellCommand = Commodore("autosell") {
    literal("add").runs { item: GreedyString? ->
        val lowercase = item?.string?.lowercase()
            ?: mc.player?.mainHandItem?.hoverName?.string?.clean?.lowercase()
            ?: return@runs modMessage("Either hold an item or write an item name to be added to autosell.")
        if (lowercase in sellList) return@runs modMessage("$lowercase is already in the Auto sell list.")

        modMessage("Added \"$lowercase\" to the Auto sell list.")
        sellList.add(lowercase)
        AutoSell.saveSellList()
    }

    literal("remove").runs { item: GreedyString ->
        val lowercase = item.string.lowercase()
        if (lowercase !in sellList) return@runs modMessage("$item isn't in the Auto sell list.")

        modMessage("Removed \"$item\" from the Auto sell list.")
        sellList.remove(lowercase)
        AutoSell.saveSellList()
    }

    literal("clear").runs {
        modMessage("Auto sell list cleared.")
        sellList.clear()
        AutoSell.saveSellList()
    }

    literal("list").runs {
        if (sellList.isEmpty()) return@runs modMessage("Auto sell list is empty.")
        val chunkedList = sellList.chunked(10)
        modMessage("Auto sell list:\n${chunkedList.joinToString("\n")}")
    }
}
