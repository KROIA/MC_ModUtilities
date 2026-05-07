package net.kroia.modutilities.gui;


import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
 * Thin abstraction wrapping Minecraft's {@link GuiGraphics} that exposes the
 * drawing primitives used by the project's GUI framework.
 * <p>
 * The owning {@link Screen} replaces the underlying {@link GuiGraphics} each
 * frame via {@link #setGraphics(GuiGraphics)} before any drawing calls are made.
 * Methods on this class delegate to that backing instance and translate between
 * the framework's API and the Minecraft client API.
 *
 * @apiNote The whole {@code gui/} package is client-only
 *          ({@code @Environment(EnvType.CLIENT)}). Calling these methods on a
 *          dedicated server will result in a {@link ClassNotFoundException}.
 */
@Environment(EnvType.CLIENT)
public class Graphics {

    GuiGraphics graphics;// mc>=1.20.1
    //PoseStack graphics; // mc<1.19.4

    /**
     * Creates a new graphics wrapper.
     * The backing {@link GuiGraphics} must be supplied via
     * {@link #setGraphics(GuiGraphics)} before any drawing call.
     */
    public Graphics()
    {
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
     * @param font  the font to render with
     * @param text  the text to draw
     * @param x     the x position in GUI pixels
     * @param y     the y position in GUI pixels
     * @param color the packed ARGB color to use for the text
     */
    public void drawString(Font font, String text, int x, int y, int color)
    {
        //font.draw(graphics, text, x, y, color);  // mc<=1.19.4
        graphics.drawString(font, text, x, y, color); // mc>=1.20.1
    }

    /**
     * Draws a single line of text with optional drop shadow.
     *
     * @param font       the font to render with
     * @param text       the text to draw
     * @param x          the x position in GUI pixels
     * @param y          the y position in GUI pixels
     * @param color      the packed ARGB color to use for the text
     * @param dropShadow if {@code true}, a drop shadow is rendered behind the text
     */
    public void drawString(Font font, String text, int x, int y, int color, boolean dropShadow)
    {
        //font.draw(graphics, text, x, y, color);  // mc<=1.19.4
        graphics.drawString(font, text, x, y, color, dropShadow); // mc>=1.20.1
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
    public void renderOutline(int x1, int y1, int width, int height, int color)
    {
        //Screen.renderOutline(graphics, x1, y1, width, height, color); // mc<=1.19.4
        graphics.renderOutline(x1, y1, width, height, color); // mc>=1.20.1
    }

    /**
     * Renders a single-line tooltip at the given coordinates.
     *
     * @param font the font to use
     * @param text the tooltip text component
     * @param x    the x position
     * @param y    the y position
     */
    public void renderTooltip(Font font, Component text, int x, int y)
    {
        //screen.renderTooltip(graphics, text, x, y); // mc<=1.19.4
        graphics.renderTooltip(font, text, x, y); // mc>=1.20.1
    }

    /**
     * Renders a multi-line tooltip at the given coordinates.
     *
     * @param font  the font to use
     * @param lines the tooltip lines, in order
     * @param x     the x position
     * @param y     the y position
     */
    public void renderTooltip(Font font, List<Component> lines, int x, int y)
    {
        //screen.renderTooltip(graphics, lines, Optional.empty(), x, y); // mc<=1.19.4
        graphics.renderTooltip(font, lines, Optional.empty(), x, y); // mc>=1.20.1
    }

    /**
     * Renders an item tooltip at the given coordinates, including the item's
     * standard tooltip lines.
     *
     * @param font      the font to use
     * @param itemStack the item stack whose tooltip should be drawn
     * @param x         the x position
     * @param y         the y position
     */
    public void renderTooltip(Font font, ItemStack itemStack, int x, int y)
    {
        //screen.renderTooltip(graphics, screen.getTooltipFromItem(itemStack), itemStack.getTooltipImage(), x, y); // mc<=1.19.4
        graphics.renderTooltip(font, itemStack, x, y); // mc>=1.20.1
    }

    /**
     * Renders an item stack and its standard decorations (count, durability bar, etc.).
     *
     * @param itemStack the item stack to render
     * @param font      the font used for decoration overlays
     * @param x         the x position
     * @param y         the y position
     */
    public void renderItem(ItemStack itemStack, Font font, int x, int y)
    {
        //ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer(); // mc<=1.19.4
        //itemRenderer.renderGuiItem(graphics, itemStack, x, y); // mc<=1.19.4
        graphics.renderItem(itemStack, x, y); // mc>=1.20.1
        graphics.renderItemDecorations(font, itemStack, x, y); // mc>=1.20.1
    }

    /**
     * Renders an item stack and its standard decorations using a deterministic
     * seed for animations such as enchantment glints.
     *
     * @param itemStack the item stack to render
     * @param font      the font used for decoration overlays
     * @param x         the x position
     * @param y         the y position
     * @param seed      the seed used for animated effects
     */
    public void renderItem(ItemStack itemStack, Font font, int x, int y, int seed)
    {
        //ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer(); // mc<=1.19.4
        //itemRenderer.renderGuiItem(graphics, itemStack, x, y); // mc<=1.19.4
        graphics.renderItem(itemStack, x, y, seed); // mc>=1.20.1
        graphics.renderItemDecorations(font, itemStack, x, y); // mc>=1.20.1
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
    public void enableScissor(int minX, int minY, int maxX, int maxY)
    {
        //Screen.enableScissor(minX, minY, maxX, maxY); // mc<=1.19.4
        graphics.enableScissor(minX, minY, maxX, maxY); // mc>=1.20.1
    }

    /**
     * Disables the currently active scissor rectangle, allowing drawing across
     * the full screen again.
     */
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
    public void rotateAround(Quaternionf quaternion, float x, float y, float z)
    {
        //graphics.rotateAround(quaternion, x, y, z); // mc<=1.19.4
        getPoseStack().rotateAround(quaternion, x, y, z); // mc>=1.20.1
    }

    /**
     * Pushes a copy of the current pose onto the pose stack so subsequent
     * transformations can later be reverted with {@link #popPose()}.
     */
    public void pushPose()
    {
        //graphics.pushPose(); // mc<=1.19.4
        getPoseStack().pushPose(); // mc>=1.20.1
    }

    /**
     * Pops the most recently pushed pose, reverting the pose stack to that state.
     */
    public void popPose()
    {
        //graphics.popPose(); // mc<=1.19.4
        getPoseStack().popPose(); // mc>=1.20.1
    }







    /**
     * @return the model-view matrix of the topmost pose currently on the pose stack
     */
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
    public void flush()
    {
        //Minecraft.getInstance().renderBuffers().bufferSource().endBatch(); // mc<=1.19.4
        graphics.flush(); // mc>=1.20.1
    }

}
