package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.config.ModuleConfig;
import cn.hkim.addon.events.impl.TickEvent;
import cn.hkim.addon.features.impl.TitleManager;
import cn.hkim.addon.gui.Background;
import cn.hkim.addon.utils.render.nvg.NVGRenderer;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

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
}