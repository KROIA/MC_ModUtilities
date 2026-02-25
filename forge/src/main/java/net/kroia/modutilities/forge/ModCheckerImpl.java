package net.kroia.modutilities.forge;

import net.minecraftforge.fml.ModList;

public class ModCheckerImpl {
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
