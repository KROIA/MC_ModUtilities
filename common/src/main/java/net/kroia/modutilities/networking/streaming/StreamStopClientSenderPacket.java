package net.kroia.modutilities.networking.streaming;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.NetworkPacket;
import net.kroia.modutilities.networking.PacketHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * This Class is used by the "streaming system".
 * The packet is used to stop the stream or to notify the other side that the stream has stopped.
 */
public class StreamStopClientSenderPacket extends NetworkPacket {
    public static final Type<StreamStopClientSenderPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "stream_stop_client_sender_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StreamStopClientSenderPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, p -> p.streamID,
            StreamStopClientSenderPacket::new
    );

    public static final PacketHandler<StreamStopClientSenderPacket> HANDLER = new PacketHandler<>(){

        @Override
        public void handleServer(StreamStopClientSenderPacket packet, NetworkManager.PacketContext context) {
            StreamSystem.handlePacket(packet, (ServerPlayer) context.getPlayer());
        }

        @Override
        public void handleClient(StreamStopClientSenderPacket packet, NetworkManager.PacketContext context) {
            //StreamSystem.handlePacket(packet);
        }
    };

    UUID streamID;

    public StreamStopClientSenderPacket(UUID streamID) {
        super();
        this.streamID = streamID;
    }


    public UUID getStreamID() {
        return streamID;
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
