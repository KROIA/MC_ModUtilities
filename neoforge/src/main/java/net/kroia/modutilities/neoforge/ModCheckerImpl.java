package net.kroia.modutilities.neoforge;

import net.neoforged.fml.ModList;

public class ModCheckerImpl {
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
