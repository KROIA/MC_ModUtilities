package net.kroia.modutilities.networking.multi_server;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Handler interface for packets that are forwarded between Master and Slave
 * servers in the multi-server topology.
 * <p>
 * Implementations decide what happens when a forward-relay packet of type {@code T}
 * arrives on either side of the link. The same handler instance is registered for
 * both directions; the appropriate method is invoked depending on which side of the
 * connection received the payload.
 *
 * @param <T> The concrete {@link CustomPacketPayload} type this handler accepts.
 *
 * @apiNote
 * Although the Netty I/O thread initially receives the packet, {@link MultiServerPacketRegistry}
 * dispatches the handler call to the server main thread via {@code server.execute()}.
 * Implementations may safely access and mutate Minecraft game state.
 */
public interface ForwardPacketHandler<T extends CustomPacketPayload> {
    /**
     * Called on the Master server when a forwarded packet of type {@code T} is received
     * from a connected Slave.
     *
     * @param packet  The decoded payload that arrived from the slave.
     * @param context The forwarding context containing sender server ID, optional
     *                player UUID, and the underlying Netty channel context.
     *
     * @apiNote
     * Dispatched to the server main thread. Safe to access game state.
     */
    void handleMaster(T packet, ForwardPacketContext context);

    /**
     * Called on a Slave server when a forwarded packet of type {@code T} is received
     * from the Master (originally relayed from another slave or originated by the master).
     *
     * @param packet  The decoded payload that arrived from the master.
     * @param context The forwarding context containing the original sender server ID,
     *                optional player UUID, and the underlying Netty channel context.
     *
     * @apiNote
     * Dispatched to the server main thread. Safe to access game state.
     */
    void handleSlave(T packet,  ForwardPacketContext context);
}
