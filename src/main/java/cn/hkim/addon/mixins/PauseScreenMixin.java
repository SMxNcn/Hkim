package cn.hkim.addon.mixins;

//? if 26.2 {
import cn.hkim.addon.mixins.accessors.ScreenAccessor;
import java.util.List;
//? } else {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.multiplayer.ServerData;
*///? }
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

import static cn.hkim.addon.Hkim.mc;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {

    @Unique
    private static final Component SERVER_LIST_LABEL = Component.translatable("hkim.server_list");

    //? if 26.2 {
    @Unique
    private static final Component OPTIONS_KEY = Component.translatable("menu.options");

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
    //? } else {
    /*@Unique
    private static final Component SERVER_LINKS_TEXT = Component.translatable("menu.server_links");
    @Unique
    private static final int HALF_WIDTH = 98;
    @Unique
    private boolean hkim$serverLinksRowSplit;

    @Inject(method = "createPauseMenu", at = @At("HEAD"))
    private void hkim$resetServerListState(CallbackInfo ci) {
        this.hkim$serverLinksRowSplit = false;
    }

    @Unique
    private static boolean hkim$shouldShowServerList() {
        if (mc.player == null || mc.hasSingleplayerServer()) return false;
        ServerData server = mc.getCurrentServer();
        return server == null || !server.isLan();
    }

    @WrapOperation(method = "addCustomDialogButtons", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;I)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private LayoutElement hkim$splitServerLinksRow(GridLayout.RowHelper helper, LayoutElement widget, int columnWidth, Operation<LayoutElement> original) {
        if (!hkim$shouldShowServerList() || !(widget instanceof AbstractWidget child) || !SERVER_LINKS_TEXT.equals(child.getMessage())) {
            return original.call(helper, widget, columnWidth);
        }

        child.setWidth(HALF_WIDTH);
        this.hkim$serverLinksRowSplit = true;
        original.call(helper, widget, 1);
        return original.call(helper, hkim$buildServerListButton(HALF_WIDTH), 1);
    }

    @Inject(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;", ordinal = 2))
    private void hkim$addServerListRow(CallbackInfo ci, @Local(name = "helper") GridLayout.RowHelper helper) {
        if (this.hkim$serverLinksRowSplit || !hkim$shouldShowServerList()) return;
        helper.addChild(hkim$buildServerListButton(204), 2);
    }

    @Unique
    private Button hkim$buildServerListButton(int width) {
        return Button.builder(
            SERVER_LIST_LABEL, _ -> mc.gui.setScreen(new JoinMultiplayerScreen((Screen) (Object) this))
        ).width(width).build();
    }
    *///? }
}
