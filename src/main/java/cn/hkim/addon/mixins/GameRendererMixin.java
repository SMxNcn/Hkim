package cn.hkim.addon.mixins;

import cn.hkim.addon.features.impl.FreeCam;
import cn.hkim.addon.utils.render.pip.PIPRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(at = @At("HEAD"), method = "render")
    private void freecam$onRender(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        FreeCam.onRenderTick();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;"))
    private List<PictureInPictureRenderer<?>> onListOf(Object e1, Object e2, Object e3, Object e4, Object e5) {
        System.out.println("GameRendererMixin: registering PIPRenderer");
        List<PictureInPictureRenderer<?>> list = new ArrayList<>();
        list.add((PictureInPictureRenderer<?>) e1);
        list.add((PictureInPictureRenderer<?>) e2);
        list.add((PictureInPictureRenderer<?>) e3);
        list.add((PictureInPictureRenderer<?>) e4);
        list.add((PictureInPictureRenderer<?>) e5);
        list.add(new PIPRenderer());
        return list;
    }
}
