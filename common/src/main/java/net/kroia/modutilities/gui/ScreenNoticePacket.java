package net.kroia.modutilities.gui;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//import net.minecraft.resources.ResourceLocation; // mc<=1.21.10
import net.minecraft.resources.Identifier; // mc>=1.21.11

/** S2C: shows a {@link ScreenNotice} on the client. Sent by {@link ScreenNotice#send}. */
public class ScreenNoticePacket extends NetworkPacket {
    //public static final Type<ScreenNoticePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "screen_notice_packet")); // mc<=1.21.10
    public static final Type<ScreenNoticePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "screen_notice_packet")); // mc>=1.21.11

    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenNoticePacket> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, ScreenNoticePacket::getMessage,
            ScreenNoticePacket::new
    );

    private final Component message;

    public ScreenNoticePacket(Component message) {
        this.message = message;
    }

    public Component getMessage() {
        return message;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    protected void handleOnClient(NetworkManager.PacketContext context) {
        ScreenNotice.show(message);
    }

    @Override
    protected void handleOnServer(NetworkManager.PacketContext context) {
    }
}
