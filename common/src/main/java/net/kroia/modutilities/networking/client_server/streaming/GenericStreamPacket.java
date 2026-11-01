package net.kroia.modutilities.networking.client_server.streaming;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.ExtraCodecUtils;
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
 * This packet is used to send a data chunk from client to server or vice versa.
 * It contains the stream ID and the data associated with the stream.
 */
public class GenericStreamPacket extends NetworkPacket {


    public static final Type<GenericStreamPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "generic_stream_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GenericStreamPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, p -> p.streamID,
            ExtraCodecUtils.REGISTRY_FRIENDLY_BYTE_BUF_CODEC, p -> p.data,
            GenericStreamPacket::new

    );

    UUID streamID;
    RegistryFriendlyByteBuf data;


    public static class GenericStreamPacketHandler implements
            PacketHandler<GenericStreamPacket>,
            ForwardPacketHandler<GenericStreamPacket>
    {

        @Override
        public void handleServer(GenericStreamPacket packet, NetworkManager.PacketContext context) {
            packet.handleOnServer(context);
        }

        @Override
        public void handleClient(GenericStreamPacket packet, NetworkManager.PacketContext context) {
            packet.handleOnClient(context);
        }

        @Override
        public void handleMaster(GenericStreamPacket packet, ForwardPacketContext context) {
            packet.handleOnMaster(context);
        }

        @Override
        public void handleSlave(GenericStreamPacket packet, ForwardPacketContext context) {
            packet.handleOnSlave(context);
        }
    }

    public static final GenericStreamPacketHandler HANDLER = new GenericStreamPacketHandler();



    /**
     * Creates a new GenericStreamPacket carrying a chunk of stream data.
     *
     * @param streamID The unique stream UUID identifying the stream this packet belongs to.
     * @param data     The encoded payload of the stream data chunk.
     */
    public GenericStreamPacket(UUID streamID, RegistryFriendlyByteBuf data) {
        super();
        this.streamID = streamID;
        this.data = data;
    }


    /**
     * @return The unique stream UUID identifying which stream instance this packet belongs to.
     */
    public UUID getStreamID() {
        return streamID;
    }

    /**
     * @return The encoded payload buffer of this stream data chunk.
     */
    public RegistryFriendlyByteBuf getData() {
        return data;
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
        //StreamSystem.handlePacket(this, (ServerPlayer) context.getPlayer());
    }

    @Override
    protected void handleOnMaster(ForwardPacketContext context) {

    }

    @Override
    protected void handleOnSlave(ForwardPacketContext context) {
        StreamSystem.handleRedirectedPacket(this);
    }
}
