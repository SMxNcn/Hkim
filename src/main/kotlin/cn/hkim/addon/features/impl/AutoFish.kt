package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.KeybindSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.InputEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.mixins.accessors.FishingHookAccessor
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.ItemStack

@ModuleInfo("auto_fish", Category.SKYBLOCK)
object AutoFish : Module("Auto Fish", "Automatically casts and reels the fishing rod.") {
    private val rethrowDelay by NumberSetting("Rethrow Delay (ms)", "Delay before rethrowing fishing hook.", 200f, 20f, 400f, 10f)
    private val waitTime by NumberSetting("Max Wait Time (s)", "Maximum time to wait for bite before rethrowing.", 20f, 20f, 40f, 1f)
    private val toggleKeybind by KeybindSetting("Toggle Keybind", "Key to toggle auto fish.")

    enum class FishingState {
        IDLE, THROW, WAIT, CAST
    }

    private var currentState = FishingState.IDLE
    private var fishBitten = false
    private var waitStartTick = 0L
    private var hookUpTick = 0L
    private var reelInTick = 0L
    private var hasReeledIn = false
    private var skipNextCast = false

    override fun onEnable() {
        val player = mc.player ?: run {
            enabled = false
            return
        }
        val item = player.mainHandItem
        if (!isValidRod(item)) {
            val name = item.hoverName.legacy
            modMessage("$name §cis not a valid fishing rod!")
            enabled = false
            return
        }
        reset()
        modMessage("§6Auto Fish§a enabled.")
    }

    override fun onDisable() {
        reset()
        schedule(5) {
            if (mc.player?.fishing != null) useItemAction()
            modMessage("§6Auto Fish§c disabled.")
        }
    }

    @EventHandler
    private fun onKey(event: InputEvent) {
        if (!LocationUtils.inSkyBlock) return
        if (mc.screen != null) return
        if (event.key.value == toggleKeybind) toggle()
    }

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        if (!enabled || !LocationUtils.inSkyBlock) return
        handleFishing()
    }

    private fun useItemAction() {
        val player = mc.player ?: return
        mc.gameMode?.useItem(player, InteractionHand.MAIN_HAND)
        player.swing(InteractionHand.MAIN_HAND)
    }

    private fun handleFishing() {
        val player = mc.player ?: return
        val level = mc.level ?: return
        val currentTime = level.gameTime

        if (!isValidRod(player.mainHandItem)) {
            currentState = FishingState.IDLE
            fishBitten = false
            return
        }

        when (currentState) {
            FishingState.IDLE -> {
                currentState = FishingState.THROW
            }

            FishingState.THROW -> {
                if (player.fishing == null) {
                    useItemAction()
                    currentState = FishingState.WAIT
                    waitStartTick = currentTime
                }
            }

            FishingState.WAIT -> {
                val hook = player.fishing
                val elapsed = currentTime - waitStartTick

                if (hook == null) {
                    if (elapsed < 40) return
                    currentState = FishingState.IDLE
                    return
                }

                val outOfWaterTime = (hook as FishingHookAccessor).outOfWaterTime
                if (!hook.isInLava && outOfWaterTime > 5) {
                    reelInTick = currentTime + (0..2).random()
                    hasReeledIn = false
                    currentState = FishingState.CAST
                    return
                }

                if (elapsed >= waitTime * 20) {
                    useItemAction()
                    modMessage("Max wait time reached! Reset. §c(${waitTime}s)")
                    fishBitten = false
                    currentState = FishingState.IDLE
                    return
                }

                checkHookArmorStand()
                if (fishBitten) {
                    reelInTick = currentTime + (0..2).random()
                    hasReeledIn = false
                    currentState = FishingState.CAST
                }
            }

            FishingState.CAST -> {
                if (!hasReeledIn) {
                    if (currentTime >= reelInTick) {
                        useItemAction()
                        hasReeledIn = true
                        skipNextCast = (0..98).random() == 0
                        hookUpTick = if (!skipNextCast) {
                            currentTime + (rethrowDelay / 50).toLong() + (-1..1).random()
                        } else {
                            currentTime + 2
                        }
                    }
                } else {
                    if (currentTime >= hookUpTick) {
                        fishBitten = false
                        hasReeledIn = false
                        if (skipNextCast) {
                            skipNextCast = false
                            currentState = FishingState.IDLE
                        } else {
                            currentState = FishingState.THROW
                        }
                        hookUpTick = 0L
                    }
                }
            }
        }
    }

    private fun checkHookArmorStand() {
        val hook = mc.player?.fishing ?: return
        val armorStand = mc.level?.getEntitiesOfClass(ArmorStand::class.java, hook.boundingBox.inflate(1.0)) { entity: ArmorStand ->
            entity.isInvisible && entity.hasCustomName() && entity.customName?.cleanString?.contains("!!!") == true
        }?.firstOrNull()

        armorStand?.let { fishBitten = true }
    }

    private fun isValidRod(item: ItemStack): Boolean {
        return item.item is FishingRodItem && !item.itemId.containsOneOf("FLAY", "WHIP")
    }

    private fun reset() {
        currentState = FishingState.IDLE
        fishBitten = false
        waitStartTick = 0L
        hookUpTick = 0L
        reelInTick = 0L
        hasReeledIn = false
        skipNextCast = false
    }
}