package net.kroia.modutilities.fabric;

import net.fabricmc.loader.api.FabricLoader;

public class ModCheckerImpl {
    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
