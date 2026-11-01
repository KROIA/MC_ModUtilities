package net.kroia.modutilities.gui.client;

// mc>=1.21.6: GUI rendering is deferred (GuiGraphics records into a GuiRenderState). ClientGraphics
// submits its raw position-color quads (drawLine, drawVertexBuffer_QUADS) through this element,
// which replaces the immediate RenderType.debugQuads() drawing used up to 1.21.5.
// The whole file only exists on 1.21.6+; on older versions every line is commented out.

import com.mojang.blaze3d.pipeline.RenderPipeline; // mc>=1.21.6 mc<=26.2
//import com.mojang.renderpearl.api.pipeline.RenderPipeline; // mc>=26.3
import com.mojang.blaze3d.vertex.VertexConsumer; // mc>=1.21.6
import net.minecraft.client.gui.navigation.ScreenRectangle; // mc>=1.21.6
import net.minecraft.client.gui.render.TextureSetup; // mc>=1.21.6
import net.minecraft.client.gui.render.state.GuiElementRenderState; // mc>=1.21.6 mc<=1.21.11
//import net.minecraft.client.renderer.state.gui.GuiElementRenderState; // mc>=26.1
import net.minecraft.client.renderer.RenderPipelines; // mc>=1.21.6
import org.joml.Matrix3x2f; // mc>=1.21.6

/** Batch of colored quads (4 vertices each) in GUI coordinates, transformed by {@code pose}. */
record GuiColoredQuadsRenderState(Matrix3x2f pose, float[] xy, int[] colors, // mc>=1.21.6
                                  ScreenRectangle scissorArea, ScreenRectangle bounds) // mc>=1.21.6
        implements GuiElementRenderState { // mc>=1.21.6
    @Override // mc>=1.21.6
    //public void buildVertices(VertexConsumer consumer, float z) { // mc>=1.21.6 mc<=1.21.8
    public void buildVertices(VertexConsumer consumer) { // mc>=1.21.9 (GUI depth is implicit in submission order)
        for (int i = 0; i < colors.length; i++) { // mc>=1.21.6
            //consumer.addVertexWith2DPose(pose, xy[i * 2], xy[i * 2 + 1], z).setColor(colors[i]); // mc>=1.21.6 mc<=1.21.8
            consumer.addVertexWith2DPose(pose, xy[i * 2], xy[i * 2 + 1]).setColor(colors[i]); // mc>=1.21.9
        } // mc>=1.21.6
    } // mc>=1.21.6

    @Override // mc>=1.21.6
    public RenderPipeline pipeline() { // mc>=1.21.6
        return RenderPipelines.GUI; // mc>=1.21.6
    } // mc>=1.21.6

    @Override // mc>=1.21.6
    public TextureSetup textureSetup() { // mc>=1.21.6
        return TextureSetup.noTexture(); // mc>=1.21.6
    } // mc>=1.21.6

    /** Axis-aligned screen bounds of the transformed vertices (used by the GUI renderer for sorting/culling). */
    static ScreenRectangle boundsOf(Matrix3x2f pose, float[] xy) { // mc>=1.21.6
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE; // mc>=1.21.6
        org.joml.Vector2f p = new org.joml.Vector2f(); // mc>=1.21.6
        for (int i = 0; i + 1 < xy.length; i += 2) { // mc>=1.21.6
            pose.transformPosition(xy[i], xy[i + 1], p); // mc>=1.21.6
            minX = Math.min(minX, p.x); minY = Math.min(minY, p.y); // mc>=1.21.6
            maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y); // mc>=1.21.6
        } // mc>=1.21.6
        if (minX > maxX) return null; // mc>=1.21.6
        int x0 = (int) Math.floor(minX), y0 = (int) Math.floor(minY); // mc>=1.21.6
        return new ScreenRectangle(x0, y0, (int) Math.ceil(maxX) - x0, (int) Math.ceil(maxY) - y0); // mc>=1.21.6
    } // mc>=1.21.6
} // mc>=1.21.6
