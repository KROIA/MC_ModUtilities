package net.kroia.modutilities.networking.client_server;

//import dev.architectury.networking.NetworkManager; // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
//import dev.architectury.networking.simple.BaseS2CMessage; // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
//import dev.architectury.utils.Env; // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
//import net.minecraft.client.Minecraft; // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
//import net.minecraft.network.RegistryFriendlyByteBuf; // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)

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
//public abstract class NetworkPacketS2C extends BaseS2CMessage { // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
    /**
     * Creates an empty packet, typically used by sender-side code that fills its fields before encoding.
     */
    //public NetworkPacketS2C() { // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
        //super(); // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
    //} // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)

    /**
     * Creates a packet by decoding its contents from the given buffer via {@link #fromBytes(RegistryFriendlyByteBuf)}.
     *
     * @param buf the buffer to decode from.
     */
    //public NetworkPacketS2C(RegistryFriendlyByteBuf buf) { // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
        //super(); // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
        //this.fromBytes(buf); // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
    //} // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)

    /**
     * Handles this packet on the client after it has been received from the server.
     *
     * @apiNote
     * Invoked on the client render thread, so it is safe to access {@link Minecraft},
     * the player's level and on-screen UI from this method.
     */
    //abstract protected void handleOnClient(); // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)

    /**
     * Serializes this packet's fields into the given buffer.
     *
     * @param buf the buffer to write to.
     *
     * @apiNote
     * The default implementation writes nothing. Override for packets that carry payload data.
     */
    //public void toBytes(RegistryFriendlyByteBuf buf){} // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)

    /**
     * Deserializes this packet's fields from the given buffer.
     *
     * @param buf the buffer to read from.
     *
     * @apiNote
     * The default implementation reads nothing. Override for packets that carry payload data.
     */
    //public void fromBytes(RegistryFriendlyByteBuf buf){} // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)

    /**
     * Writes this packet to the given buffer. Delegates to {@link #toBytes(RegistryFriendlyByteBuf)}.
     *
     * @param buf the buffer to write to.
     */
    //@Override // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
    //public void write(RegistryFriendlyByteBuf buf) { // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
        //this.toBytes(buf); // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
    //} // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)

    /**
     * Architectury entry point invoked when this packet arrives. Verifies that the receiving side is
     * the client and dispatches {@link #handleOnClient()} onto the client render thread.
     *
     * @param context the Architectury packet context.
     */
    //@Override // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
    //public void handle(NetworkManager.PacketContext context) { // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
        //if(context.getEnvironment() == Env.CLIENT) { // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
            //Minecraft.getInstance().submit(this::handleOnClient); // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
        //} // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
    //} // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
//} // mc<=1.21.8 (Architectury 18 removed dev.architectury.networking.simple)
