package net.kroia.modutilities.gui.client;


import com.mojang.blaze3d.vertex.PoseStack;
import net.kroia.modutilities.gui.IGraphics;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.kroia.modutilities.gui.elements.base.Vertex;
import net.kroia.modutilities.gui.elements.base.VertexBuffer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics; // mc>=1.20.1 mc<=1.21.11
//import net.minecraft.client.gui.GuiGraphicsExtractor; // mc>=26.1
//import com.mojang.blaze3d.vertex.PoseStack; // mc<=1.19.4
import net.minecraft.client.renderer.MultiBufferSource; // mc<=26.1
import net.minecraft.client.renderer.RenderType; // mc<=1.21.10
//import net.minecraft.client.renderer.rendertype.RenderType; import net.minecraft.client.renderer.rendertype.RenderTypes; // mc>=1.21.11
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation; // mc<=1.21.10
//import net.minecraft.resources.Identifier; // mc>=1.21.11
import net.minecraft.util.ARGB; // mc>=1.21.2
import net.kroia.mixin.GuiGraphicsAccessor;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
//import net.minecraft.client.renderer.RenderPipelines; // mc>=1.21.6
//import net.minecraft.client.gui.navigation.ScreenRectangle; // mc>=1.21.6
//import org.joml.Matrix3x2f; // mc>=1.21.6

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
 * @apiNote This class is client-only.
 */
public class ClientGraphics implements IGraphics {

    GuiGraphics graphics;// mc>=1.20.1 mc<=1.21.11
    //GuiGraphicsExtractor graphics;// mc>=26.1
    //PoseStack graphics; // mc<1.19.4
    private Font font;
    private boolean enableShadow = true;
    // mc>=1.21.6: scissor rects in GUI space, mirrored from enableScissor/disableScissor so that custom
    // quad elements (drawLine, drawVertexBuffer_QUADS) are clipped like vanilla elements.
    //private final java.util.ArrayDeque<ScreenRectangle> scissorStack = new java.util.ArrayDeque<>(); // mc>=1.21.6

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
    public void setGraphics(GuiGraphics graphics) // mc<=1.21.11
    //public void setGraphics(GuiGraphicsExtractor graphics) // mc>=26.1
    {
        this.graphics = graphics;
        //scissorStack.clear(); // mc>=1.21.6
    }

    /**
     * @return the currently active {@link GuiGraphics}, or {@code null} if none is set
     */
    public GuiGraphics getGraphics() // mc<=1.21.11
    //public GuiGraphicsExtractor getGraphics() // mc>=26.1
    {
        return graphics;
    }

