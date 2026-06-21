package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.ActionSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.config.settings.SelectorSetting
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.*
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import meteordevelopment.orbit.EventHandler
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ContainerInput
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

@ModuleInfo("auto_sell", Category.SKYBLOCK)
object AutoSell : Module("Auto Sell", "Automatically sell items in trades and cookie menus. (/autosell)") {
    private val delay by NumberSetting("Delay", "Tick delay between each sell action.", 100f, 75f, 300f, 5f)
    private val clickType by SelectorSetting("Click Type", "Click type to use when selling.", listOf("Shift", "Middle", "Left"), "Shift")

    private val addDefaults by ActionSetting("Add defaults", "Add default dungeon items to the auto sell list.") {
        sellList.addAll(defaultItems)
        saveSellList()
        modMessage("§aAdded default items to auto sell list")
    }

    private var lastClickTime = 0L

    private val dataDir = File(FabricLoader.getInstance().configDir.toFile(), "hkim/data")
    private val sellFile = File(dataDir, "sell_list.json")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val _sellList = mutableSetOf<String>()
    val sellList: MutableSet<String> get() = _sellList

    init {
        loadSellList()
    }

    fun loadSellList() {
        if (!sellFile.exists()) return
        try {
            val content = Files.readString(sellFile.toPath(), StandardCharsets.UTF_8)
            val list: List<String> = gson.fromJson(content, object : TypeToken<List<String>>() {}.type)
            _sellList.clear()
            _sellList.addAll(list)
        } catch (e: Exception) {
            Hkim.logger.error("Failed to load sell list: ${e.message}")
        }
    }

    fun saveSellList() {
        dataDir.mkdirs()
        try {
            Files.writeString(sellFile.toPath(), gson.toJson(_sellList.toList()), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Hkim.logger.error("Failed to save sell list: ${e.message}")
        }
    }

    @EventHandler
    private fun onTick(event: TickEvent.Start) {
        if (!enabled || sellList.isEmpty()) return
        schedule(randomDelay(delay.toInt(), 50).toInt()) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 50) return@schedule

            val container = mc.screen as? AbstractContainerScreen<*> ?: return@schedule
            val player = mc.player ?: return@schedule
            val title = container.title.string

            if (!title.clean.equalsOneOf("Trades", "Booster Cookie", "Farm Merchant", "Ophelia")) return@schedule

            val index = container.menu.slots.filter { it.container is Inventory }.firstOrNull {
                val stack = it.item
                if (stack.isEmpty) return@firstOrNull false
                stack.hoverName.string.clean.containsOneOf(sellList, ignoreCase = true)
            }?.index ?: return@schedule

            val ct = when (clickType) {
                1 -> ContainerInput.QUICK_MOVE
                2 -> ContainerInput.CLONE
                3 -> ContainerInput.PICKUP
                else -> ContainerInput.QUICK_MOVE
            }
            mc.gameMode?.handleContainerInput(container.menu.containerId, index, 0, ct, player)
            lastClickTime = currentTime
        }
    }

    private val defaultItems = arrayOf(
        "enchanted ice", "rotten", "skeleton grunt", "cutlass",
        "skeleton lord", "skeleton soldier", "zombie soldier", "zombie knight", "zombie commander", "zombie lord",
        "skeletor", "super heavy", "heavy", "sniper helmet", "dreadlord", "earth shard", "zombie commander whip",
        "machine gun", "sniper bow", "soulstealer bow", "silent death", "training weight",
        "beating heart", "premium flesh", "mimic fragment", "enchanted rotten flesh", "sign",
        "enchanted bone", "defuse kit", "optical lens", "tripwire hook", "button", "carpet", "lever", "diamond atom",
        "healing viii splash potion", "healing 8 splash potion", "candycomb"
    )
}
