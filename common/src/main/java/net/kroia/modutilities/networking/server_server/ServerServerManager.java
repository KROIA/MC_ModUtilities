package net.kroia.modutilities.networking.server_server;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.server_server.master.MasterTCPServer;
import net.kroia.modutilities.networking.server_server.slave.SlaveServerClient;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public class ServerServerManager
{
    private static ServerServerManager instance;

    private final @Nullable MasterTCPServer  tcpServer;
    private final @Nullable SlaveServerClient slavePacketHandler;



    private ServerServerManager(@Nullable MasterTCPServer master, @Nullable SlaveServerClient slave) {
        this.tcpServer = master;
        this.slavePacketHandler = slave;
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
        SlaveServerClient slave = new SlaveServerClient(mcServer, sharedSecret, slaveServerID,  masterHostIP, masterHostTcpPort);
        instance = new ServerServerManager(null, slave);
        return true;
    }



    public static boolean isSlave()
    {
        if(instance == null)
            return false;
        return instance.slavePacketHandler != null;
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
            assert instance.slavePacketHandler != null;
            if(instance.slavePacketHandler.isConnected())
            {
                warn("Already connected to the master");
                return true;
            }
            instance.slavePacketHandler.connect();
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
            instance.slavePacketHandler.disconnect();
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
            assert instance.slavePacketHandler != null;
            return instance.slavePacketHandler.isConnected();
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
                assert instance.slavePacketHandler != null;
                instance.slavePacketHandler.disconnect();
            }
        }
        instance = null;
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
