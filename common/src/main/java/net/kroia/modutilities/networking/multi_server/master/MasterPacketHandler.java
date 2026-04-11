package net.kroia.modutilities.networking.multi_server.master;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.multi_server.ForwardPacketContext;
import net.kroia.modutilities.networking.multi_server.MultiServerPacketRegistry;
import net.kroia.modutilities.networking.multi_server.payload.HandshakePayload;
import net.kroia.modutilities.networking.multi_server.payload.ForwardPacketPayload;
import net.kroia.modutilities.networking.multi_server.payload.HandshakeResultPayload;
import net.kroia.modutilities.networking.multi_server.payload.Payload;
import net.kroia.modutilities.networking.multi_server.slave.SlaveServerClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public class MasterPacketHandler extends SimpleChannelInboundHandler<Payload> {



    private final MinecraftServer mcServer;
    private final MasterTCPServer  masterTCPServer;

    /** Set after a successful handshake. Null until then. */
    private String serverId = null;

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
                info("HandshakePayload request received for server '"+hs.serverId()+"'.");
                if (!hs.token().equals(masterTCPServer.getSharedSecret())) {
                    HandshakeResultPayload response = new HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState.INVALID_SHARED_SECRET);
                    warn("Rejected connection from '"+hs.serverId()+"' - "+response.result());
                    Channel channel = ctx.channel();
                    if (channel != null && channel.isActive()) {
                        channel.writeAndFlush(response);
                    }
                    ctx.close();
                    return;
                }

                List<String> currentSlaves = masterTCPServer.getConnectedSlaveIDs();
                for(String slave :  currentSlaves) {
                    if(slave.equals(hs.serverId())) {
                        HandshakeResultPayload response = new HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState.SLAVE_ID_ALREADY_USED);
                        warn("Rejected connection from '"+hs.serverId()+"' - "+response.result());
                        Channel channel = ctx.channel();
                        if (channel != null && channel.isActive()) {
                            channel.writeAndFlush(response);
                        }
                        ctx.close();
                        return;
                    }
                }
                serverId = hs.serverId();
                masterTCPServer.putChildConnection(serverId, ctx.channel());
                masterTCPServer.sendToSlave(serverId, new HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState.SUCCESS));
            }
            case ForwardPacketPayload bb -> {
                if (serverId == null) {
                    warn("Received data packet before handshake — closing.");
                    ctx.close();
                    return;
                }
                //info("Received ForwardPacketPayload from child server: "+bb.senderServerID());

                ResourceLocation packetResouceLoc = bb.packetType();
                ByteBuf buf = Unpooled.buffer();
                buf.writeBytes(bb.data());
                RegistryFriendlyByteBuf dataBuf =  new RegistryFriendlyByteBuf(buf, mcServer.registryAccess());
                //RegistryFriendlyByteBuf dataBuf =  new RegistryFriendlyByteBuf(Unpooled.buffer(), mcServer.registryAccess());
                //ByteBufCodecs.BYTE_ARRAY.encode(dataBuf, bb.data());
                ForwardPacketContext context = new ForwardPacketContext(ctx, bb.senderServerID(), bb.senderPlayerUUID());
                MultiServerPacketRegistry.handleByteBufOnMasterSide(packetResouceLoc, dataBuf, context);
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
