package net.kroia.modutilities.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class GuiTexture {
    private final ResourceLocation resourceLocation;
    private final int width;
    private final int height;
    private float uvOffsetX = 0;
    private float uvOffsetY = 0;

    public GuiTexture(String modID, String path, int imageWidth, int imageHeight) {
        this.resourceLocation = Gui.createResourceLocation(modID, path);
        this.width = imageWidth;
        this.height = imageHeight;
    }

    public void setUVOffset(float x, float y) {
        this.uvOffsetX = x;
        this.uvOffsetY = y;
    }
    public float getUVOffsetX() {
        return uvOffsetX;
    }
    public float getUVOffsetY() {
        return uvOffsetY;
    }

    public ResourceLocation getResourceLocation() {
        return resourceLocation;
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
}
