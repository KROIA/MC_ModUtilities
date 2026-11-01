package net.kroia.modutilities.networking.client_server;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.utils.Env;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * Direction-specific marker base class for client-to-server packets built on Architectury's
 * {@link BaseC2SMessage}. Subclasses implement {@link #handleOnServer(ServerPlayer)} along with
 * {@link #toBytes(RegistryFriendlyByteBuf)} / {@link #fromBytes(RegistryFriendlyByteBuf)} to define
 * their wire format and behavior.
 *
 * @apiNote
 * The dispatch in {@link #handle(NetworkManager.PacketContext)} ensures {@link #handleOnServer(ServerPlayer)}
 * is invoked on the server main thread, so it is safe to mutate world state from there.
 */
public abstract class NetworkPacketC2S extends BaseC2SMessage {
    /**
     * Creates an empty packet, typically used by sender-side code that fills its fields before encoding.
     */
    public NetworkPacketC2S() {
        super();
    }

    /**
     * Creates a packet by decoding its contents from the given buffer via {@link #fromBytes(RegistryFriendlyByteBuf)}.
     *
     * @param buf the buffer to decode from.
     */
    public NetworkPacketC2S(RegistryFriendlyByteBuf buf) {
        super();
        this.fromBytes(buf);
    }

    /**
     * Handles this packet on the server after it has been received from a client.
     *
     * @param sender the player that sent the packet.
     *
     * @apiNote
     * Invoked on the server main thread, so it is safe to access and mutate world state.
     */
    abstract protected void handleOnServer(ServerPlayer sender);

    /**
     * Serializes this packet's fields into the given buffer.
     *
     * @param buf the buffer to write to.
     *
     * @apiNote
     * The default implementation writes nothing. Override for packets that carry payload data.
     */
    public void toBytes(RegistryFriendlyByteBuf buf){}

    /**
     * Deserializes this packet's fields from the given buffer.
     *
     * @param buf the buffer to read from.
     *
     * @apiNote
     * The default implementation reads nothing. Override for packets that carry payload data.
     */
    public void fromBytes(RegistryFriendlyByteBuf buf){}

    /**
     * Writes this packet to the given buffer. Delegates to {@link #toBytes(RegistryFriendlyByteBuf)}.
     *
     * @param buf the buffer to write to.
     */
    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        this.toBytes(buf);
    }

    /**
     * Architectury entry point invoked when this packet arrives. Verifies that the receiving side is
     * the server and dispatches {@link #handleOnServer(ServerPlayer)} onto the server main thread.
     *
     * @param context the Architectury packet context.
     */
    @Override
    public void handle(NetworkManager.PacketContext context) {
        if(context.getEnvironment() == Env.SERVER &&
                context.getPlayer() instanceof ServerPlayer serverPlayer) {
            Objects.requireNonNull(serverPlayer.getServer()).submit(() -> {
                this.handleOnServer(serverPlayer);
            });
        }
    }
}
