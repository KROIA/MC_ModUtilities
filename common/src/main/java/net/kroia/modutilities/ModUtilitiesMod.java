package net.kroia.modutilities;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModUtilitiesMod {
    public static final String MOD_ID = "modutilities";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);


    public static void init()
    {

    }

    public static boolean isClientInitialized() {
        return UtilitiesPlatform.getPlatform() != null;
    }
    public static boolean isServerInitialized() {
        if(UtilitiesPlatform.getPlatform() != null) {
            return UtilitiesPlatform.getServer() != null;
        }
        return false;
    }

}
