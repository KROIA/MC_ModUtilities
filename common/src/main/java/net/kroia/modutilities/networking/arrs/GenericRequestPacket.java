package net.kroia.modutilities.networking.arrs;


import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.NetworkPacket;
import net.minecraft.network.FriendlyByteBuf;
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

    public GenericRequestPacket(FriendlyByteBuf buf) {
        super(buf);
    }
    public GenericRequestPacket(String requestTypeID, FriendlyByteBuf data) {
        super();
        this.requestID = UUID.randomUUID();
        this.requestTypeID = requestTypeID;
        this.data = data;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(requestID);
        buf.writeUtf(requestTypeID);
        byte[] bytes = Arrays.copyOf(data.asByteBuf().array(), data.asByteBuf().readableBytes());
        buf.writeBytes(bytes);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        this.requestID = buf.readUUID();
        this.requestTypeID = buf.readUtf();

        int length = buf.readableBytes();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        this.data = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(bytes));
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
    protected void handleOnClient() {
        var request = AsynchronousRequestResponseSystem.getRegisteredRequest(requestTypeID);
        if (request == null) {
            return; // No factory found for this request type
        }

        FriendlyByteBuf responseData = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            request.decodeHandleEncodeOnClient(data, responseData);
        }
        catch (Exception e) {
            // Handle any exceptions that may occur during decoding/encoding
            ModUtilitiesMod.LOGGER.error("Error handling GenericRequestPacket: " + e.getMessage(), e);
            return; // Exit if an error occurs
        }

        sendResponse(new GenericResponsePacket(requestID, requestTypeID, responseData));
    }

    @Override
    protected void handleOnServer(ServerPlayer sender) {
        var request = AsynchronousRequestResponseSystem.getRegisteredRequest(requestTypeID);
        if (request == null) {
            return; // No factory found for this request type
        }

        FriendlyByteBuf responseData = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            request.decodeHandleEncodeOnServer(data, responseData, sender);
        }
        catch (Exception e) {
            // Handle any exceptions that may occur during decoding/encoding
            ModUtilitiesMod.LOGGER.error("Error handling GenericRequestPacket: " + e.getMessage(), e);
            return; // Exit if an error occurs
        }

        sendResponse(new GenericResponsePacket(requestID, requestTypeID, responseData));
    }
}