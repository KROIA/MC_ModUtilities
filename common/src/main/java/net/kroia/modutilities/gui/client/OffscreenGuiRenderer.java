package net.kroia.modutilities.gui.client;

// mc>=1.21.6: GuiGraphics no longer draws; it records into a GuiRenderState that a GuiRenderer turns
// into draw calls at the end of the frame. Offscreen GUI rendering (DisplayBlocks, RecipeImageExporter)
// therefore needs its own GuiRenderState + GuiRenderer. The vanilla GuiRenderer sizes its projection,
// scissor and item atlas from the window; GuiRendererWindowMixin substitutes the offscreen target size
// while render(...) runs. The whole file only exists on 1.21.6+; on older versions every line is commented out.

import com.mojang.blaze3d.pipeline.RenderTarget; // mc>=1.21.6
import com.mojang.blaze3d.systems.RenderSystem; // mc>=1.21.6
import net.fabricmc.api.EnvType; // mc>=1.21.6
import net.fabricmc.api.Environment; // mc>=1.21.6
import net.minecraft.client.Minecraft; // mc>=1.21.6
import net.minecraft.client.gui.GuiGraphics; // mc>=1.21.6
import net.minecraft.client.gui.render.GuiRenderer; // mc>=1.21.6
import net.minecraft.client.gui.render.state.GuiRenderState; // mc>=1.21.6
import net.minecraft.client.renderer.MultiBufferSource; // mc>=1.21.6
import net.minecraft.client.renderer.fog.FogRenderer; // mc>=1.21.6

import java.util.List; // mc>=1.21.6

/**
 * Renders a GUI frame into a {@link RenderTarget} instead of the screen. Usage per frame:
 * {@code GuiGraphics g = begin();} draw into {@code g}, then {@code render(g, target, scale)}.
 * {@code scale} is pixels per GUI unit; the GUI area is {@code target.width / scale} x {@code target.height / scale}.
 */
@Environment(EnvType.CLIENT) // mc>=1.21.6
public final class OffscreenGuiRenderer implements AutoCloseable { // mc>=1.21.6

    /** Window metrics GuiRendererWindowMixin reports while an offscreen render is running, or null. */
    public record Size(int width, int height, int guiScale) {} // mc>=1.21.6
    private static Size activeSize; // mc>=1.21.6

    /** @return the offscreen size to report instead of the window's, or null when rendering normally. */
    public static Size activeSize() { // mc>=1.21.6
        return activeSize; // mc>=1.21.6
    } // mc>=1.21.6

    private final GuiRenderState state = new GuiRenderState(); // mc>=1.21.6
    private final GuiRenderer renderer; // mc>=1.21.6
    private final FogRenderer fogRenderer = new FogRenderer(); // mc>=1.21.6
    // mc>=1.21.9: GUI item rendering goes through a submit-node collector + feature dispatcher. Own instances,
    // so an offscreen render during world rendering never flushes the level's submitted nodes.
    //private final net.minecraft.client.renderer.SubmitNodeStorage submitNodes = new net.minecraft.client.renderer.SubmitNodeStorage(); // mc>=1.21.9
    //private final net.minecraft.client.renderer.feature.FeatureRenderDispatcher featureDispatcher; // mc>=1.21.9

    /** @param bufferSource buffer source used for item rendering into the item atlas */
    public OffscreenGuiRenderer(MultiBufferSource.BufferSource bufferSource) { // mc>=1.21.6
        renderer = new GuiRenderer(state, bufferSource, List.of()); // mc>=1.21.6 mc<=1.21.8
        //Minecraft mc = Minecraft.getInstance(); // mc>=1.21.9
        //featureDispatcher = new net.minecraft.client.renderer.feature.FeatureRenderDispatcher(submitNodes, mc.getBlockRenderer(), bufferSource, // mc>=1.21.9
                //mc.getAtlasManager(), mc.renderBuffers().outlineBufferSource(), mc.renderBuffers().crumblingBufferSource(), mc.font); // mc>=1.21.9
        //renderer = new GuiRenderer(state, bufferSource, submitNodes, featureDispatcher, List.of()); // mc>=1.21.9
    } // mc>=1.21.6

    /** Starts a new frame and returns the GuiGraphics that records into it. */
    public GuiGraphics begin() { // mc>=1.21.6
        state.reset(); // mc>=1.21.6
        return new GuiGraphics(Minecraft.getInstance(), state); // mc>=1.21.6
    } // mc>=1.21.6

    /**
     * Renders everything recorded since {@link #begin()} into {@code target}. Does not clear the target.
     * Restores the main render target, projection and fog afterwards.
     */
    public void render(GuiGraphics graphics, RenderTarget target, int guiScale) { // mc>=1.21.6
        Minecraft mc = Minecraft.getInstance(); // mc>=1.21.6
        graphics.renderDeferredTooltip(); // mc>=1.21.6 mc<=1.21.8
        //graphics.renderDeferredElements(); // mc>=1.21.9 (tooltip + deferred outlines)
        RenderTarget prevTarget = mc.getMainRenderTarget(); // mc>=1.21.6
        var prevFog = RenderSystem.getShaderFog(); // mc>=1.21.6
        RenderSystem.backupProjectionMatrix(); // mc>=1.21.6
        activeSize = new Size(target.width, target.height, Math.max(1, guiScale)); // mc>=1.21.6
        ((net.kroia.mixin.MinecraftAccessor) mc).modutilities$setMainRenderTarget(target); // mc>=1.21.6
        try { // mc>=1.21.6
            renderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE)); // mc>=1.21.6
            renderer.incrementFrameNumber(); // mc>=1.21.6
        } finally { // mc>=1.21.6
            ((net.kroia.mixin.MinecraftAccessor) mc).modutilities$setMainRenderTarget(prevTarget); // mc>=1.21.6
            activeSize = null; // mc>=1.21.6
            RenderSystem.restoreProjectionMatrix(); // mc>=1.21.6
            if (prevFog != null) RenderSystem.setShaderFog(prevFog); // mc>=1.21.6
        } // mc>=1.21.6
    } // mc>=1.21.6

    @Override // mc>=1.21.6
    public void close() { // mc>=1.21.6
        renderer.close(); // mc>=1.21.6
        fogRenderer.close(); // mc>=1.21.6
        //featureDispatcher.close(); // mc>=1.21.9
    } // mc>=1.21.6
} // mc>=1.21.6
