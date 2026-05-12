package net.kroia.modutilities.gui.client;


import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.IGraphics;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.kroia.modutilities.gui.elements.base.Vertex;
import net.kroia.modutilities.gui.elements.base.VertexBuffer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics; // mc>=1.20.1
//import com.mojang.blaze3d.vertex.PoseStack; // mc<=1.19.4
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Optional;

/**
 * Client-side implementation of {@link IGraphics} backed by Minecraft's
 * {@link GuiGraphics}.
 * <p>
 * The owning screen replaces the underlying {@link GuiGraphics} each frame via
 * {@link #setGraphics(GuiGraphics)} before any drawing calls are made. Methods
 * on this class delegate to that backing instance and translate between the
 * framework's API and the Minecraft client API.
 * <p>
 * In addition to the {@link IGraphics} contract this class exposes several
 * client-only extras that are not part of the common interface:
 * <ul>
 *   <li>{@link #setGraphics(GuiGraphics)}</li>
 *   <li>{@link #getGraphics()}</li>
 *   <li>{@link #getPoseStack()}</li>
 *   <li>{@link #bufferSource()}</li>
 *   <li>Overloads that accept {@link RenderType} or {@link TextureAtlasSprite}</li>
 * </ul>
 *
 * @apiNote This class is client-only ({@code @Environment(EnvType.CLIENT)}).
 */
@Environment(EnvType.CLIENT)
public class ClientGraphics implements IGraphics {

    GuiGraphics graphics;// mc>=1.20.1
    //PoseStack graphics; // mc<1.19.4
    private Font font;
    private boolean enableShadow = true;

    /**
     * Creates a new graphics wrapper.
     * The backing {@link GuiGraphics} must be supplied via
     * {@link #setGraphics(GuiGraphics)} before any drawing call.
     */
    public ClientGraphics()
    {
    }

    /**
     * Updates the font used for text measurement queries.
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

    /**
     * Controls whether text is rendered with a drop shadow.
     * Default is {@code true} (matching Minecraft's screen rendering).
     * Set to {@code false} for block-face rendering where shadows may be undesirable.
     */
    public void setEnableShadow(boolean enableShadow) {
        this.enableShadow = enableShadow;
    }

    public boolean isEnableShadow() {
        return enableShadow;
    }



    /**
     * Updates the underlying {@link GuiGraphics} that drawing calls are forwarded
     * to. Typically called once per frame by the parent screen.
     *
     * @param graphics the current frame's {@link GuiGraphics} instance
     */
    public void setGraphics(GuiGraphics graphics)
    {
        this.graphics = graphics;
    }

    /**
     * @return the currently active {@link GuiGraphics}, or {@code null} if none is set
     */
    public GuiGraphics getGraphics()
    {
        return graphics;
    }

    /**
     * @return the {@link PoseStack} currently associated with the active
     *         {@link GuiGraphics}
     */
    public PoseStack getPoseStack()
    {
        //return graphics; // mc<=1.19.4
        return graphics.pose(); // mc>=1.20.1
    }

    /**
     * Draws a single line of text at the given screen coordinates.
     *
     * @param font  the font to render with (must be {@link Font})
     * @param text  the text to draw
     * @param x     the x position in GUI pixels
     * @param y     the y position in GUI pixels
     * @param color the packed ARGB color to use for the text
     */
    @Override
    public void drawString(Object font, String text, int x, int y, int color)
    {
        //font.draw(graphics, text, x, y, color);  // mc<=1.19.4
        graphics.drawString((Font) font, text, x, y, color, enableShadow); // mc>=1.20.1
    }

    /**
     * Draws a single line of text with optional drop shadow.
     *
     * @param font       the font to render with (must be {@link Font})
     * @param text       the text to draw
     * @param x          the x position in GUI pixels
     * @param y          the y position in GUI pixels
     * @param color      the packed ARGB color to use for the text
     * @param dropShadow if {@code true}, a drop shadow is rendered behind the text
     */
    @Override
    public void drawString(Object font, String text, int x, int y, int color, boolean dropShadow)
    {
        //font.draw(graphics, text, x, y, color);  // mc<=1.19.4
        graphics.drawString((Font) font, text, x, y, color, dropShadow); // mc>=1.20.1
    }

    /**
     * Fills a rectangular area defined by two opposite corners with a solid color.
     *
     * @param x1    the first x coordinate (inclusive)
     * @param y1    the first y coordinate (inclusive)
     * @param x2    the second x coordinate (exclusive)
     * @param y2    the second y coordinate (exclusive)
     * @param color the packed ARGB fill color
     */
    @Override
    public void fill(int x1, int y1, int x2, int y2, int color)
    {
        //Screen.fill(graphics, x1, y1, x2, y2, color); // mc<=1.19.4
        graphics.fill(x1, y1, x2, y2, color); // mc>=1.20.1
    }

