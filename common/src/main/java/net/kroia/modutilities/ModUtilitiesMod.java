package net.kroia.modutilities;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
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


    // Set to false for release builds to hide dev-only commands (exportrecipes, testScreen, etc.)
    public static final boolean ENABLE_DEV_FEATURES = false;

    public static void init()
    {
        registerSharedPackets();
        DisplayNetworking.init();
        if (ENABLE_DEV_FEATURES) {
            Sandbox.init();
        }
    }

    private static void registerSharedPackets() {
        final boolean isClient = Platform.getEnvironment() == Env.CLIENT;

        // Stream system — Architectury registration.
        //
        // On the client, registerReceiver(Side.S2C, ...) also registers the payload TYPE via
        // Fabric's PayloadTypeRegistry, so pre-calling registerS2CPayloadType there would double-
        // register and crash. On the dedicated server the client-only adaptor path is stripped
        // (@Environment(EnvType.CLIENT) in architectury-fabric 13.0.8), so we register just the
        // TYPE so the server can still encode outgoing S2C packets.
        if (isClient) {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, GenericStreamPacket.TYPE,
                    GenericStreamPacket.STREAM_CODEC, GenericStreamPacket.HANDLER::handleClient);
        } else {
            NetworkManager.registerS2CPayloadType(GenericStreamPacket.TYPE, GenericStreamPacket.STREAM_CODEC);
        }
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, StreamStartPacket.TYPE,
                StreamStartPacket.STREAM_CODEC, StreamStartPacket.HANDLER::handleServer);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, StreamStopClientSenderPacket.TYPE,
                StreamStopClientSenderPacket.STREAM_CODEC, StreamStopClientSenderPacket.HANDLER::handleServer);
        if (isClient) {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, StreamStopServerSenderPacket.TYPE,
                    StreamStopServerSenderPacket.STREAM_CODEC, StreamStopServerSenderPacket.HANDLER::handleClient);
        } else {
            NetworkManager.registerS2CPayloadType(StreamStopServerSenderPacket.TYPE, StreamStopServerSenderPacket.STREAM_CODEC);
        }

        // Stream system — MultiServer forwarding
        MultiServerPacketRegistry.register(GenericStreamPacket.TYPE, GenericStreamPacket.STREAM_CODEC, GenericStreamPacket.HANDLER);
        MultiServerPacketRegistry.register(StreamStartPacket.TYPE, StreamStartPacket.STREAM_CODEC, StreamStartPacket.HANDLER);
        MultiServerPacketRegistry.register(StreamStopClientSenderPacket.TYPE, StreamStopClientSenderPacket.STREAM_CODEC, StreamStopClientSenderPacket.HANDLER);
        MultiServerPacketRegistry.register(StreamStopServerSenderPacket.TYPE, StreamStopServerSenderPacket.STREAM_CODEC, StreamStopServerSenderPacket.HANDLER);

        // ARRS — Architectury registration
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, GenericRequestPacket.TYPE,
                GenericRequestPacket.STREAM_CODEC, NetworkPacket.HANDLER::handleServer);
        if (isClient) {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, GenericResponsePacket.TYPE,
                    GenericResponsePacket.STREAM_CODEC, NetworkPacket.HANDLER::handleClient);
        } else {
            NetworkManager.registerS2CPayloadType(GenericResponsePacket.TYPE, GenericResponsePacket.STREAM_CODEC);
        }

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
