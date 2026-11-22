package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.events.impl.BossBarRenderEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {

    @Shadow
    @Final
    private Map<UUID, LerpingBossEvent> events;

    @Unique
    private final List<UUID> hkim$hiddenIds = new ArrayList<>();

    @Unique
    private final List<LerpingBossEvent> hkim$hiddenBars = new ArrayList<>();

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void postPreLayoutEvents(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        hkim$hiddenIds.clear();
        hkim$hiddenBars.clear();

        var iterator = this.events.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            BossBarRenderEvent event = new BossBarRenderEvent(
                    entry.getValue(),
                    BossBarRenderEvent.Stage.PRE_LAYOUT
            );
            Hkim.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                hkim$hiddenIds.add(entry.getKey());
                hkim$hiddenBars.add(entry.getValue());
                iterator.remove();
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void hkim$restoreHiddenBars(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        for (int i = 0; i < hkim$hiddenIds.size(); i++) {
            this.events.put(hkim$hiddenIds.get(i), hkim$hiddenBars.get(i));
        }
        hkim$hiddenIds.clear();
        hkim$hiddenBars.clear();
    }

    @Inject(method = "extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V", at = @At("HEAD"), cancellable = true)
    private void postBarRenderEvent(GuiGraphicsExtractor graphics, int x, int y, BossEvent event, CallbackInfo ci) {
        BossBarRenderEvent barEvent = new BossBarRenderEvent(event, BossBarRenderEvent.Stage.BAR, x, y);
        Hkim.EVENT_BUS.post(barEvent);
        if (barEvent.isCancelled()) ci.cancel();
    }
}
