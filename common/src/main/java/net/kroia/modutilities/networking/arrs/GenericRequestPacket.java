package net.kroia.modutilities.networking.arrs;


import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.modutilities.networking.NetworkPacket;
import net.kroia.modutilities.networking.PacketHandler;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.UUID;

/**
 * This Class is used by the "asynchronous request-response system" (ARRS).
 * This packet is used to send a generic request from client to server or vice versa.
 * It contains a request ID, a request type ID, and the data associated with the request.
 * The request type ID is used to determine how to handle the request on the server or client side.
 */
public final class GenericRequestPacket extends NetworkPacket
{

    public static final Type<GenericRequestPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "generic_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GenericRequestPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, p -> p.requestID,
            ByteBufCodecs.STRING_UTF8, p -> p.requestTypeID,
            ExtraCodecUtils.FRIENDLY_BYTE_BUF_CODEC, p -> p.data,
            GenericRequestPacket::new
    );

    public static final PacketHandler<GenericRequestPacket> HANDLER = new PacketHandler<>(){

        @Override
        public void handleServer(GenericRequestPacket packet, NetworkManager.PacketContext context) {
            var request = AsynchronousRequestResponseSystem.getRegisteredRequest(packet.requestTypeID);
            if (request == null) {
                return; // No factory found for this request type
            }

            FriendlyByteBuf responseData = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                request.decodeHandleEncodeOnServer(packet.data, responseData, (ServerPlayer) context.getPlayer());
            }
            catch (Exception e) {
                // Handle any exceptions that may occur during decoding/encoding
                ModUtilitiesMod.LOGGER.error("Error handling GenericRequestPacket: " + e.getMessage(), e);
                return; // Exit if an error occurs
            }
            //sendResponse(new GenericResponsePacket(packet.requestID, packet.requestTypeID, responseData));
        }

        @Override
        public void handleClient(GenericRequestPacket packet, NetworkManager.PacketContext context) {
            var request = AsynchronousRequestResponseSystem.getRegisteredRequest(packet.requestTypeID);
            if (request == null) {
                return; // No factory found for this request type
            }

            FriendlyByteBuf responseData = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                request.decodeHandleEncodeOnClient(packet.data, responseData);
            }
            catch (Exception e) {
                // Handle any exceptions that may occur during decoding/encoding
                ModUtilitiesMod.LOGGER.error("Error handling GenericRequestPacket: " + e.getMessage(), e);
                return; // Exit if an error occurs
            }

            //sendResponse(new GenericResponsePacket(packet.requestID, packet.requestTypeID, responseData));
        }
    };

    /**
     * Unique identifier for the request.
     * This is used to match requests with their responses.
     */
    UUID requestID;

    /**
     * Identifier for the type of request.
     * This is used to determine how to handle the request on the server or client side.
     */
    String requestTypeID;

    /**
     * Data associated with the request.
     * This is a byte buffer that contains the data to be sent with the request.
     */
    FriendlyByteBuf data;

    public GenericRequestPacket(String requestTypeID, FriendlyByteBuf data) {
        super();
        this.requestID = UUID.randomUUID();
        this.requestTypeID = requestTypeID;
        this.data = data;
    }

    public GenericRequestPacket(UUID requestID, String requestTypeID, FriendlyByteBuf data) {
        this.requestID = requestID;
        this.requestTypeID = requestTypeID;
        this.data = data;
    }


    public UUID getRequestID() {
        return requestID;
    }
    public String getRequestTypeID() {
        return requestTypeID;
    }
    public FriendlyByteBuf getData() {
        return data;
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}