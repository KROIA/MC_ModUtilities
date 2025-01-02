package net.kroia.quilt;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

import net.kroia.modutilities.ModUtilitiesMod;

public final class ModUtilitiesQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        // Run our common setup.
        ModUtilitiesMod.init();
    }
}
