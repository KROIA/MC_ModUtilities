package net.kroia.modutilities.gui;

import com.mojang.blaze3d.vertex.*;
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

import java.util.ArrayList;

public class Gui {

    protected final Graphics graphics;
    protected Screen parent;
    protected int mousePosX, mousePosY;
    protected float partialTick;
    private Rectangle globalScissorArea = null;

    protected GuiElement focusedElement = null;

    private ArrayList<GuiElement> elements = new ArrayList<>();

    public Gui(Screen parent)
    {
        this.parent = parent;
        graphics = new Graphics(parent);
    }
    public void init()
    {
        for(GuiElement element : elements)
        {
            element.init();
        }
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

    public void setMousePos(int x, int y)
    {
        this.mousePosX = x;
        this.mousePosY = y;
    }
    public void setPartialTick(float partialTick)
    {
        this.partialTick = partialTick;
    }
    public void renderBackground()
    {
        for(GuiElement element : elements)
        {
            element.renderBackgroundInternal();
        }
    }
    public void render()
    {
        for(GuiElement element : elements)
        {
            element.renderInternal();
        }
    }
    public void renderTooltip()
    {
        for(GuiElement element : elements)
        {
            element.renderTooltipInternal();
        }
    }
    public void renderGizmos()
    {
        for(GuiElement element : elements)
        {
            element.renderGizmosInternal();
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        this.mousePosX = (int)mouseX;
        this.mousePosY = (int)mouseY;
        for(GuiElement element : elements)
        {
            if(element.mouseClickedInternal(button, true))
                return true;
        }
        return false;
    }
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY)
    {
        this.mousePosX = (int)mouseX;
        this.mousePosY = (int)mouseY;
        for(GuiElement element : elements)
        {
            if(element.mouseDraggedInternal(button, deltaX, deltaY))
                return true;
        }
        return false;
    }
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        this.mousePosX = (int)mouseX;
        this.mousePosY = (int)mouseY;
        for(GuiElement element : elements)
        {
            if(element.mouseReleasedInternal(button,true))
                return true;
        }
        return false;
    }
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        this.mousePosX = (int)mouseX;
        this.mousePosY = (int)mouseY;
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
        //VertexConsumer vertexconsumer = graphics.bufferSource().getBuffer(RenderType.debugQuads()); // mc<=1.19.4
        VertexConsumer vertexconsumer = graphics.bufferSource().getBuffer(RenderType.solid()); // mc<=1.19.3
        vertexconsumer.vertex(matrix4f, (float)p1.x, (float)p1.y, (float)0).color(red, green, blue, alpha).endVertex();
        vertexconsumer.vertex(matrix4f, (float)p2.x, (float)p2.y, (float)0).color(red, green, blue, alpha).endVertex();
        vertexconsumer.vertex(matrix4f, (float)p3.x, (float)p3.y, (float)0).color(red, green, blue, alpha).endVertex();
        vertexconsumer.vertex(matrix4f, (float)p4.x, (float)p4.y, (float)0).color(red, green, blue, alpha).endVertex();
        graphics.flush();
    }
    public void drawVertexBuffer_QUADS(VertexBuffer buffer) {
        Matrix4f matrix4f = graphics.getLastPoseMatrix();
        //RenderType renderType = RenderType.debugQuads();// mc>=1.19.4
        RenderType renderType = RenderType.solid();// mc<=1.19.3
        /*
        VertexConsumer vertexconsumer = graphics.bufferSource().getBuffer(renderType);
        for(Vertex vertex : buffer.getVertices()) // mc>=1.19.4
        {
            vertexconsumer.vertex(matrix4f, vertex.x, vertex.y, 0).color(vertex.red, vertex.green, vertex.blue, vertex.alpha).endVertex();
        }
        graphics.flush();
        */

        // mc<=1.19.3
        // Colors dont work for some reason =(
        ArrayList<Vertex> vertices = buffer.getVertices();
        int lightmapUV = 15728880; // Bright lighting
        for(int i = 0; i < vertices.size(); i+=4)
        {
            Vertex vertex1 = vertices.get(i);
            Vertex vertex2 = vertices.get(i+1);
            Vertex vertex3 = vertices.get(i+2);
            Vertex vertex4 = vertices.get(i+3);
            VertexConsumer vertexconsumer = graphics.bufferSource().getBuffer(renderType);
            vertexconsumer.vertex(matrix4f, vertex1.x, vertex1.y, 0).color(vertex1.red, vertex1.green, vertex1.blue, vertex1.alpha).uv(0,0).uv2(lightmapUV).normal(0,0,1).endVertex();
            vertexconsumer.vertex(matrix4f, vertex2.x, vertex2.y, 0).color(vertex2.red, vertex2.green, vertex2.blue, vertex2.alpha).uv(0,0).uv2(lightmapUV).normal(0,0,1).endVertex();
            vertexconsumer.vertex(matrix4f, vertex3.x, vertex3.y, 0).color(vertex3.red, vertex3.green, vertex3.blue, vertex3.alpha).uv(0,0).uv2(lightmapUV).normal(0,0,1).endVertex();
            vertexconsumer.vertex(matrix4f, vertex4.x, vertex4.y, 0).color(vertex4.red, vertex4.green, vertex4.blue, vertex4.alpha).uv(0,0).uv2(lightmapUV).normal(0,0,1).endVertex();
            graphics.flush();
        }
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
        int thickness = 1;
        // Horizontal
        drawRect(x, y, width, thickness, color);
        drawRect(x, y+height-thickness, width, thickness, color);

        // Vertical
        drawRect(x, y+thickness, thickness, height-2*thickness, color);
        drawRect(x+width-thickness, y+thickness, thickness, height-2*thickness, color);
        //graphics.renderOutline(x,y,width,height,color); // mc>=1.19.4
    }
    public void drawTooltip(Component tooltip, int x, int y)
    {
        if(isScissorEnabled())
        {
            scissorPause();
            //pushPose();
            graphics.renderTooltip(getFont(), tooltip, x,y);
            //popPose();
            scissorResume();
            return;
        }
        graphics.renderTooltip(getFont(), tooltip, x,y);
    }
    public void drawTooltip(ItemStack stack, int x, int y)
    {
        if(isScissorEnabled())
        {
            scissorPause();
            //pushPose();
            //graphics.pose().translate(0.0D, 0.0D, (double)(200));
            graphics.renderTooltip(getFont(), stack, x,y);
            //popPose();
            scissorResume();
            return;
        }
        graphics.renderTooltip(getFont(), stack, x,y);
    }
    public void drawItem(ItemStack item, int x, int y, int seed)
    {
        graphics.renderItem(item, x, y, seed);
    }
    public void drawItemWithDecoration(ItemStack item, int x, int y, int seed)
    {
        graphics.renderItem(item, x, y, seed);
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
        graphics.renderItem(item, x, y, seed);
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

    public void drawTexture(ResourceLocation texture, int x, int y, int width, int height, int uOffset, int vOffset)
    {
        graphics.blit(texture, x, y, uOffset, vOffset, width, height);
    }
    public void drawTexture(TextureAtlasSprite sprite, int x, int y, int width, int height, int blitOffset)
    {
        graphics.blit(x, y, blitOffset, width, height, sprite);
    }

    /*
    // mc>=1.19.4
    public void drawTexture(TextureAtlasSprite sprite, int x, int y, int width, int height, int blitOffset, float red, float green, float blue, float alpha)
    {
        graphics.blit(x, y, blitOffset, width, height, sprite, red, green, blue, alpha);
    }
    */

    public static ResourceLocation createResourceLocation(String modID, String path)
    {
        return new ResourceLocation(modID, path);
        //return ResourceLocation.fromNamespaceAndPath(modID, path);
    }
    public static double getGuiScale()
    {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }
    public void enableScissor(Rectangle rect)
    {
        globalScissorArea = rect;
        //int guiScale = (int)getGuiScale();
        int x1 = rect.x;
        int y1 = rect.y;
        int x2 = (rect.x+rect.width);
        int y2 = (rect.y+rect.height);

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
