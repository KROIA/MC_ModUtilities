package net.kroia.modutilities.networking;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public abstract class NetworkPacketS2C extends BaseS2CMessage {
    public NetworkPacketS2C() {
        super();
    }

    public NetworkPacketS2C(RegistryFriendlyByteBuf buf) {
        super();
        this.fromBytes(buf);
    }

    abstract protected void handleOnClient();
    public void toBytes(RegistryFriendlyByteBuf buf){}

    public void fromBytes(RegistryFriendlyByteBuf buf){}

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        this.toBytes(buf);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if(context.getEnvironment() == Env.CLIENT) {
            Minecraft.getInstance().submit(this::handleOnClient);
        }
    }
}
