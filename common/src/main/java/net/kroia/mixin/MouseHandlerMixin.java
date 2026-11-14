package net.kroia.mixin;

import net.kroia.modutilities.gui.client.InputCompat;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps an autotest client from grabbing the mouse: the grab centers the OS cursor in the test window while the
 * user works elsewhere. releaseMouse() needs no counterpart, it returns early when the mouse is not grabbed.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void modutilities$suppressGrabUnderAutotest(CallbackInfo ci) {
        if (InputCompat.suppressCursor()) ci.cancel();
    }
}
