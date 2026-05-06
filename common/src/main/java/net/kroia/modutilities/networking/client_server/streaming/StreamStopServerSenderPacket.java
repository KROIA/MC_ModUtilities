package net.kroia.modutilities.networking.client_server.streaming;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.kroia.modutilities.networking.client_server.PacketHandler;
import net.kroia.modutilities.networking.multi_server.ForwardPacketContext;
import net.kroia.modutilities.networking.multi_server.ForwardPacketHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * This Class is used by the "streaming system".
 * The packet is sent from the server to the client to notify that a server-sender stream has been stopped.
 */
public class StreamStopServerSenderPacket  extends NetworkPacket {
    public static final Type<StreamStopServerSenderPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "stream_stop_server_sender_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StreamStopServerSenderPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, p -> p.streamID,
            StreamStopServerSenderPacket::new
    );

    public static class StreamStopServerSenderPacketHandler implements
            PacketHandler<StreamStopServerSenderPacket>,
            ForwardPacketHandler<StreamStopServerSenderPacket>
    {

        @Override
        public void handleServer(StreamStopServerSenderPacket packet, NetworkManager.PacketContext context) {
            packet.handleOnServer(context);
        }

        @Override
        public void handleClient(StreamStopServerSenderPacket packet, NetworkManager.PacketContext context) {
            packet.handleOnClient(context);
        }

        @Override
        public void handleMaster(StreamStopServerSenderPacket packet, ForwardPacketContext context) {
            packet.handleOnMaster(context);
        }

        @Override
        public void handleSlave(StreamStopServerSenderPacket packet, ForwardPacketContext context) {
            packet.handleOnSlave(context);
        }
    }

    public static final StreamStopServerSenderPacketHandler HANDLER = new StreamStopServerSenderPacketHandler();

    UUID streamID;

    /**
     * Creates a new StreamStopServerSenderPacket signaling that the server-side sender has stopped the stream.
     *
     * @param streamID The unique stream UUID identifying the stream to stop.
     */
    public StreamStopServerSenderPacket(UUID streamID) {
        super();
        this.streamID = streamID;
    }


    /**
     * @return The unique stream UUID identifying which stream is being stopped.
     */
    public UUID getStreamID() {
        return streamID;
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    protected void handleOnClient(NetworkManager.PacketContext context) {
        StreamSystem.handlePacketOnClient(this);
    }

    @Override
    protected void handleOnServer(NetworkManager.PacketContext context) {
        //StreamSystem.handlePacket(packet, (ServerPlayer) context.getPlayer());
    }

    @Override
    protected void handleOnMaster(ForwardPacketContext context) {

    }

    @Override
    protected void handleOnSlave(ForwardPacketContext context) {
        StreamSystem.handleRedirectedPacket(this);
    }
}
