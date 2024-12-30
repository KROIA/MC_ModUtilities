package net.kroia.modutilities.networking;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class NetworkPacket implements INetworkPacket {
    public NetworkPacket() {
        super();
    }

    public NetworkPacket(FriendlyByteBuf buf) {
        super();
        this.fromBytes(buf);
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
        if(env == Env.CLIENT) {
            Minecraft.getInstance().submit(this::handleOnClient);

        } else if(env == Env.SERVER) {
            Player sender = context.getPlayer();
            if(sender instanceof ServerPlayer serverPlayer) {
                Objects.requireNonNull(serverPlayer.getServer()).submit(() -> {
                    this.handleOnServer(serverPlayer);
                });
            }
        }

    }
}
