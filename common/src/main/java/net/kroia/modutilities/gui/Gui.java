package net.kroia.modutilities.gui;

import com.mojang.blaze3d.vertex.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.Vertex;
import net.kroia.modutilities.gui.elements.base.VertexBuffer;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Root logical container of the project's GUI framework.
 * <p>
 * A {@code Gui} is not a Minecraft {@link Screen} itself; instead it owns a
 * collection of top-level {@link GuiElement}s and forwards lifecycle, render,
 * and input events from a hosting screen ({@link GuiScreen} or
 * {@link GuiContainerScreen}) to those elements. It also tracks shared global
 * state such as mouse position, GUI scale, scissor rect, focused element and
 * z-ordering for the various render passes.
 *
 * @apiNote The whole {@code gui/} package is client-only
 *          ({@code @Environment(EnvType.CLIENT)}); this class must only be used
 *          on the client.
 */
@Environment(EnvType.CLIENT)
public class Gui {

    protected int backgroundZ = 0;
    protected int foregroundZ = 1;
    protected int tooltipZ = 200;
    protected int gizmoZ = 300;

    protected final Graphics graphics;
    protected Screen parent;
    protected int mousePosX, mousePosY;

    private float guiScale = 1.0f; // Scale of the GUI, default is 1.0 (no scaling)
    private float invGuiScale = 1.0f; // Inverse scale of the GUI, default is 1.0 (no scaling)

    protected float partialTick;
    private Rectangle globalScissorArea = null;

    protected GuiElement focusedElement = null;

    private final List<GuiElement> elements = new ArrayList<>();

    /**
     * Creates a new GUI root bound to the given hosting screen.
     *
     * @param parent the Minecraft screen that will dispatch lifecycle events to
     *               this {@code Gui}; typically a {@link GuiScreen} or
     *               {@link GuiContainerScreen}
     */
    public Gui(Screen parent)
    {
        this.parent = parent;
        this.graphics = new Graphics(parent);

    }

    /**
     * Initializes every top-level element. Called by the parent screen during
     * its {@code init()} pass.
     */
    public void init()
    {
        for(GuiElement element : elements)
        {
            element.init();
        }
    }

    /**
     * @return the GUI scale factor applied to this root (1.0 means no scaling)
     */
    public float getGuiScale() {
        return guiScale;
    }

    /**
     * @return the inverse of the GUI scale factor, useful for converting from
     *         window pixel space to GUI element space
     */
    public float getInvGuiScale() {
        return invGuiScale;
    }

    /**
     * Sets the GUI scale factor used when rendering and translating mouse
     * coordinates.
     * <p>
     * Values that are zero or negative are treated as invalid and silently
     * replaced with {@code 1.0}. The inverse scale is recomputed from the
     * effective (corrected) value to keep both factors consistent.
     *
     * @param guiScale the new scale factor; values {@code <= 0} are corrected
     *                 to {@code 1.0}
     */
    public void setGuiScale(float guiScale) {
        if(guiScale <= 0.0f) {
            guiScale = 1.0f;
        }
        this.guiScale = guiScale;
        this.invGuiScale = 1.0f / this.guiScale;
    }

    /**
     * @return the {@link Graphics} wrapper through which all drawing is performed
     */
    public Graphics getGraphics()
    {
        return this.graphics;
    }

    /**
     * @return the active {@link PoseStack} from the underlying graphics wrapper
     */
    public PoseStack getPoseStack()
    {
        return this.graphics.getPoseStack();
    }

    /**
     * @return the current mouse x position in GUI element coordinates (after
     *         applying the GUI scale)
     */
    public int getMousePosX()
    {
        return this.mousePosX;
    }

    /**
     * @return the current mouse y position in GUI element coordinates (after
     *         applying the GUI scale)
     */
    public int getMousePosY()
    {
        return this.mousePosY;
    }

    /**
     * @return the partial tick value for the current frame
     */
    public float getPartialTick()
    {
        return this.partialTick;
    }

    /**
     * @return the default Minecraft {@link Font} from the active client instance
     */
    public static Font getFont()
    {
        return Minecraft.getInstance().font;
    }

    /**
     * @return the active {@link Minecraft} client instance
     */
    public static Minecraft getMinecraft()
    {
        return Minecraft.getInstance();
    }

