package net.kroia.modutilities.gui;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//import net.minecraft.client.gui.GuiGraphics; // mc>=1.20.1
import com.mojang.blaze3d.vertex.PoseStack; // mc<=1.19.4
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Triple;


import java.awt.*;
import java.nio.FloatBuffer;
import java.util.Deque;

public class Graphics {

    //GuiGraphics graphics;// mc>=1.20.1
    PoseStack graphics; // mc<1.19.4
    private final Screen screen;

    public Graphics(Screen screen)
    {
        this.screen = screen;
    }


    /*
    public void setGraphics(GuiGraphics graphics)
    {
        this.graphics = graphics;
    }
    public GuiGraphics getGraphics()
    {
        return graphics;
    }
    */
    public void setGraphics(PoseStack graphics)
    {
        this.graphics = graphics;
    }
    public PoseStack getGraphics()
    {
        return graphics;
    }

    public void drawString(Font font, String text, int x, int y, int color)
    {
        font.draw(graphics, text, x, y, color);  // mc<=1.19.4
        //graphics.drawString(font, text, x, y, color); // mc>=1.20.1
    }
    public void drawString(Font font, String text, int x, int y, int color, boolean dropShadow)
    {
        font.draw(graphics, text, x, y, color);  // mc<=1.19.4
        //graphics.drawString(font, text, x, y, color, dropShadow); // mc>=1.20.1
    }
    public void fill(int x1, int y1, int x2, int y2, int color)
    {
        Screen.fill(graphics, x1, y1, x2, y2, color); // mc<=1.19.4
        //graphics.fill(x1, y1, x2, y2, color); // mc>=1.20.1
    }
    public void fillGradient(int x1, int y1, int x2, int y2, int colorFrom, int colorTo)
    {
        Screen.fill(graphics, x1, y1, x2, y2, colorFrom); // mc<=1.19.4
        //graphics.fillGradient(x1, y1, x2, y2, colorFrom, colorTo); // mc>=1.20.1
    }
    public void fillGradient(RenderType renderType, int x1, int y1, int x2, int y2, int colorFrom, int colorTo, int z)
    {
        Screen.fill(graphics, x1, y1, x2, y2, colorFrom); // mc<=1.19.4
        //graphics.fillGradient(renderType, x1, y1, x2, y2, colorFrom, colorTo, z); // mc>=1.20.1
    }
    /*
    // mc>=1.19.4
    public void renderOutline(int x1, int y1, int width, int height, int color)
    {
        Screen.renderOutline(graphics, x1, y1, width, height, color); // mc=1.19.4
        //graphics.renderOutline(x1, y1, width, height, color); // mc>=1.20.1
    }*/
    public void renderTooltip(Font font, Component text, int x, int y)
    {
        screen.renderTooltip(graphics, text, x, y); // mc<=1.19.4
        //graphics.renderTooltip(font, text, x, y); // mc>=1.20.1
    }
    public void renderTooltip(Font font, ItemStack itemStack, int x, int y)
    {
        screen.renderTooltip(graphics, screen.getTooltipFromItem(itemStack), itemStack.getTooltipImage(), x, y); // mc<=1.19.4
        //graphics.renderTooltip(font, itemStack, x, y); // mc>=1.20.1
    }
    public void renderItem(ItemStack itemStack, int x, int y)
    {
        // mc<=1.19.2
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        // Get current transformation matrix for x and y pos offset
        Matrix4f matrix = graphics.last().pose();
        Vector3f translation = new Vector3f(0,0,0);
        getTranslation(matrix, translation);
        itemRenderer.renderGuiItem(itemStack, x+(int)translation.x(), y+(int)translation.y());


        /*
        // mc<=1.19.3
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        // Get current transformation matrix for x and y pos offset
        Matrix4f matrix = graphics.last().pose();
        int xOffset = (int) matrix.m30();
        int yOffset = (int) matrix.m31();
        itemRenderer.renderGuiItem(itemStack, x+xOffset, y+yOffset);
        */


        /*
        // mc=1.19.4
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        itemRenderer.renderGuiItem(graphics, itemStack, x, y);
        */



        //graphics.renderItem(itemStack, x, y); // mc>=1.20.1
    }
    public void renderItem(ItemStack itemStack, int x, int y, int seed)
    {
        // mc<=1.19.2
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        // Get current transformation matrix for x and y pos offset
       // Triple<Quaternion, Vector3f, Quaternion> triple = graphics.last().pose().svdDecompose();
        Matrix4f matrix = graphics.last().pose();
        Vector3f translation = new Vector3f(0,0,0);
        getTranslation(matrix, translation);
        itemRenderer.renderGuiItem(itemStack, x+(int)translation.x(), y+(int)translation.y());


        /*
        // mc=1.19.3
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        // Get current transformation matrix for x and y pos offset
        Matrix4f matrix = graphics.last().pose();
        int xOffset = (int) matrix.m30();
        int yOffset = (int) matrix.m31();
        itemRenderer.renderGuiItem(itemStack, x+xOffset, y+yOffset);
        */

        /*
        // mc=1.19.4
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        itemRenderer.renderGuiItem(graphics, itemStack, x, y);
        */

        //graphics.renderItem(itemStack, x, y, seed); // mc>=1.20.1
    }


