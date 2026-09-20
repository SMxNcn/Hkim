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
    private static final Component SERVER_LINKS_TEXT = Component.translatable("menu.server_links");
    @Unique
    private static final Component SERVER_LIST_LABEL = Component.translatable("hkim.server_list");

    @Unique
    private static final int BUTTON_HEIGHT = 20;
    @Unique
    private static final int COLUMN_GAP = 8;
    @Unique
    private static final int HALF_WIDTH = 98;

    @Inject(method = "init", at = @At("RETURN"))
    private void addServerListButton(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (mc.player == null) return;

        ScreenAccessor accessor = (ScreenAccessor) screen;
        List<AbstractWidget> renderables = accessor.getRenderables();

        Button links = null;
        for (AbstractWidget widget : renderables) {
            if (widget instanceof Button button && SERVER_LINKS_TEXT.equals(button.getMessage())) {
                links = button;
                break;
            }
        }

        Button serverListBtn;
        if (links != null) {
            int oldX = links.getX(), oldY = links.getY(), oldW = links.getWidth(), oldH = links.getHeight();
            int newX = oldX + (oldW - HALF_WIDTH * 2 - COLUMN_GAP) / 2;

            links.setX(newX);
            links.setWidth(HALF_WIDTH);
            serverListBtn = build(screen, newX + HALF_WIDTH + COLUMN_GAP, oldY, HALF_WIDTH, oldH);
        } else {
            int rowY = Integer.MIN_VALUE;
            for (AbstractWidget widget : renderables) {
                if (!(widget instanceof Button button)) continue;
                if (button.getWidth() != HALF_WIDTH) continue;
                rowY = Math.max(rowY, button.getY());
            }
            if (rowY == Integer.MIN_VALUE) return;

            int left = Integer.MAX_VALUE;
            int right = Integer.MIN_VALUE;
            for (AbstractWidget widget : renderables) {
                if (!(widget instanceof Button button) || button.getY() != rowY) continue;
                left = Math.min(left, button.getX());
                right = Math.max(right, button.getX() + button.getWidth());
            }

            int rowWidth = right - left;
            int width = Math.max(rowWidth, 204);
            int x = left - (width - rowWidth) / 2;
            int y = rowY - (BUTTON_HEIGHT + 4);

            serverListBtn = build(screen, x, y, width, BUTTON_HEIGHT);
        }

        renderables.add(serverListBtn);
        accessor.getChildren().add(serverListBtn);
    }

    @Unique
    private static Button build(Screen parent, int x, int y, int width, int height) {
        return Button.builder(
            SERVER_LIST_LABEL, _ -> mc.gui.setScreen(new JoinMultiplayerScreen(parent))
        ).bounds(x, y, width, height).build();
    }
}
