package net.kroia.modutilities.gui.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.IGraphics;
import net.kroia.modutilities.gui.elements.base.Vertex;
import net.kroia.modutilities.gui.elements.base.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

/**
 * World-space implementation of {@link IGraphics} for rendering GUI elements
 * on block faces via block entity renderers.
 * <p>
 * Uses {@link RenderType#debugQuads()} for filled rectangles, gradients,
 * outlines, lines, and vertex buffers. Uses {@link Font#drawInBatch} with a
 * 180-degree Y rotation for text rendering.
 * <p>
 * GUI z-values are compressed by {@link #Z_COMPRESS} so that large GUI z
 * offsets (e.g. 200 for tooltips) produce only tiny depth offsets in world
 * space, keeping everything visually on the block face while preserving
 * z-ordering.
 * <p>
 * Call {@link #setup(PoseStack, MultiBufferSource)} each frame from the
 * block entity renderer before rendering through the GUI element tree.
 *
 * @apiNote This class is client-only ({@code @Environment(EnvType.CLIENT)}).
 * @see ClientGraphics
 */
@Environment(EnvType.CLIENT)
public class WorldGraphics implements IGraphics {

    /**
     * Auto-incrementing z offset per draw call. Prevents z-fighting between
     * overlapping primitives. Accumulates globally across all elements in a
     * frame and resets in {@link #setup}. The render pass z-offsets (background=0,
     * foreground=1) provide coarse ordering; drawZ provides fine ordering within.
     */
    private float drawZ = 0;
    private static final float DRAW_Z_STEP = 0.01f;

    private PoseStack poseStack;
    private MultiBufferSource bufferSource;
    private Font font;

    /**
     * Creates a new WorldGraphics with no backing PoseStack or buffer source.
     * Call {@link #setup(PoseStack, MultiBufferSource)} before rendering.
     */
    public WorldGraphics() {
    }

    /**
     * Sets the PoseStack and MultiBufferSource for the current render frame.
     * Must be called once per frame from the block entity renderer before any
     * drawing calls.
     *
     * @param poseStack    the pose stack from the BER render call
     * @param bufferSource the buffer source from the BER render call
     */
    public void setup(PoseStack poseStack, MultiBufferSource bufferSource) {
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.drawZ = 0;
    }

    /**
     * Sets the font used for text rendering and measurement.
     *
     * @param font the Minecraft font instance
     */
    public void setFont(Font font) {
        this.font = font;
    }

    /**
     * @return the font currently used by this graphics backend, or {@code null}
     */
    public Font getFont() {
        return font;
    }

    // ── Text ────────────────────────────────────────────────────────────────

    @Override
    public void drawString(Object fontObj, String text, int x, int y, int color) {
        if (font == null || text == null || text.isEmpty()) return;
        drawZ += DRAW_Z_STEP;
        poseStack.pushPose();
        poseStack.translate(x, y, drawZ);
        MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
        font.drawInBatch(text, 0, 0, color, false,
                poseStack.last().pose(), immediate,
                Font.DisplayMode.NORMAL, 0, 0xF000F0);
        immediate.endBatch();
        poseStack.popPose();
    }

    @Override
    public void drawString(Object fontObj, String text, int x, int y, int color, boolean dropShadow) {
        if (font == null || text == null || text.isEmpty()) return;
        drawZ += DRAW_Z_STEP;
        poseStack.pushPose();
        poseStack.translate(x, y, drawZ);
        MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
        font.drawInBatch(text, 0, 0, color, dropShadow,
                poseStack.last().pose(), immediate,
                Font.DisplayMode.NORMAL, 0, 0xF000F0);
        immediate.endBatch();
        poseStack.popPose();
    }

