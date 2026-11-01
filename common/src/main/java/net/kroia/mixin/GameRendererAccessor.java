package net.kroia.mixin;

// mc>=26.2: the main render target moved from Minecraft to GameRenderer. Only listed in the 26.2+ mixin
// config; on older versions every line is commented out.

import com.mojang.blaze3d.pipeline.RenderTarget; // mc>=26.2
import net.minecraft.client.renderer.GameRenderer; // mc>=26.2
import org.spongepowered.asm.mixin.Mixin; // mc>=26.2
import org.spongepowered.asm.mixin.Mutable; // mc>=26.2
import org.spongepowered.asm.mixin.gen.Accessor; // mc>=26.2

@Mixin(GameRenderer.class) // mc>=26.2
public interface GameRendererAccessor { // mc>=26.2
    @Mutable // mc>=26.2
    @Accessor("mainRenderTarget") // mc>=26.2
    void modutilities$setMainRenderTarget(RenderTarget target); // mc>=26.2
} // mc>=26.2