    /**
     * @return the z position used for the background render pass
     */
    public int getBackgroundRenderZPos()
    {
        return backgroundZ;
    }

    /**
     * @return the z position used for the foreground (main) render pass
     */
    public int getForegroundRenderZPos()
    {
        return foregroundZ;
    }

    /**
     * @return the z position used for the tooltip render pass
     */
    public int getTooltipRenderZPos()
    {
        return tooltipZ;
    }

    /**
     * @return the z position used for the gizmo (debug overlay) render pass
     */
    public int getGizmoRenderZPos()
    {
        return gizmoZ;
    }

    /**
     * Sets the z position used for the background render pass.
     *
     * @param z the new z position
     */
    public void setBackgroundRenderZPos(int z)
    {
        this.backgroundZ = z;
    }

    /**
     * Sets the z position used for the foreground render pass.
     *
     * @param z the new z position
     */
    public void setForegroundRenderZPos(int z)
    {
        this.foregroundZ = z;
    }

    /**
     * Sets the z position used for the tooltip render pass.
     *
     * @param z the new z position
     */
    public void setTooltipRenderZPos(int z)
    {
        this.tooltipZ = z;
    }

    /**
     * Sets the z position used for the gizmo render pass.
     *
     * @param z the new z position
     */
    public void setGizmoRenderZPos(int z)
    {
        this.gizmoZ = z;
    }


    /**
     * @return the Minecraft screen that hosts this {@code Gui}
     */
    public Screen getScreen()
    {
        return parent;
    }

    /**
     * Reports whether the parent screen has finished its initialization.
     * <p>
     * Mirrors the parent's {@code isInitialized()} flag for {@link GuiScreen}
     * and {@link GuiContainerScreen}; returns {@code false} for any other or
     * absent parent.
     *
     * @return {@code true} if the parent screen is initialized, {@code false}
     *         otherwise
     */
    public boolean isInitialized()
    {
        if(parent == null)
            return false;
        if(parent instanceof GuiScreen guiScreen)
            return guiScreen.isInitialized();
        if(parent instanceof GuiContainerScreen guiContainerScreen)
            return guiContainerScreen.isInitialized();
        return false;
    }

    /**
     * Registers a top-level element with this GUI and assigns this {@code Gui}
     * as its root.
     *
     * @param element the element to add
     */
    public void addElement(GuiElement element)
    {
        element.setRoot(this);
        elements.add(element);
    }

    /**
     * Removes a top-level element from this GUI and clears its root reference.
     *
     * @param element the element to remove
     */
    public void removeElement(GuiElement element)
    {
        element.setRoot(null);
        elements.remove(element);
    }

    /**
     * Removes every top-level element from this GUI and clears their root
     * references.
     */
    public void removeAllElements()
    {
        for(GuiElement element : elements)
        {
            element.setRoot(null);
        }
        elements.clear();
    }

    /**
     * @return the live list of top-level elements registered with this GUI
     */
    public List<GuiElement> getElements()
    {
        return elements;
    }

    /**
     * Updates which element holds keyboard focus. The previously focused element
     * receives a {@code focusLost} callback and the new one receives
     * {@code focusGained}; passing {@code null} clears focus.
     *
     * @param element the new focused element, or {@code null} to clear focus
     */
    public void setFocusedElement(GuiElement element)
    {
        if(element == this.focusedElement)
            return;
        if(this.focusedElement != null)
            this.focusedElement.focusLost();
        this.focusedElement = element;
        if(this.focusedElement != null)
            this.focusedElement.focusGained();
    }

    /**
     * @return the element that currently holds keyboard focus, or {@code null}
     *         if no element is focused
     */
    public GuiElement getFocusedElement()
    {
        return this.focusedElement;
    }

    /**
     * Stores the current mouse position, converting from window-pixel
     * coordinates to GUI element coordinates using the inverse GUI scale.
     *
     * @param x the mouse x position in window pixels
     * @param y the mouse y position in window pixels
     */
    public void storeMousePos(int x, int y)
    {
        this.mousePosX = (int)((float)x*invGuiScale);
        this.mousePosY = (int)((float)y*invGuiScale);
    }

