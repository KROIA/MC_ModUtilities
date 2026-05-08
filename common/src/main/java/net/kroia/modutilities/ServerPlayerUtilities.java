package net.kroia.modutilities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Server-side helpers for messaging players, looking up online players, and modifying their inventories.
 * <p>
 * Most methods route through {@link UtilitiesPlatform#getServer()} to access the running
 * {@link MinecraftServer} and require the calling thread to be on the server side.
 *
 * @apiNote Use {@link ClientPlayerUtilities} for the client-side counterpart.
 */
public class ServerPlayerUtilities {

    /**
     * Sends a literal system message to the given player's chat.
     *
     * @param player the recipient; nothing happens if {@code null}
     * @param msg    the message to send
     */
    public static void printToClientConsole(ServerPlayer player, String msg)
    {
        if(player == null)
            return;
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
    }
    /**
     * Sends a literal system message to the player with the given UUID, if online.
     *
     * @param playerUUID the recipient's UUID
     * @param msg        the message to send
     */
    public static void printToClientConsole(UUID playerUUID, String msg)
    {
        ServerPlayer player = getOnlinePlayer(playerUUID);
        printToClientConsole(player, msg);
    }
    /**
     * Broadcasts a literal system message to every online player on the server.
     *
     * @param msg the message to broadcast
     * @throws IllegalStateException if the server instance is not available
     */
    public static void printToClientConsole(String msg)
    {
        MinecraftServer server = UtilitiesPlatform.getServer();

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
    /**
     * Sends a literal system message to the player with the given name, if online.
     *
     * @param userName the recipient's display name
     * @param msg      the message to send
     */
    public static void printToClientConsole(String userName, String msg)
    {
        ServerPlayer player = getOnlinePlayer(userName);
        if(player == null)
            return;
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
    }

    /**
     * Looks up a currently online player by UUID.
     *
     * @param uuid the player's UUID; {@code null} returns {@code null}
     * @return the online {@link ServerPlayer}, or {@code null} if not online
     * @throws IllegalStateException if the server instance is not available
     */
    public static ServerPlayer getOnlinePlayer(UUID uuid)
    {
        if(uuid == null)
            return null;

        // Get the Minecraft server_sender instance
        MinecraftServer server = UtilitiesPlatform.getServer();

        if (server == null) {
            throw new IllegalStateException("Server instance is null. Are you calling this from the server_sender?");
        }

        // Get the player list and fetch the player by UUID
        PlayerList playerList = server.getPlayerList();
        return playerList.getPlayer(uuid); // Returns null if the player is not online
    }
    /**
     * Looks up a currently online player by display name.
     *
     * @param name the player's name; {@code null} returns {@code null}
     * @return the online {@link ServerPlayer}, or {@code null} if no player with that name is online
     * @throws IllegalStateException if the server instance is not available
     */
    public static ServerPlayer getOnlinePlayer(String name)
    {
        if(name == null)
            return null;
        // Get the Minecraft server_sender instance
        MinecraftServer server = UtilitiesPlatform.getServer();

        if (server == null) {
            throw new IllegalStateException("Server instance is null. Are you calling this from the server_sender?");
        }

        // Get the player list and fetch the player by UUID
        PlayerList playerList = server.getPlayerList();
        return playerList.getPlayerByName(name); // Returns null if the player is not online
    }

    /**
     * Returns a snapshot of every currently online player.
     *
     * @return a new list containing all online {@link ServerPlayer}s
     * @throws IllegalStateException if the server instance is not available
     */
    public static ArrayList<ServerPlayer> getOnlinePlayers()
    {
        MinecraftServer server = UtilitiesPlatform.getServer();

        if (server == null) {
            throw new IllegalStateException("Server instance is null. Are you calling this from the server_sender?");
        }

        PlayerList playerList = server.getPlayerList();
        return new ArrayList<>(playerList.getPlayers());
    }

    /**
     * Returns the display names of every currently online player.
     *
     * @return a new list containing all online player names
     * @throws IllegalStateException if the server instance is not available
     */
    public static ArrayList<String> getOnlinePlayerNames()
    {
        ArrayList<String> playerNames = new ArrayList<>();
        MinecraftServer server = UtilitiesPlatform.getServer();

        if (server == null) {
            throw new IllegalStateException("Server instance is null. Are you calling this from the server_sender?");
        }

        PlayerList playerList = server.getPlayerList();
        for(ServerPlayer player : playerList.getPlayers())
        {
            playerNames.add(player.getName().getString());
        }
        return playerNames;
    }


    /**
     * Builds a map from UUID to display name for every currently online player.
     *
     * @return a new map keyed by player UUID with their display names as values
     * @throws IllegalStateException if the server instance is not available
     */
    public static Map<UUID, String> getUUIDToNameMap()
    {
        Map<UUID, String> uuidToNameMap = new HashMap<>();
        MinecraftServer server = UtilitiesPlatform.getServer();

        if (server == null) {
            throw new IllegalStateException("Server instance is null. Are you calling this from the server_sender?");
        }

        PlayerList playerList = server.getPlayerList();
        for(ServerPlayer player : playerList.getPlayers())
        {
            uuidToNameMap.put(player.getUUID(), player.getName().getString());
        }
        return uuidToNameMap;
    }

    /**
     * Adds the given item stack to the player's inventory.
     * It tries to add the item stack to the player's inventory until it is full or the stack is empty.
     *
     * @param player the player whose inventory will be modified
     * @param stack of items to be placed in the inventory
     * @return remaining amount that did not fit in the inventory
     *
     * @apiNote This method <b>mutates</b> the input {@code stack}'s count, setting it to the
     *          remaining amount that did not fit in the inventory. This is intentional —
     *          dependent mods (e.g. BankSystem) rely on passing the mutated stack to
     *          {@code dropItemAtPlayer} afterwards to handle overflow items.
     */
    public static int addToPlayerInventory(ServerPlayer player, ItemStack stack)
    {
        if(player == null || stack == null || stack.isEmpty())
            return 0;
        int remainingAmount = stack.getCount();
        int maxStackSize = stack.getMaxStackSize();
        ItemStack stackCpy = stack.copy();
        Inventory inventory = player.getInventory();

        for(int i=0; i<inventory.getContainerSize(); i++)
        {
            // Check if the index is one of armor slots or side hand
            if(!isMainInventorySlot(i))
                continue;

            ItemStack currentStack = inventory.getItem(i);
            if(currentStack.isEmpty() || (currentStack.is(stackCpy.getItem()) && currentStack.getCount() < maxStackSize))
            {
                // If the slot is empty or contains the same item and has space, add the stack
                int spaceInSlot = maxStackSize - currentStack.getCount();
                if(spaceInSlot > 0)
                {
                    int amountToAdd = Math.min(remainingAmount, spaceInSlot);
                    int currentAmount = currentStack.getCount();
                    currentStack = stack.copy();
                    currentStack.setCount(currentAmount + amountToAdd);
                    remainingAmount -= amountToAdd;
                    inventory.setItem(i, currentStack);
                }
            }
            if(remainingAmount <= 0)
                break; // No more items to add
        }
        stack.setCount(remainingAmount); // Update the original stack with the remaining amount
        return remainingAmount;
    }

    /**
     * Checks if the given index is one of the normal inventory slots:
     *  - Large inventory container
     *  - Toolbar
     * @param slotIndex
     * @return true if index is a normal item slot
     */
    public static boolean isMainInventorySlot(int slotIndex)
    {
        /*for(int i=0; i<Inventory.ALL_ARMOR_SLOTS.length; i++)
        {
            if(Inventory.ALL_ARMOR_SLOTS[i] == slotIndex)
                return false;
        }
        return slotIndex != Inventory.SLOT_OFFHAND;*/
        return slotIndex < 36 && slotIndex >= 0;
    }


}