    /**
     * Fills a rectangular area with a vertical color gradient.
     *
     * @param x1        the first x coordinate
     * @param y1        the first y coordinate
     * @param x2        the second x coordinate
     * @param y2        the second y coordinate
     * @param colorFrom the packed ARGB color at the top of the gradient
     * @param colorTo   the packed ARGB color at the bottom of the gradient
     */
    @Override
    public void fillGradient(int x1, int y1, int x2, int y2, int colorFrom, int colorTo)
    {
        //Screen.fill(graphics, x1, y1, x2, y2, colorFrom); // mc<=1.19.4
        graphics.fillGradient(x1, y1, x2, y2, colorFrom, colorTo); // mc>=1.20.1
    }

    /**
     * Fills a rectangular area with a vertical color gradient, using a custom
     * {@link RenderType} and z offset.
     *
     * @param renderType the render type to use
     * @param x1         the first x coordinate
     * @param y1         the first y coordinate
     * @param x2         the second x coordinate
     * @param y2         the second y coordinate
     * @param colorFrom  the packed ARGB color at the top of the gradient
     * @param colorTo    the packed ARGB color at the bottom of the gradient
     * @param z          the z (depth) offset
     */
    public void fillGradient(RenderType renderType, int x1, int y1, int x2, int y2, int colorFrom, int colorTo, int z)
    {
        //Screen.fill(graphics, x1, y1, x2, y2, colorFrom); // mc<=1.19.4
        graphics.fillGradient(renderType, x1, y1, x2, y2, colorFrom, colorTo, z); // mc>=1.20.1
    }

    /**
     * Renders a 1px outline around the given rectangle.
     *
     * @param x1     the x position of the rectangle
     * @param y1     the y position of the rectangle
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @param color  the packed ARGB color of the outline
     */
    @Override
    public void renderOutline(int x1, int y1, int width, int height, int color)
    {
        //Screen.renderOutline(graphics, x1, y1, width, height, color); // mc<=1.19.4
        graphics.renderOutline(x1, y1, width, height, color); // mc>=1.20.1
    }

    /**
     * Renders a single-line tooltip at the given coordinates.
     *
     * @param font the font to use (must be {@link Font})
     * @param text the tooltip text component
     * @param x    the x position
     * @param y    the y position
     */
    @Override
    public void renderTooltip(Object font, Component text, int x, int y)
    {
        //screen.renderTooltip(graphics, text, x, y); // mc<=1.19.4
        graphics.renderTooltip((Font) font, text, x, y); // mc>=1.20.1
    }

    /**
     * Renders a multi-line tooltip at the given coordinates.
     *
     * @param font  the font to use (must be {@link Font})
     * @param lines the tooltip lines, in order
     * @param x     the x position
     * @param y     the y position
     */
    @Override
    public void renderTooltip(Object font, List<Component> lines, int x, int y)
    {
        //screen.renderTooltip(graphics, lines, Optional.empty(), x, y); // mc<=1.19.4
        graphics.renderTooltip((Font) font, lines, Optional.empty(), x, y); // mc>=1.20.1
    }

    /**
     * Renders an item tooltip at the given coordinates, including the item's
     * standard tooltip lines.
     *
     * @param font      the font to use (must be {@link Font})
     * @param itemStack the item stack whose tooltip should be drawn
     * @param x         the x position
     * @param y         the y position
     */
    @Override
    public void renderTooltip(Object font, ItemStack itemStack, int x, int y)
    {
        //screen.renderTooltip(graphics, screen.getTooltipFromItem(itemStack), itemStack.getTooltipImage(), x, y); // mc<=1.19.4
        graphics.renderTooltip((Font) font, itemStack, x, y); // mc>=1.20.1
    }

    /**
     * Renders an item stack and its standard decorations (count, durability bar, etc.).
     *
     * @param itemStack the item stack to render
     * @param font      the font used for decoration overlays (must be {@link Font})
     * @param x         the x position
     * @param y         the y position
     */
    @Override
    public void renderItem(ItemStack itemStack, Object font, int x, int y)
    {
        //ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer(); // mc<=1.19.4
        //itemRenderer.renderGuiItem(graphics, itemStack, x, y); // mc<=1.19.4
        graphics.renderItem(itemStack, x, y); // mc>=1.20.1
        graphics.renderItemDecorations((Font) font, itemStack, x, y); // mc>=1.20.1
    }