    /**
     * Stores the current mouse position, converting from window-pixel
     * coordinates to GUI element coordinates using the inverse GUI scale.
     *
     * @param x the mouse x position in window pixels
     * @param y the mouse y position in window pixels
     */
    public void storeMousePos(double x, double y)
    {
        this.mousePosX = (int)(x*(double)invGuiScale);
        this.mousePosY = (int)(y*(double)invGuiScale);
    }

    /**
     * Moves the OS-level cursor to the given GUI-coordinate position, factoring
     * in both Minecraft's GUI scale and this {@code Gui}'s additional scale.
     *
     * @param x the desired x position in GUI element coordinates
     * @param y the desired y position in GUI element coordinates
     */
    public void moveMouseToPos(int x, int y)
    {
        double guiScaleFactor = getMinecraftGuiScale();
        double newX = x * guiScaleFactor * guiScale;
        double newY = y * guiScaleFactor * guiScale;
        long windowHandle = getWindowHandle();
        GLFW.glfwSetCursorPos(windowHandle, newX, newY);
    }

    /**
     * Sets the partial tick value for the current frame so that elements can
     * interpolate animations.
     *
     * @param partialTick the partial tick value
     */
    public void setPartialTick(float partialTick)
    {
        this.partialTick = partialTick;
    }
    /**
     * Runs the background render pass for every top-level element, applying the
     * configured background z position and GUI scale.
     */
    public void renderBackground()
    {
        pushPose();
        translate(0, 0, backgroundZ); // Ensure background is rendered below everything else
        scale(guiScale, guiScale, 1.0f);
        for(GuiElement element : elements)
        {
            element.renderBackgroundInternal();
        }
        popPose();
    }

    /**
     * Runs the foreground render pass for every top-level element.
     */
    public void render()
    {
        pushPose();
        translate(0, 0, foregroundZ); // Ensure foreground is rendered above background
        scale(guiScale, guiScale, 1.0f);
        for(GuiElement element : elements)
        {
            element.renderInternal();
        }
        popPose();
    }

    /**
     * Runs the tooltip render pass for every top-level element.
     */
    public void renderTooltip()
    {
        pushPose();
        translate(0, 0, tooltipZ); // Ensure tooltip is rendered on top
        scale(guiScale, guiScale, 1.0f);
        for(GuiElement element : elements)
        {
            element.renderTooltipInternal();
        }
        popPose();
    }

    /**
     * Runs the gizmo (debug overlay) render pass for every top-level element.
     */
    public void renderGizmos()
    {
        pushPose();
        translate(0, 0, gizmoZ); // Ensure tooltip is rendered on top
        scale(guiScale, guiScale, 1.0f);
        for(GuiElement element : elements)
        {
            element.renderGizmosInternal();
        }
        popPose();
    }

    /**
     * Dispatches a mouse-click event to the top-level elements, updating the
     * stored mouse position before delivery.
     *
     * @param mouseX the mouse x position in window pixels
     * @param mouseY the mouse y position in window pixels
     * @param button the GLFW mouse button code
     * @return {@code true} if any element consumed the event
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        storeMousePos(mouseX, mouseY);
        for(GuiElement element : elements)
        {
            if(element.mouseClickedInternal(button, true))
                return true;
        }
        return false;
    }

    /**
     * Dispatches a mouse-drag event to the top-level elements.
     *
     * @param mouseX the current mouse x position in window pixels
     * @param mouseY the current mouse y position in window pixels
     * @param button the GLFW mouse button code held during the drag
     * @param deltaX the change in x since the previous event
     * @param deltaY the change in y since the previous event
     * @return {@code true} if any element consumed the event
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY)
    {
        storeMousePos(mouseX, mouseY);
        for(GuiElement element : elements)
        {
            if(element.mouseDraggedInternal(button, deltaX, deltaY))
                return true;
        }
        return false;
    }

    /**
     * Dispatches a mouse-release event to the top-level elements.
     *
     * @param mouseX the mouse x position in window pixels
     * @param mouseY the mouse y position in window pixels
     * @param button the GLFW mouse button code that was released
     * @return {@code true} if any element consumed the event
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        storeMousePos(mouseX, mouseY);
        for(GuiElement element : elements)
        {
            if(element.mouseReleasedInternal(button,true))
                return true;
        }
        return false;
    }

    /**
     * Dispatches a mouse-scroll event to the top-level elements.
     *
     * @param mouseX the mouse x position in window pixels
     * @param mouseY the mouse y position in window pixels
     * @param delta  the scroll delta
     * @return {@code true} if any element consumed the event
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        storeMousePos(mouseX, mouseY);
        for(GuiElement element : elements)
        {
            if(element.mouseScrolledInternal(delta,true))
                return true;
        }
        return false;
    }

    /**
     * Dispatches a key-press event to the top-level elements.
     *
     * @param keyCode   the GLFW key code
     * @param scanCode  the platform-specific scan code
     * @param modifiers a bitmask of active modifier keys
     * @return {@code true} if any element consumed the event
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        for(GuiElement element : elements)
        {
            if(element.keyPressedInternal(keyCode, scanCode, modifiers))
                return true;
        }
        return false;
    }

    /**
     * Dispatches a character-typed event to the top-level elements.
     *
     * @param codePoint the typed character
     * @param modifiers a bitmask of active modifier keys
     * @return {@code true} if any element consumed the event
     */
    public boolean charTyped(char codePoint, int modifiers)
    {
        for(GuiElement element : elements)
        {
            if(element.charTypedInternal(codePoint, modifiers))
                return true;
        }
        return false;
    }



