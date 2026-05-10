package net.kroia.modutilities.forge;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.forge.EventBuses;
import net.kroia.modutilities.UtilitiesPlatform;
import net.minecraftforge.fml.common.Mod;

import net.kroia.modutilities.ModUtilitiesMod;

@Mod(ModUtilitiesMod.MOD_ID)
public final class ModUtilitiesForge {
    public ModUtilitiesForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(ModUtilitiesMod.MOD_ID, Mod.EventBusSubscriber.Bus.MOD.bus().get());

        UtilitiesPlatform.setPlatform(new UtilitiesPlatformForge());
        LifecycleEvent.SERVER_STARTING.register(server -> {
            ModUtilitiesMod.LOGGER.info("[ForgeSetup] SERVER_STARTING");
            UtilitiesPlatformForge.setServer(server);
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            ModUtilitiesMod.LOGGER.info("[ForgeSetup] SERVER_STOPPED");
            UtilitiesPlatformForge.setServer(null);
        });

        // Run our common setup.
        ModUtilitiesMod.init();
    }
}