    /**
     * Renders an item stack and its standard decorations using a deterministic
     * seed for animations such as enchantment glints.
     *
     * @param itemStack the item stack to render
     * @param font      the font used for decoration overlays (must be {@link Font})
     * @param x         the x position
     * @param y         the y position
     * @param seed      the seed used for animated effects
     */
    @Override
    public void renderItem(ItemStack itemStack, Object font, int x, int y, int seed)
    {
        //ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer(); // mc<=1.19.4
        //itemRenderer.renderGuiItem(graphics, itemStack, x, y); // mc<=1.19.4
        graphics.renderItem(itemStack, x, y, seed); // mc>=1.20.1
        graphics.renderItemDecorations((Font) font, itemStack, x, y); // mc>=1.20.1
    }


    /**
     * Draws a sub-region of a texture using integer UV offsets, assuming the texture
     * dimensions equal the requested {@code width} and {@code height}.
     *
     * @param atlasLocation the texture resource location
     * @param x             the destination x coordinate
     * @param y             the destination y coordinate
     * @param uOffset       the u (horizontal) texture offset
     * @param vOffset       the v (vertical) texture offset
     * @param width         the width of the rendered region
     * @param height        the height of the rendered region
     */
    @Override
    public void blit(ResourceLocation atlasLocation, int x, int y, int uOffset, int vOffset, int width, int height)
    {
        //RenderSystem.setShaderTexture(0, atlasLocation);
        //Screen.blit(graphics, x, y, uOffset, vOffset, width, height, width, height); // mc<=1.19.4
        graphics.blit(atlasLocation, x, y, uOffset, vOffset, width, height);
    }

    /**
     * Draws a sub-region of a texture using floating point UV offsets, with explicit
     * total texture dimensions for proper UV normalization.
     *
     * @param atlasLocation the texture resource location
     * @param x             the destination x coordinate
     * @param y             the destination y coordinate
     * @param uOffset       the u (horizontal) texture offset
     * @param vOffset       the v (vertical) texture offset
     * @param width         the width of the rendered region
     * @param height        the height of the rendered region
     * @param textureWidth  the total texture width
     * @param textureHeight the total texture height
     */
    @Override
    public void blit(ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight)
    {
        //RenderSystem.setShaderTexture(0, atlasLocation);
        //Screen.blit(graphics, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight); // mc<=1.19.4
        graphics.blit(atlasLocation, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight); // mc>=1.20.1
    }

    /**
     * Draws a {@link TextureAtlasSprite} at the given location and size.
     *
     * @param x          the destination x coordinate
     * @param y          the destination y coordinate
     * @param blitOffset the z (depth) offset
     * @param width      the width of the rendered region
     * @param height     the height of the rendered region
     * @param sprite     the sprite to render
     */
    public void blit(int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite)
    {
        //Screen.blit(graphics, x, y, blitOffset, width, height, sprite); // mc<=1.19.4
        graphics.blit(x, y, blitOffset, width, height, sprite); // mc>=1.20.1
    }

    /**
     * Draws a tinted {@link TextureAtlasSprite} at the given location and size.
     *
     * @param x          the destination x coordinate
     * @param y          the destination y coordinate
     * @param blitOffset the z (depth) offset
     * @param width      the width of the rendered region
     * @param height     the height of the rendered region
     * @param sprite     the sprite to render
     * @param red        the red color modulation (0.0 - 1.0)
     * @param green      the green color modulation (0.0 - 1.0)
     * @param blue       the blue color modulation (0.0 - 1.0)
     * @param alpha      the alpha modulation (0.0 - 1.0)
     */
    public void blit(int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite, float red, float green, float blue, float alpha)
    {
        //Screen.blit(graphics, x, y, blitOffset, width, height, sprite, red, green, blue, alpha); // mc<=1.19.4
        graphics.blit(x, y, blitOffset, width, height, sprite, red, green, blue, alpha); // mc>=1.20.1
    }

    /**
     * Enables a scissor rectangle that constrains all subsequent drawing calls
     * to the specified screen-space bounds (in window-pixel coordinates).
     *
     * @param minX the left edge of the scissor area
     * @param minY the top edge of the scissor area
     * @param maxX the right edge (exclusive) of the scissor area
     * @param maxY the bottom edge (exclusive) of the scissor area
     */
    @Override
    public void enableScissor(int minX, int minY, int maxX, int maxY)
    {
        //Screen.enableScissor(minX, minY, maxX, maxY); // mc<=1.19.4
        graphics.enableScissor(minX, minY, maxX, maxY); // mc>=1.20.1
    }

    /**
     * Disables the currently active scissor rectangle, allowing drawing across
     * the full screen again.
     */
    @Override
    public void disableScissor()
    {
        //Screen.disableScissor(); // mc<=1.19.4
        graphics.disableScissor(); // mc>=1.20.1
    }


    /**
     * Translates the current pose by the given offsets.
     *
     * @param x the x translation
     * @param y the y translation
     * @param z the z translation
     */
    @Override
    public void translate(float x, float y, float z)
    {
        //graphics.translate(x, y, z); // mc<=1.19.4
        getPoseStack().translate(x, y, z); // mc>=1.20.1
    }

