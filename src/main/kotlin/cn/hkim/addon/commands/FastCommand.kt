package cn.hkim.addon.commands

import cn.hkim.addon.Hkim
import cn.hkim.addon.utils.sendCommand
import cn.hkim.addon.utils.skyblock.inventory.LoadoutUtils.swapLoadoutTo
import cn.hkim.addon.utils.skyblock.inventory.WardrobeUtils.swapArmorTo
import com.github.stivais.commodore.Commodore
import kotlinx.coroutines.launch

val ldCommand = Commodore("ld") {
    runs { arg: String? ->
        val index = arg?.toIntOrNull()
        if (index != null && index in 1..27) {
            Hkim.scope.launch { swapLoadoutTo(index) }
        } else {
            sendCommand("loadout")
        }
    }
}

val wdCommand = Commodore("wd") {
    runs { arg: String? ->
        val index = arg?.toIntOrNull()
        if (index != null && index in 1..27) {
            val i = (index - 1) % 9 + 1
            val p = (index - 1) / 9 + 1
            Hkim.scope.launch { swapArmorTo(i, p) }
        } else {
            sendCommand("wardrobe")
        }
    }
}