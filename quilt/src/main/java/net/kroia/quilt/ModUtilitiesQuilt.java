package net.kroia.quilt;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.UtilitiesPlatform;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.lifecycle.api.event.ServerLifecycleEvents;

public final class ModUtilitiesQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        // Run our common setup.
        UtilitiesPlatform.setPlatform(new UtilitiesPlatformQuilt());
        ServerLifecycleEvents.STARTING.register(server -> {
            ModUtilitiesMod.LOGGER.info("[QuiltSetup] SERVER STARTING");
            UtilitiesPlatformQuilt.setServer(server);
        });
        ServerLifecycleEvents.STOPPED.register(server -> {
            ModUtilitiesMod.LOGGER.info("[QuiltSetup] SERVER STOPPED");
            UtilitiesPlatformQuilt.setServer(null);
        });
        ModUtilitiesMod.init();
    }
}
