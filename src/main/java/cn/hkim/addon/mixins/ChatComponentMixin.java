package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.events.impl.ChatReceiveEvent;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Inject(method = "addMessage", at = @At("HEAD"), cancellable = true)
    private void handleAddMessage(Component contents, MessageSignature signature, GuiMessageSource source, net.minecraft.client.multiplayer.chat.GuiMessageTag tag, CallbackInfo ci) {
        ChatReceiveEvent event = ChatReceiveEvent.get(contents);
        Hkim.EVENT_BUS.post(event);

        if (event.isCancelled()) ci.cancel();
    }
}
