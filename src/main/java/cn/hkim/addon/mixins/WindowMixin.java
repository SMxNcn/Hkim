package cn.hkim.addon.mixins;

import cn.hkim.addon.Hkim;
import cn.hkim.addon.features.impl.TitleManager;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@Mixin(Window.class)
public class WindowMixin {

    @Redirect(method = "setIcon", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/IconSet;getStandardIcons(Lnet/minecraft/server/packs/PackResources;)Ljava/util/List;"))
    public List<IoSupplier<InputStream>> setCustomIcon(IconSet instance, PackResources resources) throws IOException {
        if (!TitleManager.INSTANCE.getEnabled() || !TitleManager.getCustomIcon()) return instance.getStandardIcons(resources);
        InputStream icon16x = Hkim.class.getResourceAsStream("/assets/hkim/textures/icon16x.png");
        InputStream icon32x = Hkim.class.getResourceAsStream("/assets/hkim/textures/icon32x.png");
        return List.of(() -> Objects.requireNonNull(icon16x), () -> Objects.requireNonNull(icon32x));
    }
}
