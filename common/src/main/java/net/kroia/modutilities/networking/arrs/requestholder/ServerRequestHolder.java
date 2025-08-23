package net.kroia.modutilities.networking.arrs.requestholder;

import net.kroia.modutilities.networking.arrs.GenericRequest;
import net.kroia.modutilities.networking.arrs.GenericRequestPacket;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Consumer;

/**
 * This is an SRRS internal class and is not used for public API.
 * @param <IN> the type of input data for the request
 * @param <OUT> the type of output data for the request
 */
public class ServerRequestHolder<IN, OUT>
{
    public Consumer<OUT> responseHandler;
    public GenericRequestPacket requestPacket;
    public GenericRequest<IN, OUT> request;
    public void processResponse(FriendlyByteBuf buf)
    {
        OUT response = request.decodeOutput(buf);
        if(responseHandler != null)
        {
            responseHandler.accept(response);
        }
    }
}