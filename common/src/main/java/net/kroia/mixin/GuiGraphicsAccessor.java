package net.kroia.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code bufferSource} of {@link GuiGraphics}. The public
 * {@code bufferSource()} getter was removed in mc 1.21.2; the field exists in both
 * 1.21.1 and 1.21.2, so this accessor works on both versions.
 */
@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
    @Accessor("bufferSource")
    MultiBufferSource.BufferSource modutilities$getBufferSource();
}
