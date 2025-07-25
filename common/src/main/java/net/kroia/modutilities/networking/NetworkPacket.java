package net.kroia.modutilities.networking;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class NetworkPacket implements INetworkPacket {

    private net.kroia.modutilities.networking.NetworkManager manager;
    private Env environment;
    private ServerPlayer player;
    public NetworkPacket() {
        super();
    }

    public NetworkPacket(FriendlyByteBuf buf) {
        super();
        this.fromBytes(buf);
    }
    public void setManager(net.kroia.modutilities.networking.NetworkManager manager) {
        this.manager = manager;
    }
    protected net.kroia.modutilities.networking.NetworkManager getManager() {
        return this.manager;
    }

    protected void handleOnClient() {
    }
    protected void handleOnServer(ServerPlayer sender) {
    }
    @Override
    public void receive(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = (NetworkManager.PacketContext)contextSupplier.get();
        // Check if is client
        Env env = context.getEnvironment();
        environment = env;
        if(env == Env.CLIENT) {
            Minecraft.getInstance().submit(this::handleOnClient);

        } else if(env == Env.SERVER) {
            Player sender = context.getPlayer();
            if(sender instanceof ServerPlayer serverPlayer) {
                player = serverPlayer;
                Objects.requireNonNull(serverPlayer.getServer()).submit(() -> {
                    this.handleOnServer(serverPlayer);
                });
            }
        }
    }

    protected boolean sendResponse(INetworkPacket packet)
    {
        if(manager == null || packet == null || environment == null)
            return false;
        if(environment == Env.CLIENT)
        {
            manager.sendToServer(packet);
        }
        else if(environment == Env.SERVER)
        {
            if(player == null)
                return false;
            manager.sendToClient(player, packet);
        }
        return true;
    }
}