    public void blit(ResourceLocation atlasLocation, int x, int y, int uOffset, int vOffset, int width, int height)
    {
        RenderSystem.setShaderTexture(0, atlasLocation);
        screen.blit(graphics, x, y, uOffset, vOffset, width, height); // mc<=1.19.3
        //Screen.blit(graphics, x, y, uOffset, vOffset, width, height); // mc=1.19.4
        //graphics.blit(atlasLocation, x, y, uOffset, vOffset, width, height);
    }
    public void blit(ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight)
    {
        RenderSystem.setShaderTexture(0, atlasLocation);
        Screen.blit(graphics, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight); // mc<=1.19.4
        //graphics.blit(atlasLocation, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight); // mc>=1.20.1
    }
    public void blit(int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite)
    {
        Screen.blit(graphics, x, y, blitOffset, width, height, sprite); // mc<=1.19.4
        //graphics.blit(x, y, blitOffset, width, height, sprite); // mc>=1.20.1
    }

    /*
    // mc>=1.19.4
    public void blit(int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite, float red, float green, float blue, float alpha)
    {
        Screen.blit(graphics, x, y, blitOffset, width, height, sprite, red, green, blue, alpha); // mc<=1.19.4
        //graphics.blit(x, y, blitOffset, width, height, sprite, red, green, blue, alpha); // mc>=1.20.1
    }
    */

    public void enableScissor(int minX, int minY, int maxX, int maxY)
    {
        Screen.enableScissor(minX, minY, maxX, maxY); // mc<=1.19.4
        //graphics.enableScissor(minX, minY, maxX, maxY); // mc>=1.20.1
    }
    public void disableScissor()
    {
        Screen.disableScissor(); // mc<=1.19.4
        //graphics.disableScissor(); // mc>=1.20.1
    }


    public void translate(float x, float y, float z)
    {
        graphics.translate(x, y, z); // mc<=1.19.4
        //graphics.pose().translate(x, y, z); // mc>=1.20.1
    }
    public void translate(double x, double y, double z)
    {
        graphics.translate(x, y, z); // mc<=1.19.4
        //graphics.pose().translate(x, y, z); // mc>=1.20.1
    }

    public void pushPose()
    {
        graphics.pushPose(); // mc<=1.19.4
        //graphics.pose().pushPose(); // mc>=1.20.1
    }
    public void popPose()
    {
        graphics.popPose(); // mc<=1.19.4
        //graphics.pose().popPose(); // mc>=1.20.1
    }







    public Matrix4f getLastPoseMatrix()
    {
        return graphics.last().pose(); // mc<=1.19.4
        //return graphics.pose().last().pose(); // mc>=1.20.1
    }

    public MultiBufferSource.BufferSource bufferSource()
    {
        return Minecraft.getInstance().renderBuffers().bufferSource(); // mc<=1.19.4
        //return graphics.bufferSource(); // mc>=1.20.1
    }
    public void flush()
    {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch(); // mc<=1.19.4
        //graphics.flush(); // mc>=1.20.1
    }


    public static void getTranslation(Matrix4f matrix, Vector3f translation)
    {
        FloatBuffer buffer = FloatBuffer.allocate(16);
        matrix.store(buffer);
        translation.set(buffer.get(12), buffer.get(13), buffer.get(14));
    }

}
