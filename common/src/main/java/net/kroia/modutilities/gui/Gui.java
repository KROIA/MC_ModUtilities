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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

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

    public Gui(Screen parent)
    {
        this.parent = parent;
        this.graphics = new Graphics(parent);

    }
    public void init()
    {
        for(GuiElement element : elements)
        {
            element.init();
        }
    }

    public float getGuiScale() {
        return guiScale;
    }
    public float getInvGuiScale() {
        return invGuiScale;
    }
    public void setGuiScale(float guiScale) {
        this.guiScale = guiScale;
        if(guiScale <= 0.0f) {
            guiScale = 1.0f; // Ensure scale is always positive
        }
        invGuiScale = 1.0f / guiScale; // Calculate inverse scale
    }

    public Graphics getGraphics()
    {
        return this.graphics;
    }
    public int getMousePosX()
    {
        return this.mousePosX;
    }
    public int getMousePosY()
    {
        return this.mousePosY;
    }
    public float getPartialTick()
    {
        return this.partialTick;
    }
    public static Font getFont()
    {
        return Minecraft.getInstance().font;
    }
    public static Minecraft getMinecraft()
    {
        return Minecraft.getInstance();
    }

    public int getBackgroundRenderZPos()
    {
        return backgroundZ;
    }
    public int getForegroundRenderZPos()
    {
        return foregroundZ;
    }
    public int getTooltipRenderZPos()
    {
        return tooltipZ;
    }
    public int getGizmoRenderZPos()
    {
        return gizmoZ;
    }
    public void setBackgroundRenderZPos(int z)
    {
        this.backgroundZ = z;
    }
    public void setForegroundRenderZPos(int z)
    {
        this.foregroundZ = z;
    }
    public void setTooltipRenderZPos(int z)
    {
        this.tooltipZ = z;
    }
    public void setGizmoRenderZPos(int z)
    {
        this.gizmoZ = z;
    }


    public Screen getScreen()
    {
        return parent;
    }

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

    public void addElement(GuiElement element)
    {
        element.setRoot(this);
        elements.add(element);
    }
    public void removeElement(GuiElement element)
    {
        element.setRoot(null);
        elements.remove(element);
    }
    public void removeAllElements()
    {
        for(GuiElement element : elements)
        {
            element.setRoot(null);
        }
        elements.clear();
    }
    public List<GuiElement> getElements()
    {
        return elements;
    }
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
    public GuiElement getFocusedElement()
    {
        return this.focusedElement;
    }

    public void storeMousePos(int x, int y)
    {
        this.mousePosX = (int)((float)x*invGuiScale);
        this.mousePosY = (int)((float)y*invGuiScale);
    }
    public void storeMousePos(double x, double y)
    {
        this.mousePosX = (int)(x*(double)invGuiScale);
        this.mousePosY = (int)(y*(double)invGuiScale);
    }
    public void moveMouseToPos(int x, int y)
    {
        double guiScaleFactor = getMinecraftGuiScale();
        double newX = x * guiScaleFactor * guiScale;
        double newY = y * guiScaleFactor * guiScale;
        long windowHandle = getWindowHandle();
        GLFW.glfwSetCursorPos(windowHandle, newX, newY);
    }
    public void setPartialTick(float partialTick)
    {
        this.partialTick = partialTick;
    }
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        for(GuiElement element : elements)
        {
            if(element.keyPressedInternal(keyCode, scanCode, modifiers))
                return true;
        }
        return false;
    }
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
    public void drawText(String text, int x, int y, int color)
    {
        // Split text by new line
        String[] lines = text.split("\n");
        for(int i = 0; i < lines.length; i++)
        {
            graphics.drawString(getFont(), lines[i], x, y + i*getFont().lineHeight, color);
        }
    }
    public void drawText(String text, int x, int y, int color, boolean dropShadow)
    {
        // Split text by new line
        String[] lines = text.split("\n");
        for(int i = 0; i < lines.length; i++)
        {
            graphics.drawString(getFont(), lines[i], x, y + i*getFont().lineHeight, color, dropShadow);
        }
    }
    public void drawText(Component text, int x, int y, int color)
    {
        // Split text by new line
        String[] lines = text.getString().split("\n");
        for(int i = 0; i < lines.length; i++)
        {
            graphics.drawString(getFont(), lines[i], x, y + i*getFont().lineHeight, color);
        }
    }
    public void drawText(Component text, int x, int y, int color, boolean dropShadow)
    {
        // Split text by new line
        String[] lines = text.getString().split("\n");
        for(int i = 0; i < lines.length; i++)
        {
            graphics.drawString(getFont(), lines[i], x, y + i*getFont().lineHeight, color, dropShadow);
        }
    }
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
        //VertexConsumer vertexconsumer = graphics.bufferSource().getBuffer(RenderType.gui()); // mc>=1.20.1
        VertexConsumer vertexconsumer = graphics.bufferSource().getBuffer(RenderType.debugQuads()); // mc<=1.19.4
        vertexconsumer.vertex(matrix4f, (float)p1.x, (float)p1.y, (float)0).color(red, green, blue, alpha).endVertex();
        vertexconsumer.vertex(matrix4f, (float)p2.x, (float)p2.y, (float)0).color(red, green, blue, alpha).endVertex();
        vertexconsumer.vertex(matrix4f, (float)p3.x, (float)p3.y, (float)0).color(red, green, blue, alpha).endVertex();
        vertexconsumer.vertex(matrix4f, (float)p4.x, (float)p4.y, (float)0).color(red, green, blue, alpha).endVertex();
        graphics.flush();
    }
    public void drawVertexBuffer_QUADS(VertexBuffer buffer) {
        Matrix4f matrix4f = graphics.getLastPoseMatrix();
        RenderType renderType = RenderType.debugQuads();
        VertexConsumer vertexconsumer = graphics.bufferSource().getBuffer(renderType);
        for(Vertex vertex : buffer.getVertices())
        {
            vertexconsumer.vertex(matrix4f, vertex.x, vertex.y, 0).color(vertex.red, vertex.green, vertex.blue, vertex.alpha).endVertex();
        }
        graphics.flush();
    }
    public void drawRect(int x,int y, int width, int height, int color)
    {
        graphics.fill(x,y,width+x,height+y,color);
    }

    public void drawGradient(int x, int y, int width, int height, int colorFrom, int colorTo)
    {
        graphics.fillGradient(x,y,width+x,height+y,colorFrom,colorTo);
    }
    public void drawGradient(RenderType renderType, int x, int y, int z, int width, int height, int colorFrom, int colorTo)
    {
        graphics.fillGradient(renderType, x,y,width+x,height+y,colorFrom,colorTo, z);
    }
    public void drawOutline(int x, int y, int width, int height, int color)
    {
        graphics.renderOutline(x,y,width,height,color);
    }
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
    public void drawItem(ItemStack item, int x, int y, int seed)
    {
        graphics.renderItem(item, getFont(), x, y, seed);
    }
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

    public void drawTexture(ResourceLocation texture, int x, int y,  int uOffset, int vOffset, int width, int height)
    {
        graphics.blit(texture, x, y, uOffset, vOffset, width, height);
    }
    public void drawTexture(ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight)
    {
        graphics.blit(atlasLocation, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }
    public void drawTexture(TextureAtlasSprite sprite, int x, int y, int width, int height, int blitOffset)
    {
        graphics.blit(x, y, blitOffset, width, height, sprite);
    }
    public void drawTexture(TextureAtlasSprite sprite, int x, int y, int width, int height, int blitOffset, float red, float green, float blue, float alpha)
    {
        graphics.blit(x, y, blitOffset, width, height, sprite, red, green, blue, alpha);
    }

    public static ResourceLocation createResourceLocation(String modID, String path)
    {
        return new ResourceLocation(modID, path);
        //return ResourceLocation.fromNamespaceAndPath(modID, path);
    }
    public static double getMinecraftGuiScale()
    {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }
    public long getWindowHandle()
    {
        return Minecraft.getInstance().getWindow().getWindow();
    }
    public void enableScissor(Rectangle rect)
    {
        globalScissorArea = rect;
        int x1 = (int)((float)rect.x * guiScale);
        int y1 = (int)((float)rect.y * guiScale);
        int x2 = (int)(((float)rect.x+(float)rect.width)*guiScale);
        int y2 = (int)(((float)rect.y+(float)rect.height)*guiScale);

        graphics.enableScissor(x1,y1,x2,y2);
    }
    public void disableScissor()
    {
        globalScissorArea = null;
        graphics.disableScissor();
    }
    public boolean isScissorEnabled()
    {
        return globalScissorArea != null;
    }
    public Rectangle getScissorArea()
    {
        return globalScissorArea;
    }
    public void scissorPause()
    {
        graphics.disableScissor();
    }
    public void scissorResume()
    {
        if(globalScissorArea != null)
        {
            enableScissor(globalScissorArea);
        }
    }

    public void translate(float x, float y, float z)
    {
        graphics.translate(x, y, z);
    }
    public void translate(double x, double y, double z)
    {
        graphics.translate(x, y, z);
    }
    public void scale(float x, float y, float z)
    {
        graphics.scale(x, y, z);
    }
    public void pushPose()
    {
        graphics.pushPose();
    }
    public void popPose()
    {
        graphics.popPose();
    }

    public static void playLocalSound(SoundEvent sound, float volume, float pitch)
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.level.playLocalSound(
                minecraft.player.getX(),            // X coordinate
                minecraft.player.getY(),            // Y coordinate
                minecraft.player.getZ(),            // Z coordinate
                sound,        // Sound to play
                SoundSource.PLAYERS,                // Sound category
                volume,                               // Volume
                pitch,                               // Pitch
                false                                // Delay
        );
    }

}
