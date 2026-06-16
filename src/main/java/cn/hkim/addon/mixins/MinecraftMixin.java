package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.config.ModuleConfig;
import cn.hkim.addon.events.impl.TickEvent;
import cn.hkim.addon.features.impl.TitleManager;
import cn.hkim.addon.gui.Background;
import cn.hkim.addon.utils.RotationUtils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Unique
    private float raySavedYaw;

    @Unique
    private float raySavedPitch;

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

    /**
     * 拦截射线检测，检测前将玩家旋转临时替换为 server 角度，使点击/交互命中瞄准目标。
     * 检测后恢复 client 真实视角，不影响玩家看到的画面。
     */
    @Inject(method = "pick(F)V", at = @At("HEAD"))
    private void beforePick(CallbackInfo ci) {
        LocalPlayer player = Hkim.mc.player;
        if (player != null) {
            this.raySavedYaw = player.getYRot();
            this.raySavedPitch = player.getXRot();

            player.setYRot(RotationUtils.getServerYaw());
            player.setXRot(RotationUtils.getServerPitch());
        }
    }

    @Inject(method = "pick(F)V", at = @At("RETURN"))
    private void afterPick(CallbackInfo ci) {
        LocalPlayer player = Hkim.mc.player;
        if (player != null) {
            player.setYRot(this.raySavedYaw);
            player.setXRot(this.raySavedPitch);
        }
    }
}