    // Drawing primitives

    /**
     * Draws multi-line text using {@code "\n"} as the line separator.
     *
     * @param text  the text to draw (may contain newline characters)
     * @param x     the x position
     * @param y     the y position of the first line
     * @param color the packed ARGB text color
     */
    public void drawText(String text, int x, int y, int color)
    {
        // Split text by new line
        String[] lines = text.split("\n");
        for(int i = 0; i < lines.length; i++)
        {
            graphics.drawString(getFont(), lines[i], x, y + i*getFont().lineHeight, color);
        }
    }

    /**
     * Draws multi-line text with optional drop shadow.
     *
     * @param text       the text to draw (may contain newline characters)
     * @param x          the x position
     * @param y          the y position of the first line
     * @param color      the packed ARGB text color
     * @param dropShadow if {@code true}, a drop shadow is rendered behind each line
     */
    public void drawText(String text, int x, int y, int color, boolean dropShadow)
    {
        // Split text by new line
        String[] lines = text.split("\n");
        for(int i = 0; i < lines.length; i++)
        {
            graphics.drawString(getFont(), lines[i], x, y + i*getFont().lineHeight, color, dropShadow);
        }
    }
    /**
     * Draws a multi-line {@link Component} using {@code "\n"} as the line
     * separator. The component is converted to its plain string form for layout.
     *
     * @param text  the component to draw
     * @param x     the x position
     * @param y     the y position of the first line
     * @param color the packed ARGB text color
     */
    public void drawText(Component text, int x, int y, int color)
    {
        // Split text by new line
        String[] lines = text.getString().split("\n");
        for(int i = 0; i < lines.length; i++)
        {
            graphics.drawString(getFont(), lines[i], x, y + i*getFont().lineHeight, color);
        }
    }

    /**
     * Draws a multi-line {@link Component} with optional drop shadow.
     *
     * @param text       the component to draw
     * @param x          the x position
     * @param y          the y position of the first line
     * @param color      the packed ARGB text color
     * @param dropShadow if {@code true}, a drop shadow is rendered behind each line
     */
    public void drawText(Component text, int x, int y, int color, boolean dropShadow)
    {
        // Split text by new line
        String[] lines = text.getString().split("\n");
        for(int i = 0; i < lines.length; i++)
        {
            graphics.drawString(getFont(), lines[i], x, y + i*getFont().lineHeight, color, dropShadow);
        }
    }
    /**
     * Draws multi-line text scaled by the given font scale.
     *
     * @param text      the text to draw
     * @param x         the x position
     * @param y         the y position
     * @param color     the packed ARGB text color
     * @param fontScale the additional scale applied to the font size
     */
    public void drawText(String text, int x, int y, int color, float fontScale)
    {
        pushPose();
        translate(x, y, 0.f);
        scale(fontScale, fontScale, 1.f);
        // Split text by new line
        String[] lines = text.split("\n");
        Font font = getFont();
        int lineHeight = font.lineHeight;
        for(int i = 0; i < lines.length; i++)
        {
            graphics.drawString(font, lines[i], 0, i*lineHeight, color);
        }
        popPose();
    }
    /**
     * Draws multi-line text scaled by the given font scale, with optional drop
     * shadow.
     *
     * @param text       the text to draw
     * @param x          the x position
     * @param y          the y position
     * @param color      the packed ARGB text color
     * @param dropShadow if {@code true}, a drop shadow is rendered behind each line
     * @param fontScale  the additional scale applied to the font size
     */
    public void drawText(String text, int x, int y, int color, boolean dropShadow, float fontScale)
    {
        pushPose();
        translate(x, y, 0.f);
        scale(fontScale, fontScale, 1.f);
        // Split text by new line
        String[] lines = text.split("\n");
        Font font = getFont();
        int lineHeight = font.lineHeight;
        for(int i = 0; i < lines.length; i++)
        {
            graphics.drawString(font, lines[i], 0, i*lineHeight, color, dropShadow);
        }
        popPose();
    }

