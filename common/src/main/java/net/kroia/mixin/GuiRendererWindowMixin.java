package net.kroia.mixin;

// mc>=1.21.6 only (GuiRenderer was added in 1.21.6); registered in modutilities.mixins.json of the 1.21.6+ branches.

//import com.mojang.blaze3d.platform.Window; // mc>=1.21.6
//import net.kroia.modutilities.gui.client.OffscreenGuiRenderer; // mc>=1.21.6
//import net.minecraft.client.gui.render.GuiRenderer; // mc>=1.21.6
//import org.spongepowered.asm.mixin.Mixin; // mc>=1.21.6
//import org.spongepowered.asm.mixin.injection.At; // mc>=1.21.6
//import org.spongepowered.asm.mixin.injection.Redirect; // mc>=1.21.6

/**
 * While {@link OffscreenGuiRenderer} renders, makes the vanilla GuiRenderer size its projection, scissor and
 * item atlas from the offscreen target instead of the window. No effect on normal screen rendering.
 */
//@Mixin(GuiRenderer.class) // mc>=1.21.6
//public class GuiRendererWindowMixin { // mc>=1.21.6
    //@Redirect(method = "*", require = 0, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I")) // mc>=1.21.6
    //private int modutilities$offscreenWidth(Window window) { // mc>=1.21.6
        //OffscreenGuiRenderer.Size size = OffscreenGuiRenderer.activeSize(); // mc>=1.21.6
        //return size != null ? size.width() : window.getWidth(); // mc>=1.21.6
    //} // mc>=1.21.6

    //@Redirect(method = "*", require = 0, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I")) // mc>=1.21.6
    //private int modutilities$offscreenHeight(Window window) { // mc>=1.21.6
        //OffscreenGuiRenderer.Size size = OffscreenGuiRenderer.activeSize(); // mc>=1.21.6
        //return size != null ? size.height() : window.getHeight(); // mc>=1.21.6
    //} // mc>=1.21.6

    //@Redirect(method = "*", require = 0, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScale()I")) // mc>=1.21.6
    //private int modutilities$offscreenGuiScale(Window window) { // mc>=1.21.6
        //OffscreenGuiRenderer.Size size = OffscreenGuiRenderer.activeSize(); // mc>=1.21.6
        //return size != null ? size.guiScale() : window.getGuiScale(); // mc>=1.21.6
    //} // mc>=1.21.6
//} // mc>=1.21.6
