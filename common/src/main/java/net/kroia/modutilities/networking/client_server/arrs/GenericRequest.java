package net.kroia.modutilities.networking.client_server.arrs;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * GenericRequest is an abstract class that defines a request with input and output types.
 * It provides methods to handle requests on the client and server sides, encode and decode inputs and outputs.
 *
 * @param <IN>  The type of input provided by the requestor.
 * @param <OUT> The type of output produced by the responder.
 */
public abstract class GenericRequest<IN, OUT>
{
    /**
     * The RequestManager that manages this request.
     * This is set by the AsynchronousRequestResponseSystem when the request is registered.
     * The manager inside this object is used to be able to send the request directly from the request object.
     */
    private RequestManager manager;


    /**
     * Returns the unique identifier for the request type.
     * This ID must be unique across all requests.
     * @apiNote
     * This methode gets called serverside and clientside.
     *
     * @return A string representing the request type ID.
     */
    public abstract String getRequestTypeID();



    public boolean needsRoutingToMaster() { return false; }


    /**
     * Handles the request on the client side.
     * This method is called when the request is processed on the client. (Server is requestor)
     *
     * @param input The input provided by the requestor.
     *
     * @apiNote
     * This methode gets called clientside only. Do not use server side calls in this method.
     *
     * @return The output produced by the responder.
     */
    //public CompletableFuture<OUT> handleOnClient(IN input)  { throw new AssertionError("handleOnClient() is not implemented in" + this.getRequestTypeID() + ". Please implement this method to handle the request on the client side."); }

    /**
     * Handles the request on the server side.
     * This method is called when the request is processed on the server. (Client is requestor)
     *
     * @param input  The input provided by the requestor.
     * @param sender The player who sent the request.
     *
     * @apiNote
     * This methode gets called serverside only. Do not use client side calls in this method.
     *
     * @return The output produced by the responder.
     */
    public CompletableFuture<OUT> handleOnServer(IN input, ServerPlayer sender) {
        throw new AssertionError("handleOnServer() is not implemented in " + this.getRequestTypeID() + ". Please implement this method to handle the request on the server side.");
    }
    public CompletableFuture<OUT> handleOnMasterServer(IN input, @Nullable UUID playerSender) {
        throw new AssertionError("handleOnMasterServer() is not implemented in " + this.getRequestTypeID() + ". Please implement this method to handle the request on the server side.");
    }


    /**
     * Encodes the input into a byte buffer for transmission.
     * This method is used to serialize the input data for network transmission.
     *
     * @param buf   The byte buffer to encode the input into.
     * @param input The input to encode.
     *
     * @apiNote
     * This methode gets called serverside and clientside.
     */
    public abstract void encodeInput(RegistryFriendlyByteBuf buf, IN input);

    /**
     * Encodes the output into a byte buffer for transmission.
     * This method is used to serialize the output data for network transmission.
     *
     * @param buf    The byte buffer to encode the output into.
     * @param output The output to encode.
     *
     * @apiNote
     * This methode gets called serverside and clientside.
     */
    public abstract void encodeOutput(RegistryFriendlyByteBuf buf, OUT output);


    /**
     * Decodes the input from a byte buffer.
     * This method is used to deserialize the input data received from the network.
     *
     * @param buf The byte buffer to decode the input from.
     *
     * @apiNote
     * This methode gets called serverside and clientside.
     *
     * @return The decoded input.
     */
    public abstract IN decodeInput(RegistryFriendlyByteBuf buf);

    /**
     * Decodes the output from a byte buffer.
     * This method is used to deserialize the output data received from the network.
     *
     * @param buf The byte buffer to decode the output from.
     *
     * @apiNote
     * This methode gets called serverside and clientside.
     *
     * @return The decoded output.
     */
    public abstract OUT decodeOutput(RegistryFriendlyByteBuf buf);


    /**
     * Sends this request to the server with the provided input and a response handler.
     * The response handler will be called with the output once the server responds.
     *
     * @param input           The input to send to the server.
     *
     * @return                CompletableFuture object containing the response data
     *
     * @apiNote
     * This methode can be called manually on the clientside only (when the client is the requestor).
     */
    public CompletableFuture<OUT> sendRequestToServer(IN input)
    {
        if(manager == null)
            throw new IllegalStateException("""
                    RequestManager is not set. Cannot send request to server.
                    Make sure to setup the ARRS correctly.
                    Call AsynchronousRequestResponseSystem.setup() once on the client and server side.
                    Also register this Request class with
                    AsynchronousRequestResponseSystem.register(new GenericRequestType());
                    once on the client and server side.
                    """);

        return manager.sendRequestToServer(this, input);
    }

