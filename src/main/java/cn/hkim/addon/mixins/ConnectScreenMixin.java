package cn.hkim.addon.mixins;

import cn.hkim.addon.mixins.accessors.JoinMultiplayerScreenAccessor;
import cn.hkim.addon.utils.ServerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {

    @Inject(method = "startConnecting", at = @At("HEAD"))
    private static void captureServerData(Screen parent, Minecraft minecraft, ServerAddress hostAndPort, ServerData data, boolean isQuickPlay, TransferState transferState, CallbackInfo ci) {
        if (data != null) {
            ServerUtils.setLastConnectionAttempt(data);
        }

        if (parent instanceof JoinMultiplayerScreen joinScreen) {
            Screen lastScreen = ((JoinMultiplayerScreenAccessor) joinScreen).getLastScreen();
            if (lastScreen instanceof PauseScreen && minecraft.level != null) {
                minecraft.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
            }
        }
    }
}
