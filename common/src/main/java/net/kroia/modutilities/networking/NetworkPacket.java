package net.kroia.modutilities.networking;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class NetworkPacket implements INetworkPacket {


    /**
     * The NetworkManager instance used to be able to call the function this.sendResponse().
     * This is only possible after the packet was received.
     * The manager gets set during the first decoding phase of the packet. (Before the receive method gets called)
     */
    private net.kroia.modutilities.networking.NetworkManager manager;

    /**
     * The environment in which the packet was received.
     * This is used to determine where the response needs to go.
     * This variable is only set after the packet was received.
     */
    private Env environment;

    /**
     * The player that sent the packet.
     * This is only set if the packet was received on the server side.
     * It is used to send responses back to the player.
     */
    private ServerPlayer player;
    public NetworkPacket() {
        super();
    }

    /**
     * Constructor that initializes the packet from a FriendlyByteBuf.
     * This is used to decode the packet data when it is received.
     *
     * @param buf The FriendlyByteBuf containing the packet data.
     */
    public NetworkPacket(FriendlyByteBuf buf) {
        super();
        this.fromBytes(buf);
    }


    /**
     * Handles the packet on the client side.
     * This method is called when the packet is received on the client.
     * Override this method to implement custom client-side handling.
     *
     * Inside this function, the data is already decoded and can be used directly.
     */
    protected void handleOnClient() {
    }

    /**
     * Handles the packet on the server side.
     * This method is called when the packet is received on the server.
     * Override this method to implement custom server-side handling.
     *
     * Inside this function, the data is already decoded and can be used directly.
     *
     * @param sender The player that sent the packet.
     */
    protected void handleOnServer(ServerPlayer sender) {
    }


    /**
     * Sets the NetworkManager instance for this packet.
     * This is used to send responses back to the server or client.
     *
     * @param manager The NetworkManager instance to set.
     */
    public void setManager(net.kroia.modutilities.networking.NetworkManager manager) {
        this.manager = manager;
    }

    /**
     * Gets the NetworkManager instance for this packet.
     * This is used to send responses back to the server or client.
     *
     * @return The NetworkManager instance.
     */
    protected net.kroia.modutilities.networking.NetworkManager getManager() {
        return this.manager;
    }



    /**
     * Receives the packet and handles it based on the environment (client or server).
     * This method is called by the NetworkManager when the packet is received.
     *
     * @param contextSupplier A supplier that provides the NetworkManager.PacketContext.
     */
    @Override
    public void receive(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = (NetworkManager.PacketContext)contextSupplier.get();
        // Check if is client
        Env env = context.getEnvironment();
        environment = env;
        if(env == Env.CLIENT) {
            Minecraft.getInstance().submit(this::handleOnClient);

        } else if(env == Env.SERVER) {
            Player sender = context.getPlayer();
            if(sender instanceof ServerPlayer serverPlayer) {
                player = serverPlayer;
                Objects.requireNonNull(serverPlayer.getServer()).submit(() -> {
                    this.handleOnServer(serverPlayer);
                });
            }
        }
    }

    /**
     * Sends a response packet back to the client or server based on the environment.
     * This method can be called from within the handleOnClient or handleOnServer methods.
     *
     * @param packet The packet to send as a response.
     * @return true if the response was sent successfully, false otherwise.
     */
    protected boolean sendResponse(INetworkPacket packet)
    {
        if(manager == null || packet == null || environment == null)
            return false;
        if(environment == Env.CLIENT)
        {
            manager.sendToServer(packet);
        }
        else if(environment == Env.SERVER)
        {
            if(player == null)
                return false;
            manager.sendToClient(player, packet);
        }
        return true;
    }
}
