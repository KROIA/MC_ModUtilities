package net.kroia.modutilities.networking;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public abstract class NetworkPacketC2S extends BaseC2SMessage {
    public NetworkPacketC2S() {
        super();
    }

    public NetworkPacketC2S(RegistryFriendlyByteBuf buf) {
        super();
        this.fromBytes(buf);
    }

    abstract protected void handleOnServer(ServerPlayer sender);

    public void toBytes(RegistryFriendlyByteBuf buf){}

    public void fromBytes(RegistryFriendlyByteBuf buf){}

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        this.toBytes(buf);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if(context.getEnvironment() == Env.SERVER &&
                context.getPlayer() instanceof ServerPlayer serverPlayer) {
            Objects.requireNonNull(serverPlayer.getServer()).submit(() -> {
                this.handleOnServer(serverPlayer);
            });
        }
    }
}
