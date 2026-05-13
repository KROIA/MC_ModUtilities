package net.kroia.modutilities;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.gui.display.DisplayNetworking;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.kroia.modutilities.networking.client_server.arrs.GenericRequestPacket;
import net.kroia.modutilities.networking.client_server.arrs.GenericResponsePacket;
import net.kroia.modutilities.networking.client_server.streaming.GenericStreamPacket;
import net.kroia.modutilities.networking.client_server.streaming.StreamStartPacket;
import net.kroia.modutilities.networking.client_server.streaming.StreamStopClientSenderPacket;
import net.kroia.modutilities.networking.client_server.streaming.StreamStopServerSenderPacket;
import net.kroia.modutilities.networking.multi_server.MultiServerPacketRegistry;
import net.kroia.modutilities.sandbox.Sandbox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModUtilitiesMod {
    public static final String MOD_ID = "modutilities";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void init()
    {
        registerSharedPackets();
        DisplayNetworking.init();
        Sandbox.init();
    }

    private static void registerSharedPackets() {
        // Stream system — Architectury registration
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, GenericStreamPacket.TYPE,
                GenericStreamPacket.STREAM_CODEC, GenericStreamPacket.HANDLER::handleClient);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, StreamStartPacket.TYPE,
                StreamStartPacket.STREAM_CODEC, StreamStartPacket.HANDLER::handleServer);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, StreamStopClientSenderPacket.TYPE,
                StreamStopClientSenderPacket.STREAM_CODEC, StreamStopClientSenderPacket.HANDLER::handleServer);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, StreamStopServerSenderPacket.TYPE,
                StreamStopServerSenderPacket.STREAM_CODEC, StreamStopServerSenderPacket.HANDLER::handleClient);

        // Stream system — MultiServer forwarding
        MultiServerPacketRegistry.register(GenericStreamPacket.TYPE, GenericStreamPacket.STREAM_CODEC, GenericStreamPacket.HANDLER);
        MultiServerPacketRegistry.register(StreamStartPacket.TYPE, StreamStartPacket.STREAM_CODEC, StreamStartPacket.HANDLER);
        MultiServerPacketRegistry.register(StreamStopClientSenderPacket.TYPE, StreamStopClientSenderPacket.STREAM_CODEC, StreamStopClientSenderPacket.HANDLER);
        MultiServerPacketRegistry.register(StreamStopServerSenderPacket.TYPE, StreamStopServerSenderPacket.STREAM_CODEC, StreamStopServerSenderPacket.HANDLER);

        // ARRS — Architectury registration
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, GenericRequestPacket.TYPE,
                GenericRequestPacket.STREAM_CODEC, NetworkPacket.HANDLER::handleServer);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, GenericResponsePacket.TYPE,
                GenericResponsePacket.STREAM_CODEC, NetworkPacket.HANDLER::handleClient);

        // ARRS — MultiServer forwarding
        MultiServerPacketRegistry.register(GenericRequestPacket.TYPE, GenericRequestPacket.STREAM_CODEC);
        MultiServerPacketRegistry.register(GenericResponsePacket.TYPE, GenericResponsePacket.STREAM_CODEC);
    }

    public static boolean isClientInitialized() {
        return UtilitiesPlatform.isPlatformSet();
    }

    public static boolean isServerInitialized() {
        if(!UtilitiesPlatform.isPlatformSet())
            return false;
        return UtilitiesPlatform.getServer() != null;
    }
}
