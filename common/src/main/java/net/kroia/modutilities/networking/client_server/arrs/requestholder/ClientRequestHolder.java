package net.kroia.modutilities.networking.client_server.arrs.requestholder;

import net.kroia.modutilities.networking.client_server.arrs.GenericRequest;
import net.kroia.modutilities.networking.client_server.arrs.GenericRequestPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

/**
 * This is an SRRS internal class and is not used for public API.
 * @param <IN> the type of input data for the request
 * @param <OUT> the type of output data for the request
 */
public class ClientRequestHolder<IN, OUT>
{
    public BiConsumer<OUT, ServerPlayer> responseHandler;
    public GenericRequestPacket requestPacket;
    public GenericRequest<IN, OUT> request;
    public final long creationTimeMs = System.currentTimeMillis();
    public void processResponse(RegistryFriendlyByteBuf buf, ServerPlayer player)
    {
        // Decode the response using the request's decodeOutput method
        OUT response = request.decodeOutput(buf);
        if(responseHandler != null)
        {
            // Pass the response and player to the handler
            responseHandler.accept(response, player);
        }
    }
}