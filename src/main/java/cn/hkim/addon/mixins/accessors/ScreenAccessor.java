package cn.hkim.addon.mixins.accessors;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Screen.class)
public interface ScreenAccessor {

    @Accessor("renderables")
    List<AbstractWidget> getRenderables();

    @Accessor("children")
    List<AbstractWidget> getChildren();
}
