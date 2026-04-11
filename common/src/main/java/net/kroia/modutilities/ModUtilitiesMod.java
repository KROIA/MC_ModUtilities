package net.kroia.modutilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModUtilitiesMod {
    public static final String MOD_ID = "modutilities";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);


    public static void init()
    {

    }

    /*public static void onClientSetup()
    {
        // This method is called when the client is initialized
        // You can put client-side initialization code here
        Sandbox.initClient();
    }*/

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
