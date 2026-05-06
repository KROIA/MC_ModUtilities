package net.kroia.modutilities;

import net.kroia.modutilities.sandbox.Sandbox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModUtilitiesMod {
    public static final String MOD_ID = "modutilities";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);


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

    public static boolean isClientInitialized() {
        return UtilitiesPlatform.isPlatformSet();
    }
    public static boolean isServerInitialized() {
        if(!UtilitiesPlatform.isPlatformSet())
            return false;
        try {
            return UtilitiesPlatform.getServer() != null;
        } catch (IllegalStateException e) {
            return false;
        }
    }

}
