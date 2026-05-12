package net.kroia.modutilities.gui.display.client;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiStateSync;
import net.kroia.modutilities.gui.client.GuiScreen;
import net.kroia.modutilities.gui.display.AbstractDisplayBlockEntity;
import net.kroia.modutilities.gui.display.ContentBuilder;
import net.kroia.modutilities.gui.display.DisplayInputSyncPacket;
import net.kroia.modutilities.gui.display.GuiInputSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client-side interaction screen for display blocks.
 * <p>
 * Reads display state from the client-side block entity (synced via
 * {@code ClientboundBlockEntityDataPacket}) and sends input state changes
 * to the server via {@link DisplayInputSyncPacket}. Works on both
 * singleplayer and dedicated servers.
 */
@Environment(EnvType.CLIENT)
public class DisplayInteractionScreen extends GuiScreen {

    private BlockPos controllerPos;
    private int syncCounter = 0;
    private boolean contentBuilt = false;
    private boolean initialSyncDone = false;

    public DisplayInteractionScreen(BlockPos controllerPos) {
        super(Component.literal("Display Interaction"));
        this.controllerPos = controllerPos;
    }

    public static void open(BlockPos controllerPos) {
        Minecraft.getInstance().setScreen(new DisplayInteractionScreen(controllerPos));
    }

    /**
     * Gets the client-side block entity's Gui for display state reading.
     * Works on both singleplayer and dedicated server.
     */
    private Gui getDisplayGui() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        BlockEntity be = mc.level.getBlockEntity(controllerPos);
        if (be instanceof AbstractDisplayBlockEntity dbe) {
            AbstractDisplayBlockEntity ctrl = dbe.getControllerEntity();
            if (ctrl != null) {
                controllerPos = ctrl.getBlockPos();
                return ctrl.getGui();
            }
            return dbe.getGui();
        }
        return null;
    }

    private ContentBuilder getContentBuilder() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        BlockEntity be = mc.level.getBlockEntity(controllerPos);
        if (be instanceof AbstractDisplayBlockEntity dbe) {
            return dbe.getContentBuilder();
        }
        return null;
    }

    @Override
    protected void updateLayout(Gui gui) {
        if (!contentBuilt) {
            ContentBuilder builder = getContentBuilder();
            if (builder != null) {
                builder.build(gui, getWidth(), getHeight());
                contentBuilt = true;
            }
        }
        gui.init();

        Gui displayGui = getDisplayGui();
        if (displayGui != null) {
            GuiStateSync.syncState(displayGui, gui);
            initialSyncDone = true;
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics,
                       int mouseX, int mouseY, float partialTick) {
        syncCounter++;
        if (syncCounter >= 2) {
            syncCounter = 0;
            if (!initialSyncDone) {
                Gui displayGui = getDisplayGui();
                if (displayGui != null && gui != null) {
                    GuiStateSync.syncState(displayGui, gui);
                    initialSyncDone = true;
                }
            } else {
                syncDisplayFromClient();
                syncInputToServer();
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void syncDisplayFromClient() {
        Gui displayGui = getDisplayGui();
        if (displayGui == null || gui == null) return;
        GuiStateSync.syncDisplayState(displayGui, gui);
    }

    private void syncInputToServer() {
        if (gui == null) return;
        CompoundTag inputState = GuiInputSerializer.serializeInput(gui);
        NetworkManager.sendToServer(new DisplayInputSyncPacket(controllerPos, inputState, false));
    }

    @Override
    public void onClose() {
        // Send final input sync + release editor lock
        if (gui != null) {
            CompoundTag inputState = GuiInputSerializer.serializeInput(gui);
            NetworkManager.sendToServer(new DisplayInputSyncPacket(controllerPos, inputState, true));
        }
        super.onClose();
    }
}
