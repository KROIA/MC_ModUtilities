package net.kroia.modutilities.networking.client_server;


import dev.architectury.networking.NetworkManager;

/**
 * Stateless functional interface for routing {@link NetworkPacket} instances to their server-side
 * or client-side handler. Implementations are typically registered with
 * {@link net.kroia.modutilities.networking.NetworkPacketManager} when registering a packet type.
 *
 * @param <T> the concrete {@link NetworkPacket} subtype this handler accepts.
 */
public interface PacketHandler<T extends NetworkPacket> {

    /**
     * Handles a packet that has been received on the server side.
     *
     * @param packet  the received packet instance.
     * @param context the Architectury packet context, including the sending player.
     */
    void handleServer(T packet, NetworkManager.PacketContext context);

    /**
     * Handles a packet that has been received on the client side.
     *
     * @param packet  the received packet instance.
     * @param context the Architectury packet context for the receiving client.
     */
    void handleClient(T packet,  NetworkManager.PacketContext context);




}