    /**
     * @return the {@link PoseStack} currently associated with the active
     *         {@link GuiGraphics}
     */
    public PoseStack getPoseStack() // mc<=1.21.5
    //public org.joml.Matrix3x2fStack getPoseStack() // mc>=1.21.6 (GUI pose is 2D)
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
        graphics.drawString((Font) font, text, x, y, opaqueIfNoAlpha(color), enableShadow); // mc>=1.20.1 mc<=1.21.11
        //graphics.text((Font) font, text, x, y, opaqueIfNoAlpha(color), enableShadow); // mc>=26.1
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
        graphics.drawString((Font) font, text, x, y, opaqueIfNoAlpha(color), dropShadow); // mc>=1.20.1 mc<=1.21.11
        //graphics.text((Font) font, text, x, y, opaqueIfNoAlpha(color), dropShadow); // mc>=26.1
    }

    /**
     * Pre-1.21.6 Font treated a text color without alpha bits (e.g. {@code 0xFFFFFF}) as opaque; since
     * 1.21.6 such text is not drawn at all. Applying the old rule here keeps behavior identical on all versions.
     */
    static int opaqueIfNoAlpha(int color)
    {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
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
        graphics.fillGradient(renderType, x1, y1, x2, y2, colorFrom, colorTo, z); // mc>=1.20.1 mc<=1.21.5
        //graphics.fillGradient(x1, y1, x2, y2, colorFrom, colorTo); // mc>=1.21.6 (GUI has no RenderType/z any more; both ignored)
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
        graphics.fill(x1, y1, x1 + width, y1 + 1, color); // mc>=1.20.1 (1.21.9 renamed renderOutline to the deferred submitOutline)
        graphics.fill(x1, y1 + height - 1, x1 + width, y1 + height, color); // mc>=1.20.1
        graphics.fill(x1, y1 + 1, x1 + 1, y1 + height - 1, color); // mc>=1.20.1
        graphics.fill(x1 + width - 1, y1 + 1, x1 + width, y1 + height - 1, color); // mc>=1.20.1
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
        graphics.renderTooltip((Font) font, text, x, y); // mc>=1.20.1 mc<=1.21.5
        //graphics.setTooltipForNextFrame((Font) font, text, x, y); // mc>=1.21.6 (drawn deferred, on top)
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
        graphics.renderTooltip((Font) font, lines, Optional.empty(), x, y); // mc>=1.20.1 mc<=1.21.5
        //graphics.setTooltipForNextFrame((Font) font, lines, Optional.empty(), x, y); // mc>=1.21.6
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
        graphics.renderTooltip((Font) font, itemStack, x, y); // mc>=1.20.1 mc<=1.21.5
        //graphics.setTooltipForNextFrame((Font) font, itemStack, x, y); // mc>=1.21.6
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
        graphics.renderItem(itemStack, x, y); // mc>=1.20.1 mc<=1.21.11
        //graphics.item(itemStack, x, y); // mc>=26.1
        graphics.renderItemDecorations((Font) font, itemStack, x, y); // mc>=1.20.1 mc<=1.21.11
        //graphics.itemDecorations((Font) font, itemStack, x, y); // mc>=26.1
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
        graphics.renderItem(itemStack, x, y, seed); // mc>=1.20.1 mc<=1.21.11
        //graphics.item(itemStack, x, y, seed); // mc>=26.1
        graphics.renderItemDecorations((Font) font, itemStack, x, y); // mc>=1.20.1 mc<=1.21.11
        //graphics.itemDecorations((Font) font, itemStack, x, y); // mc>=26.1
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
    public void blit(ResourceLocation atlasLocation, int x, int y, int uOffset, int vOffset, int width, int height) // mc<=1.21.10
    //public void blit(Identifier atlasLocation, int x, int y, int uOffset, int vOffset, int width, int height) // mc>=1.21.11
    {
        //RenderSystem.setShaderTexture(0, atlasLocation);
        //Screen.blit(graphics, x, y, uOffset, vOffset, width, height, width, height); // mc<=1.19.4
        //graphics.blit(atlasLocation, x, y, uOffset, vOffset, width, height); // mc<=1.21.1
        graphics.blit(RenderType::guiTextured, atlasLocation, x, y, uOffset, vOffset, width, height, 256, 256); // mc>=1.21.2 mc<=1.21.5
        //graphics.blit(RenderPipelines.GUI_TEXTURED, atlasLocation, x, y, uOffset, vOffset, width, height, 256, 256); // mc>=1.21.6
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
    public void blit(ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) // mc<=1.21.10
    //public void blit(Identifier atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) // mc>=1.21.11
    {
        //RenderSystem.setShaderTexture(0, atlasLocation);
        //Screen.blit(graphics, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight); // mc<=1.19.4
        //graphics.blit(atlasLocation, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight); // mc>=1.20.1 mc<=1.21.1
        graphics.blit(RenderType::guiTextured, atlasLocation, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight); // mc>=1.21.2 mc<=1.21.5
        //graphics.blit(RenderPipelines.GUI_TEXTURED, atlasLocation, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight); // mc>=1.21.6
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
        //graphics.blit(x, y, blitOffset, width, height, sprite); // mc>=1.20.1 mc<=1.21.1
        graphics.pose().pushPose(); // mc>=1.21.2 mc<=1.21.5
        graphics.pose().translate(0, 0, blitOffset); // mc>=1.21.2 mc<=1.21.5
        graphics.blitSprite(RenderType::guiTextured, sprite, x, y, width, height); // mc>=1.21.2 mc<=1.21.5
        graphics.pose().popPose(); // mc>=1.21.2 mc<=1.21.5
        //graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height); // mc>=1.21.6 (no z; blitOffset ignored, later calls draw on top)
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
        //graphics.blit(x, y, blitOffset, width, height, sprite, red, green, blue, alpha); // mc>=1.20.1 mc<=1.21.1
        graphics.pose().pushPose(); // mc>=1.21.2 mc<=1.21.5
        graphics.pose().translate(0, 0, blitOffset); // mc>=1.21.2 mc<=1.21.5
        graphics.blitSprite(RenderType::guiTextured, sprite, x, y, width, height, ARGB.colorFromFloat(alpha, red, green, blue)); // mc>=1.21.2 mc<=1.21.5
        graphics.pose().popPose(); // mc>=1.21.2 mc<=1.21.5
        //graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, ARGB.colorFromFloat(alpha, red, green, blue)); // mc>=1.21.6 (blitOffset ignored)
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
        // 1.21.4+: vanilla transforms the rect by the current pose, but callers pass absolute GUI coords
        graphics.pose().pushPose(); // mc>=1.21.4 mc<=1.21.5
        graphics.pose().setIdentity(); // mc>=1.21.4 mc<=1.21.5
        //graphics.pose().pushMatrix(); // mc>=1.21.6
        //graphics.pose().identity(); // mc>=1.21.6
        graphics.enableScissor(minX, minY, maxX, maxY); // mc>=1.20.1
        graphics.pose().popPose(); // mc>=1.21.4 mc<=1.21.5
        //graphics.pose().popMatrix(); // mc>=1.21.6
        //ScreenRectangle rect = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY); // mc>=1.21.6
        //ScreenRectangle parent = scissorStack.peekLast(); // mc>=1.21.6
        //if (parent != null) { // mc>=1.21.6
            //ScreenRectangle clipped = parent.intersection(rect); // mc>=1.21.6
            //rect = clipped != null ? clipped : ScreenRectangle.empty(); // mc>=1.21.6
        //} // mc>=1.21.6
        //scissorStack.addLast(rect); // mc>=1.21.6
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
        //scissorStack.pollLast(); // mc>=1.21.6
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
        getPoseStack().translate(x, y, z); // mc>=1.20.1 mc<=1.21.5
        //getPoseStack().translate(x, y); // mc>=1.21.6 (2D pose; z ignored, draw order decides what is on top)
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
        getPoseStack().translate(x, y, z); // mc>=1.20.1 mc<=1.21.5
        //getPoseStack().translate((float) x, (float) y); // mc>=1.21.6 (z ignored)
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
        getPoseStack().scale(x, y, z); // mc>=1.20.1 mc<=1.21.5
        //getPoseStack().scale(x, y); // mc>=1.21.6 (z ignored)
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
        getPoseStack().mulPose(quaternion); // mc>=1.20.1 mc<=1.21.5
        //getPoseStack().rotate(zAngle(quaternion)); // mc>=1.21.6 (2D: only the rotation about Z is kept)
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
        getPoseStack().rotateAround(quaternion, x, y, z); // mc>=1.20.1 mc<=1.21.5
        //getPoseStack().rotateAbout(zAngle(quaternion), x, y); // mc>=1.21.6 (2D: rotation about Z around (x, y))
    }

    /**
     * Pushes a copy of the current pose onto the pose stack so subsequent
     * transformations can later be reverted with {@link #popPose()}.
     */
    @Override
    public void pushPose()
    {
        //graphics.pushPose(); // mc<=1.19.4
        getPoseStack().pushPose(); // mc>=1.20.1 mc<=1.21.5
        //getPoseStack().pushMatrix(); // mc>=1.21.6
    }

    /**
     * Pops the most recently pushed pose, reverting the pose stack to that state.
     */
    @Override
    public void popPose()
    {
        //graphics.popPose(); // mc<=1.19.4
        getPoseStack().popPose(); // mc>=1.20.1 mc<=1.21.5
        //getPoseStack().popMatrix(); // mc>=1.21.6
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

        Matrix4f matrix4f = getLastPoseMatrix(); // mc<=1.21.5
        VertexConsumer vertexconsumer = bufferSource().getBuffer(RenderType.debugQuads()); // mc<=1.21.5
        vertexconsumer.addVertex(matrix4f, p1x, p1y, 0).setColor(red, green, blue, alpha); // mc<=1.21.5
        vertexconsumer.addVertex(matrix4f, p2x, p2y, 0).setColor(red, green, blue, alpha); // mc<=1.21.5
        vertexconsumer.addVertex(matrix4f, p3x, p3y, 0).setColor(red, green, blue, alpha); // mc<=1.21.5
        vertexconsumer.addVertex(matrix4f, p4x, p4y, 0).setColor(red, green, blue, alpha); // mc<=1.21.5
        flush(); // mc<=1.21.5
        //submitQuads(new float[]{p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y}, new int[]{color, color, color, color}); // mc>=1.21.6
    }

    @Override
    public void drawVertexBuffer_QUADS(VertexBuffer buffer) {
        Matrix4f matrix4f = getLastPoseMatrix(); // mc<=1.21.5
        VertexConsumer vertexconsumer = bufferSource().getBuffer(RenderType.debugQuads()); // mc<=1.21.5
        for (Vertex vertex : buffer.getVertices()) { // mc<=1.21.5
            vertexconsumer.addVertex(matrix4f, vertex.x, vertex.y, 0) // mc<=1.21.5
                    .setColor(vertex.red, vertex.green, vertex.blue, vertex.alpha); // mc<=1.21.5
        } // mc<=1.21.5
        flush(); // mc<=1.21.5
        //List<Vertex> vertices = buffer.getVertices(); // mc>=1.21.6
        //int n = vertices.size() - vertices.size() % 4; // mc>=1.21.6 (QUADS: whole quads only)
        //float[] xy = new float[n * 2]; // mc>=1.21.6
        //int[] colors = new int[n]; // mc>=1.21.6
        //for (int i = 0; i < n; i++) { // mc>=1.21.6
            //Vertex vertex = vertices.get(i); // mc>=1.21.6
            //xy[i * 2] = vertex.x; // mc>=1.21.6
            //xy[i * 2 + 1] = vertex.y; // mc>=1.21.6
            //colors[i] = ARGB.color(vertex.alpha, vertex.red, vertex.green, vertex.blue); // mc>=1.21.6
        //} // mc>=1.21.6
        //submitQuads(xy, colors); // mc>=1.21.6
    }

    // mc>=1.21.6: submits position-color quads to the deferred GUI render state (current pose + scissor).
    //private void submitQuads(float[] xy, int[] colors) { // mc>=1.21.6
        //if (colors.length == 0) return; // mc>=1.21.6
        //Matrix3x2f pose = new Matrix3x2f(getPoseStack()); // mc>=1.21.6
        //ScreenRectangle scissor = scissorStack.peekLast(); // mc>=1.21.6
        //ScreenRectangle bounds = GuiColoredQuadsRenderState.boundsOf(pose, xy); // mc>=1.21.6
        //if (bounds != null && scissor != null) bounds = scissor.intersection(bounds); // mc>=1.21.6
        //if (bounds == null) return; // mc>=1.21.6 (fully clipped)
        //((GuiGraphicsAccessor) graphics).modutilities$getGuiRenderState().submitGuiElement( // mc>=1.21.6 mc<=1.21.11
        //((GuiGraphicsAccessor) graphics).modutilities$getGuiRenderState().addGuiElement( // mc>=26.1
                //new GuiColoredQuadsRenderState(pose, xy, colors, scissor, bounds)); // mc>=1.21.6
    //} // mc>=1.21.6

    // mc>=1.21.6: rotation about the Z axis encoded in a quaternion (the only rotation a 2D GUI pose can apply).
    //private static float zAngle(Quaternionf q) { // mc>=1.21.6
        //return (float) Math.atan2(2.0 * (q.w * q.z + q.x * q.y), 1.0 - 2.0 * (q.y * q.y + q.z * q.z)); // mc>=1.21.6
    //} // mc>=1.21.6

    // ── Query ───────────────────────────────────────────────────────────────

    /**
     * @return the model-view matrix of the topmost pose currently on the pose stack
     */
    @Override
    public Matrix4f getLastPoseMatrix()
    {
        //return graphics.last().pose(); // mc<=1.19.4
        return getPoseStack().last().pose(); // mc>=1.20.1 mc<=1.21.5
        //Matrix3x2f m = getPoseStack(); // mc>=1.21.6 (2D pose embedded in a 4x4 matrix)
        //return new Matrix4f(m.m00(), m.m01(), 0, 0, m.m10(), m.m11(), 0, 0, 0, 0, 1, 0, m.m20(), m.m21(), 0, 1); // mc>=1.21.6
    }

    /**
     * @return the {@link MultiBufferSource.BufferSource} associated with the
     *         active {@link GuiGraphics}, used for batched rendering
     */
    // mc>=26.2: removed; Minecraft 26.2 has no buffer sources (GUI and world drawing are recorded and drawn later).
    public MultiBufferSource.BufferSource bufferSource() // mc<=26.1
    { // mc<=26.1
        //return Minecraft.getInstance().renderBuffers().bufferSource(); // mc<=1.19.4
        //return graphics.bufferSource(); // unused, superseded by GuiGraphicsAccessor on all versions (was mc>=1.20.1; public getter removed in 1.21.2)
        return ((GuiGraphicsAccessor) graphics).modutilities$getBufferSource(); // mc<=1.21.5
        // TODO(1.21.6 port): GUI rendering is deferred since 1.21.6 and GuiGraphics owns no buffer source.
        // Vertices written here are NOT part of the GUI pass; use drawVertexBuffer_QUADS/drawLine instead.
        //return net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource(); // mc>=1.21.6 mc<=26.1
    } // mc<=26.1

    /**
     * Flushes the active buffer source so any queued vertices are drawn immediately.
     */
    @Override
    public void flush()
    {
        //Minecraft.getInstance().renderBuffers().bufferSource().endBatch(); // mc<=1.19.4
        graphics.flush(); // mc>=1.20.1 mc<=1.21.5
        // mc>=1.21.6: nothing to flush, GUI elements are recorded and rendered after the screen
    }

}