    /**
     * Draws a multi-line {@link Component} scaled by the given font scale.
     *
     * @param text      the component to draw
     * @param x         the x position
     * @param y         the y position
     * @param color     the packed ARGB text color
     * @param fontScale the additional scale applied to the font size
     */
    public void drawText(Component text, int x, int y, int color, float fontScale)
    {
        pushPose();
        translate(x, y, 0.f);
        scale(fontScale, fontScale, 1.f);
        // Split text by new line
        String[] lines = text.getString().split("\n");
        Font font = getFont();
        int lineHeight = font.lineHeight;
        for(int i = 0; i < lines.length; i++)
        {
            graphics.drawString(font, lines[i], 0, i*lineHeight, color);
        }
        popPose();
    }
    /**
     * Draws a multi-line {@link Component} scaled by the given font scale, with
     * optional drop shadow.
     *
     * @param text       the component to draw
     * @param x          the x position
     * @param y          the y position
     * @param color      the packed ARGB text color
     * @param dropShadow if {@code true}, a drop shadow is rendered behind each line
     * @param fontScale  the additional scale applied to the font size
     */
    public void drawText(Component text, int x, int y, int color, boolean dropShadow, float fontScale)
    {
        pushPose();
        translate(x, y, 0.f);
        scale(fontScale, fontScale, 1.f);
        // Split text by new line
        String[] lines = text.getString().split("\n");
        Font font = getFont();
        int lineHeight = font.lineHeight;
        for(int i = 0; i < lines.length; i++)
        {
            graphics.drawString(font, lines[i], 0, i*lineHeight, color, dropShadow);
        }
        popPose();
    }


    /**
     * Draws a single line segment of a given thickness between two points.
     *
     * @param x1        the start x coordinate
     * @param y1        the start y coordinate
     * @param x2        the end x coordinate
     * @param y2        the end y coordinate
     * @param thickness the line thickness in GUI pixels; lines shorter than
     *                  ~1e-4 units are skipped
     * @param color     the packed ARGB color
     */
    public void drawLine(int x1, int y1, int x2, int y2, float thickness,  int color)
    {
        class PointF
        {
            public float x;
            public float y;
            public PointF(float x, float y)
            {
                this.x = x;
                this.y = y;
            }
        }

        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        PointF direction = new PointF(x2-x1, y2-y1);
        float length = (float)Math.sqrt(direction.x*direction.x + direction.y*direction.y);
        if(length < 0.0001F)
            return;
        PointF unitDirection = new PointF(direction.x/length, direction.y/length);
        PointF normal = new PointF(-unitDirection.y, unitDirection.x);
        PointF offset = new PointF(thickness/2*normal.x, thickness/2*normal.y);

        PointF p1 = new PointF(x1+offset.x, y1+offset.y);
        PointF p2 = new PointF(x2+offset.x, y2+offset.y);
        PointF p3 = new PointF(x2-offset.x, y2-offset.y);
        PointF p4 = new PointF(x1-offset.x, y1-offset.y);


        Matrix4f matrix4f = graphics.getLastPoseMatrix();
        VertexConsumer vertexconsumer = graphics.bufferSource().getBuffer(RenderType.debugQuads());
        vertexconsumer.addVertex(matrix4f, (float)p1.x, (float)p1.y, (float)0).setColor(red, green, blue, alpha);
        vertexconsumer.addVertex(matrix4f, (float)p2.x, (float)p2.y, (float)0).setColor(red, green, blue, alpha);
        vertexconsumer.addVertex(matrix4f, (float)p3.x, (float)p3.y, (float)0).setColor(red, green, blue, alpha);
        vertexconsumer.addVertex(matrix4f, (float)p4.x, (float)p4.y, (float)0).setColor(red, green, blue, alpha);
        graphics.flush();
    }
    /**
     * Renders the contents of a {@link VertexBuffer} as quads in a single batch.
     *
     * @param buffer the vertex buffer to render
     */
    public void drawVertexBuffer_QUADS(VertexBuffer buffer) {
        Matrix4f matrix4f = graphics.getLastPoseMatrix();
        RenderType renderType = RenderType.debugQuads();
        VertexConsumer vertexconsumer = graphics.bufferSource().getBuffer(renderType);
        for(Vertex vertex : buffer.getVertices())
        {
            vertexconsumer.addVertex(matrix4f, vertex.x, vertex.y, 0).setColor(vertex.red, vertex.green, vertex.blue, vertex.alpha);
        }
        graphics.flush();
    }
    /**
     * Fills a rectangle of the given size with a solid color.
     *
     * @param x      the x position
     * @param y      the y position
     * @param width  the rectangle width
     * @param height the rectangle height
     * @param color  the packed ARGB fill color
     */
    public void drawRect(int x,int y, int width, int height, int color)
    {
        graphics.fill(x,y,width+x,height+y,color);
    }

