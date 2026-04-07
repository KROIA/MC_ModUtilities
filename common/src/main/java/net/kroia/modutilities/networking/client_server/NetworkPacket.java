package net.kroia.modutilities.networking.client_server;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.server_server.ForwardPacketContext;
import net.kroia.modutilities.networking.server_server.ForwardPacketHandler;
import net.kroia.modutilities.networking.server_server.ServerServerManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public abstract class NetworkPacket implements CustomPacketPayload {

    public static class NetworkPacketHandler implements
            PacketHandler<NetworkPacket>,
            ForwardPacketHandler<NetworkPacket>
    {

        @Override
        public void handleServer(NetworkPacket packet, NetworkManager.PacketContext context) {
            if(ServerServerManager.isRunning() && ServerServerManager.isSlave())
            {
                if(packet.needsRoutingToMaster())
                {
                    //ModUtilitiesMod.LOGGER.info("[NetworkPacket] Redirecting packet: "+packet.type()+" to master");
                    ServerServerManager.sendToMaster(context.getPlayer().getUUID(),  packet);
                }
                else
                    packet.handleOnServer(context);
            }
            else
                packet.handleOnServer(context);
        }

        @Override
        public void handleClient(NetworkPacket packet, NetworkManager.PacketContext context) {
            packet.handleOnClient(context);
        }

        @Override
        public void handleMaster(NetworkPacket packet, ForwardPacketContext context) {
            packet.handleOnMaster(context);
        }

        @Override
        public void handleSlave(NetworkPacket packet, ForwardPacketContext context) {
            packet.handleOnSlave(context);
        }
    };

    public static final NetworkPacketHandler HANDLER = new NetworkPacketHandler();

    public NetworkPacket() {
        super();
    }



    protected abstract void handleOnClient(NetworkManager.PacketContext context);
    protected abstract void handleOnServer(NetworkManager.PacketContext context);

    protected boolean needsRoutingToMaster() { return false; }
    protected void handleOnMaster(ForwardPacketContext context) {};
    protected void handleOnSlave(ForwardPacketContext context) {};

}
