package net.kroia.modutilities;

import net.kroia.modutilities.sandbox.Sandbox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main entry point for the MC_ModUtilities library.
 * <p>
 * Holds shared constants ({@link #MOD_ID}, {@link #LOGGER}) and the cross-platform
 * initialization hook invoked from each platform module's mod entry point.
 */
public class ModUtilitiesMod {
    /** The mod ID used for resource locations, tag namespaces, and logging. */
    public static final String MOD_ID = "modutilities";

    /** Shared Log4j logger for the mod, named after {@link #MOD_ID}. */
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);


    /**
     * Performs the common initialization shared by every platform module.
     * <p>
     * Called once per JVM from the platform-specific mod entry points.
     */
    public static void init()
    {
        Sandbox.init();
    }

    /*public static void onClientSetup()
    {
        // This method is called when the client is initialized
        // You can put client-side initialization code here
        Sandbox.initClient();
    }*/

    /**
     * Indicates whether the client-side initialization has completed and the platform
     * abstraction is available.
     *
     * @return {@code true} once a {@link PlatformAbstraction} has been registered
     */
    public static boolean isClientInitialized() {
        return UtilitiesPlatform.isPlatformSet();
    }
    /**
     * Indicates whether the server side is ready: the platform abstraction is registered
     * and a {@link net.minecraft.server.MinecraftServer} instance is currently available.
     * <p>
     * Returns {@code false} (rather than throwing) if the platform has not been set or the
     * server reference is unavailable, making this safe to call in early init or shutdown.
     *
     * @return {@code true} if the server is fully initialized and reachable
     */
    public static boolean isServerInitialized() {
        if(!UtilitiesPlatform.isPlatformSet())
            return false;
        return UtilitiesPlatform.getServer() != null;
    }

}
