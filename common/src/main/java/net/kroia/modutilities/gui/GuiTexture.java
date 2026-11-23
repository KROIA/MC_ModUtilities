package net.kroia.modutilities.gui;

import net.minecraft.resources.ResourceLocation; // mc<=1.21.10
//import net.minecraft.resources.Identifier; // mc>=1.21.11

public class GuiTexture {
    private final ResourceLocation resourceLocation; // mc<=1.21.10
    //private final Identifier resourceLocation; // mc>=1.21.11
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

    public ResourceLocation getResourceLocation() { // mc<=1.21.10
    //public Identifier getResourceLocation() { // mc>=1.21.11
        return resourceLocation;
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
}