    /**
     * Fills a rectangle with a vertical color gradient.
     *
     * @param x         the x position
     * @param y         the y position
     * @param width     the rectangle width
     * @param height    the rectangle height
     * @param colorFrom the packed ARGB color at the top
     * @param colorTo   the packed ARGB color at the bottom
     */
    public void drawGradient(int x, int y, int width, int height, int colorFrom, int colorTo)
    {
        graphics.fillGradient(x,y,width+x,height+y,colorFrom,colorTo);
    }

    /**
     * Fills a rectangle with a vertical color gradient using a custom render type
     * and z offset.
     *
     * @param renderType the render type to use
     * @param x          the x position
     * @param y          the y position
     * @param z          the z offset
     * @param width      the rectangle width
     * @param height     the rectangle height
     * @param colorFrom  the packed ARGB color at the top
     * @param colorTo    the packed ARGB color at the bottom
     */
    public void drawGradient(RenderType renderType, int x, int y, int z, int width, int height, int colorFrom, int colorTo)
    {
        graphics.fillGradient(renderType, x,y,width+x,height+y,colorFrom,colorTo, z);
    }

    /**
     * Renders a 1px outline around the given rectangle.
     *
     * @param x      the x position
     * @param y      the y position
     * @param width  the rectangle width
     * @param height the rectangle height
     * @param color  the packed ARGB outline color
     */
    public void drawOutline(int x, int y, int width, int height, int color)
    {
        graphics.renderOutline(x,y,width,height,color);
    }

    /**
     * Draws a tooltip at the given location, transparently pausing the active
     * scissor rectangle (if any) so the tooltip is not clipped.
     *
     * @param tooltip the tooltip text component
     * @param x       the x position
     * @param y       the y position
     */
    public void drawTooltip(Component tooltip, int x, int y)
    {
        if(isScissorEnabled())
        {
            scissorPause();
            graphics.renderTooltip(getFont(), tooltip, x,y);
            scissorResume();
            return;
        }
        graphics.renderTooltip(getFont(), tooltip, x,y);
    }
    /**
     * Draws a multi-line string tooltip, treating {@code "\n"} as the line
     * separator. The active scissor rectangle is paused while drawing.
     *
     * @param tooltip the tooltip text (may contain newlines)
     * @param x       the x position
     * @param y       the y position
     */
    public void drawTooltip(String tooltip, int x, int y)
    {
        List<Component> tooltipList = new ArrayList<>();
        // split by newline
        String[] lines = tooltip.split("\n");
        for(String line : lines)
        {
            tooltipList.add(Component.nullToEmpty(line));
        }
        if(isScissorEnabled())
        {
            scissorPause();
            graphics.renderTooltip(getFont(), tooltipList, x,y);
            scissorResume();
            return;
        }
        graphics.renderTooltip(getFont(), tooltipList, x,y);
    }

