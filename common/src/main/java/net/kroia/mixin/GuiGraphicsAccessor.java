package net.kroia.mixin;

import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.client.renderer.MultiBufferSource; // mc<=1.21.5
import net.minecraft.client.gui.render.state.GuiRenderState; // mc>=1.21.6
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes private state of {@link GuiGraphics} that ModUtilities' raw-vertex drawing needs.
 * Up to mc 1.21.5 that is the immediate {@code bufferSource} (its public getter was removed in
 * 1.21.2). From mc 1.21.6 GUI rendering is deferred, so it is the {@code guiRenderState} that
 * custom elements are submitted to.
 */
@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
    //@Accessor("bufferSource") // mc<=1.21.5
    //MultiBufferSource.BufferSource modutilities$getBufferSource(); // mc<=1.21.5
    @Accessor("guiRenderState") // mc>=1.21.6
    GuiRenderState modutilities$getGuiRenderState(); // mc>=1.21.6
}
