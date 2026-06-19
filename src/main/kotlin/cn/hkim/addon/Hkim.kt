package cn.hkim.addon

import cn.hkim.addon.commands.autoSellCommand
import cn.hkim.addon.commands.hkimCommand
import cn.hkim.addon.config.ModuleConfig
import cn.hkim.addon.events.EventDispatcher
import cn.hkim.addon.features.ModuleManager
import cn.hkim.addon.gui.Background
import cn.hkim.addon.utils.ServerUtils
import cn.hkim.addon.utils.TickTasks
import cn.hkim.addon.utils.skyblock.DungeonUtils
import cn.hkim.addon.utils.skyblock.EquipmentUtils
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.WardrobeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import meteordevelopment.orbit.EventBus
import meteordevelopment.orbit.IEventBus
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.coroutines.EmptyCoroutineContext

object Hkim : ClientModInitializer {
    val logger: Logger = LogManager.getLogger(Hkim.javaClass)
    val scope = CoroutineScope(SupervisorJob() + EmptyCoroutineContext)
    
    @JvmField
    val mc: Minecraft = Minecraft.getInstance()
    @JvmField
    val EVENT_BUS: IEventBus = EventBus()

    val VERSION: String = FabricLoader.getInstance().getModContainer("hkim").get().metadata.version.friendlyString

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            arrayOf(hkimCommand, autoSellCommand).forEach { commodore -> commodore.register(dispatcher) }
        }

        EventDispatcher.postEvents()
        EventDispatcher.registerListeners(DungeonUtils, EquipmentUtils, LocationUtils, ServerUtils, TickTasks, WardrobeUtils)
        ModuleManager.initModules()
        ModuleConfig.loadConfig()
        Background.getDefaultBackground()
    }
}