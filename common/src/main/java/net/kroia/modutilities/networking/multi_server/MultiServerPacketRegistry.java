package net.kroia.modutilities.networking.multi_server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kroia.modutilities.ModUtilitiesMod;
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

    private static final Map<ResourceLocation, RegistryObject> registry = new HashMap<>();
    private static RegistryAccess registryAccess;

    public static void onCreate(MinecraftServer server)
    {
        if(server != null)
            registryAccess = server.registryAccess();
    }

    public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> packetType,
                                                                StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                                ForwardPacketHandler<? super T> handler)
    {
        ResourceLocation loc = packetType.id();
        if(registry.containsKey(loc))
        {
            throw new RuntimeException("MultiServerPacketRegistry.register(...): Packet with packetID = "+loc+" is already registered!");
        }

        RegistryObject registryObject = new RegistryObject(codec, handler, packetType.id().toString());
        registry.put(loc, registryObject);
    }
    public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> packetType,
                                                                StreamCodec<RegistryFriendlyByteBuf, T> codec)
    {
        ResourceLocation loc = packetType.id();
        if(registry.containsKey(loc))
        {
            throw new RuntimeException("MultiServerPacketRegistry.register(...): Packet with packetID = "+loc+" is already registered!");
        }

        RegistryObject registryObject = new RegistryObject(codec, NetworkPacket.HANDLER, packetType.id().toString());
        registry.put(loc, registryObject);
    }


    public static <T extends CustomPacketPayload> void unregister(CustomPacketPayload.Type<T> packetType)
    {
        ResourceLocation loc = packetType.id();
        if(registry.containsKey(loc))
        {
            registry.remove(loc);
        }
    }
    public static void clear()
    {
        registry.clear();
    }

    public static boolean isRegistered(ResourceLocation loc)
    {
        return registry.containsKey(loc);
    }

    public static ByteBuf encode(ResourceLocation loc, CustomPacketPayload packet)
    {
        RegistryObject  registryObject = registry.get(loc);
        if(registryObject==null)
        {
            throw new RuntimeException("MultiServerPacketRegistry.register(...): Packet with packetID = "+loc+" is not registered!");
        }
        return registryObject.encode(packet);
    }
    public static CustomPacketPayload decode(ResourceLocation loc, RegistryFriendlyByteBuf buf)
    {
        RegistryObject  registryObject = registry.get(loc);
        if(registryObject==null)
        {
            throw new RuntimeException("MultiServerPacketRegistry.register(...): Packet with packetID = "+loc+" is not registered!");
        }
        return registryObject.decode(buf);
    }

    public static void handleByteBufOnMasterSide(ResourceLocation loc, RegistryFriendlyByteBuf buf, ForwardPacketContext context)
    {
        RegistryObject  registryObject = registry.get(loc);
        if(registryObject==null)
        {
            throw new RuntimeException("MultiServerPacketRegistry.register(...): Packet with packetID = "+loc+" is not registered!");
        }
        registryObject.handleByteBufOnMasterSide(buf, context);
    }

    public static void handleByteBufOnSlaveSide(ResourceLocation loc, RegistryFriendlyByteBuf buf, ForwardPacketContext context)
    {
        RegistryObject  registryObject = registry.get(loc);
        if(registryObject==null)
        {
            throw new RuntimeException("MultiServerPacketRegistry.register(...): Packet with packetID = "+loc+" is not registered!");
        }
        registryObject.handleByteBufOnSlaveSide(buf, context);
    }


    public static ForwardPacketPayload createForwardPacketPayload(@Nullable UUID senderPlayerUUID,
                                                                  String senderServerID,
                                                                  CustomPacketPayload packet)
    {
        ResourceLocation packetType = packet.type().id();
        RegistryObject  registryObject = registry.get(packetType);
        if(registryObject==null)
            return null;
        RegistryFriendlyByteBuf encoded = registryObject.encode(packet);
        //byte[] data = ByteBufCodecs.BYTE_ARRAY.decode(encoded);
        byte[] data = encoded.array();
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
