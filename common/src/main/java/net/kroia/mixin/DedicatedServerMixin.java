package net.kroia.mixin;

import net.kroia.modutilities.networking.multi_server.MultiServerManager;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public class DedicatedServerMixin {

    // An empty linked master/slave must keep ticking: a paused server freezes tick-driven work (link, markets) for the other servers.
    @Inject(method = "pauseWhileEmptySeconds", at = @At("HEAD"), cancellable = true, require = 0) // mc<=1.21.1
    //@Inject(method = "pauseWhileEmptySeconds", at = @At("HEAD"), cancellable = true) // mc>=1.21.2 mc<=1.21.8
    //@Inject(method = "pauseWhenEmptySeconds", at = @At("HEAD"), cancellable = true) // mc>=1.21.9
    private void modutilities$noPauseWhenLinked(CallbackInfoReturnable<Integer> cir) {
        if (MultiServerManager.isInUse())
            cir.setReturnValue(0);
    }
}