    // ── Fills ───────────────────────────────────────────────────────────────

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        drawZ += DRAW_Z_STEP;
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());
        consumer.addVertex(matrix, x1, y1, drawZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, x1, y2, drawZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, x2, y2, drawZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, x2, y1, drawZ).setColor(r, g, b, a);
    }

    @Override
    public void fillGradient(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        drawZ += DRAW_Z_STEP;
        int aF = (colorFrom >> 24) & 0xFF, rF = (colorFrom >> 16) & 0xFF;
        int gF = (colorFrom >> 8) & 0xFF, bF = colorFrom & 0xFF;
        int aT = (colorTo >> 24) & 0xFF, rT = (colorTo >> 16) & 0xFF;
        int gT = (colorTo >> 8) & 0xFF, bT = colorTo & 0xFF;

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());
        consumer.addVertex(matrix, x1, y1, drawZ).setColor(rF, gF, bF, aF);
        consumer.addVertex(matrix, x1, y2, drawZ).setColor(rT, gT, bT, aT);
        consumer.addVertex(matrix, x2, y2, drawZ).setColor(rT, gT, bT, aT);
        consumer.addVertex(matrix, x2, y1, drawZ).setColor(rF, gF, bF, aF);
    }

    @Override
    public void renderOutline(int x, int y, int width, int height, int color) {
        fill(x, y, x + width, y + 1, color);                           // top
        fill(x, y + height - 1, x + width, y + height, color);         // bottom
        fill(x, y + 1, x + 1, y + height - 1, color);                  // left
        fill(x + width - 1, y + 1, x + width, y + height - 1, color);  // right
    }

    // ── Tooltips (no-op in world space) ─────────────────────────────────────

    @Override
    public void renderTooltip(Object font, Component text, int x, int y) {
        // No-op: tooltips don't make sense on block faces
    }

    @Override
    public void renderTooltip(Object font, List<Component> lines, int x, int y) {
        // No-op: tooltips don't make sense on block faces
    }

    @Override
    public void renderTooltip(Object font, ItemStack itemStack, int x, int y) {
        // No-op: tooltips don't make sense on block faces
    }

    // ── Items (no-op for now) ───────────────────────────────────────────────

    @Override
    public void renderItem(ItemStack itemStack, Object font, int x, int y) {
        // No-op: item rendering in world space needs a different approach
    }

    @Override
    public void renderItem(ItemStack itemStack, Object font, int x, int y, int seed) {
        // No-op: item rendering in world space needs a different approach
    }

    // ── Textures (no-op for now) ────────────────────────────────────────────

    @Override
    public void blit(ResourceLocation atlasLocation, int x, int y, int uOffset, int vOffset, int width, int height) {
        // No-op: texture rendering in world space needs a different approach
    }

    @Override
    public void blit(ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        // No-op: texture rendering in world space needs a different approach
    }

    // ── Scissor (no-op in world space) ──────────────────────────────────────

    @Override
    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        // No-op: scissor doesn't apply in world-space rendering
    }

    @Override
    public void disableScissor() {
        // No-op: scissor doesn't apply in world-space rendering
    }

    // ── Transforms ──────────────────────────────────────────────────────────

    @Override
    public void translate(float x, float y, float z) {
        poseStack.translate(x, y, z);
    }

    @Override
    public void translate(double x, double y, double z) {
        poseStack.translate(x, y, z);
    }

    @Override
    public void scale(float x, float y, float z) {
        poseStack.scale(x, y, z);
    }

    @Override
    public void mulPose(Quaternionf quaternion) {
        poseStack.mulPose(quaternion);
    }

    @Override
    public void rotateAround(Quaternionf quaternion, float x, float y, float z) {
        poseStack.rotateAround(quaternion, x, y, z);
    }

    @Override
    public void pushPose() {
        poseStack.pushPose();
    }

    @Override
    public void popPose() {
        poseStack.popPose();
    }

    // ── Font queries ──────────────────────────────────────────────────────

    @Override
    public int getFontLineHeight() {
        return font != null ? font.lineHeight : 9;
    }

    @Override
    public int getTextWidth(String text) {
        if (font == null || text == null) return 0;
        if (!text.contains("\n")) return font.width(text);
        int max = 0;
        for (String line : text.split("\n", -1)) {
            max = Math.max(max, font.width(line));
        }
        return max;
    }

    // ── Line / vertex drawing ──────────────────────────────────────────────

    @Override
    public void drawLine(int x1, int y1, int x2, int y2, float thickness, int color) {
        drawZ += DRAW_Z_STEP;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.0001F) return;

        float ux = dx / length;
        float uy = dy / length;
        float nx = -uy * thickness / 2;
        float ny = ux * thickness / 2;

        float p1x = x1 + nx, p1y = y1 + ny;
        float p2x = x2 + nx, p2y = y2 + ny;
        float p3x = x2 - nx, p3y = y2 - ny;
        float p4x = x1 - nx, p4y = y1 - ny;

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());
        consumer.addVertex(matrix, p1x, p1y, drawZ).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, p2x, p2y, drawZ).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, p3x, p3y, drawZ).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, p4x, p4y, drawZ).setColor(red, green, blue, alpha);
    }

    @Override
    public void drawVertexBuffer_QUADS(VertexBuffer buffer) {
        drawZ += DRAW_Z_STEP;
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());
        for (Vertex vertex : buffer.getVertices()) {
            consumer.addVertex(matrix, vertex.x, vertex.y, drawZ)
                    .setColor(vertex.red, vertex.green, vertex.blue, vertex.alpha);
        }
    }

    // ── Query ───────────────────────────────────────────────────────────────

    @Override
    public Matrix4f getLastPoseMatrix() {
        return poseStack.last().pose();
    }

    @Override
    public void flush() {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }
}
