package net.kroia.modutilities.forge;

import dev.architectury.platform.forge.EventBuses;
import net.kroia.modutilities.UtilitiesPlatform;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.kroia.modutilities.ModUtilitiesMod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(ModUtilitiesMod.MOD_ID)
//@Mod.EventBusSubscriber(modid = ModUtilitiesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModUtilitiesForge {
    public ModUtilitiesForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(ModUtilitiesMod.MOD_ID, Mod.EventBusSubscriber.Bus.MOD.bus().get());

        UtilitiesPlatform.setPlatform(new UtilitiesPlatformForge());
        // Run our common setup.
        ModUtilitiesMod.init();
    }

    /*@SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ModUtilitiesMod.onClientSetup();
    }*/
}
