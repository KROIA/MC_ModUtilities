package net.kroia.modutilities.gui;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Gui {

    protected GuiGraphics graphics;
    protected Screen parent;
    protected int mousePosX, mousePosY;
    protected float partialTick;

    protected GuiElement focusedElement = null;

    private ArrayList<GuiElement> elements = new ArrayList<>();

    public Gui(Screen parent)
    {
        this.parent = parent;
    }
    public void init()
    {
        for(GuiElement element : elements)
        {
            element.init();
        }
    }


    public GuiGraphics getGraphics()
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
        return parent.getMinecraft() != null;
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
    public void renderBackground(GuiGraphics pGuiGraphics)
    {
        this.graphics = pGuiGraphics;
        for(GuiElement element : elements)
        {
            element.renderBackgroundInternal();
        }
    }
    public void render(GuiGraphics pGuiGraphics)
    {
        this.graphics = pGuiGraphics;
        for(GuiElement element : elements)
        {
            element.renderInternal();
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
        graphics.drawString(getFont(), text, x, y, color);
    }
    public void drawText(Component text, int x, int y, int color)
    {
        graphics.drawString(getFont(), text, x, y, color);
    }
    public void drawText(Component text, int x, int y, int color, boolean dropShadow)
    {
        graphics.drawString(getFont(), text, x, y, color, dropShadow);
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
        graphics.renderTooltip(getFont(), tooltip, x,y);
    }
    public void drawTooltip(List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int x, int y)
    {
        graphics.renderTooltip(getFont(), textComponents, tooltipComponent, stack, x,y);
    }
    public void drawTooltip(ItemStack pStack, int x, int y)
    {
        graphics.renderTooltip(getFont(), pStack, x, y);
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
            graphics.pose().pushPose();
            graphics.pose().translate(0.0D, 0.0D, (double)(200));
            drawText(s, x + 19 - 2 - getFont().width(s), y + 6 + 3, 16777215);
            graphics.pose().popPose();
        }
    }
    public void drawItemWithDecoration(ItemStack item, int x, int y, int z, int seed)
    {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0D, 0.0D, (double)(z));
        graphics.renderItem(item, x, y, seed);
        int count = item.getCount();
        if(count > 1)
        {
            // Render item count
            String s = String.valueOf(count);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0D, 0.0D, (double)(200));
            drawText(s, x + 19 - 2 - getFont().width(s), y + 6 + 3, 16777215);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }

    public void drawTexture(ResourceLocation texture, int x, int y, int width, int height, int uOffset, int vOffset)
    {
        graphics.blit(texture, x, y, uOffset, vOffset, width, height);
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
    public static double getGuiScale()
    {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }
    public void enableScissor(Rectangle rect)
    {
        //int guiScale = (int)getGuiScale();
        int x1 = rect.x;
        int y1 = rect.y;
        int x2 = (rect.x+rect.width);
        int y2 = (rect.y+rect.height);

        graphics.enableScissor(x1,y1,x2,y2);
    }
    public void disableScissor()
    {
        graphics.disableScissor();
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
