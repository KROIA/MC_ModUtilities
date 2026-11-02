package net.kroia.mixin;

// mc>=1.21.6 mc<=1.21.8 only; registered in modutilities.mixins.json of those branches.
// Fabric API's fabric-rendering-v1 calls SpecialGuiElementRegistryImpl.onReady from every GuiRenderer constructor.
// In fabric-rendering-v1 12.4.0 (Fabric API 0.128.x/0.129.x for 1.21.6/1.21.7) onReady throws
// "Already transitioned to late registration handler" for any GuiRenderer after the game's own, which crashes
// OffscreenGuiRenderer. Its GuiRenderer only draws ModUtilities GUIs and needs no Fabric-registered special
// elements, so the call is skipped while it is constructed. @Pseudo + require = 0: no-op on NeoForge and on
// Fabric API builds without this method.

//import net.kroia.modutilities.gui.client.OffscreenGuiRenderer; // mc>=1.21.6 mc<=1.21.8
//import org.spongepowered.asm.mixin.Mixin; // mc>=1.21.6 mc<=1.21.8
//import org.spongepowered.asm.mixin.Pseudo; // mc>=1.21.6 mc<=1.21.8
//import org.spongepowered.asm.mixin.injection.At; // mc>=1.21.6 mc<=1.21.8
//import org.spongepowered.asm.mixin.injection.Inject; // mc>=1.21.6 mc<=1.21.8
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo; // mc>=1.21.6 mc<=1.21.8

//@Pseudo // mc>=1.21.6 mc<=1.21.8
//@Mixin(targets = "net.fabricmc.fabric.impl.client.rendering.SpecialGuiElementRegistryImpl", remap = false) // mc>=1.21.6 mc<=1.21.8
//public class FabricSpecialGuiElementRegistryMixin { // mc>=1.21.6 mc<=1.21.8
    //@Inject(method = "onReady", at = @At("HEAD"), cancellable = true, remap = false, require = 0) // mc>=1.21.6 mc<=1.21.8
    //private static void modutilities$skipForOffscreenRenderer(CallbackInfo ci) { // mc>=1.21.6 mc<=1.21.8
        //if (OffscreenGuiRenderer.isConstructing()) ci.cancel(); // mc>=1.21.6 mc<=1.21.8
    //} // mc>=1.21.6 mc<=1.21.8
//} // mc>=1.21.6 mc<=1.21.8
