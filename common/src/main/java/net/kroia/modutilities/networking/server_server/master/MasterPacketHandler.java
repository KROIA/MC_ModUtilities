package net.kroia.modutilities.networking.server_server.master;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.server_server.ForwardPacketContext;
import net.kroia.modutilities.networking.server_server.ServerServerPacketRegistry;
import net.kroia.modutilities.networking.server_server.payload.HandshakePayload;
import net.kroia.modutilities.networking.server_server.payload.ForwardPacketPayload;
import net.kroia.modutilities.networking.server_server.payload.Payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

public class MasterPacketHandler extends SimpleChannelInboundHandler<Payload> {



    private final MinecraftServer mcServer;
    private final MasterTCPServer  masterTCPServer;

    /** Set after a successful handshake. Null until then. */
    private String serverId;

    public MasterPacketHandler(MinecraftServer mcServer, MasterTCPServer serverTCPServer) {
        this.mcServer = mcServer;
        this.masterTCPServer = serverTCPServer;
    }

    // ── Channel events ────────────────────────────────────────────────────────

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Payload payload) {
        switch (payload) {

            // ── Handshake: child server identifies itself ─────────────────────
            case HandshakePayload hs -> {
                if (!hs.token().equals(masterTCPServer.getSharedSecret())) {
                    warn("Rejected connection from '"+hs.serverId()+"' — bad token");
                    ctx.close();
                    return;
                }
                serverId = hs.serverId();
                masterTCPServer.putChildConnection(serverId, ctx.channel());
            }
            case ForwardPacketPayload bb -> {
                if (serverId == null) {
                    warn("Received StringMessage before handshake — closing.");
                    ctx.close();
                    return;
                }
                info("Received ForwardPacketPayload from child server: "+bb.senderServerID());

                ResourceLocation packetResouceLoc = bb.packetType();
                RegistryFriendlyByteBuf dataBuf =  new RegistryFriendlyByteBuf(Unpooled.buffer(), mcServer.registryAccess());
                ByteBufCodecs.BYTE_ARRAY.encode(dataBuf, bb.data());
                ForwardPacketContext context = new ForwardPacketContext(ctx, bb.senderServerID(), bb.senderPlayerUUID());
                ServerServerPacketRegistry.handleByteBufOnMasterSide(packetResouceLoc, dataBuf, context);
            }

            default ->
                    warn("Unhandled payload type: "+ payload.getClass().getSimpleName());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (serverId != null) {
            masterTCPServer.removeChildConnection(serverId);

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        error("Exception in child handler for '"+ serverId+"'", cause);
        ctx.close();
    }
    
    
    
    
    
    


    private static void info(String message) {
        ModUtilitiesMod.LOGGER.info("[MasterPacketHandler]: "+message);
    }
    private static void error(String message) {
        ModUtilitiesMod.LOGGER.error("[MasterPacketHandler]: "+message);
    }
    private static void error(String message, Throwable throwable) {
        ModUtilitiesMod.LOGGER.error("[MasterPacketHandler]: "+message, throwable);
    }
    private static void warn(String message) {
        ModUtilitiesMod.LOGGER.warn("[MasterPacketHandler]: "+message);
    }
    private static void debug(String message) {
        ModUtilitiesMod.LOGGER.debug("[MasterPacketHandler]: "+message);
    }
}
