package net.kroia.modutilities.gui.display;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client-to-server packet that sends GUI input state from the interaction screen
 * to the server-side block entity.
 * <p>
 * Contains the controller block position, a CompoundTag with serialized input state
 * (slider values, textbox text, checkbox state, button click counts), and a closing
 * flag that releases the editor lock when the player closes the screen.
 */
public class DisplayInputSyncPacket extends NetworkPacket {

    public static final Type<DisplayInputSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "display_input_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DisplayInputSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, p -> p.posX,
                    ByteBufCodecs.INT, p -> p.posY,
                    ByteBufCodecs.INT, p -> p.posZ,
                    ByteBufCodecs.COMPOUND_TAG, p -> p.inputState,
                    ByteBufCodecs.BOOL, p -> p.closing,
                    DisplayInputSyncPacket::new
            );

    private final int posX;
    private final int posY;
    private final int posZ;
    private final CompoundTag inputState;
    private final boolean closing;

    public DisplayInputSyncPacket(BlockPos controllerPos, CompoundTag inputState, boolean closing) {
        this(controllerPos.getX(), controllerPos.getY(), controllerPos.getZ(), inputState, closing);
    }

    private DisplayInputSyncPacket(int posX, int posY, int posZ, CompoundTag inputState, boolean closing) {
        super();
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.inputState = inputState;
        this.closing = closing;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    protected void handleOnClient(NetworkManager.PacketContext context) {
        // C2S only — no client handling
    }

    @Override
    protected void handleOnServer(NetworkManager.PacketContext context) {
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        if (player == null) return;

        BlockPos controllerPos = new BlockPos(posX, posY, posZ);
        BlockEntity be = player.level().getBlockEntity(controllerPos);
        if (!(be instanceof AbstractDisplayBlockEntity dbe)) return;

        // Ensure we apply to the controller entity
        AbstractDisplayBlockEntity controller = dbe.getControllerEntity();
        if (controller == null) controller = dbe;

        Gui gui = controller.getGui();
        if (gui != null && !inputState.isEmpty()) {
            GuiInputSerializer.applyInput(inputState, gui);
            controller.onInputSynced();
            controller.syncToClientPublic();
        }

        if (closing) {
            controller.releaseEditor(player.getUUID());
        }
    }
}
