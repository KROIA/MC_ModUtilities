package net.modutilities.neoforge;

import dev.architectury.event.events.common.LifecycleEvent;
import net.kroia.modutilities.UtilitiesPlatform;
import net.neoforged.fml.common.Mod;
import net.kroia.modutilities.ModUtilitiesMod;

@Mod(ModUtilitiesMod.MOD_ID)
public final class ModUtilitiesNeoForge {
    public ModUtilitiesNeoForge() {

        UtilitiesPlatform.setPlatform(new UtilitiesPlatformNeoForge());
        LifecycleEvent.SERVER_STARTING.register(server -> {
            ModUtilitiesMod.LOGGER.info("[NeoForgeSetup] SERVER_STARTING");
            UtilitiesPlatformNeoForge.setServer(server);
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            ModUtilitiesMod.LOGGER.info("[NeoForgeSetup] SERVER_STOPPED");
            UtilitiesPlatformNeoForge.setServer(server);
        });



        // Run our common setup.
        ModUtilitiesMod.init();
    }
}
