package net.kroia.modutilities;

import dev.architectury.injectables.annotations.ExpectPlatform;

/**
 * Cross-platform helper for checking whether a mod is currently loaded.
 * <p>
 * The actual lookup is delegated to a platform-specific implementation through Architectury's
 * {@link ExpectPlatform} mechanism. Implementations live under
 * {@code fabric/.../fabric/ModCheckerImpl} and {@code neoforge/.../neoforge/ModCheckerImpl}.
 */
public class ModChecker {
    /**
     * Checks whether a mod with the given ID is currently loaded on this platform.
     *
     * @param modId the mod ID to check (e.g. {@code "modutilities"})
     * @return {@code true} if the mod is loaded, {@code false} otherwise
     */
    @ExpectPlatform
    public static boolean isModLoaded(String modId) {
        throw new AssertionError();
    }
}
