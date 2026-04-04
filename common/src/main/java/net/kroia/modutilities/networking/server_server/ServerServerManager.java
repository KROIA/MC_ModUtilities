package net.kroia.modutilities.networking.server_server;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.server_server.master.MasterTCPServer;
import net.kroia.modutilities.networking.server_server.slave.SlaveServerClient;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ServerServerManager
{
    private static ServerServerManager instance;

    private final @Nullable MasterTCPServer  tcpServer;
    private final @Nullable SlaveServerClient slaveClient;



    private ServerServerManager(@Nullable MasterTCPServer master, @Nullable SlaveServerClient slave) {
        this.tcpServer = master;
        this.slaveClient = slave;
    }

    /**
     * ServerServerManager factory
     * @param mcServer
     * @param sharedSecret
     * @param tcpPort
     * @return
     */
    public static boolean createMaster(MinecraftServer mcServer, String sharedSecret, int tcpPort)
    {
        if(instance != null)
        {
            warn("Can't create master since there is already an existing instance of type: "+ ((instance.isSlave())?"Slave":"Master"));
            return false;
        }
        ServerServerPacketRegistry.onCreate(mcServer);
        MasterTCPServer master = new MasterTCPServer(mcServer, sharedSecret, tcpPort);
        instance = new ServerServerManager(master, null);
        return true;
    }
    public static boolean createSlave(MinecraftServer mcServer, String sharedSecret, String slaveServerID, String masterHostIP, int masterHostTcpPort)
    {
        if(instance != null)
        {
            warn("Can't create master since there is already an existing instance of type: "+ ((instance.isSlave())?"Slave":"Master"));
            return false;
        }
        ServerServerPacketRegistry.onCreate(mcServer);
        SlaveServerClient slave = new SlaveServerClient(mcServer, sharedSecret, slaveServerID,  masterHostIP, masterHostTcpPort);
        instance = new ServerServerManager(null, slave);
        return true;
    }



    public static boolean isSlave()
    {
        if(instance == null)
            return false;
        return instance.slaveClient != null;
    }
    public static boolean isMaster()
    {
        if(instance == null)
            return false;
        return instance.tcpServer != null;
    }

    public static boolean start()
    {
        if(instance == null)
        {
            error("Can't start master/slave since no master/slave instance exists");
            return false;
        }
        if(instance.tcpServer != null)
        {
            if(instance.tcpServer.isRunning())
            {
                warn("Server already running");
                return true;
            }
            instance.tcpServer.start();
        }
        else
        {
            assert instance.slaveClient != null;
            if(instance.slaveClient.isConnected())
            {
                warn("Already connected to the master");
                return true;
            }
            instance.slaveClient.connect();
        }
        return true;
    }

    public static boolean stop()
    {
        if(instance == null)
        {
            error("Can't stop master/slave since no master/slave instance exists");
            return false;
        }
        if(instance.tcpServer != null)
        {
            instance.tcpServer.stop();
        }
        else
        {
            instance.slaveClient.disconnect();
        }
        return true;
    }

    public static boolean isRunning()
    {
        if(instance == null)
            return false;
        if(instance.tcpServer != null)
        {
            return instance.tcpServer.isRunning();
        }
        else
        {
            assert instance.slaveClient != null;
            return instance.slaveClient.isConnected();
        }
    }

    public static boolean instanceExists()
    {
        return instance != null;
    }

    public static void cleanup()
    {
        if(instance == null)
            return;
        if(isRunning())
        {
            if(instance.tcpServer != null)
            {
                instance.tcpServer.stop();
            }
            else
            {
                assert instance.slaveClient != null;
                instance.slaveClient.disconnect();
            }
        }
        instance = null;
    }



    public static void sendToMaster(CustomPacketPayload packet)
    {
        if(checkSendToMaster())
            instance.slaveClient.sendToMaster(null, packet);
    }
    public static void sendToMaster(UUID senderPlayerUUID, CustomPacketPayload packet)
    {
        if(checkSendToMaster())
            instance.slaveClient.sendToMaster(senderPlayerUUID, packet);
    }
    public static void sendToMaster(@Nullable UUID packetIdentifier, @Nullable UUID senderPlayerUUID, CustomPacketPayload packet)
    {
        if(checkSendToMaster())
            instance.slaveClient.sendToMaster(packetIdentifier, senderPlayerUUID, packet);
    }




    public static void sendToSlave(String serverId, CustomPacketPayload packet)
    {
        if(checkSendToSlave(serverId))
            instance.tcpServer.sendToSlave(null,serverId, packet);
    }
    public static void sendToSlave(UUID senderPlayerUUID, String serverId, CustomPacketPayload packet)
    {
        if(checkSendToSlave(serverId))
            instance.tcpServer.sendToSlave(senderPlayerUUID,serverId, packet);
    }
    public static void broadcastToSlaves(CustomPacketPayload packet)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(null,packet);
    }
    public static void broadcastToSlaves(UUID senderPlayerUUID, CustomPacketPayload packet)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(senderPlayerUUID,packet);
    }
    public static void broadcastToSlaves(CustomPacketPayload packet, String excludeServerId)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(null,packet, excludeServerId);
    }
    public static void broadcastToSlaves(UUID senderPlayerUUID, CustomPacketPayload packet, String excludeServerId)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(senderPlayerUUID,packet, excludeServerId);
    }
    public static void broadcastToSlaves(CustomPacketPayload packet, List<String> excludeServerIds)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(null,packet, excludeServerIds);
    }
    public static void broadcastToSlaves(UUID senderPlayerUUID, CustomPacketPayload packet, List<String> excludeServerIds)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(senderPlayerUUID,packet, excludeServerIds);
    }



    private static boolean checkSendToMaster()
    {
        if(instance == null)
        {
            error("sendToMaster(packet): Cant send a packet before initializing the ServerServerManager as slave by calling ServerServerManager.createSlave(...)");
            return false;
        }
        if(instance.slaveClient == null)
        {
            if(instance.tcpServer != null)
            {
                error("sendToMaster(packet): It seems like this ServerServerManager is initialized as master. A master can't send packets to another master!");
            }
            else  {
                throw new IllegalStateException("sendToMaster(packet): It should not be possible that the ServerServerManager is initialized, but neither as master nor as slave");
            }
            return false;
        }
        if(!instance.slaveClient.isConnected())
        {
            if(instance.slaveClient.isConnected())
            {
                error("sendToMaster(packet): Failed to establish a connection to the Master during initialization. Reason: "+instance.slaveClient.getConnectionFailReason());
            }
            else
                error("sendToMaster(packet): The connection to the Master has not yet been established. Have you forgotten to call ServerServerManager.start()?");
            return false;
        }
        return true;
    }
    private static boolean checkSendToSlave(String serverId)
    {
        if(instance == null)
        {
            error("sendToSlave(targetServer='"+serverId+"', packet): Cant send a packet before initializing the ServerServerManager as master by calling ServerServerManager.createMaster(...)");
            return false;
        }

        if(instance.tcpServer == null)
        {
            if(instance.slaveClient != null)
            {
                error("sendToSlave(targetServer='"+serverId+"', packet): It seems like this ServerServerManager is initialized as slave. A slave can't send packets to another slave!");
            }
            else  {
                throw new IllegalStateException("sendToSlave(targetServer='"+serverId+"', packet): It should not be possible that the ServerServerManager is initialized, but neither as master nor as slave");
            }
            return false;
        }
        if(!instance.tcpServer.isRunning())
        {
            if(instance.tcpServer.isStartupFailed())
            {
                error("sendToSlave(targetServer='"+serverId+"', packet): The TCP server had failed to startup during initialization. Reason: "+instance.tcpServer.getStartupFailReason());
            }
            else
                error("sendToSlave(targetServer='"+serverId+"', packet): The TCP server is not running. Have you forgotten to call ServerServerManager.start()?");
            return false;
        }
        return true;
    }
    private static boolean checkBroadcastToSlaves()
    {
        if(instance == null)
        {
            error("broadcastToSlaves(...): Cant broadcast a packet before initializing the ServerServerManager as master by calling ServerServerManager.createMaster(...)");
            return false;
        }

        if(instance.tcpServer == null)
        {
            if(instance.slaveClient != null)
            {
                error("broadcastToSlaves(...): It seems like this ServerServerManager is initialized as slave. A slave can't broadcast packets to another slave!");
            }
            else  {
                throw new IllegalStateException("broadcastToSlaves(...): It should not be possible that the ServerServerManager is initialized, but neither as master nor as slave");
            }
            return false;
        }
        if(!instance.tcpServer.isRunning())
        {
            if(instance.tcpServer.isStartupFailed())
            {
                error("broadcastToSlaves(...): The TCP server had failed to startup during initialization. Reason: "+instance.tcpServer.getStartupFailReason());
            }
            else
                error("broadcastToSlaves(...): The TCP server is not running. Have you forgotten to call ServerServerManager.start()?");
            return false;
        }
        return true;
    }

    private static void info(String message) {
        ModUtilitiesMod.LOGGER.info("[ServerServerManager]: "+message);
    }
    private static void error(String message) {
        ModUtilitiesMod.LOGGER.error("[ServerServerManager]: "+message);
    }
    private static void error(String message, Throwable throwable) {
        ModUtilitiesMod.LOGGER.error("[ServerServerManager]: "+message, throwable);
    }
    private static void warn(String message) {
        ModUtilitiesMod.LOGGER.warn("[ServerServerManager]: "+message);
    }
    private static void debug(String message) {
        ModUtilitiesMod.LOGGER.debug("[ServerServerManager]: "+message);
    }
}
