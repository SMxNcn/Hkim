package cn.hkim.addon.mixins;

import cn.hkim.addon.mixins.accessors.ScreenAccessor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static cn.hkim.addon.Hkim.mc;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {

    @Unique
    private static final Component REPORT_PLAYER_KEY = Component.translatable("menu.playerReporting");
    @Unique
    private static final Component SERVER_LIST_LABEL = Component.translatable("hkim.server_list");

    @Inject(method = "init", at = @At("RETURN"))
    private void replaceReportPlayerButton(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ScreenAccessor accessor = (ScreenAccessor) screen;
        List<AbstractWidget> renderables = accessor.getRenderables();

        for (int i = 0; i < renderables.size(); i++) {
            AbstractWidget widget = renderables.get(i);
            if (widget instanceof Button button && REPORT_PLAYER_KEY.equals(button.getMessage())) {
                Button serverListBtn = Button.builder(
                    SERVER_LIST_LABEL, _ -> mc.setScreen(new JoinMultiplayerScreen(screen))
                ).bounds(button.getX(), button.getY(), button.getWidth(), button.getHeight()).build();

                renderables.set(i, serverListBtn);

                List<AbstractWidget> children = accessor.getChildren();
                int childIndex = children.indexOf(button);
                if (childIndex != -1) {
                    children.set(childIndex, serverListBtn);
                }

                break;
            }
        }
    }
}
