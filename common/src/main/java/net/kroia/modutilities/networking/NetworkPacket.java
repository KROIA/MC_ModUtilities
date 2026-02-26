package net.kroia.modutilities.networking;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class NetworkPacket implements CustomPacketPayload {


    public NetworkPacket() {
        super();
    }


}
