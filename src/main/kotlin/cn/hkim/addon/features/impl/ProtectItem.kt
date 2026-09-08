package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.KeybindSetting
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.events.impl.InputEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.mixins.accessors.AbstractContainerScreenAccessor
import cn.hkim.addon.utils.*
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import meteordevelopment.orbit.EventHandler
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

@ModuleInfo("protect_item", Category.SKYBLOCK, true)
object ProtectItem : Module("Protect Item", "Protect your item.") {
    private val sendMessage by BooleanSetting("Send Message", "Send protect messages.", false)
    private val renderProtectTag by BooleanSetting("Render Protect", "Render protect tag on items.", false)
    private val protectKeybind by KeybindSetting("Protect Keybind", "Key to protect item in inventory", GLFW.GLFW_KEY_V)

    private val dataFile = File(File(FabricLoader.getInstance().configDir.toFile(), "hkim/data"), "protected_items.json")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val protectedIds = mutableSetOf<String>()
    private inline val ItemStack.isAutoProtectable: Boolean
        get() = this.itemUpgradeLevel >= 1 || this.isRecombobulated

    @EventHandler
    private fun onKeyPress(event: InputEvent) {
        val dropKey = mc.options.keyDrop.boundKey
        if (!enabled || event.key != dropKey) return
        val player = mc.player ?: return
        val item = player.mainHandItem
        if (containsUUID(item.itemUUID)) {
            sendMsg("${item.hoverName.legacy} §r§cis protected!")
            event.cancel()
        }
    }

    @EventHandler
    private fun onGuiOpen(event: GuiEvent.Open) {
        if (!enabled) return
        val screen = event.screen as? AbstractContainerScreen<*> ?: return
        val slots = screen.menu.slots

        schedule(1) {
            for (slot in slots) {
                if (slot.item.isAutoProtectable) addUUID(slot.item.itemUUID)
            }
        }
    }

    @EventHandler
    private fun onKeyDown(event: GuiEvent.KeyPress) {
        if (!enabled || mc.gui.screen() == null) return
        val screen = mc.gui.screen() as? AbstractContainerScreen<*> ?: return
        val accessor = screen as AbstractContainerScreenAccessor

        if (event.input.key == protectKeybind) {
            val slot = accessor.hoveredSlot
            if (slot == null || !slot.hasItem()) return
            val uuid = slot.item.itemUUID
            if (uuid.isBlank()) {
                modMessage("§cThis item doesn't have a UUID!")
                return
            }
            if (containsUUID(uuid)) {
                if (removeUUID(uuid)) sendMsg("${slot.item.hoverName.legacy} §r§7is now no longer protected.")
            } else {
                if (addUUID(uuid)) sendMsg("${slot.item.hoverName.legacy} §r§7is now protected.")
            }
            return
        }

        if (!mc.options.keyDrop.matches(event.input)) return
        val slot = accessor.hoveredSlot ?: return
        if (!slot.hasItem()) return
        if (containsUUID(slot.item.itemUUID)) {
            sendMsg("${slot.item.hoverName.legacy} §r§cis protected!")
            event.cancel()
        }
    }

    private fun addUUID(uuid: String): Boolean {
        val normalized = uuid.trim().lowercase()
        if (normalized.isEmpty() || containsUUID(normalized)) return false
        protectedIds.add(normalized)
        save()
        return true
    }

    private fun removeUUID(uuid: String): Boolean {
        val removed = protectedIds.removeIf { it.equals(uuid.trim(), ignoreCase = true) }
        if (removed) save()
        return removed
    }

    private fun containsUUID(uuid: String): Boolean = protectedIds.contains(uuid.trim().lowercase())

    fun loadUUID() {
        if (!dataFile.exists()) return
        try {
            val json = Files.readString(dataFile.toPath(), StandardCharsets.UTF_8)
            val loaded: List<String> = gson.fromJson(json, object : TypeToken<List<String>>() {}.type) ?: return
            protectedIds.clear()
            protectedIds.addAll(loaded.map { it.trim().lowercase() })
        } catch (e: Exception) {
            Hkim.logger.error("Failed to load protected items: ${e.message}")
        }
    }

    private fun save() {
        dataFile.parentFile.mkdirs()
        try {
            Files.writeString(dataFile.toPath(), gson.toJson(protectedIds.toList()), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Hkim.logger.error("Failed to save protected items: ${e.message}")
        }
    }

    private fun sendMsg(message: String) {
        if (sendMessage) modMessage(message)
    }

    @JvmStatic
    val canRenderTag: Boolean get() = enabled && renderProtectTag

    @JvmStatic
    fun isProtected(stack: ItemStack): Boolean = containsUUID(stack.itemUUID)
}
