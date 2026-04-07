package net.kroia.modutilities.networking.client_server.arrs.requestholder;

import net.kroia.modutilities.networking.client_server.arrs.GenericRequest;
import net.kroia.modutilities.networking.client_server.arrs.GenericRequestPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.concurrent.CompletableFuture;

/**
 * This is an SRRS internal class and is not used for public API.
 * @param <IN> the type of input data for the request
 * @param <OUT> the type of output data for the request
 */
public class ServerRequestHolder<IN, OUT>
{
    public CompletableFuture<OUT> responseFuture;
    public GenericRequestPacket requestPacket;
    public GenericRequest<IN, OUT> request;
    public void processResponse(RegistryFriendlyByteBuf buf)
    {
        OUT response = request.decodeOutput(buf);
        if(responseFuture != null)
        {
            responseFuture.complete(response);
        }
    }
}