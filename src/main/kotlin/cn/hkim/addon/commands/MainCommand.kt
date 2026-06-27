package cn.hkim.addon.commands

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.clickgui.ClickGUIScreen
import cn.hkim.addon.gui.HudEditScreen
import cn.hkim.addon.utils.skyblock.inventory.EquipmentUtils.swapEquipment
import cn.hkim.addon.utils.skyblock.inventory.LoadoutUtils.swapLoadoutTo
import cn.hkim.addon.utils.skyblock.inventory.WardrobeUtils.swapArmorTo
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import kotlinx.coroutines.launch

val hkimCommand = Commodore("hkim") {
    runs {
        mc.execute { mc.setScreen(ClickGUIScreen(null)) }
    }

    literal("hud").runs {
        mc.execute { mc.setScreen(HudEditScreen(null)) }
    }

    literal("swapArmor").runs { index: Int, page: Int? ->
        Hkim.scope.launch {
            if (page != null) swapArmorTo(index, page)
            else swapArmorTo(index)
        }
    }

    literal("swapEquipment").runs { inputId: GreedyString ->
        val itemIds = inputId.toString().split(Regex("[\\s,]+")).filter { it.isNotBlank() }
        Hkim.scope.launch {
            swapEquipment(itemIds)
        }
    }

    literal("swapLoadout").runs { index: Int ->
        Hkim.scope.launch {
            swapLoadoutTo(index)
        }
    }
}