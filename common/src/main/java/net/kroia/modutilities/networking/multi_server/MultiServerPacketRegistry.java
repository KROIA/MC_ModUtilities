package net.kroia.modutilities.networking.multi_server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.UtilitiesPlatform;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.kroia.modutilities.networking.multi_server.payload.ForwardPacketPayload;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Registry for packet types that can be forwarded across the multi-server (Master/Slave)
 * link. Each registered packet is associated with a {@link StreamCodec} for
 * encode/decode, plus a {@link ForwardPacketHandler} that processes it on either
 * side of the connection.
 * <p>
 * The registry is thread-safe (backed by a {@link java.util.concurrent.ConcurrentHashMap})
 * so packet types can be registered and looked up from multiple threads, including
 * the Netty event loop.
 *
 * @apiNote
 * {@link #onCreate(MinecraftServer)} must be called before any encoding occurs so
 * that the shared {@link RegistryAccess} (used to construct
 * {@link RegistryFriendlyByteBuf} instances) is available.
 */
public class MultiServerPacketRegistry
{
    private static class RegistryObject<T extends CustomPacketPayload>
    {
        public final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;
        public final ForwardPacketHandler<? super T> handler;
        public final String name;


        public RegistryObject(StreamCodec<RegistryFriendlyByteBuf, T> streamCodec,
                              ForwardPacketHandler<? super T> handler,
                              String name)
        {
            this.streamCodec = streamCodec;
            this.handler = handler;
            this.name = name;
        }

        public RegistryFriendlyByteBuf encode(T packet)
        {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            streamCodec.encode(buf, packet);
            return buf;
        }
        public T decode(RegistryFriendlyByteBuf buf)
        {
            return streamCodec.decode(buf);
        }
        public void handleByteBufOnMasterSide(RegistryFriendlyByteBuf buf, ForwardPacketContext context)
        {
            T payload = decode(buf);
            handler.handleMaster(payload, context);
        }
        public void handleByteBufOnSlaveSide(RegistryFriendlyByteBuf buf, ForwardPacketContext context)
        {
            T payload = decode(buf);
            handler.handleSlave(payload, context);
        }
    }

    private static final Map<ResourceLocation, RegistryObject<?>> registry = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile RegistryAccess registryAccess;

    /**
     * Initializes the registry with a {@link RegistryAccess} obtained from the given
     * Minecraft server. This must be called once on each side (master and slaves)
     * before any packet is encoded, because encoding builds a
     * {@link RegistryFriendlyByteBuf} from this registry access.
     *
     * @param server The current {@link MinecraftServer}. If {@code null}, the call is a no-op.
     */
    public static void onCreate(MinecraftServer server)
    {
        if(server != null)
            registryAccess = server.registryAccess();
    }

    /**
     * Registers a forward-relay packet type with a custom handler.
     *
     * @param packetType The {@link CustomPacketPayload.Type} identifying the packet.
     * @param codec      Stream codec used to serialize/deserialize the payload.
     * @param handler    Handler invoked when a packet of this type is received on either
     *                   the master or a slave.
     * @param <T>        The concrete payload type.
     *
     * @throws RuntimeException If a packet with the same ID has already been registered.
     *
     * @apiNote
     * Should be called once on each participating server (master and all slaves) and
     * with matching codec/handler signatures so the relay can decode on the other end.
     */
    public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> packetType,
                                                                StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                                ForwardPacketHandler<? super T> handler)
    {
        ResourceLocation loc = packetType.id();
        if(registry.containsKey(loc)) return;

        RegistryObject registryObject = new RegistryObject(codec, handler, packetType.id().toString());
        registry.put(loc, registryObject);
    }
    /**
     * Registers a forward-relay packet type using the default {@link NetworkPacket#HANDLER}.
     * Use this overload for packets whose handling is performed via the regular
     * {@link NetworkPacket} dispatch on the receiving side.
     *
     * @param packetType The {@link CustomPacketPayload.Type} identifying the packet.
     * @param codec      Stream codec used to serialize/deserialize the payload.
     * @param <T>        The concrete payload type.
     */
    public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> packetType,
                                                                StreamCodec<RegistryFriendlyByteBuf, T> codec)
    {
        ResourceLocation loc = packetType.id();
        if(registry.containsKey(loc)) return;

        RegistryObject registryObject = new RegistryObject(codec, NetworkPacket.HANDLER, packetType.id().toString());
        registry.put(loc, registryObject);
    }


    /**
     * Removes a previously registered packet type. Has no effect if the type was not
     * registered.
     *
     * @param packetType The {@link CustomPacketPayload.Type} to remove.
     * @param <T>        The concrete payload type.
     */
    public static <T extends CustomPacketPayload> void unregister(CustomPacketPayload.Type<T> packetType)
    {
        ResourceLocation loc = packetType.id();
        registry.remove(loc);
    }

    /**
     * Removes every registered packet type. Mainly intended for shutdown or test cleanup.
     */
    public static void clear()
    {
        registry.clear();
    }

    /**
     * Tests whether a packet type with the given ID is registered.
     *
     * @param loc The packet's {@link ResourceLocation} ID.
     * @return    {@code true} if a packet with this ID is registered, {@code false} otherwise.
     */
    public static boolean isRegistered(ResourceLocation loc)
    {
        return registry.containsKey(loc);
    }

    /**
     * Encodes a registered payload into a fresh buffer using its registered stream codec.
     *
     * @param loc    The packet ID.
     * @param packet The payload instance to encode.
     * @return       A {@link ByteBuf} containing the encoded payload.
     *
     * @throws RuntimeException If no packet is registered under the given ID.
     */
    public static ByteBuf encode(ResourceLocation loc, CustomPacketPayload packet)
    {
        RegistryObject  registryObject = registry.get(loc);
        if(registryObject==null)
        {
            throw new RuntimeException("MultiServerPacketRegistry.register(...): Packet with packetID = "+loc+" is not registered!");
        }
        return registryObject.encode(packet);
    }
    /**
     * Decodes a payload of the given registered type from the supplied buffer.
     *
     * @param loc The packet ID.
     * @param buf The buffer to decode from.
     * @return    The decoded {@link CustomPacketPayload}.
     *
     * @throws RuntimeException If no packet is registered under the given ID.
     */
    public static CustomPacketPayload decode(ResourceLocation loc, RegistryFriendlyByteBuf buf)
    {
        RegistryObject  registryObject = registry.get(loc);
        if(registryObject==null)
        {
            throw new RuntimeException("MultiServerPacketRegistry.register(...): Packet with packetID = "+loc+" is not registered!");
        }
        return registryObject.decode(buf);
    }

    /**
     * Decodes the buffer for the given packet ID and dispatches it to the registered
     * handler's master-side method. Called on the master server when a forwarded
     * payload arrives from a slave.
     *
     * @param loc     The packet ID.
     * @param buf     The encoded payload buffer.
     * @param context The forwarding context describing the sender.
     *
     * @throws RuntimeException If no packet is registered under the given ID.
     *
     * @apiNote
     * Called from the Netty IO thread; dispatches to the server main thread
     * via {@code server.execute()} so all downstream handlers run thread-safely.
     * Takes ownership of {@code buf} — the caller must NOT release it.
     */
    public static void handleByteBufOnMasterSide(ResourceLocation loc, RegistryFriendlyByteBuf buf, ForwardPacketContext context)
    {
        RegistryObject  registryObject = registry.get(loc);
        if(registryObject==null)
        {
            buf.release();
            throw new RuntimeException("MultiServerPacketRegistry.register(...): Packet with packetID = "+loc+" is not registered!");
        }
        MinecraftServer server = UtilitiesPlatform.getServer();
        if(server == null) {
            buf.release();
            return;
        }
        server.execute(() -> {
            try {
                registryObject.handleByteBufOnMasterSide(buf, context);
            } finally {
                if (buf.refCnt() > 0) buf.release();
            }
        });
    }

    /**
     * Decodes the buffer for the given packet ID and dispatches it to the registered
     * handler's slave-side method. Called on a slave server when a forwarded
     * payload arrives from the master.
     *
     * @param loc     The packet ID.
     * @param buf     The encoded payload buffer.
     * @param context The forwarding context describing the original sender.
     *
     * @throws RuntimeException If no packet is registered under the given ID.
     *
     * @apiNote
     * Called from the Netty IO thread; dispatches to the server main thread
     * via {@code server.execute()} so all downstream handlers run thread-safely.
     * Takes ownership of {@code buf} — the caller must NOT release it.
     */
    public static void handleByteBufOnSlaveSide(ResourceLocation loc, RegistryFriendlyByteBuf buf, ForwardPacketContext context)
    {
        RegistryObject  registryObject = registry.get(loc);
        if(registryObject==null)
        {
            buf.release();
            throw new RuntimeException("MultiServerPacketRegistry.register(...): Packet with packetID = "+loc+" is not registered!");
        }
        MinecraftServer server = UtilitiesPlatform.getServer();
        if(server == null) {
            buf.release();
            return;
        }
        server.execute(() -> {
            try {
                registryObject.handleByteBufOnSlaveSide(buf, context);
            } finally {
                if (buf.refCnt() > 0) buf.release();
            }
        });
    }


    /**
     * Wraps an arbitrary registered {@link CustomPacketPayload} into a
     * {@link ForwardPacketPayload} ready to be sent across the master/slave link.
     * <p>
     * The packet is encoded via its registered stream codec, the resulting bytes are
     * extracted into a {@code byte[]}, and the underlying buffer is released.
     *
     * @param senderPlayerUUID The UUID of the player that triggered this packet, or
     *                         {@code null} for server-initiated traffic.
     * @param senderServerID   The slave server ID that is sending (or originating) the packet.
     * @param packet           The payload to wrap; its type must already be registered.
     * @return                 A populated {@link ForwardPacketPayload}, or {@code null} if
     *                         the packet's type is not registered.
     */
    public static ForwardPacketPayload createForwardPacketPayload(@Nullable UUID senderPlayerUUID,
                                                                  String senderServerID,
                                                                  CustomPacketPayload packet)
    {
        ResourceLocation packetType = packet.type().id();
        RegistryObject  registryObject = registry.get(packetType);
        if(registryObject==null)
            return null;
        RegistryFriendlyByteBuf encoded = registryObject.encode(packet);
        byte[] data = new byte[encoded.readableBytes()];
        encoded.readBytes(data);
        encoded.release();
        ForwardPacketPayload payload = new ForwardPacketPayload(senderPlayerUUID, senderServerID, packetType, data);
        return payload;
    }



    private static void info(String message) {
        ModUtilitiesMod.LOGGER.info("[MultiServerPacketRegistry]: "+message);
    }
    private static void error(String message) {
        ModUtilitiesMod.LOGGER.error("[MultiServerPacketRegistry]: "+message);
    }
    private static void error(String message, Throwable throwable) {
        ModUtilitiesMod.LOGGER.error("[MultiServerPacketRegistry]: "+message, throwable);
    }
    private static void warn(String message) {
        ModUtilitiesMod.LOGGER.warn("[MultiServerPacketRegistry]: "+message);
    }
    private static void debug(String message) {
        ModUtilitiesMod.LOGGER.debug("[MultiServerPacketRegistry]: "+message);
    }
}
