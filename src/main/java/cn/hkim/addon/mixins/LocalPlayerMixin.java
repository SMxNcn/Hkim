package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.events.impl.PlayerEvent;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Unique
    private boolean lastSneaking = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        boolean sneaking = self.input.keyPresses.shift();

        if (!lastSneaking && sneaking) {
            Hkim.EVENT_BUS.post(new PlayerEvent.Sneak());
        }

        lastSneaking = sneaking;
    }
}
