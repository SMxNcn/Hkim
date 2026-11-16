package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.events.impl.InputEvent;
import cn.hkim.addon.utils.KeyAction;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Redirect(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"))
    private static void hkim$postInputEvent(InputConstants.Key key, long handle, int action, KeyEvent event) {
        if (key.equals(InputConstants.UNKNOWN)) return;

        InputEvent inputEvent = new InputEvent(key, KeyAction.get(action));
        Hkim.EVENT_BUS.post(inputEvent);
        if (!inputEvent.isCancelled()) {
            KeyMapping.click(key);
        }
    }
}
