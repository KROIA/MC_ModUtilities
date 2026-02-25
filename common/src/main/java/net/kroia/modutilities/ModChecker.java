package net.kroia.modutilities;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class ModChecker {
    @ExpectPlatform
    public static boolean isModLoaded(String modId) {
        throw new AssertionError();
    }
}
