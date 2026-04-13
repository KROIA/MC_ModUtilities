package net.kroia.modutilities.gui.elements.base;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.ClientPlayerUtilities;
import net.kroia.modutilities.TimerMillis;
import net.kroia.modutilities.gui.Graphics;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiTexture;
import net.kroia.modutilities.gui.geometry.Point;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.kroia.modutilities.gui.layout.Layout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public abstract class GuiElement {

    public enum Alignment
    {
        CENTER,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public static final class HoverTooltipData
    {
        public Supplier<String> textSupplier = null;
        public boolean enableBackground = true;
        public int textColor = DEFAULT_TEXT_COLOR;
        public int backgroundColor = DEFAULT_TOOLTIP_BACKGROUND_COLOR;
        public int backgroundPadding = DEFAULT_TOOLTIP_BACKGROUND_PADDING;
        public float fontScale = 1.0f;
        public Alignment mousePositionAlignment = Alignment.TOP_LEFT;
    }



    public static int DEFAULT_PADDING = 1;
    public static int DEFAULT_TOOLTIP_BACKGROUND_PADDING = 2;
    public static int DEFAULT_TOOLTIP_BACKGROUND_COLOR = 0xDD555555;
    public static int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;
    public static int DEFAULT_BACKGROUND_COLOR = 0xAA888888;
    public static int DEFAULT_FOCUSED_BACKGROUND_COLOR = 0xAA666666;
    public static int DEFAULT_HOVER_BACKGROUND_COLOR = 0xFFAAAAAA;
    public static int DEFAULT_OUTLINE_COLOR = 0xFF333333;
    public static int DEFAULT_HOVER_TOOLTIP_MOUSE_OFFSET = 5;

    private Gui root;
    private GuiElement parent = null;
    private GuiElement rootParent = null;
    private final Rectangle bounds;
    private float zPos = 0.0f;
    private final Point globalPositon = new Point(0,0);
    private final List<GuiElement> childs = new ArrayList<>();

    private boolean isEnabled = true;
    private boolean checkOverlapForRendering = true; // If true, the element will only be rendered if it is visible (overlaps with the parent)
    private int gizmoColor = 0x55FF0000;
    private int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private int outlineColor = DEFAULT_OUTLINE_COLOR;
    protected boolean enableBackground = true;
    protected boolean enableOutline = true;
    protected int outlineThickness = 1;

    protected Layout layout = null;
    private boolean isRelayoutingThis = false;

    private final TimerMillis tooltipTimer = new TimerMillis(false);
    private boolean tooltipCreateBackground = true;
    private int tooltipBackgroundColor = DEFAULT_TOOLTIP_BACKGROUND_COLOR;
    private int tooltipBackgroundPadding = DEFAULT_TOOLTIP_BACKGROUND_PADDING;
    private Alignment tooltipPositionAlingment = Alignment.TOP_LEFT;
    private int tooltipDelay = 700; // milliseconds
    private boolean tooltipTimerStarted = false;
    private int textColor = DEFAULT_TEXT_COLOR;
    private int tooltipTextColor = DEFAULT_TEXT_COLOR;
    private float textFontScale = 1.0f;
    private float tooltipFontScale = 1.0f;
    private int tooltipHoverMouseOffset = DEFAULT_HOVER_TOOLTIP_MOUSE_OFFSET;

    HoverTooltipData hoverTooltipData = new HoverTooltipData();
    public class TooltipData
    {
        public int x,y;
        public ItemStack item;
        public Component component;
        public String customString;
        public boolean createBackground = tooltipCreateBackground;
        public int backgroundColor = tooltipBackgroundColor;
        public int textColor = tooltipTextColor;
        public int backgroundPadding = tooltipBackgroundPadding;
        public Alignment alignment = tooltipPositionAlingment;
        public float fontScale = tooltipFontScale;
    }
    private final List<TooltipData> drawTooltipLater = new ArrayList<>();



    public GuiElement() {
        this(0, 0, 0, 0);
    }
    public GuiElement(int x, int y, int width, int height) {
        bounds = new Rectangle(x,y,width, height);
        rootParent = this;
    }

    public void setRoot(Gui root) {
        this.root = root;
        for (GuiElement child : childs) {
            child.setRoot(root);
        }
    }
    public void init()
    {
        for (GuiElement child : childs) {
            child.init();
        }
        layoutChangedInternal();
    }

    public void setLayout(Layout layout)
    {
        this.layout = layout;
    }
    public Layout getLayout()
    {
        return layout;
    }

    public Gui getRoot() {
        return root;
    }
    public GuiElement getParent() {
        return parent;
    }
    public GuiElement getRootParent() {
        return rootParent;
    }

    public Minecraft getMinecraft() {
        return root.getMinecraft();
    }
    public void setEnabled(boolean visible)
    {
        isEnabled = visible;
        if(!isEnabled)
        {
            if(root != null && root.getFocusedElement() == this)
                root.setFocusedElement(null);
        }
    }
    public boolean isEnabled()
    {
        return isEnabled;
    }
    public void setCheckOverlapForRendering(boolean checkOverlapForRendering)
    {
        this.checkOverlapForRendering = checkOverlapForRendering;
    }
    public boolean isCheckOverlapForRendering() {
        return checkOverlapForRendering;
    }
    public boolean isVisible()
    {
        if(!isEnabled)
            return false;
        if(parent != null && checkOverlapForRendering)
        {
            Rectangle rect1 = new Rectangle(parent.globalPositon.x,parent.globalPositon.y, parent.getWidth(), parent.getHeight());
            Rectangle rect2 = new Rectangle(globalPositon.x,globalPositon.y, bounds.width, bounds.height);
            if(rect1.intersects(rect2))
                return true;
            return false;
        }
        return true;
    }
    public void setFocused()
    {
        root.setFocusedElement(this);
    }
    public void removeFocus()
    {
        if(root.getFocusedElement() == this)
            root.setFocusedElement(null);
    }
    public boolean isFocused()
    {
        return root.getFocusedElement() == this;
    }
    public void setGizmoColor(int color)
    {
        gizmoColor = color;
    }
    public int getGizmoColor()
    {
        return gizmoColor;
    }
    public void setBackgroundColor(int color)
    {
        backgroundColor = color;
    }
    public int getBackgroundColor()
    {
        return backgroundColor;
    }
    public void setOutlineColor(int outlineColor) {
        this.outlineColor = outlineColor;
    }
    public int getOutlineColor() {
        return outlineColor;
    }
    public int setOutlineThickness(int outlineThickness) {
        return this.outlineThickness = outlineThickness;
    }
    public int getOutlineThickness() {
        return outlineThickness;
    }
    public void setEnableBackground(boolean enableBackground) {
        this.enableBackground = enableBackground;
    }
    public boolean isBackgroundEnabled() {
        return enableBackground;
    }
    public void setEnableOutline(boolean enableOutline) {
        this.enableOutline = enableOutline;
    }
    public boolean isOutlineEnabled() {
        return enableOutline;
    }



    // Hover Tooltip
    public void setHoverTooltipSupplier(Supplier<String> hoverTooltipSupplier)
    {
        hoverTooltipData.textSupplier = hoverTooltipSupplier;
    }
    public Supplier<String> getHoverTooltipSupplier()
    {
        return hoverTooltipData.textSupplier;
    }
    public void setHoverTooltipBackgroundColor(int tooltipBackgroundColor) {
        hoverTooltipData.backgroundColor = tooltipBackgroundColor;
    }
    public int getHoverTooltipBackgroundColor() {
        return hoverTooltipData.backgroundColor;
    }
    public void setHoverTooltipEnableBackground(boolean enableTooltipBackground) {
        hoverTooltipData.enableBackground = enableTooltipBackground;
    }
    public boolean isHoverTooltipBackgroundEnabled() {
        return hoverTooltipData.enableBackground;
    }
    public void setHoverTooltipTextColor(int tooltipTextColor) {
        hoverTooltipData.textColor = tooltipTextColor;
    }
    public int getHoverTooltipTextColor() {
        return hoverTooltipData.textColor;
    }
    public void setHoverTooltipBackgroundPadding(int tooltipBackgroundPadding) {
        hoverTooltipData.backgroundPadding = tooltipBackgroundPadding;
    }
    public int getHoverTooltipBackgroundPadding() {
        return hoverTooltipData.backgroundPadding;
    }
    public void setHoverTooltipFontScale(float tooltipFontScale) {
        hoverTooltipData.fontScale = tooltipFontScale;
    }
    public float getHoverTooltipFontScale() {
        return hoverTooltipData.fontScale;
    }
    public void setHoverTooltipMouseOffset(int offset) {
        tooltipHoverMouseOffset = offset;
    }
    public int getHoverTooltipMouseOffset() {
        return tooltipHoverMouseOffset;
    }
    public void setHoverTooltipMousePositionAlignment(Alignment alignment) {
        hoverTooltipData.mousePositionAlignment = alignment;
    }
    public Alignment getHoverTooltipMousePositionAlignment() {
        return hoverTooltipData.mousePositionAlignment;
    }
    public void setHoverTooltipData(HoverTooltipData data)
    {
        if(data == null)
            return;
        hoverTooltipData = data;
    }
    public HoverTooltipData getHoverTooltipData()
    {
        return hoverTooltipData;
    }





    public void setTooltipBackgroundColor(int tooltipBackgroundColor) {
        this.tooltipBackgroundColor = tooltipBackgroundColor;
    }
    public int getTooltipBackgroundColor() {
        return tooltipBackgroundColor;
    }
    public void setTooltipEnableBackground(boolean enableTooltipBackground) {
        this.tooltipCreateBackground = enableTooltipBackground;
    }
    public boolean isTooltipBackgroundEnabled() {
        return tooltipCreateBackground;
    }
    public void setTooltipBackgroundPadding(int toolripBackgroundPadding) {
        this.tooltipBackgroundPadding = toolripBackgroundPadding;
    }
    public int getTooltipBackgroundPadding() {
        return tooltipBackgroundPadding;
    }
    public void setTooltipPositionAlignment(Alignment alignment) {
        this.tooltipPositionAlingment = alignment;
    }
    public Alignment getTooltipPositionAlignment() {
        return tooltipPositionAlingment;
    }
    public void setHoverTooltipDelay(int tooltipDelay) {
        this.tooltipDelay = tooltipDelay;
    }
    public int getHoverTooltipDelay() {
        return tooltipDelay;
    }

    public void setTooltipFontScale(float tooltipFontScale) {
        this.tooltipFontScale = tooltipFontScale;
    }
    public float getTooltipFontScale() {
        return tooltipFontScale;
    }

    public void setTooltipTextColor(int tooltipTextColor) {
        this.tooltipTextColor = tooltipTextColor;
    }
    public int getTooltipTextColor() {
        return tooltipTextColor;
    }


    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }
    public int getTextColor() {
        return textColor;
    }
    public void setTextFontScale(float textFontScale) {
        this.textFontScale = textFontScale;
    }
    public float getTextFontScale() {
        return textFontScale;
    }

    protected void renderBackground()
    {
        if(enableBackground)
            renderBackgroundColor();
        if(enableOutline)
            renderOutline();
    }
    protected abstract void render();
    protected void renderGizmos()
    {
        // Draw debug infos to help debug the layout and so on
        drawOutline(0,0,bounds.width, bounds.height, gizmoColor);
    }
    protected void renderBackgroundColor()
    {
        drawRect(0,0,getWidth(), getHeight(),backgroundColor);
    }
    protected void renderOutline()
    {
        drawFrame(0,0,getWidth(),getHeight(), outlineColor,outlineThickness);
    }

    public void renderBackgroundInternal()
    {
        if(!isVisible())
            return;
        Graphics graphics = root.getGraphics();
        graphics.pushPose();
        graphics.translate((float)getX(), (float)getY(), zPos);
        renderBackground();
        for (GuiElement child : childs) {
            child.renderBackgroundInternal();
        }
        graphics.popPose();
    }
    public void renderInternal()
    {
        if(!isVisible())
            return;
        Graphics graphics = root.getGraphics();
        graphics.pushPose();
        graphics.translate((float)getX(), (float)getY(), zPos);
        render();
        for (GuiElement child : childs) {
            child.renderInternal();
        }
        graphics.popPose();

    }
    public void renderTooltipInternal()
    {
        if(!isVisible())
            return;
        if(hoverTooltipData.textSupplier != null && isMouseOver())
        {
            if(!tooltipTimerStarted) {
                tooltipTimer.start(tooltipDelay);
                tooltipTimerStarted = true;
            }
            else if(tooltipTimer.isFinished())
            {
                String tooltip = hoverTooltipData.textSupplier.get();
                if(tooltip != null && !tooltip.isEmpty())
                {
                    Point pos = getMousePos();
                    TooltipData data = new TooltipData();
                    data.x = pos.x;
                    data.y = pos.y;
                    data.customString = tooltip;
                    data.createBackground = hoverTooltipData.enableBackground;
                    data.backgroundColor = hoverTooltipData.backgroundColor;
                    data.backgroundPadding = hoverTooltipData.backgroundPadding;
                    data.alignment = hoverTooltipData.mousePositionAlignment;
                    data.textColor = hoverTooltipData.textColor;
                    data.fontScale = hoverTooltipData.fontScale;
                    drawTooltip(data);
                }
            }
        }
        else {
            tooltipTimer.stop();
            tooltipTimerStarted = false;
        }

        Graphics graphics = root.getGraphics();
        graphics.pushPose();
        graphics.translate((float)getX(), (float)getY(), 200.0F + zPos);
        for(TooltipData data : drawTooltipLater)
        {
            if(data.item != null)
                drawTooltipInternal(data.item, data.x, data.y);
            else if(data.component != null)
                drawText(data.component, data.x, data.y, data.textColor, data.fontScale);
            else if(data.customString != null) {


                Point size = getTextBounds(data.customString, data.fontScale);
                int x = 0;
                int y = 0;
                switch(data.alignment)
                {
                    case CENTER:
                    case LEFT:
                        x = tooltipHoverMouseOffset;
                        break;
                    case RIGHT:
                        x = -tooltipHoverMouseOffset;
                        break;
                    case TOP:
                        y = tooltipHoverMouseOffset;
                        break;
                    case BOTTOM:
                        y = -tooltipHoverMouseOffset;
                        break;
                    case TOP_LEFT:
                        x = tooltipHoverMouseOffset;
                        y = tooltipHoverMouseOffset;
                        break;
                    case TOP_RIGHT:
                        x = -tooltipHoverMouseOffset;
                        y = tooltipHoverMouseOffset;
                        break;
                    case BOTTOM_LEFT:
                        x = tooltipHoverMouseOffset;
                        y = -tooltipHoverMouseOffset;
                        break;
                    case BOTTOM_RIGHT:
                        x = -tooltipHoverMouseOffset;
                        y = -tooltipHoverMouseOffset;
                        break;
                }

                Rectangle rect = getAlignedBounds(0,0, size.x, size.y, data.alignment,
                        data.x+x, data.y+y,0,0);
                if(data.createBackground)
                {
                    drawRect(rect.x - data.backgroundPadding,
                            rect.y - data.backgroundPadding,
                            size.x + data.backgroundPadding * 2,
                            size.y + data.backgroundPadding * 2, data.backgroundColor);
                }
                drawText(data.customString, rect.x, rect.y, data.textColor, data.fontScale);
            }
        }
        drawTooltipLater.clear();
        for (GuiElement child : childs) {
            child.renderTooltipInternal();
        }
        graphics.popPose();

    }
    public void renderGizmosInternal()
    {
        if(!isVisible())
            return;
        Graphics graphics = root.getGraphics();
        graphics.pushPose();
        graphics.translate((float)getX(), (float)getY(), zPos);
        renderGizmos();
        for (GuiElement child : childs) {
            child.renderGizmosInternal();
        }
        graphics.popPose();
    }

    protected void enableGlobalScissor(Rectangle area)
    {
        root.enableScissor(area);
    }
    protected void enableScissor(int x, int y, int width, int height)
    {
        enableGlobalScissor(new Rectangle(globalPositon.x+x, globalPositon.y+y, width, height));
    }
    protected void enableScissor(Rectangle area)
    {
        enableGlobalScissor(new Rectangle(globalPositon.x+area.x, globalPositon.y+area.y, area.width, area.height));
    }
    protected void enableScissor()
    {
        enableGlobalScissor(new Rectangle(globalPositon.x, globalPositon.y, bounds.width, bounds.height));
    }

    protected void disableScissor()
    {
        root.disableScissor();
    }
    protected void scissorPause()
    {
        root.scissorPause();
    }
    protected void scissorResume()
    {
        root.scissorResume();
    }

    protected abstract void layoutChanged();
    public void layoutChangedInternal()
    {
        if(isRelayoutingThis)
            return;
        if(getGui() == null)
            return;
        if(layout != null) {
            if(layout.enabled)
                layout.apply(this);
        }
        isRelayoutingThis = true;
        layoutChanged();
        isRelayoutingThis = false;

        for (GuiElement child : childs) {
            child.layoutChangedInternal();
        }
        if(rootParent == this)
        {
            updateTransform(0,0);
        }
        else {
            if(parent != null)
                updateTransform(parent.globalPositon.x, parent.globalPositon.y);
        }
    }
    private void updateTransform(int parentX, int parentY)
    {
        globalPositon.x = parentX + bounds.x;
        globalPositon.y = parentY + bounds.y;
        for(GuiElement child : childs)
        {
            child.updateTransform(globalPositon.x, globalPositon.y);
        }
    }

    public void focusGained()
    {

    }
    public void focusLost()
    {

    }
    public boolean isOver(int globalPosX, int globalPosY) {
        if(parent != null && !parent.isOver(globalPosX, globalPosY))
            return false;
        return isOverIgoreParents(globalPosX, globalPosY);
    }
    public boolean isOverIgoreParents(int globalPosX, int globalPosY) {
        return (globalPosX - globalPositon.x) >= 0 && (globalPosX - globalPositon.x) < bounds.width &&
                (globalPosY - globalPositon.y) >= 0 && (globalPosY - globalPositon.y) < bounds.height;
    }

    public boolean isMouseOver() {
        return isOver(root.getMousePosX(), root.getMousePosY());
    }
    public boolean isMouseOverIgnoreParents() {
        return isOverIgoreParents(root.getMousePosX(), root.getMousePosY());
    }

    /**
     * Called when the mouse is clicked
     * @param button The mouse button that was clicked
     */
    protected void mouseClicked(int button) {
    }

    /**
     * Called when the mouse is clicked over the element
     * @param button The mouse button that was clicked
     * @see GLFW.GLFW_MOUSE_BUTTON_LEFT
     * @return true if the click was consumed, false otherwise
     */
    protected boolean mouseClickedOverElement(int button) {
        return false;
    }

    /**
     * Called when the mouse is dragged
     * @see GLFW.GLFW_MOUSE_BUTTON_LEFT
     * @param button The mouse button that was dragged
     * @param deltaX The change in the x position of the mouse
     * @param deltaY The change in the y position of the mouse
     * @return true if the drag was consumed, false otherwise
     */
    protected boolean mouseDragged(int button, double deltaX, double deltaY) {
        return false;
    }

    /**
     * Called when the mouse is released
     * @see GLFW.GLFW_MOUSE_BUTTON_LEFT
     * @param button The mouse button that was released
     */
    protected void mouseReleased(int button) {
    }

    /**
     * Called when the mouse is released over the element
     * @see GLFW.GLFW_MOUSE_BUTTON_LEFT
     * @param button The mouse button that was released
     * @return true if the release was consumed, false otherwise
     */
    protected boolean mouseReleasedOverElement(int button) {
        return false;
    }

    /**
     * Called when the mouse is scrolled
     * @param delta The amount of scrolling, positive for scrolling up, negative for scrolling down
     */
    protected void mouseScrolled(double delta) {
    }

    /**
     * Called when the mouse is scrolled over the element
     * @param delta The amount of scrolling, positive for scrolling up, negative for scrolling down
     * @return true if the scroll was consumed, false otherwise
     */
    protected boolean mouseScrolledOverElement(double delta) {
        return false;
    }

    /**
     * Called when a key is pressed
     * @param keyCode The key code of the pressed key
     *                @see GLFW.GLFW_MOUSE_BUTTON_LEFT
     * @param scanCode The scan code of the pressed key
     * @param modifiers The modifiers that were pressed (e.g. shift, ctrl, alt)
     * @return true if the key press was consumed, false otherwise
     */
    protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Called when a character is typed
     * @param codePoint The character that was typed
     * @param modifiers The modifiers that were pressed (e.g. shift, ctrl, alt)
     * @return true if the character was consumed, false otherwise
     */
    protected boolean charTyped(char codePoint, int modifiers) {
        return false;
    }


    /**
     * Checks if the the keyboard key is pressed down.
     * @see GLFW.GLFW_KEY_SPACE... Keys
     *
     * @return true if the given key is pressed
     */
    protected boolean isKeyPressed(int keyCode)
    {
        return GLFW.glfwGetKey(getRoot().getWindowHandle(), keyCode) == GLFW.GLFW_PRESS;
    }
    protected boolean isControlPressed()
    {
        return GLFW.glfwGetKey(getRoot().getWindowHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
    }
    protected boolean isShiftPressed()
    {
        return GLFW.glfwGetKey(getRoot().getWindowHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
    }
    protected boolean isAltPressed()
    {
        return GLFW.glfwGetKey(getRoot().getWindowHandle(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
    }

    public boolean mouseClickedInternal(int button, boolean isOverParent)
    {
        if(!isEnabled)
            return false;
        isOverParent &= isMouseOver();
        boolean consumed = false;
        for(GuiElement child : childs)
        {
            if(child.mouseClickedInternal(button, isOverParent && !consumed)) {
                consumed = true;
            }
        }
        mouseClicked(button);
        if (isOverParent && !consumed)
            return mouseClickedOverElement(button);
        return consumed;
    }
    public boolean mouseReleasedInternal(int button, boolean isOverParent)
    {
        if(!isEnabled)
            return false;
        isOverParent &= isMouseOver();
        boolean consumed = false;
        for(GuiElement child : childs)
        {
            if(child.mouseReleasedInternal(button, isOverParent && !consumed))
                consumed = true;
        }

        mouseReleased(button);
        if (isOverParent && !consumed)
            return mouseReleasedOverElement(button);

        return consumed;
    }
    public boolean mouseDraggedInternal(int button, double deltaX, double deltaY)
    {
        if(!isEnabled)
            return false;
        for(GuiElement child : childs)
        {
            if(child.mouseDraggedInternal(button, deltaX, deltaY))
                return true;
        }
        return mouseDragged(button, deltaX, deltaY);
    }
    public boolean mouseScrolledInternal(double delta, boolean isOverParent)
    {
        if(!isEnabled)
            return false;
        isOverParent &= isMouseOver();
        boolean consumed = false;
        for(GuiElement child : childs)
        {
            if(child.mouseScrolledInternal(delta, isOverParent && !consumed))
                consumed = true;
        }
        mouseScrolled(delta);
        if (isOverParent && !consumed)
            return mouseScrolledOverElement(delta);
        return consumed;
    }
    public boolean keyPressedInternal(int keyCode, int scanCode, int modifiers)
    {
        if(!isEnabled)
            return false;
        for(GuiElement child : childs)
        {
            if(child.keyPressedInternal(keyCode, scanCode, modifiers))
                return true;
        }
        return keyPressed(keyCode, scanCode, modifiers);
    }
    public boolean charTypedInternal(char codePoint, int modifiers)
    {
        if(!isEnabled)
            return false;
        for(GuiElement child : childs)
        {
            if(child.isEnabled() && child.charTypedInternal(codePoint, modifiers))
                return true;
        }
        return charTyped(codePoint, modifiers);
    }



    public Graphics getGraphics() {
        return root.getGraphics();
    }
    public PoseStack getPoseStack()
    {
        return root.getPoseStack();
    }
    public void graphicsPushPose() {
        root.pushPose();
    }
    public void graphicsPopPose() {
        root.popPose();
    }
    public void graphicsTranslate(float x, float y, float z) {
        root.translate(x, y, z);
    }
    public void graphicsTranslate(float x, float y) {
        root.translate(x, y, 0);
    }
    public void graphicsScale(float x, float y, float z)
    {
        root.scale(x, y, z);
    }
    public void graphicsScale(float x, float y)
    {
        root.scale(x, y, 1.0f);
    }
    public void graphicMulPose(Quaternionf quaternion)
    {
        root.mulPose(quaternion);
    }
    public void graphicsRotateAround(Quaternionf quaternion, float x, float y, float z)
    {
        root.rotateAround(quaternion, x, y, z);
    }
    public Gui getGui()
    {
        return root;
    }
    public Screen getScreen()
    {
        if(root == null)
            return null;
        return root.getScreen();
    }
    public Font getFont()
    {
        return Gui.getFont();
    }
    public int getMouseX() {
        return root.getMousePosX()-globalPositon.x;
    }
    public int getMouseY() {
        return root.getMousePosY()-globalPositon.y;
    }
    public Point getMousePos()
    {
        return new Point(getMouseX(), getMouseY());
    }
    public int getMouseXGlobal() {
        return root.getMousePosX();
    }
    public int getMouseYGlobal() {
        return root.getMousePosY();
    }
    public Point getMousePosGlobal()
    {
        return new Point(getMouseXGlobal(), getMouseYGlobal());
    }

    public void setMousePos(int x, int y)
    {
        root.moveMouseToPos(x + globalPositon.x, globalPositon.y + y);
    }
    public void setMousePosGlobal(int x, int y)
    {
        root.moveMouseToPos(x, y);
    }

    public float getPartialTick() {
        return root.getPartialTick();
    }

    protected void setParent(GuiElement parent, GuiElement rootParent)
    {
        this.parent = parent;
        this.rootParent = rootParent;
        for(GuiElement child : childs)
        {
            child.setParent(this, rootParent);
        }
    }
    public void addChild(GuiElement el)
    {
        if(el.getParent() != null)
        {
            el.getParent().removeChild(el);
        }
        el.setRoot(root);
        el.setParent(this, rootParent);
        childs.add(el);
        layoutChangedInternal();
    }
    public void removeChild(GuiElement el)
    {
        el.setRoot(null);
        el.setParent(null, el);
        childs.remove(el);
        layoutChangedInternal();
    }
    public void removeChilds()
    {
        for(GuiElement child : childs)
        {
            child.setRoot(null);
            child.setParent(null, child);
        }
        childs.clear();
        layoutChangedInternal();
    }
    public List<GuiElement> getChilds()
    {
        return childs;
    }



    public int getX() {
        return bounds.x;
    }
    public int getY() {
        return bounds.y;
    }
    public float getZ() {
        return zPos;
    }

    public Point getPosition()
    {
        return new Point(bounds.x, bounds.y);
    }
    public int getWidth() {
        return bounds.width;
    }
    public int getHeight() {
        return bounds.height;
    }
    public Point getSize()
    {
        return new Point(bounds.width, bounds.height);
    }
    public void setX(int x) {
        bounds.x = x;
        layoutChangedInternal();
    }
    public void setY(int y) {
        bounds.y = y;
        layoutChangedInternal();
    }
    public void setZ(float z) {
        zPos = z;
    }
    public void setPosition(int x, int y)
    {
        bounds.x = x;
        bounds.y = y;
        layoutChangedInternal();
    }
    public void setPosition(Point pos)
    {
        bounds.x = pos.x;
        bounds.y = pos.y;
        layoutChangedInternal();
    }
    public void setWidth(int width) {
        bounds.width = width;
        layoutChangedInternal();
    }
    public void setHeight(int height) {
        bounds.height = height;
        layoutChangedInternal();
    }
    public void setSize(int width, int height)
    {
        bounds.width = width;
        bounds.height = height;
        layoutChangedInternal();
    }
    public void setSize(Point size)
    {
        bounds.width = size.x;
        bounds.height = size.y;
        layoutChangedInternal();
    }
    public void setBounds(Rectangle rect)
    {
        bounds.x = rect.x;
        bounds.y = rect.y;
        bounds.width = rect.width;
        bounds.height = rect.height;
        layoutChangedInternal();
    }
    public void setBounds(int x, int y, int width, int height)
    {
        bounds.x = x;
        bounds.y = y;
        bounds.width = width;
        bounds.height = height;
        layoutChangedInternal();
    }
    public int getTop()
    {
        return bounds.y;
    }
    public int getBottom()
    {
        return bounds.y+bounds.height;
    }
    public int getLeft()
    {
        return bounds.x;
    }
    public int getRight()
    {
        return bounds.x+bounds.width;
    }

    public Rectangle getBounds()
    {
        return bounds;
    }
    public Point getGlobalPositon()
    {
        return globalPositon;
    }
    public Rectangle getChildFrame()
    {
        Rectangle frame = new Rectangle(0,0,0,0);
        var localChilds = getChilds();
        for (GuiElement child : localChilds) {
            Rectangle childFrame = child.getChildFrame();
            if(childFrame.x < frame.x)
                frame.x = childFrame.x;
            if(childFrame.y < frame.y)
                frame.y = childFrame.y;
            if(childFrame.x+childFrame.width > frame.x+frame.width)
                frame.width = childFrame.x+childFrame.width-frame.x;
            if(childFrame.y+childFrame.height > frame.y+frame.height)
                frame.height = childFrame.y+childFrame.height-frame.y;
        }
        return frame;
    }

    public int getTextWidth(String text)
    {
        return (int)((float)getFont().width(text) * textFontScale);
    }
    public int getTextHeight()
    {
        return (int)((float)getFont().lineHeight * textFontScale);
    }

    public void applyAlignment(Alignment alignment, int x, int y, int width, int height) {
        var b = getBounds();
        Rectangle bounds = getAlignedBounds(b.x, b.y ,b.width ,b.height , alignment, x, y, width, height);
        setBounds(bounds);
    }


    /**
     * Takes a rectangle1 defined by "x1", "y1", "width1", "height1" and a rectangle2 defined by "x2", "y2", "width2", "height2" and then returns a rectangle
     * that is the same size as rectangle1 but moved inside rectangle2 according to the specified alignment.
     * It moves the rectangle1 inside rectangle2 so that it is aligned according to the specified alignment.
     *
     * @return The new bounds of the rectangle, aligned according to the specified alignment.
     */
    public static Rectangle getAlignedBounds(int x1, int y1, int width1, int height1, Alignment alignment, int x2, int y2, int width2, int height2) {
        Rectangle bounds = new Rectangle(x1, y1, width1, height1);
        int xCenter = x2+(width2 - width1) / 2;
        int yCenter = y2+(height2 - height1) / 2;
        switch (alignment) {
            case CENTER:
                bounds.x = xCenter;
                bounds.y = yCenter;
                break;
            case LEFT:
                bounds.x = x2;
                bounds.y = yCenter;
                break;
            case RIGHT:
                bounds.x = width2 - bounds.width+x2;
                bounds.y = yCenter;
                break;
            case TOP:
                bounds.x = xCenter;
                bounds.y = y2;
                break;
            case BOTTOM:
                bounds.x = xCenter;
                bounds.y = height2 - bounds.height+y2;
                break;
            case TOP_LEFT:
                bounds.x = x2;
                bounds.y = y2;
                break;
            case TOP_RIGHT:
                bounds.x = width2 - bounds.width+x2;
                bounds.y = y2;
                break;
            case BOTTOM_LEFT:
                bounds.x = x2;
                bounds.y = height2 - bounds.height+y2;
                break;
            case BOTTOM_RIGHT:
                bounds.x = width2 - bounds.width+x2;
                bounds.y = height2 - bounds.height+y2;
                break;
        }
        return bounds;
    }



    public void drawText(String text, int x, int y, int color)
    {
        root.drawText(text, x,y, color, textFontScale);
    }
    public void drawText(String text, int x, int y)
    {
        drawText(text, x, y, textColor, textFontScale);
    }
    public void drawText(String text, Point pos, int color)
    {
        drawText(text, pos.x, pos.y, color, textFontScale);
    }
    public void drawText(String text, Point pos)
    {
        drawText(text, pos.x, pos.y, textColor, textFontScale);
    }
    public void drawText(Component text, int x, int y, int color)
    {
        root.drawText(text, x, y, color, textFontScale);
    }
    public void drawText(Component text, int x, int y)
    {
        drawText(text, x, y, textColor, textFontScale);
    }
    public void drawText(Component text, Point pos, int color)
    {
        drawText(text, pos.x, pos.y, color, textFontScale);
    }
    public void drawText(Component text, Point pos)
    {
        drawText(text, pos.x, pos.y, textColor, textFontScale);
    }
    public void drawText(Component text, int x, int y, int color, boolean dropShadow)
    {
        root.drawText(text, x, y, color, dropShadow, textFontScale);
    }
    public void drawText(Component text, int x, int y, boolean dropShadow)
    {
        drawText(text, x, y, textColor, dropShadow, textFontScale);
    }
    public void drawText(Component text, Point pos, int color, boolean dropShadow)
    {
        drawText(text, pos.x, pos.y, color, dropShadow, textFontScale);
    }
    public void drawText(Component text, Point pos, boolean dropShadow)
    {
        drawText(text, pos.x, pos.y, textColor, dropShadow, textFontScale);
    }
    public void drawText(String text, Point pos, Alignment posAlignment)
    {
        Point size = getTextBounds(text);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, textColor, textFontScale);
    }
    public void drawText(String text, int x, int y, Alignment posAlignment)
    {
        Point size = getTextBounds(text);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, textColor, textFontScale);
    }
    public void drawText(String text, Point pos, int color, Alignment posAlignment)
    {
        Point size = getTextBounds(text);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, color, textFontScale);
    }
    public void drawText(String text, int x, int y, int color, Alignment posAlignment)
    {
        Point size = getTextBounds(text);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, color, textFontScale);
    }
    public void drawText(Component text, Point pos, boolean dropShadow, Alignment posAlignment)
    {
        Point size = getTextBounds(text.getString());
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, textColor, dropShadow, textFontScale);
    }
    public void drawText(Component text, int x, int y, boolean dropShadow, Alignment posAlignment)
    {
        Point size = getTextBounds(text.getString());
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, textColor, dropShadow, textFontScale);
    }
    public void drawText(Component text, Point pos, int color, boolean dropShadow, Alignment posAlignment)
    {
        Point size = getTextBounds(text.getString());
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, color, dropShadow, textFontScale);
    }
    public void drawText(Component text, int x, int y, int color, boolean dropShadow, Alignment posAlignment)
    {
        Point size = getTextBounds(text.getString());
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, color, dropShadow, textFontScale);
    }


    public void drawText(String text, int x, int y, int color, float fontScale)
    {
        root.drawText(text, x,y, color, fontScale);
    }
    public void drawText(String text, int x, int y, float fontScale)
    {
        drawText(text, x, y, textColor, fontScale);
    }
    public void drawText(String text, Point pos, int color, float fontScale)
    {
        drawText(text, pos.x, pos.y, color, fontScale);
    }
    public void drawText(String text, Point pos, float fontScale)
    {
        drawText(text, pos.x, pos.y, textColor, fontScale);
    }

    public void drawText(Component text, int x, int y, int color, float fontScale)
    {
        root.drawText(text, x, y, color, fontScale);
    }
    public void drawText(Component text, int x, int y, float fontScale)
    {
        drawText(text, x, y, textColor, fontScale);
    }
    public void drawText(Component text, Point pos, int color, float fontScale)
    {
        drawText(text, pos.x, pos.y, color, fontScale);
    }
    public void drawText(Component text, Point pos, float fontScale)
    {
        drawText(text, pos.x, pos.y, textColor, fontScale);
    }
    public void drawText(Component text, int x, int y, int color, boolean dropShadow, float fontScale)
    {
        root.drawText(text, x, y, color, dropShadow, fontScale);
    }
    public void drawText(Component text, int x, int y, boolean dropShadow, float fontScale)
    {
        drawText(text, x, y, textColor, dropShadow, fontScale);
    }
    public void drawText(Component text, Point pos, int color, boolean dropShadow, float fontScale)
    {
        drawText(text, pos.x, pos.y, color, dropShadow, fontScale);
    }
    public void drawText(Component text, Point pos, boolean dropShadow, float fontScale)
    {
        drawText(text, pos.x, pos.y, textColor, dropShadow, fontScale);
    }
    public void drawText(String text, Point pos, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text, fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, fontScale);
    }
    public void drawText(String text, int x, int y, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text, fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, fontScale);
    }
    public void drawText(String text, Point pos, int color, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text, fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, color, fontScale);
    }
    public void drawText(String text, int x, int y, int color, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text, fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, color, fontScale);
    }
    public void drawText(Component text, Point pos, boolean dropShadow, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text.getString(), fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, textColor, dropShadow, fontScale);
    }
    public void drawText(Component text, int x, int y, boolean dropShadow, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text.getString(), fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, textColor, dropShadow, fontScale);
    }
    public void drawText(Component text, Point pos, int color, boolean dropShadow, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text.getString(), fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, color, dropShadow, fontScale);
    }
    public void drawText(Component text, int x, int y, int color, boolean dropShadow, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text.getString(), fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, color, dropShadow, fontScale);
    }






    public void drawLine(int x1, int y1, int x2, int y2, float thickness, int color)
    {
        root.drawLine(x1,y1,x2,y2,thickness, color);
    }
    public void drawLine(Point start, Point end, float thickness,int color)
    {
        drawLine(start.x, start.y, end.x, end.y, thickness, color);
    }
    public void drawLine(Point start, Point end, float thickness)
    {
        drawLine(start.x, start.y, end.x, end.y, thickness, 0xFFFFFFFF);
    }
    public void drawLine(Point start, Point end)
    {
        drawLine(start.x, start.y, end.x, end.y, 1, 0xFFFFFFFF);
    }

    public void drawVertexBuffer_QUADS(VertexBuffer buffer)
    {
        root.drawVertexBuffer_QUADS(buffer);
    }

    public void drawCross(int x, int y, int size, int color)
    {
        drawRect(x - size, y, size * 2 + 1, 1, color);
        drawRect(x, y - size, 1, size * 2 + 1, color);
    }

    /**
     *   ╔═
     */
    public void drawCornerTL(int x, int y, int size, int color)
    {
        drawRect(x, y, size, 1, color);
        drawRect(x , y, 1, size, color);
    }

    /**
     *   ═╗
     */
    public void drawCornerTR(int x, int y, int size, int color)
    {
        drawRect(x - size+1, y, size, 1, color);
        drawRect(x, y, 1, size, color);
    }

    /**
     *   ╚═
     */
    public void drawCornerBL(int x, int y, int size, int color)
    {
        drawRect(x, y, size, 1, color);
        drawRect(x , y-size+1, 1, size, color);
    }

    /**
     *   ═╝
     */
    public void drawCornerBR(int x, int y, int size, int color)
    {
        drawRect(x - size+1, y, size, 1, color);
        drawRect(x , y- size+1, 1, size, color);
    }



    public void drawRect(int x,int y, int width, int height, int color)
    {
        root.drawRect(x,y,width, height, color);
    }
    public void drawRect(Rectangle rect, int color)
    {
        drawRect(rect.x,rect.y, rect.width, rect.height, color);
    }

    public void drawGradient(int x, int y, int width, int height, int colorFrom, int colorTo)
    {
        root.drawGradient(x,y,width, height, colorFrom, colorTo);
    }
    public void drawGradient(Rectangle rect, int colorFrom, int colorTo)
    {
        drawGradient(rect.x,rect.y, rect.width, rect.height, colorFrom, colorTo);
    }
    public void drawGradient(RenderType renderType, int x, int y, int z, int width, int height, int colorFrom, int colorTo)
    {
        root.drawGradient(renderType, x,y,z,width, height, colorFrom, colorTo);
    }
    public void drawGradient(RenderType renderType, Rectangle rect, int z, int colorFrom, int colorTo)
    {
        drawGradient(renderType, rect.x,rect.y, z, rect.width, rect.height, colorFrom, colorTo);
    }

    public void drawOutline(int x, int y, int width, int height, int color)
    {
        root.drawOutline(x,y,width, height, color);
    }
    public void drawOutline(Rectangle rect, int color)
    {
        drawOutline(rect.x,rect.y, rect.width, rect.height, color);
    }
    public void drawItem(ItemStack item, int x, int y, int seed)
    {
        root.drawItem(item, x, y, seed);
    }
    public void drawItem(ItemStack item, int x, int y)
    {
        drawItem(item, x, y, 0);
    }
    public void drawItem(ItemStack item, Point pos)
    {
        drawItem(item, pos.x, pos.y, 0);
    }
    public void drawItem(ItemStack item, Point pos, int seed)
    {
        drawItem(item, pos.x, pos.y, seed);
    }
    public void drawItemWithDecoration(ItemStack item, int x, int y, int seed)
    {
        root.drawItemWithDecoration(item, x,y, seed);
    }
    public void drawItemWithDecoration(ItemStack item, int x, int y)
    {
        drawItemWithDecoration(item, x, y, 0);
    }
    public void drawItemWithDecoration(ItemStack item, Point pos)
    {
        drawItemWithDecoration(item, pos.x, pos.y, 0);
    }

    public void drawItemWithDecoration(ItemStack item, Point pos, int seed)
    {
        drawItemWithDecoration(item, pos.x, pos.y, seed);
    }
    public void drawItemWithDecoration(ItemStack item, int x, int y, int z, int seed)
    {
        root.drawItemWithDecoration(item, x,y, z, seed);
    }
    public void drawItemWithDecoration(ItemStack item, Point pos, int z, int seed)
    {
        drawItemWithDecoration(item, pos.x, pos.y, z, seed);
    }

    public void drawTexture(GuiTexture texture, int x, int y)
    {
        root.drawTexture(texture.getResourceLocation(), x, y, texture.getUVOffsetX(), texture.getUVOffsetY(), texture.getWidth(), texture.getHeight(), texture.getWidth(), texture.getHeight());
    }
    public void drawTexture(GuiTexture texture, int x, int y, int width, int height)
    {
        root.drawTexture(texture.getResourceLocation(), x, y, texture.getUVOffsetX(), texture.getUVOffsetY(), width, height, width, height);
    }
    public void drawTexture(ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight)
    {
        root.drawTexture(atlasLocation, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }
    public void drawTextureFillArea(GuiTexture texture, int x, int y, int areaWidth, int areaHeight)
    {
        root.drawTexture(texture.getResourceLocation(), x, y, texture.getUVOffsetX(), texture.getUVOffsetY(), areaWidth, areaHeight, texture.getWidth(), texture.getHeight());
    }

    public void drawTexture(GuiTexture texture, Point pos)
    {
        drawTexture(texture, pos.x, pos.y);
    }

    public void drawTexture(TextureAtlasSprite sprite, int x, int y, int width, int height, int blitOffset)
    {
        root.drawTexture(sprite, x, y, width, height, blitOffset);
    }
    public void drawTexture(TextureAtlasSprite sprite, Rectangle area, int blitOffset)
    {
        drawTexture(sprite, area.x, area.y, area.width, area.height, blitOffset);
    }
    public void drawTexture(TextureAtlasSprite sprite, int x, int y, int width, int height, int blitOffset, float red, float green, float blue, float alpha)
    {
        root.drawTexture(sprite, x, y, width, height, blitOffset, red, green, blue, alpha);
    }
    public void drawTexture(TextureAtlasSprite sprite, Rectangle area, int blitOffset, float red, float green, float blue, float alpha)
    {
        drawTexture(sprite, area.x, area.y, area.width, area.height, blitOffset, red, green, blue, alpha);
    }


    private void drawTooltipInternal(Component tooltip, int x, int y)
    {
        root.drawTooltip(tooltip, x,y);
    }
    private void drawTooltipInternal(Component tooltip, Point pos) {
        drawTooltipInternal(tooltip, pos.x, pos.y);
    }

    private void drawTooltipInternal(ItemStack stack, int x, int y)
    {
        root.drawTooltip(stack, x,y);
    }
    private void drawTooltipInternal(ItemStack stack, Point pos)
    {
        drawTooltipInternal(stack, pos.x, pos.y);
    }

    private void drawTooltipInternal(String msg, int x, int y)
    {
        root.drawTooltip(msg, x,y);
    }
    private void drawTooltipInternal(String msg, Point pos)
    {
        drawTooltipInternal(msg, pos.x, pos.y);
    }
    public void drawTooltipNative(ItemStack stack, Point pos)
    {
        TooltipData data = new TooltipData();
        data.x = pos.x;
        data.y = pos.y;
        data.item = stack;
        data.createBackground = true; // Default to true, can be changed later
        drawTooltipLater.add(data);
    }


    public void drawTooltip(String tooltip, Point pos)
    {
        drawTooltip(tooltip, pos.x, pos.y);
    }
    public void drawTooltip(ItemStack stack, int x, int y)
    {
        drawTooltip(ClientPlayerUtilities.getItemDisplayText(stack), x, y);
    }
    public void drawTooltip(ItemStack stack, Point pos)
    {
        drawTooltip(stack, pos.x, pos.y);
    }

    public void drawTooltip(Component component, int x, int y)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.component = component;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(Component component, Point pos)
    {
        drawTooltip(component, pos.x, pos.y);
    }
    public void drawTooltip(String tooltip, int x, int y)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true; // Default to true, can be changed later
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, boolean createBackground)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = createBackground;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, int backgroundColor, int backgroundPadding, Alignment alignment)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true;
        data.backgroundColor = backgroundColor;
        data.backgroundPadding = backgroundPadding;
        data.alignment = alignment;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(ItemStack stack, int x, int y, int textColor)
    {
        drawTooltip(ClientPlayerUtilities.getItemDisplayText(stack), x, y);
    }
    public void drawTooltip(ItemStack stack, Point pos, int textColor)
    {
        drawTooltip(stack, pos.x, pos.y);
    }
    public void drawTooltip(Component component, int x, int y, int textColor)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.component = component;
        data.textColor = textColor;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(Component component, Point pos, int textColor)
    {
        drawTooltip(component, pos.x, pos.y, textColor);
    }
    public void drawTooltip(String tooltip, int x, int y, int textColor)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true; // Default to true, can be changed later
        data.textColor = textColor;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, boolean createBackground, int textColor)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = createBackground;
        data.textColor = textColor;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, int backgroundColor, int backgroundPadding, Alignment alignment, int textColor)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true;
        data.backgroundColor = backgroundColor;
        data.backgroundPadding = backgroundPadding;
        data.alignment = alignment;
        data.textColor = textColor;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(ItemStack stack, int x, int y, float fontScale)
    {
        drawTooltip(ClientPlayerUtilities.getItemDisplayText(stack), x, y, fontScale);
    }
    public void drawTooltip(ItemStack stack, Point pos, float fontScale)
    {
        drawTooltip(stack, pos.x, pos.y, fontScale);
    }
    public void drawTooltip(Component component, int x, int y, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.component = component;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(Component component, Point pos, float fontScale)
    {
        drawTooltip(component, pos.x, pos.y, fontScale);
    }
    public void drawTooltip(String tooltip, int x, int y, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true; // Default to true, can be changed later
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, boolean createBackground, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = createBackground;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, int backgroundColor, int backgroundPadding, Alignment alignment, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true;
        data.backgroundColor = backgroundColor;
        data.backgroundPadding = backgroundPadding;
        data.alignment = alignment;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(ItemStack stack, int x, int y, int textColor, float fontScale)
    {
        drawTooltip(ClientPlayerUtilities.getItemDisplayText(stack), x, y, fontScale);
    }
    public void drawTooltip(ItemStack stack, Point pos, int textColor, float fontScale)
    {
        drawTooltip(stack, pos.x, pos.y, fontScale);
    }
    public void drawTooltip(Component component, int x, int y, int textColor, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.component = component;
        data.textColor = textColor;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(Component component, Point pos, int textColor, float fontScale)
    {
        drawTooltip(component, pos.x, pos.y, textColor, fontScale);
    }
    public void drawTooltip(String tooltip, int x, int y, int textColor, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true; // Default to true, can be changed later
        data.textColor = textColor;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, boolean createBackground, int textColor, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = createBackground;
        data.textColor = textColor;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, int backgroundColor, int backgroundPadding, Alignment alignment, int textColor, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true;
        data.backgroundColor = backgroundColor;
        data.backgroundPadding = backgroundPadding;
        data.alignment = alignment;
        data.textColor = textColor;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public final void drawTooltip(TooltipData data)
    {
        drawTooltipLater.add(data);
    }





    public Point getTextBounds(String text)
    {
        var lines = text.split("\n");
        int maxWidth = 0;
        for (String line : lines) {
            int width = getTextWidth(line);
            if (width > maxWidth)
                maxWidth = width;
        }
        int height = getTextHeight() * lines.length;
        return new Point(maxWidth, height);
    }
    public Point getTextBounds(String text, float fontScale)
    {
        var lines = text.split("\n");
        int maxWidth = 0;
        for (String line : lines) {
            int width = getFont().width(line);
            if (width > maxWidth)
                maxWidth = width;
        }
        int height = (int)((float)(getFont().lineHeight * lines.length) * fontScale);
        return new Point((int)((float)maxWidth*fontScale), height);
    }


    public void drawFrame(int x, int y, int width, int height, int color, int thickness)
    {
        // Horizontal
        drawRect(x, y, width, thickness, color);
        drawRect(x, y+height-thickness, width, thickness, color);

        // Vertical
        drawRect(x, y+thickness, thickness, height-2*thickness, color);
        drawRect(x+width-thickness, y+thickness, thickness, height-2*thickness, color);
    }
    public void drawFrame(Rectangle rect, int color, int thickness)
    {
        drawFrame(rect.x, rect.y, rect.width, rect.height, color, thickness);
    }
    public double getGuiScale()
    {
        return Gui.getMinecraftGuiScale();
    }

    public void playLocalSound(SoundEvent sound, float volume, float pitch)
    {
        Gui.playLocalSound(sound, volume, pitch);
    }
    public void playLocalSound(SoundEvent sound, float volume)
    {
        playLocalSound(sound, volume, 1.0F);
    }
    public void playLocalSound(SoundEvent sound)
    {
        playLocalSound(sound, 1.0F, 1.0F);
    }

    public static float map(float value, float start1, float stop1, float start2, float stop2)
    {
        return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
    }
}
