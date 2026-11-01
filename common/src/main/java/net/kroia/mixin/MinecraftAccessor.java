package net.kroia.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets offscreen renderers temporarily replace the main render target. Since MC 1.21.5 all
 * GUI/RenderType draws go to {@link Minecraft#getMainRenderTarget()} (there is no
 * {@code RenderTarget.bindWrite} any more), so swapping it is how ModUtilities renders a GUI into
 * its own texture. Always restore the previous target afterwards. The field exists on all
 * supported versions.
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Mutable
    @Accessor("mainRenderTarget")
    void modutilities$setMainRenderTarget(RenderTarget target);
}