    /**
     * Draws an item tooltip at the given location. The active scissor rectangle
     * is paused while drawing so the tooltip is not clipped.
     *
     * @param stack the item stack whose tooltip should be drawn
     * @param x     the x position
     * @param y     the y position
     */
    public void drawTooltip(ItemStack stack, int x, int y)
    {
        if(isScissorEnabled())
        {
            scissorPause();
            graphics.renderTooltip(getFont(), stack, x,y);
            scissorResume();
            return;
        }
        graphics.renderTooltip(getFont(), stack, x,y);
    }

    /**
     * Renders an item stack and its standard decorations using the given seed
     * for animated effects.
     *
     * @param item the item stack to render
     * @param x    the x position
     * @param y    the y position
     * @param seed the seed used for animated effects
     */
    public void drawItem(ItemStack item, int x, int y, int seed)
    {
        graphics.renderItem(item, getFont(), x, y, seed);
    }

    /**
     * Renders an item stack along with its decorations and an explicit count
     * label drawn above the standard decorations when the stack contains more
     * than one item.
     *
     * @param item the item stack to render
     * @param x    the x position
     * @param y    the y position
     * @param seed the seed used for animated effects
     */
    public void drawItemWithDecoration(ItemStack item, int x, int y, int seed)
    {
        graphics.renderItem(item, getFont(), x, y, seed);
        int count = item.getCount();
        if(count > 1)
        {
            // Render item count
            String s = String.valueOf(count);
            pushPose();
            graphics.translate(0.0D, 0.0D, (double)(200));
            drawText(s, x + 19 - 2 - getFont().width(s), y + 6 + 3, 16777215);
            popPose();
        }
    }
    /**
     * Renders an item stack with decorations at an explicit z position.
     *
     * @param item the item stack to render
     * @param x    the x position
     * @param y    the y position
     * @param z    the z (depth) offset
     * @param seed the seed used for animated effects
     */
    public void drawItemWithDecoration(ItemStack item, int x, int y, int z, int seed)
    {
        pushPose();
        graphics.translate(0.0D, 0.0D, (double)(z));
        graphics.renderItem(item, getFont(), x, y, seed);
        int count = item.getCount();
        if(count > 1)
        {
            // Render item count
            String s = String.valueOf(count);
            pushPose();
            graphics.translate(0.0D, 0.0D, (double)(200));
            drawText(s, x + 19 - 2 - getFont().width(s), y + 6 + 3, 16777215);
            popPose();
        }
        popPose();
    }

    /**
     * Draws a sub-region of a texture, assuming the texture dimensions equal the
     * requested {@code width} and {@code height}.
     *
     * @param texture the texture resource location
     * @param x       the destination x coordinate
     * @param y       the destination y coordinate
     * @param uOffset the u (horizontal) texture offset
     * @param vOffset the v (vertical) texture offset
     * @param width   the rendered width
     * @param height  the rendered height
     */
    public void drawTexture(ResourceLocation texture, int x, int y,  int uOffset, int vOffset, int width, int height)
    {
        graphics.blit(texture, x, y, uOffset, vOffset, width, height);
    }

