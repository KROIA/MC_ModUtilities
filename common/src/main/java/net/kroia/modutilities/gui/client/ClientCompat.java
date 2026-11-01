package net.kroia.modutilities.gui.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Client state that moved between Minecraft versions. Code that goes through this class compiles
 * unchanged on every supported version.
 */
public final class ClientCompat {
    private ClientCompat() {}

    /** @return the open screen, or {@code null} */
    public static Screen screen() {
        //return Minecraft.getInstance().screen; // mc<=26.1
        return Minecraft.getInstance().gui.screen(); // mc>=26.2
    }

    /** Opens {@code screen}, or closes the open screen if {@code null}. */
    public static void setScreen(Screen screen) {
        //Minecraft.getInstance().setScreen(screen); // mc<=26.1
        Minecraft.getInstance().gui.setScreen(screen); // mc>=26.2
    }

    /** @return the render target the game and GUI draw into */
    public static RenderTarget mainRenderTarget() {
        //return Minecraft.getInstance().getMainRenderTarget(); // mc<=26.1
        return Minecraft.getInstance().gameRenderer.mainRenderTarget(); // mc>=26.2
    }

    /** Replaces the main render target; used to render GUI frames into offscreen targets. */
    public static void setMainRenderTarget(RenderTarget target) {
        //((net.kroia.mixin.MinecraftAccessor) Minecraft.getInstance()).modutilities$setMainRenderTarget(target); // mc<=26.1
        ((net.kroia.mixin.GameRendererAccessor) Minecraft.getInstance().gameRenderer).modutilities$setMainRenderTarget(target); // mc>=26.2
    }
}
