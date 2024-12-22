package net.kroia.modutilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.UUID;

public class ClientInteraction {


    public static void printToConsole(UUID playerUUID, String msg)
    {
        ServerPlayer player = getOnlinePlayer(playerUUID);
        if(player == null)
            return;
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
    }
    public static void printToClientConsole(String msg)
    {
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            throw new IllegalStateException("Server instance is null. Are you calling this from the server_sender?");
        }

        // Get the player list and fetch the player by UUID
        PlayerList playerList = server.getPlayerList();
        for(ServerPlayer player : playerList.getPlayers())
        {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
        }
    }

    public static ServerPlayer getOnlinePlayer(UUID uuid)
    {
        if(uuid == null)
            return null;

        // Get the Minecraft server_sender instance
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            throw new IllegalStateException("Server instance is null. Are you calling this from the server_sender?");
        }

        // Get the player list and fetch the player by UUID
        PlayerList playerList = server.getPlayerList();
        return playerList.getPlayer(uuid); // Returns null if the player is not online
    }
    public static ServerPlayer getOnlinePlayer(String name)
    {
        if(name == null)
            return null;
        // Get the Minecraft server_sender instance
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            throw new IllegalStateException("Server instance is null. Are you calling this from the server_sender?");
        }

        // Get the player list and fetch the player by UUID
        PlayerList playerList = server.getPlayerList();
        return playerList.getPlayerByName(name); // Returns null if the player is not online
    }
}
