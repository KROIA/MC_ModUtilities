package net.kroia.fabric;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.UtilitiesPlatform;
import net.minecraft.server.level.ServerLevel;

public final class ModUtilitiesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        UtilitiesPlatform.setPlatform(new UtilitiesPlatformFabric());
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ModUtilitiesMod.LOGGER.info("[FabricSetup] SERVER_STARTING");
            UtilitiesPlatformFabric.setServer(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ModUtilitiesMod.LOGGER.info("[FabricSetup] SERVER_STOPPED");
            UtilitiesPlatformFabric.setServer(null);
        });

        /*ServerWorldEvents.LOAD.register((server, world)-> {
            if(world.isClientSide())
                return;
            UtilitiesPlatformFabric.setServer(server);
        });

        ServerWorldEvents.UNLOAD.register((server, world)-> {
            if(world.isClientSide())
                return;
            UtilitiesPlatformFabric.setServer(null);
        });*/

        ModUtilitiesMod.init();
    }
}