    public CompletableFuture<OUT> sendRequestToMaster(IN input)
    {
        if(manager == null)
            throw new IllegalStateException("""
                    RequestManager is not set. Cannot send request to server.
                    Make sure to setup the ARRS correctly.
                    Call AsynchronousRequestResponseSystem.setup() once on the client and server side.
                    Also register this Request class with
                    AsynchronousRequestResponseSystem.register(new GenericRequestType());
                    once on the client and server side.
                    """);

        return manager.sendRequestToMaster(this, input);
    }


    /**
     * Sends this request to a specific client with the provided input and a response handler.
     * The response handler will be called with the output once the client responds.
     *
     * @param input           The input to send to the client.
     * @param receiver        The player who will receive the request.
     * @param responseHandler The handler to call with the output once received.
     *
     * @apiNote
     * This methode can be called manually on the serverside only (when the server is the requestor).
     */
    /*public void sendRequestToClient(
            IN input,
            @NotNull ServerPlayer receiver,
            @NotNull BiConsumer<OUT, ServerPlayer> responseHandler)
    {
        if(manager == null)
            throw new IllegalStateException("""
                    RequestManager is not set. Cannot send request to client.
                    Make sure to setup the ARRS correctly.
                    Call AsynchronousRequestResponseSystem.setup() once on the client and server side.
                    Also register this Request class with
                    AsynchronousRequestResponseSystem.register(new GenericRequestType());
                    once on the client and server side.
                    """);

        manager.sendRequestToClient(this, input, receiver, responseHandler);
    }*/


    /*
     * ----------------------------------------------------------------------------------------------------
     *
     *      I N T E R N A L   M E T H O D S
     *
     * ----------------------------------------------------------------------------------------------------
     */


    /**
     * Processes the request on the server side and encodes the response data into the output buffer.
     *
     * @param inputBuf  The byte buffer to encode the input into.
     * @param outputBuf The byte buffer to encode the output into.
     * @param sender    The player who sent the request (for server-side handling).
     *
     * @apiNote
     * This methode gets called serverside only (when the client is the requestor).
     * This function is called by the ARRS (do not call this method manually).
     */
    public CompletableFuture<RegistryFriendlyByteBuf> decodeHandleEncodeOnServer(RegistryFriendlyByteBuf inputBuf, RegistryFriendlyByteBuf outputBuf, ServerPlayer sender)
    {
        IN input = decodeInput(inputBuf);
        CompletableFuture<OUT> output = handleOnServer(input, sender);
        CompletableFuture<RegistryFriendlyByteBuf> byteBufFut = new CompletableFuture<>();
        output.thenAccept(responseData -> {
            encodeOutput(outputBuf, responseData);
            byteBufFut.complete(outputBuf);
        });
        return byteBufFut;
    }

    public CompletableFuture<RegistryFriendlyByteBuf> decodeHandleEncodeOnMasterServer(RegistryFriendlyByteBuf inputBuf, RegistryFriendlyByteBuf outputBuf, @Nullable UUID playerSender)
    {
        IN input = decodeInput(inputBuf);
        CompletableFuture<OUT> output = handleOnMasterServer(input, playerSender);
        CompletableFuture<RegistryFriendlyByteBuf> byteBufFut = new CompletableFuture<>();
        output.thenAccept(responseData -> {
            encodeOutput(outputBuf, responseData);
            byteBufFut.complete(outputBuf);
        });
        return byteBufFut;
    }

    /**
     * Processes the request on the client side and encodes the response data into the output buffer.
     *
     * @param inputBuf  The byte buffer to encode the input into.
     * @param outputBuf The byte buffer to encode the output into.
     *
     * @apiNote
     * This methode gets called clientside only (when the server is the requestor).
     * This function is called by the ARRS (do not call this method manually).
     */
    /*public CompletableFuture<RegistryFriendlyByteBuf> decodeHandleEncodeOnClient(RegistryFriendlyByteBuf inputBuf, RegistryFriendlyByteBuf outputBuf)
    {
        IN input = decodeInput(inputBuf);
        CompletableFuture<OUT> output = handleOnClient(input);
        CompletableFuture<RegistryFriendlyByteBuf> byteBufFut = new CompletableFuture<>();
        output.thenAccept(responseData -> {
            encodeOutput(outputBuf, responseData);
            byteBufFut.complete(outputBuf);
                });
        return byteBufFut;
    }*/




    /**
     * Sets the RequestManager for this request.
     * This method is called by the AsynchronousRequestResponseSystem when the request is registered.
     *
     * @param manager The RequestManager to set.
     */
    public void setManager(RequestManager manager)
    {
        this.manager = manager;
    }
    public RequestManager getManager()
    {
        return this.manager;
    }

    @Override
    public String toString() {
        return "GenericRequest{" +
                "requestTypeID='" + getRequestTypeID() + '\'' +
                '}';
    }
}