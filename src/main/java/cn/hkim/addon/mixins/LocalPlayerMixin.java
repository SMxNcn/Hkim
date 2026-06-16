package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.events.impl.PlayerEvent;
import cn.hkim.addon.utils.RotationUtils;
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

    @Unique
    private float savedYaw;

    @Unique
    private float savedPitch;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        boolean sneaking = self.input.keyPresses.shift();
        if (!lastSneaking && sneaking) {
            Hkim.EVENT_BUS.post(new PlayerEvent.Sneak());
        }
        lastSneaking = sneaking;

        RotationUtils.syncClientRotation(self.getYRot(), self.getXRot());
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickEnd(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!RotationUtils.isSilentAiming() && !RotationUtils.isStoppingAiming()) return;
        RotationUtils.applyModelRotation(RotationUtils.getServerYaw());
    }

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void beforeSendPosition(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!RotationUtils.isSilentAiming() && !RotationUtils.isStoppingAiming()) return;

        this.savedYaw = self.getYRot();
        this.savedPitch = self.getXRot();
        self.setYRot(RotationUtils.getServerYaw());
        self.setXRot(RotationUtils.getServerPitch());
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void afterSendPosition(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!RotationUtils.isSilentAiming() && !RotationUtils.isStoppingAiming()) return;

        self.setYRot(this.savedYaw);
        self.setXRot(this.savedPitch);
    }
}
