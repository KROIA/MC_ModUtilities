package net.kroia.fabric;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.UtilitiesPlatform;

public final class ModUtilitiesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        UtilitiesPlatform.setPlatform(new UtilitiesPlatformFabric());
        ServerWorldEvents.LOAD.register((server, world)-> {
            if(world.isClientSide())
                return;
            UtilitiesPlatformFabric.setServer(server);
        });

        ModUtilitiesMod.init();
    }
}
