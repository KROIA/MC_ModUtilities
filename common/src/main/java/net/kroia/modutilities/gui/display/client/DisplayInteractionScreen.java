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
    private boolean initialSyncDone = false;
    private int lastStructureVersion = -1;

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
        ContentBuilder builder = getContentBuilder();
        if (builder != null) {
            gui.removeAllElements();
            builder.build(gui, getWidth(), getHeight());
        }
        gui.init();

        Gui displayGui = getDisplayGui();
        if (displayGui != null) {
            GuiStateSync.syncState(displayGui, gui);
            lastStructureVersion = displayGui.getStructureVersion();
            initialSyncDone = true;
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics,
                       int mouseX, int mouseY, float partialTick) {
        boolean inputDirty = gui != null && gui.hasAnyDirty();
        syncCounter++;
        if (syncCounter >= 2 || inputDirty) {
            syncCounter = 0;
            if (!initialSyncDone) {
                Gui displayGui = getDisplayGui();
                if (displayGui != null && gui != null) {
                    GuiStateSync.syncState(displayGui, gui);
                    lastStructureVersion = displayGui.getStructureVersion();
                    initialSyncDone = true;
                }
            } else {
                Gui displayGui = getDisplayGui();
                if (displayGui != null && gui != null
                        && displayGui.getStructureVersion() != lastStructureVersion) {
                    CompoundTag treeData = displayGui.serializeTree();
                    gui.deserializeTree(treeData);
                    gui.init();
                    lastStructureVersion = displayGui.getStructureVersion();
                } else {
                    syncDisplayFromClient();
                }
                syncInputToServer();
                if (gui != null) gui.clearAllDirty();
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
