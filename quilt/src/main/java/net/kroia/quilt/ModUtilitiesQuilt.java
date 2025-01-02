package net.kroia.quilt;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.kroia.modutilities.UtilitiesPlatform;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

import net.kroia.modutilities.ModUtilitiesMod;

public final class ModUtilitiesQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        // Run our common setup.
        UtilitiesPlatform.setPlatform(new UtilitiesPlatformQuilt());
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            UtilitiesPlatformQuilt.setServer(server);
        });
        ModUtilitiesMod.init();
    }
}