    /**
     * Draws a sub-region of a texture using floating-point UV offsets and
     * explicit total texture dimensions.
     *
     * @param atlasLocation the texture resource location
     * @param x             the destination x coordinate
     * @param y             the destination y coordinate
     * @param uOffset       the u (horizontal) texture offset
     * @param vOffset       the v (vertical) texture offset
     * @param width         the rendered width
     * @param height        the rendered height
     * @param textureWidth  the total texture width
     * @param textureHeight the total texture height
     */
    public void drawTexture(ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight)
    {
        graphics.blit(atlasLocation, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    /**
     * Draws a {@link TextureAtlasSprite} at the given location and size.
     *
     * @param sprite     the sprite to render
     * @param x          the x position
     * @param y          the y position
     * @param width      the rendered width
     * @param height     the rendered height
     * @param blitOffset the z (depth) offset
     */
    public void drawTexture(TextureAtlasSprite sprite, int x, int y, int width, int height, int blitOffset)
    {
        graphics.blit(x, y, blitOffset, width, height, sprite);
    }

    /**
     * Draws a tinted {@link TextureAtlasSprite} at the given location and size.
     *
     * @param sprite     the sprite to render
     * @param x          the x position
     * @param y          the y position
     * @param width      the rendered width
     * @param height     the rendered height
     * @param blitOffset the z (depth) offset
     * @param red        the red color modulation (0.0 - 1.0)
     * @param green      the green color modulation (0.0 - 1.0)
     * @param blue       the blue color modulation (0.0 - 1.0)
     * @param alpha      the alpha modulation (0.0 - 1.0)
     */
    public void drawTexture(TextureAtlasSprite sprite, int x, int y, int width, int height, int blitOffset, float red, float green, float blue, float alpha)
    {
        graphics.blit(x, y, blitOffset, width, height, sprite, red, green, blue, alpha);
    }

    /**
     * Convenience wrapper around {@link ResourceLocation#fromNamespaceAndPath(String, String)}
     * for use within mod code.
     *
     * @param modID the mod namespace
     * @param path  the resource path under that namespace
     * @return a resource location for the given namespace and path
     */
    public static ResourceLocation createResourceLocation(String modID, String path)
    {
        return ResourceLocation.fromNamespaceAndPath(modID, path);
    }

    /**
     * @return the Minecraft window's reported GUI scale factor (1, 2, 3, ...)
     */
    public static double getMinecraftGuiScale()
    {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    /**
     * @return the GLFW window handle used by the active Minecraft client
     */
    public long getWindowHandle()
    {
        return Minecraft.getInstance().getWindow().getWindow();
    }

    /**
     * Enables a scissor rectangle in GUI coordinates. The provided rectangle is
     * scaled into window coordinates internally.
     *
     * @param rect the scissor area in GUI coordinates
     */
    public void enableScissor(Rectangle rect)
    {
        globalScissorArea = rect;
        int x1 = (int)((float)rect.x * guiScale);
        int y1 = (int)((float)rect.y * guiScale);
        int x2 = (int)(((float)rect.x+(float)rect.width)*guiScale);
        int y2 = (int)(((float)rect.y+(float)rect.height)*guiScale);

        graphics.enableScissor(x1,y1,x2,y2);
    }

    /**
     * Disables the active scissor rectangle, if any.
     */
    public void disableScissor()
    {
        globalScissorArea = null;
        graphics.disableScissor();
    }

    /**
     * @return {@code true} if a scissor rectangle is currently active
     */
    public boolean isScissorEnabled()
    {
        return globalScissorArea != null;
    }

    /**
     * @return the active scissor area in GUI coordinates, or {@code null} if no
     *         scissor is currently enabled
     */
    public Rectangle getScissorArea()
    {
        return globalScissorArea;
    }

    /**
     * Temporarily disables scissor without forgetting the current scissor area;
     * pair with {@link #scissorResume()} to restore it.
     */
    public void scissorPause()
    {
        graphics.disableScissor();
    }

    /**
     * Re-enables the scissor area saved by a previous {@link #scissorPause()}
     * call. Has no effect if no scissor area is currently set.
     */
    public void scissorResume()
    {
        if(globalScissorArea != null)
        {
            enableScissor(globalScissorArea);
        }
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
        graphics.translate(x, y, z);
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
        graphics.translate(x, y, z);
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
        graphics.scale(x, y, z);
    }

    /**
     * Multiplies the current pose by the given rotation.
     *
     * @param quaternion the rotation to apply
     */
    public void mulPose(Quaternionf quaternion)
    {
        graphics.mulPose(quaternion);
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
        graphics.rotateAround(quaternion, x, y, z);
    }

    /**
     * Pushes a copy of the current pose onto the pose stack.
     */
    public void pushPose()
    {
        graphics.pushPose();
    }

    /**
     * Pops the most recently pushed pose, reverting to the previous state.
     */
    public void popPose()
    {
        graphics.popPose();
    }

    /**
     * Plays a sound at the local player's position. Has no effect if no level or
     * player is currently available (e.g. on the main menu).
     *
     * @param sound  the sound event to play
     * @param volume the volume multiplier
     * @param pitch  the pitch multiplier
     */
    public static void playLocalSound(SoundEvent sound, float volume, float pitch)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        minecraft.level.playLocalSound(
                minecraft.player.getX(),
                minecraft.player.getY(),
                minecraft.player.getZ(),
                sound,
                SoundSource.PLAYERS,
                volume,
                pitch,
                false
        );
    }

}
