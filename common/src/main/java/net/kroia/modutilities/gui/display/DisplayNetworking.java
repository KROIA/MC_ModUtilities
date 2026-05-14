package net.kroia.modutilities.gui.display;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.networking.client_server.NetworkPacket;

/**
 * Registers display-related network packets.
 * Must be called during mod initialization before any player can send packets.
 */
public class DisplayNetworking {
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                DisplayInputSyncPacket.TYPE,
                DisplayInputSyncPacket.STREAM_CODEC,
                NetworkPacket.HANDLER::handleServer
        );
    }
}
