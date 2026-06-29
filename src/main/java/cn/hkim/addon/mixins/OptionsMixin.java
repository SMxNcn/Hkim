package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.FreeCam;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsMixin {

    @Inject(at = @At("HEAD"), method = "bobView", cancellable = true)
    private void freecam$onBobView(CallbackInfoReturnable<OptionInstance<Boolean>> cir) {
        if (FreeCam.isFreecamActive()) {
            cir.setReturnValue(OptionInstance.createBoolean("", false));
        }
    }
}
