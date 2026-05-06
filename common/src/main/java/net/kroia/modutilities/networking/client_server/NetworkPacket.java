package net.kroia.modutilities.networking.client_server;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.networking.multi_server.ForwardPacketContext;
import net.kroia.modutilities.networking.multi_server.ForwardPacketHandler;
import net.kroia.modutilities.networking.multi_server.MultiServerManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Abstract base class for all client/server network packets handled by the MC_ModUtilities networking layer.
 * Subclasses implement {@link #handleOnClient(NetworkManager.PacketContext)} and {@link #handleOnServer(NetworkManager.PacketContext)}
 * to define direction-specific behavior, and may optionally override {@link #handleOnMaster(ForwardPacketContext)}
 * and {@link #handleOnSlave(ForwardPacketContext)} to participate in the multi-server master/slave relay flow.
 *
 * @apiNote
 * Architectury auto-dispatches {@link #handleOnClient(NetworkManager.PacketContext)} on the client render thread
 * and {@link #handleOnServer(NetworkManager.PacketContext)} on the server main thread, so it is safe to touch game
 * state from those methods. In contrast {@link #handleOnMaster(ForwardPacketContext)} and
 * {@link #handleOnSlave(ForwardPacketContext)} are invoked on Netty I/O threads while relaying between servers,
 * and must not access world/level state.
 */
public abstract class NetworkPacket implements CustomPacketPayload {

    /**
     * Default {@link PacketHandler} / {@link ForwardPacketHandler} implementation that delegates each side
     * back to the corresponding {@code handleOn...} method on the {@link NetworkPacket} instance.
     * On the server side it also consults {@link MultiServerManager} to decide whether the packet should
     * be forwarded to the master server before being handled locally.
     */
    public static class NetworkPacketHandler implements
            PacketHandler<NetworkPacket>,
            ForwardPacketHandler<NetworkPacket>
    {

        @Override
        public void handleServer(NetworkPacket packet, NetworkManager.PacketContext context) {
            if(MultiServerManager.isRunning() && MultiServerManager.isSlave())
            {
                if(packet.needsRoutingToMaster())
                {
                    //ModUtilitiesMod.LOGGER.info("[NetworkPacket] Redirecting packet: "+packet.type()+" to master");
                    MultiServerManager.sendToMaster(context.getPlayer().getUUID(),  packet);
                }
                else
                    packet.handleOnServer(context);
            }
            else
                packet.handleOnServer(context);
        }

        @Override
        public void handleClient(NetworkPacket packet, NetworkManager.PacketContext context) {
            packet.handleOnClient(context);
        }

        @Override
        public void handleMaster(NetworkPacket packet, ForwardPacketContext context) {
            packet.handleOnMaster(context);
        }

        @Override
        public void handleSlave(NetworkPacket packet, ForwardPacketContext context) {
            packet.handleOnSlave(context);
        }
    };

    /**
     * Shared singleton {@link NetworkPacketHandler} used as the default dispatcher when registering
     * packet types via {@link net.kroia.modutilities.networking.NetworkPacketManager} without a custom handler.
     */
    public static final NetworkPacketHandler HANDLER = new NetworkPacketHandler();

    /**
     * Creates a new {@code NetworkPacket}. Subclasses typically expose a no-arg constructor for the
     * {@link net.minecraft.network.codec.StreamCodec} as well as one that decodes from a
     * {@link net.minecraft.network.RegistryFriendlyByteBuf}.
     */
    public NetworkPacket() {
        super();
    }



    /**
     * Handles this packet after it has been received on the client.
     *
     * @param context the Architectury packet context for the receiving client.
     *
     * @apiNote
     * Invoked on the client render thread by Architectury, so it is safe to touch client-side game state
     * (e.g. the {@link net.minecraft.client.Minecraft} instance, GUIs, the player's level, etc.).
     * Do not call this method directly.
     */
    protected abstract void handleOnClient(NetworkManager.PacketContext context);

    /**
     * Handles this packet after it has been received on the server.
     *
     * @param context the Architectury packet context for the receiving server, including the sending player.
     *
     * @apiNote
     * Invoked on the server main thread by Architectury, so it is safe to mutate world state.
     * If {@link #needsRoutingToMaster()} returns {@code true} and the current server is a slave in a
     * multi-server topology, the packet is forwarded to the master instead of being handled locally.
     * Do not call this method directly.
     */
    protected abstract void handleOnServer(NetworkManager.PacketContext context);

    /**
     * Indicates whether this packet must be relayed from a slave server to the master server before being handled.
     *
     * @return {@code true} to forward the packet to the master when received on a slave server, {@code false} (default)
     *         to handle it on whichever server originally received it.
     */
    protected boolean needsRoutingToMaster() { return false; }

    /**
     * Handles this packet after it has been forwarded to the master server in a multi-server setup.
     *
     * @param context the forward-packet context describing the originating slave and player.
     *
     * @apiNote
     * Invoked on a Netty I/O thread on the master server during cross-server relay. This handler must not
     * access world/level state directly; schedule work onto the server main thread if game state must be touched.
     */
    protected void handleOnMaster(ForwardPacketContext context) {};

    /**
     * Handles this packet after it has been forwarded back from the master server to a slave server.
     *
     * @param context the forward-packet context describing the master/slave relay metadata.
     *
     * @apiNote
     * Invoked on a Netty I/O thread on the slave server during cross-server relay. This handler must not
     * access world/level state directly; schedule work onto the server main thread if game state must be touched.
     */
    protected void handleOnSlave(ForwardPacketContext context) {};

}
