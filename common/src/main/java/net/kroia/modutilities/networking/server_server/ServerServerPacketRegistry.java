package net.kroia.modutilities.networking.server_server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import net.kroia.modutilities.ModUtilitiesMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ServerServerPacketRegistry
{
    private static class RegistryObject<T extends CustomPacketPayload>
    {
        public final StreamCodec<? super ByteBuf, T> streamCodec;
        public final ForwardPacketHandler<T> handler;

        public RegistryObject(StreamCodec<? super ByteBuf, T> streamCodec, ForwardPacketHandler<T> handler)
        {
            this.streamCodec = streamCodec;
            this.handler = handler;
        }

        public ByteBuf encode(T packet)
        {
            ByteBuf buf = Unpooled.buffer();
            StreamCodec<ByteBuf, T> codec = (StreamCodec<ByteBuf, T>)streamCodec;
            codec.encode(buf, packet);
            return buf;
        }
        public T decode(ByteBuf buf)
        {
            return streamCodec.decode(buf);
        }
        public void handleByteBufOnMasterSide(ByteBuf buf, ChannelHandlerContext context)
        {
            T payload = decode(buf);
            handler.handleMaster(payload, context);
        }
        public void handleByteBufOnSlaveSide(ByteBuf buf, ChannelHandlerContext context)
        {
            T payload = decode(buf);
            handler.handleSlave(payload, context);
        }
    }

    private static final Map<ResourceLocation, RegistryObject> registry = new HashMap<>();


    public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> packetType,
                                                                StreamCodec<? super ByteBuf, T> codec,
                                                                ForwardPacketHandler<T> handler)
    {
        ResourceLocation loc = packetType.id();
        if(registry.containsKey(loc))
        {
            throw new RuntimeException("ServerServerPacketRegistry.register(...): Packet with packetID = "+loc+" is already registered!");
        }

        RegistryObject registryObject = new RegistryObject(codec, handler);
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
            throw new RuntimeException("ServerServerPacketRegistry.register(...): Packet with packetID = "+loc+" is not registered!");
        }
        return registryObject.encode(packet);
    }
    public static CustomPacketPayload decode(ResourceLocation loc, ByteBuf buf)
    {
        RegistryObject  registryObject = registry.get(loc);
        if(registryObject==null)
        {
            throw new RuntimeException("ServerServerPacketRegistry.register(...): Packet with packetID = "+loc+" is not registered!");
        }
        return registryObject.decode(buf);
    }

    public static void handleByteBufOnMasterSide(ResourceLocation loc, ByteBuf buf, ChannelHandlerContext context)
    {
        RegistryObject  registryObject = registry.get(loc);
        if(registryObject==null)
        {
            throw new RuntimeException("ServerServerPacketRegistry.register(...): Packet with packetID = "+loc+" is not registered!");
        }
        registryObject.handleByteBufOnMasterSide(buf, context);
    }

    public static void handleByteBufOnSlaveSide(ResourceLocation loc, ByteBuf buf, ChannelHandlerContext context)
    {
        RegistryObject  registryObject = registry.get(loc);
        if(registryObject==null)
        {
            throw new RuntimeException("ServerServerPacketRegistry.register(...): Packet with packetID = "+loc+" is not registered!");
        }
        registryObject.handleByteBufOnSlaveSide(buf, context);
    }




    private static void info(String message) {
        ModUtilitiesMod.LOGGER.info("[ServerServerPacketRegistry]: "+message);
    }
    private static void error(String message) {
        ModUtilitiesMod.LOGGER.error("[ServerServerPacketRegistry]: "+message);
    }
    private static void error(String message, Throwable throwable) {
        ModUtilitiesMod.LOGGER.error("[ServerServerPacketRegistry]: "+message, throwable);
    }
    private static void warn(String message) {
        ModUtilitiesMod.LOGGER.warn("[ServerServerPacketRegistry]: "+message);
    }
    private static void debug(String message) {
        ModUtilitiesMod.LOGGER.debug("[ServerServerPacketRegistry]: "+message);
    }
}
