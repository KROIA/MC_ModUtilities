package net.kroia.modutilities.networking.arrs;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.NetworkPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.UUID;

/**
 * This Class is used by the "asynchronous request-response system" (ARRS).
 * Packet used for responding to requests
 */
public final class GenericResponsePacket extends NetworkPacket
{
    /**
     * The ID of the request this specific request response is for.
     */
    UUID requestID;

    /**
     * The type ID of the request this specific request response is for.
     * This is used to identify the type of request object
     */
    String requestTypeID;

    /**
     * The data of the response.
     * This is a byte buffer that contains the serialized data of the request response.
     */
    FriendlyByteBuf data;

    public GenericResponsePacket(FriendlyByteBuf buf) {
        super(buf);
    }

    public GenericResponsePacket(UUID requestID, String requestTypeID, FriendlyByteBuf data) {
        super();
        this.requestID = requestID;
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
        try{
        AsynchronousRequestResponseSystem.processResponseOnClient(this);
        }
        catch (Exception e) {
            // Handle any exceptions that may occur during decoding/encoding
            ModUtilitiesMod.LOGGER.error("Error processing GenericResponsePacket on client: " + e.getMessage(), e);
            return; // Exit if an error occurs
        }
    }

    @Override
    protected void handleOnServer(ServerPlayer sender) {
        try {
            AsynchronousRequestResponseSystem.processResponseOnServer(this, sender);
        }
        catch (Exception e) {
            // Handle any exceptions that may occur during decoding/encoding
            ModUtilitiesMod.LOGGER.error("Error processing GenericResponsePacket on server: " + e.getMessage(), e);
            return; // Exit if an error occurs
        }
    }
}
