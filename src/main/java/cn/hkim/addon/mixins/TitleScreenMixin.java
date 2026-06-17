package cn.hkim.addon.mixins;

import cn.hkim.addon.config.ModuleConfig;
import cn.hkim.addon.features.impl.MainMenuModule;
import cn.hkim.addon.gui.ClientButton;
import cn.hkim.addon.gui.MainMenu;
import cn.hkim.addon.mixins.accessors.ScreenAccessor;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cn.hkim.addon.Hkim.mc;
import static cn.hkim.addon.utils.ChatUtilsKt.coloredChar;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Inject(method = "init", at = @At("RETURN"))
    public void init(CallbackInfo ci) {
        TitleScreen screen = (TitleScreen)(Object)this;

        if (MainMenuModule.INSTANCE.getEnabled()) {
            mc.gui.setScreen(new MainMenu());
            return;
        }

        ClientButton ncBtn = new ClientButton(screen.width - 55, -7, 50, 18, coloredChar("Necron", 0xFF8EDDFF), _ -> {
            MainMenuModule.INSTANCE.setEnabled(true);
            ModuleConfig.INSTANCE.saveConfig();
            mc.gui.setScreen(new MainMenu());
        });

        ScreenAccessor accessor = (ScreenAccessor)screen;
        accessor.getRenderables().add(ncBtn);
        accessor.getChildren().add(ncBtn);
    }
}
