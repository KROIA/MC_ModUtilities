package net.kroia.modutilities.gui.client;

// mc>=1.21.6 mc<=1.21.11 only: extra item atlas used by net.kroia.mixin.GuiRendererScaledItemsMixin (26.x uses vanilla GuiItemAtlas).

//import com.mojang.blaze3d.systems.GpuDevice; // mc>=1.21.6 mc<=1.21.11
//import com.mojang.blaze3d.systems.RenderSystem; // mc>=1.21.6 mc<=1.21.11
//import com.mojang.blaze3d.textures.FilterMode; // mc>=1.21.6 mc<=1.21.10
//import com.mojang.blaze3d.textures.GpuTexture; // mc>=1.21.6 mc<=1.21.11
//import com.mojang.blaze3d.textures.GpuTextureView; // mc>=1.21.6 mc<=1.21.11
//import com.mojang.blaze3d.textures.TextureFormat; // mc>=1.21.6 mc<=1.21.11

//import java.util.HashMap; // mc>=1.21.6 mc<=1.21.11
//import java.util.Map; // mc>=1.21.6 mc<=1.21.11

/** One extra atlas: a square texture divided into a grid of {@code slot} px cells. */
//public final class ScaledGuiItemAtlas { // mc>=1.21.6 mc<=1.21.11
    //public final int slot, size; // mc>=1.21.6 mc<=1.21.11
    //public final GpuTexture color, depth; // mc>=1.21.6 mc<=1.21.11
    //public final GpuTextureView colorView, depthView; // mc>=1.21.6 mc<=1.21.11
    //public final Map<Object, int[]> cells = new HashMap<>(); // identity -> {x, y, lastRenderedFrame, lastSeenFrame} // mc>=1.21.6 mc<=1.21.11
    //public int next; // mc>=1.21.6 mc<=1.21.11

    //public ScaledGuiItemAtlas(int slot, int size) { // mc>=1.21.6 mc<=1.21.11
        //this.slot = slot; // mc>=1.21.6 mc<=1.21.11
        //this.size = size; // mc>=1.21.6 mc<=1.21.11
        //GpuDevice device = RenderSystem.getDevice(); // mc>=1.21.6 mc<=1.21.11
        //color = device.createTexture("ModUtilities scaled UI items atlas", 12, TextureFormat.RGBA8, size, size, 1, 1); // mc>=1.21.6 mc<=1.21.11
        //color.setTextureFilter(FilterMode.NEAREST, false); // mc>=1.21.6 mc<=1.21.10
        //colorView = device.createTextureView(color); // mc>=1.21.6 mc<=1.21.11
        //depth = device.createTexture("ModUtilities scaled UI items atlas depth", 8, TextureFormat.DEPTH32, size, size, 1, 1); // mc>=1.21.6 mc<=1.21.11
        //depthView = device.createTextureView(depth); // mc>=1.21.6 mc<=1.21.11
        //device.createCommandEncoder().clearColorAndDepthTextures(color, 0, depth, 1.0); // mc>=1.21.6 mc<=1.21.11
    //} // mc>=1.21.6 mc<=1.21.11

    //public int capacity() { return (size / slot) * (size / slot); } // mc>=1.21.6 mc<=1.21.11

    //public void close() { // mc>=1.21.6 mc<=1.21.11
        //colorView.close(); // mc>=1.21.6 mc<=1.21.11
        //color.close(); // mc>=1.21.6 mc<=1.21.11
        //depthView.close(); // mc>=1.21.6 mc<=1.21.11
        //depth.close(); // mc>=1.21.6 mc<=1.21.11
    //} // mc>=1.21.6 mc<=1.21.11
//} // mc>=1.21.6 mc<=1.21.11
