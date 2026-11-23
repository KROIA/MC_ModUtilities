package net.kroia.modutilities.neoforge;

import dev.architectury.event.events.common.LifecycleEvent;
import net.kroia.modutilities.UtilitiesPlatform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.kroia.modutilities.ModUtilitiesMod;

@Mod(ModUtilitiesMod.MOD_ID)
public final class ModUtilitiesNeoForge {
    public ModUtilitiesNeoForge(IEventBus modBus) {
        // NeoForge constructs mods in parallel, and Architectury's event lists are plain ArrayLists: registering from
        // the constructor raced other mods doing the same. The deferred queue runs on the main thread, one task at a
        // time, after all constructors and before RegisterEvent, config loading and payload registration (TK-12).
        modBus.addListener((FMLConstructModEvent e) -> e.enqueueWork(() -> {
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
        }));
    }
}
