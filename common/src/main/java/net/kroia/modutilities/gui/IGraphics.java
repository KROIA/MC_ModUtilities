package net.kroia.modutilities.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Rendering abstraction for the GUI framework.
 * <p>
 * Every method has a {@code default} no-op implementation so that a server-side
 * {@link Gui} can hold an {@code IGraphics} reference without pulling in any
 * client-only classes. The client provides a full implementation via
 * {@link net.kroia.modutilities.gui.client.ClientGraphics ClientGraphics}.
 * <p>
 * <b>Font handling:</b> Minecraft's {@code Font} class is client-only. Methods
 * that require a font accept {@link Object} here; the client implementation
 * casts to {@code Font} internally. Callers on the client pass
 * {@code Minecraft.getInstance().font} as usual.
 *
 * @see net.kroia.modutilities.gui.client.ClientGraphics
 */
public interface IGraphics {

    // ── Text ────────────────────────────────────────────────────────────────

    /**
     * Draws a single line of text.
     *
     * @param font  the font (client: {@code net.minecraft.client.gui.Font})
     * @param text  the text to draw
     * @param x     x position in GUI pixels
     * @param y     y position in GUI pixels
     * @param color packed ARGB color
     */
    default void drawString(Object font, String text, int x, int y, int color) {}

    /**
     * Draws a single line of text with optional drop shadow.
     *
     * @param font       the font (client: {@code net.minecraft.client.gui.Font})
     * @param text       the text to draw
     * @param x          x position in GUI pixels
     * @param y          y position in GUI pixels
     * @param color      packed ARGB color
     * @param dropShadow if {@code true}, render a drop shadow behind the text
     */
    default void drawString(Object font, String text, int x, int y, int color, boolean dropShadow) {}

    // ── Fills ───────────────────────────────────────────────────────────────

    /**
     * Fills a rectangle with a solid color.
     *
     * @param x1    first x coordinate (inclusive)
     * @param y1    first y coordinate (inclusive)
     * @param x2    second x coordinate (exclusive)
     * @param y2    second y coordinate (exclusive)
     * @param color packed ARGB fill color
     */
    default void fill(int x1, int y1, int x2, int y2, int color) {}

