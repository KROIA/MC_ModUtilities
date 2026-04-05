package net.kroia.modutilities.networking.client_server.streaming;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.kroia.modutilities.networking.client_server.PacketHandler;
import net.kroia.modutilities.networking.server_server.ForwardPacketContext;
import net.kroia.modutilities.networking.server_server.ForwardPacketHandler;
import net.kroia.modutilities.networking.server_server.ServerServerManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;


/**
 * This Class is used by the "streaming system".
 * This packet is used to start a stream from client to server or vice versa.
 * It contains a stream ID, a stream type ID, and the context data associated with the stream.
 */
public class StreamStartPacket extends NetworkPacket {

    public static final Type<StreamStartPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "stream_start_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StreamStartPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, p -> p.streamID,
            ByteBufCodecs.STRING_UTF8, p -> p.streamTypeID,
            ExtraCodecUtils.REGISTRY_FRIENDLY_BYTE_BUF_CODEC, p -> p.data,
            StreamStartPacket::new

    );

    public static class StreamStartPacketHandler implements
            PacketHandler<StreamStartPacket>,
            ForwardPacketHandler<StreamStartPacket>
    {

        @Override
        public void handleServer(StreamStartPacket packet, NetworkManager.PacketContext context) {
            packet.handleOnServer(context);
        }

        @Override
        public void handleClient(StreamStartPacket packet, NetworkManager.PacketContext context) {
            packet.handleOnClient(context);
        }

        @Override
        public void handleMaster(StreamStartPacket packet, ForwardPacketContext context) {
            packet.handleOnMaster(context);
        }

        @Override
        public void handleSlave(StreamStartPacket packet, ForwardPacketContext context) {
            packet.handleOnSlave(context);
        }
    }

    public static final StreamStartPacketHandler HANDLER = new StreamStartPacketHandler();



    UUID streamID;
    String streamTypeID;
    RegistryFriendlyByteBuf data;



    public StreamStartPacket(String streamTypeID, RegistryFriendlyByteBuf data) {
        super();
        this.streamID = UUID.randomUUID();
        this.streamTypeID = streamTypeID;
        this.data = data;
    }

    public StreamStartPacket(UUID streamID, String streamTypeID, RegistryFriendlyByteBuf data) {
        super();
        this.streamID = streamID;
        this.streamTypeID = streamTypeID;
        this.data = data;
    }


    public UUID getStreamID() {
        return streamID;
    }
    public String getStreamTypeID() {
        return streamTypeID;
    }
    public RegistryFriendlyByteBuf getData() {
        return data;
    }



    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    protected void handleOnClient(NetworkManager.PacketContext context) {
        //StreamSystem.handlePacket(this);
    }

    @Override
    protected void handleOnServer(NetworkManager.PacketContext context) {
        StreamSystem.handlePacket(this, null, context.getPlayer().getUUID());
    }

    @Override
    protected void handleOnMaster(ForwardPacketContext context) {
        StreamSystem.handlePacket(this, context.senderServerID, context.senderPlayerUUID);
    }

    @Override
    protected void handleOnSlave(ForwardPacketContext context) {

    }
}
