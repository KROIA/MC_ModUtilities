package net.kroia.modutilities.sandbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiStateSync;
import net.kroia.modutilities.gui.client.GuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

@Environment(EnvType.CLIENT)
public class DisplayInteractionScreen extends GuiScreen {

    private BlockPos controllerPos;
    private int syncCounter = 0;
    private boolean dashboardBuilt = false;

    public DisplayInteractionScreen(BlockPos controllerPos) {
        super(Component.literal("Display Interaction"));
        this.controllerPos = controllerPos;
    }

    public static void open(BlockPos controllerPos) {
        Minecraft.getInstance().setScreen(new DisplayInteractionScreen(controllerPos));
    }

    @Override
    protected void updateLayout(Gui gui) {
        if (!dashboardBuilt) {
            int w = getWidth();
            int h = getHeight();
            DisplayDemoBlockEntity.buildDashboard(gui, w, h, null);
            dashboardBuilt = true;
        }

        // Re-init after elements were added/screen was resized
        gui.init();

        // Sync server state into the screen
        Gui serverGui = getServerGui();
        if (serverGui != null) {
            GuiStateSync.syncState(serverGui, gui);
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics,
                       int mouseX, int mouseY, float partialTick) {
        // Sync server → client: get latest plot data, label updates
        syncCounter++;
        if (syncCounter >= 2) {
            syncCounter = 0;
            syncDisplayFromServer();
            syncInputToServer();
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private boolean loggedNull = false;

    private void syncDisplayFromServer() {
        Gui serverGui = getServerGui();
        if (serverGui == null) {
            if (!loggedNull) {
                net.kroia.modutilities.ModUtilitiesMod.LOGGER.warn("[DisplayScreen] serverGui is null!");
                loggedNull = true;
            }
            return;
        }
        loggedNull = false;
        GuiStateSync.syncDisplayState(serverGui, gui);
    }

    private void syncInputToServer() {
        DisplayDemoBlockEntity controller = getServerController();
        if (controller != null && controller.getGui() != null) {
            GuiStateSync.syncInputState(gui, controller.getGui());
            controller.readStateFromGui();
        }
    }

    private DisplayDemoBlockEntity getServerController() {
        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (server == null || mc.level == null) return null;

        var serverLevel = server.getLevel(mc.level.dimension());
        if (serverLevel == null) {
            net.kroia.modutilities.ModUtilitiesMod.LOGGER.warn(
                    "[DisplayScreen] serverLevel is null for dimension {}", mc.level.dimension());
            return null;
        }

        BlockEntity be = serverLevel.getBlockEntity(controllerPos);
        net.kroia.modutilities.ModUtilitiesMod.LOGGER.info(
                "[DisplayScreen] Looking up pos={} -> be={} isLoaded={}",
                controllerPos,
                be != null ? be.getClass().getSimpleName() : "null",
                serverLevel.isLoaded(controllerPos));

        if (be instanceof DisplayDemoBlockEntity dbe) {
            DisplayDemoBlockEntity ctrl = dbe.getControllerEntity();
            if (ctrl != null && ctrl.getGui() != null) {
                controllerPos = ctrl.getBlockPos();
                return ctrl;
            }
            net.kroia.modutilities.ModUtilitiesMod.LOGGER.warn(
                    "[DisplayScreen] Found DisplayDemoBlockEntity but ctrl={} gui={}",
                    ctrl != null ? ctrl.getBlockPos() : "null",
                    ctrl != null ? (ctrl.getGui() != null) : "n/a");
        }

        return null;
    }

    private Gui getServerGui() {
        DisplayDemoBlockEntity ctrl = getServerController();
        return ctrl != null ? ctrl.getGui() : null;
    }

    @Override
    public void onClose() {
        syncInputToServer();

        DisplayDemoBlockEntity controller = getServerController();
        if (controller != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                controller.releaseEditor(mc.player.getUUID());
            }
        }

        super.onClose();
    }
}
