package net.kroia.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets offscreen GUI rendering temporarily swap Minecraft's main render target.
 * Since mc 1.21.2 the MAIN_TARGET output state of every GUI RenderType binds
 * {@code getMainRenderTarget()} on each batch flush, so draws into a custom
 * TextureTarget only stay there while it is installed as the main target.
 * The field exists in 1.21.1 too, so this accessor compiles on all versions.
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Mutable
    @Accessor("mainRenderTarget")
    void modutilities$setMainRenderTarget(RenderTarget target);
}