    /**
     * Fills a rectangle with a vertical color gradient.
     *
     * @param x1        first x coordinate
     * @param y1        first y coordinate
     * @param x2        second x coordinate
     * @param y2        second y coordinate
     * @param colorFrom packed ARGB color at top
     * @param colorTo   packed ARGB color at bottom
     */
    default void fillGradient(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {}

    /**
     * Renders a 1px outline around the given rectangle.
     *
     * @param x1     x position
     * @param y1     y position
     * @param width  width
     * @param height height
     * @param color  packed ARGB outline color
     */
    default void renderOutline(int x1, int y1, int width, int height, int color) {}

    // ── Tooltips ────────────────────────────────────────────────────────────

    /**
     * Renders a single-line tooltip.
     *
     * @param font the font (client: {@code Font})
     * @param text the tooltip text component
     * @param x    x position
     * @param y    y position
     */
    default void renderTooltip(Object font, Component text, int x, int y) {}

    /**
     * Renders a multi-line tooltip.
     *
     * @param font  the font (client: {@code Font})
     * @param lines the tooltip lines
     * @param x     x position
     * @param y     y position
     */
    default void renderTooltip(Object font, List<Component> lines, int x, int y) {}

    /**
     * Renders an item tooltip.
     *
     * @param font      the font (client: {@code Font})
     * @param itemStack the item whose tooltip to draw
     * @param x         x position
     * @param y         y position
     */
    default void renderTooltip(Object font, ItemStack itemStack, int x, int y) {}

    // ── Items ───────────────────────────────────────────────────────────────

    /**
     * Renders an item stack and its standard decorations.
     *
     * @param itemStack the item to render
     * @param font      the font (client: {@code Font}) for decoration overlays
     * @param x         x position
     * @param y         y position
     */
    default void renderItem(ItemStack itemStack, Object font, int x, int y) {}

    /**
     * Renders an item stack and its standard decorations using a deterministic seed.
     *
     * @param itemStack the item to render
     * @param font      the font (client: {@code Font}) for decoration overlays
     * @param x         x position
     * @param y         y position
     * @param seed      seed for animated effects
     */
    default void renderItem(ItemStack itemStack, Object font, int x, int y, int seed) {}

    // ── Textures (ResourceLocation) ─────────────────────────────────────────

    /**
     * Draws a sub-region of a texture (integer UV offsets, texture dims = w x h).
     *
     * @param atlasLocation the texture resource location
     * @param x             destination x
     * @param y             destination y
     * @param uOffset       horizontal texture offset
     * @param vOffset       vertical texture offset
     * @param width         rendered width
     * @param height        rendered height
     */
    default void blit(ResourceLocation atlasLocation, int x, int y, int uOffset, int vOffset, int width, int height) {}

    /**
     * Draws a sub-region of a texture (float UV offsets, explicit texture dims).
     *
     * @param atlasLocation the texture resource location
     * @param x             destination x
     * @param y             destination y
     * @param uOffset       horizontal texture offset
     * @param vOffset       vertical texture offset
     * @param width         rendered width
     * @param height        rendered height
     * @param textureWidth  total texture width
     * @param textureHeight total texture height
     */
    default void blit(ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {}

    // ── Scissor ─────────────────────────────────────────────────────────────

    /**
     * Enables a scissor rectangle.
     *
     * @param minX left edge
     * @param minY top edge
     * @param maxX right edge (exclusive)
     * @param maxY bottom edge (exclusive)
     */
    default void enableScissor(int minX, int minY, int maxX, int maxY) {}

    /**
     * Disables the currently active scissor rectangle.
     */
    default void disableScissor() {}

    // ── Transforms ──────────────────────────────────────────────────────────

    /**
     * Translates the current pose (float).
     */
    default void translate(float x, float y, float z) {}

    /**
     * Translates the current pose (double).
     */
    default void translate(double x, double y, double z) {}

    /**
     * Scales the current pose.
     */
    default void scale(float x, float y, float z) {}

    /**
     * Multiplies the current pose by a quaternion rotation.
     */
    default void mulPose(Quaternionf quaternion) {}

    /**
     * Rotates the current pose around a pivot point.
     */
    default void rotateAround(Quaternionf quaternion, float x, float y, float z) {}

    /**
     * Pushes the current pose onto the stack.
     */
    default void pushPose() {}

    /**
     * Pops the most recently pushed pose.
     */
    default void popPose() {}

    // ── Font queries ──────────────────────────────────────────────────────

    /**
     * @return the line height of the current font, or {@code 9} (the default
     *         Minecraft line height) when no font is available
     */
    default int getFontLineHeight() { return 9; }

    /**
     * Returns the rendered width of the given text. If the text contains
     * newline characters, returns the maximum width across all lines.
     *
     * @param text the text to measure (may contain {@code \n})
     * @return the width in GUI pixels, or {@code 0} when no font is available
     */
    default int getTextWidth(String text) { return 0; }

    // ── Line / vertex drawing ──────────────────────────────────────────────

    /**
     * Draws a line segment between two points as a quad with the given
     * thickness. On the server this is a no-op.
     *
     * @param x1        the start x coordinate
     * @param y1        the start y coordinate
     * @param x2        the end x coordinate
     * @param y2        the end y coordinate
     * @param thickness the line thickness in GUI pixels
     * @param color     the packed ARGB color
     */
    default void drawLine(int x1, int y1, int x2, int y2, float thickness, int color) {}

    /**
     * Renders the contents of a {@link net.kroia.modutilities.gui.elements.base.VertexBuffer}
     * as quads. On the server this is a no-op.
     *
     * @param buffer the vertex buffer to render
     */
    default void drawVertexBuffer_QUADS(net.kroia.modutilities.gui.elements.base.VertexBuffer buffer) {}

    // ── Query ───────────────────────────────────────────────────────────────

    /**
     * @return the model-view matrix of the topmost pose, or {@code null} on
     *         the server
     */
    default Matrix4f getLastPoseMatrix() { return null; }

    /**
     * Flushes any queued vertices.
     */
    default void flush() {}
}
