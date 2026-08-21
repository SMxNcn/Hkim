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
    private static final Component OPTIONS_KEY = Component.translatable("menu.options");
    @Unique
    private static final Component SERVER_LIST_LABEL = Component.translatable("hkim.server_list");

    @Inject(method = "init", at = @At("RETURN"))
    private void addServerListButton(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (mc.player == null) return;

        ScreenAccessor accessor = (ScreenAccessor) screen;
        List<AbstractWidget> renderables = accessor.getRenderables();

        for (int i = 0; i < renderables.size(); i++) {
            AbstractWidget widget = renderables.get(i);
            if (widget instanceof Button button && OPTIONS_KEY.equals(button.getMessage())) {
                if (button.getWidth() != 204) return;

                int oldX = button.getX(), oldY = button.getY(), oldW = button.getWidth(), oldH = button.getHeight();
                int halfW = 98;
                int gap = 4;
                int newX = oldX + (oldW - halfW * 2 - gap) / 2;

                button.setX(newX);
                button.setWidth(halfW);

                Button serverListBtn = Button.builder(
                    SERVER_LIST_LABEL, _ -> mc.gui.setScreen(new JoinMultiplayerScreen(screen))
                ).bounds(newX + halfW + gap, oldY, halfW, oldH).build();

                renderables.add(i + 1, serverListBtn);

                List<AbstractWidget> children = accessor.getChildren();
                int childIndex = children.indexOf(button);
                if (childIndex != -1) {
                    children.add(childIndex + 1, serverListBtn);
                }
                break;
            }
        }
    }
}
