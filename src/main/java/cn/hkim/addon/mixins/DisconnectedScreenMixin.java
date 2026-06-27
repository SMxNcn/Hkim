package cn.hkim.addon.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cn.hkim.addon.Hkim.mc;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin {

    @Shadow @Final
    private Screen parent;

    @Shadow @Final
    private LinearLayout layout;

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;arrangeElements()V", shift = At.Shift.BEFORE))
    private void addReconnectButton(CallbackInfo ci) {
        ServerData serverData = Minecraft.getInstance().getCurrentServer();
        if (serverData == null) return;

        final Screen parentScreen = this.parent;

        Button reconnectBtn = Button.builder(
            Component.translatable("hkim.reconnect"),
                _ -> {
                ServerAddress address = ServerAddress.parseString(serverData.ip);
                ConnectScreen.startConnecting(parentScreen, mc, address, serverData, false, null);
            }
        ).width(200).build();

        this.layout.addChild(reconnectBtn);
    }
}
