package net.kroia.modutilities.neoforge;

import dev.architectury.event.events.common.LifecycleEvent;
import net.kroia.modutilities.UtilitiesPlatform;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.kroia.modutilities.ModUtilitiesMod;

@Mod(ModUtilitiesMod.MOD_ID)
public final class ModUtilitiesNeoForge {
    public ModUtilitiesNeoForge() {

        UtilitiesPlatform.setPlatform(new UtilitiesPlatformNeoForge());
        LifecycleEvent.SERVER_STARTING.register(server -> {
            UtilitiesPlatformNeoForge.setServer(server);
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            UtilitiesPlatformNeoForge.setServer(null);
        });
        NeoForge.EVENT_BUS.addListener(PermissionCompatImpl::onGatherNodes);



        // Run our common setup.
        ModUtilitiesMod.init();
    }
}
