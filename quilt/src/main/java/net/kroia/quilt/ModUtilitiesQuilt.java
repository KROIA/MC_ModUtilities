package net.kroia.quilt;

import net.fabricmc.api.ModInitializer;
//import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.UtilitiesPlatform;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;


public final class ModUtilitiesQuilt implements ModInitializer {

    @Override
    public void onInitialize() {

        UtilitiesPlatform.setPlatform(new UtilitiesPlatformQuilt());
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ModUtilitiesMod.LOGGER.info("[QuiltSetup] SERVER STARTING");
            UtilitiesPlatformQuilt.setServer(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ModUtilitiesMod.LOGGER.info("[QuiltSetup] SERVER STOPPED");
            UtilitiesPlatformQuilt.setServer(null);
        });

        // Run our common setup.
        ModUtilitiesMod.init();
    }
}
