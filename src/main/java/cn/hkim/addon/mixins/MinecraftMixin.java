package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.config.ModuleConfig;
import cn.hkim.addon.events.impl.TickEvent;
import cn.hkim.addon.features.impl.FreeCam;
import cn.hkim.addon.features.impl.TitleManager;
import cn.hkim.addon.gui.Background;
import cn.hkim.addon.utils.RotationUtils;
import cn.hkim.addon.utils.render.nvg.NVGRenderer;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Unique
    private float hkim$raySavedYaw;

    @Unique
    private float hkim$raySavedPitch;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onStartTick(CallbackInfo ci) {
        Hkim.EVENT_BUS.post(new TickEvent.Start());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onEndTick(CallbackInfo ci) {
        Hkim.EVENT_BUS.post(new TickEvent.End());
    }

    @Inject(method = "onGameLoadFinished", at = @At("HEAD"))
    private void onGameLoadFinished(CallbackInfo ci) {
        Background.loadBackgrounds();
        NVGRenderer.init();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci) {
        ModuleConfig.INSTANCE.saveConfig();
    }

    @ModifyReturnValue(method = "createTitle", at = @At("RETURN"))
    private String modifyTitle(String originalTitle) {
        if (!TitleManager.INSTANCE.getEnabled()) return originalTitle;
        return TitleManager.INSTANCE.buildTitle();
    }

    @Inject(at = @At("HEAD"), method = "clearClientLevel")
    private void freecam$onClearClientLevel(Screen screen, CallbackInfo ci) {
        if (FreeCam.isFreecamActive()) {
            FreeCam.INSTANCE.disable();
        }
    }

    @Inject(at = @At("HEAD"), method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V")
    private void freecam$onDisconnect(Screen screen, boolean keepResourcePacks, boolean stopSounds, CallbackInfo ci) {
        if (FreeCam.isFreecamActive()) {
            FreeCam.INSTANCE.disable();
        }
    }

    @Inject(at = @At("HEAD"), method = "setLevel")
    private void freecam$onSetLevel(ClientLevel level, CallbackInfo ci) {
        if (FreeCam.isFreecamActive()) {
            FreeCam.INSTANCE.disable();
        }
    }

    @Inject(method = "pick(F)V", at = @At("HEAD"))
    private void beforePick(CallbackInfo ci) {
        LocalPlayer player = Hkim.mc.player;
        if (player == null) return;
        if (!RotationUtils.isSilentAiming() && !RotationUtils.isStoppingAiming()) return;

        this.hkim$raySavedYaw = player.getYRot();
        this.hkim$raySavedPitch = player.getXRot();

        player.setYRot(RotationUtils.getServerYaw());
        player.setXRot(RotationUtils.getServerPitch());
    }

    @Inject(method = "pick(F)V", at = @At("RETURN"))
    private void afterPick(CallbackInfo ci) {
        LocalPlayer player = Hkim.mc.player;
        if (player == null) return;
        if (!RotationUtils.isSilentAiming() && !RotationUtils.isStoppingAiming()) return;

        player.setYRot(this.hkim$raySavedYaw);
        player.setXRot(this.hkim$raySavedPitch);
    }
}