    /**
     * Translates the current pose by the given offsets.
     *
     * @param x the x translation
     * @param y the y translation
     * @param z the z translation
     */
    @Override
    public void translate(double x, double y, double z)
    {
        //graphics.translate(x, y, z); // mc<=1.19.4
        getPoseStack().translate(x, y, z); // mc>=1.20.1
    }

    /**
     * Scales the current pose by the given per-axis factors.
     *
     * @param x the x scale factor
     * @param y the y scale factor
     * @param z the z scale factor
     */
    @Override
    public void scale(float x, float y, float z)
    {
        //graphics.scale(x, y, z); // mc<=1.19.4
        getPoseStack().scale(x, y, z); // mc>=1.20.1
    }

    /**
     * Multiplies the current pose by the given quaternion rotation.
     *
     * @param quaternion the rotation to apply
     */
    @Override
    public void mulPose(Quaternionf quaternion)
    {
        //graphics.mulPose(quaternion); // mc<=1.19.4
        getPoseStack().mulPose(quaternion); // mc>=1.20.1
    }

    /**
     * Rotates the current pose around the given pivot point.
     *
     * @param quaternion the rotation to apply
     * @param x          the x coordinate of the pivot
     * @param y          the y coordinate of the pivot
     * @param z          the z coordinate of the pivot
     */
    @Override
    public void rotateAround(Quaternionf quaternion, float x, float y, float z)
    {
        //graphics.rotateAround(quaternion, x, y, z); // mc<=1.19.4
        getPoseStack().rotateAround(quaternion, x, y, z); // mc>=1.20.1
    }

    /**
     * Pushes a copy of the current pose onto the pose stack so subsequent
     * transformations can later be reverted with {@link #popPose()}.
     */
    @Override
    public void pushPose()
    {
        //graphics.pushPose(); // mc<=1.19.4
        getPoseStack().pushPose(); // mc>=1.20.1
    }

    /**
     * Pops the most recently pushed pose, reverting the pose stack to that state.
     */
    @Override
    public void popPose()
    {
        //graphics.popPose(); // mc<=1.19.4
        getPoseStack().popPose(); // mc>=1.20.1
    }







    // ── Font queries ──────────────────────────────────────────────────────

    @Override
    public int getFontLineHeight() {
        return font != null ? font.lineHeight : 9;
    }

    @Override
    public int getTextWidth(String text) {
        return font != null ? font.width(text) : 0;
    }

    // ── Line / vertex drawing ──────────────────────────────────────────────

    @Override
    public void drawLine(int x1, int y1, int x2, int y2, float thickness, int color) {
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

        Matrix4f matrix4f = getLastPoseMatrix();
        VertexConsumer vertexconsumer = bufferSource().getBuffer(RenderType.debugQuads());
        vertexconsumer.addVertex(matrix4f, p1x, p1y, 0).setColor(red, green, blue, alpha);
        vertexconsumer.addVertex(matrix4f, p2x, p2y, 0).setColor(red, green, blue, alpha);
        vertexconsumer.addVertex(matrix4f, p3x, p3y, 0).setColor(red, green, blue, alpha);
        vertexconsumer.addVertex(matrix4f, p4x, p4y, 0).setColor(red, green, blue, alpha);
        flush();
    }

    @Override
    public void drawVertexBuffer_QUADS(VertexBuffer buffer) {
        Matrix4f matrix4f = getLastPoseMatrix();
        VertexConsumer vertexconsumer = bufferSource().getBuffer(RenderType.debugQuads());
        for (Vertex vertex : buffer.getVertices()) {
            vertexconsumer.addVertex(matrix4f, vertex.x, vertex.y, 0)
                    .setColor(vertex.red, vertex.green, vertex.blue, vertex.alpha);
        }
        flush();
    }

    // ── Query ───────────────────────────────────────────────────────────────

    /**
     * @return the model-view matrix of the topmost pose currently on the pose stack
     */
    @Override
    public Matrix4f getLastPoseMatrix()
    {
        //return graphics.last().pose(); // mc<=1.19.4
        return getPoseStack().last().pose(); // mc>=1.20.1
    }

    /**
     * @return the {@link MultiBufferSource.BufferSource} associated with the
     *         active {@link GuiGraphics}, used for batched rendering
     */
    public MultiBufferSource.BufferSource bufferSource()
    {
        //return Minecraft.getInstance().renderBuffers().bufferSource(); // mc<=1.19.4
        return graphics.bufferSource(); // mc>=1.20.1
    }

    /**
     * Flushes the active buffer source so any queued vertices are drawn immediately.
     */
    @Override
    public void flush()
    {
        //Minecraft.getInstance().renderBuffers().bufferSource().endBatch(); // mc<=1.19.4
        graphics.flush(); // mc>=1.20.1
    }

}
