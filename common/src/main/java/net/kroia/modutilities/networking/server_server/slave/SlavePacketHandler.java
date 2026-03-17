package net.kroia.modutilities.networking.server_server.slave;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.server_server.ServerServerPacketRegistry;
import net.kroia.modutilities.networking.server_server.payload.BroadcastPayload;
import net.kroia.modutilities.networking.server_server.payload.ForwardPacketPayload;
import net.kroia.modutilities.networking.server_server.payload.Payload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

public class SlavePacketHandler extends SimpleChannelInboundHandler<Payload> {

    /** We need the MC server reference to broadcast messages to players. */
    private final MinecraftServer mcServer;

    private final SlaveServerClient connector;

    public SlavePacketHandler(MinecraftServer mcServer, SlaveServerClient connector) {
        this.mcServer = mcServer;
        this.connector = connector;
    }

    // ── Inbound packets from hub ──────────────────────────────────────────────

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Payload payload) {
        switch (payload) {

            // Hub routed a string message to this server — display it to players
            case BroadcastPayload bc -> {
                info("Received from master: ["+bc.fromServer()+"] "+bc.senderName()+": "+bc.message());

                if (mcServer != null) {
                    // mcServer.execute() ensures we run on the MC main thread
                    mcServer.execute(() -> {
                        Component chat = Component.literal(
                                "§7[§b" + bc.fromServer() + "§7→§aThis Server§7] §f" +
                                        bc.senderName() + "§7: §e" + bc.message()
                        );
                        mcServer.getPlayerList().broadcastSystemMessage(chat, false);
                    });
                }
            }
            case ForwardPacketPayload bb -> {
                debug("bytes received from: "+bb.senderServerID()+" "+bb.data().length+" bytes");

                ResourceLocation packetResouceLoc = bb.packetType();
                ByteBuf dataBuf =  Unpooled.buffer();
                ByteBufCodecs.BYTE_ARRAY.encode(dataBuf, bb.data());
                ServerServerPacketRegistry.handleByteBufOnSlaveSide(packetResouceLoc, dataBuf, ctx);
            }

            default ->
                    warn("Unhandled payload from hub: "+payload.getClass().getSimpleName());
        }
    }

    // ── Connection events ─────────────────────────────────────────────────────

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        warn("Lost connection to master — scheduling reconnect...");
        connector.scheduleReconnect();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        error("Exception in child inbound handler", cause);
        ctx.close();
    }


    private static void info(String message) {
        ModUtilitiesMod.LOGGER.info("[SlavePacketHandler]: "+message);
    }
    private static void error(String message) {
        ModUtilitiesMod.LOGGER.error("[SlavePacketHandler]: "+message);
    }
    private static void error(String message, Throwable throwable) {
        ModUtilitiesMod.LOGGER.error("[SlavePacketHandler]: "+message, throwable);
    }
    private static void warn(String message) {
        ModUtilitiesMod.LOGGER.warn("[SlavePacketHandler]: "+message);
    }
    private static void debug(String message) {
        ModUtilitiesMod.LOGGER.debug("[SlavePacketHandler]: "+message);
    }
}
