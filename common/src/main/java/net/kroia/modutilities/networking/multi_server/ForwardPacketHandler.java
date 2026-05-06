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
 * The handler methods are invoked on the Netty event loop thread, NOT the Minecraft
 * main thread. They are intended for relay-only logic and must not directly access
 * or mutate Minecraft game state. If interaction with the world is required, the
 * implementation must schedule that work onto the server thread.
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
     * Runs on the Netty event loop. Do not access Minecraft game state directly.
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
     * Runs on the Netty event loop. Do not access Minecraft game state directly.
     */
    void handleSlave(T packet,  ForwardPacketContext context);
}
