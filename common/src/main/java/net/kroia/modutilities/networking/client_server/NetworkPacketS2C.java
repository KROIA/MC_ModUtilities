package net.kroia.modutilities.networking.client_server;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Direction-specific marker base class for server-to-client packets built on Architectury's
 * {@link BaseS2CMessage}. Subclasses implement {@link #handleOnClient()} along with
 * {@link #toBytes(RegistryFriendlyByteBuf)} / {@link #fromBytes(RegistryFriendlyByteBuf)} to define
 * their wire format and behavior.
 *
 * @apiNote
 * The dispatch in {@link #handle(NetworkManager.PacketContext)} ensures {@link #handleOnClient()}
 * is invoked on the client render thread, so it is safe to access {@link Minecraft} and other client state.
 */
public abstract class NetworkPacketS2C extends BaseS2CMessage {
    /**
     * Creates an empty packet, typically used by sender-side code that fills its fields before encoding.
     */
    public NetworkPacketS2C() {
        super();
    }

    /**
     * Creates a packet by decoding its contents from the given buffer via {@link #fromBytes(RegistryFriendlyByteBuf)}.
     *
     * @param buf the buffer to decode from.
     */
    public NetworkPacketS2C(RegistryFriendlyByteBuf buf) {
        super();
        this.fromBytes(buf);
    }

    /**
     * Handles this packet on the client after it has been received from the server.
     *
     * @apiNote
     * Invoked on the client render thread, so it is safe to access {@link Minecraft},
     * the player's level and on-screen UI from this method.
     */
    abstract protected void handleOnClient();

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
     * the client and dispatches {@link #handleOnClient()} onto the client render thread.
     *
     * @param context the Architectury packet context.
     */
    @Override
    public void handle(NetworkManager.PacketContext context) {
        if(context.getEnvironment() == Env.CLIENT) {
            Minecraft.getInstance().submit(this::handleOnClient);
        }
    }
}
