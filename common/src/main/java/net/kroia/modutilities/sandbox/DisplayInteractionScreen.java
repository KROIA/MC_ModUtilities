package net.kroia.modutilities.sandbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiStateSync;
import net.kroia.modutilities.gui.client.GuiScreen;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.HorizontalSlider;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.TextBox;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

@Environment(EnvType.CLIENT)
public class DisplayInteractionScreen extends GuiScreen {

    private volatile BlockPos controllerPos;
    private int syncCounter = 0;
    private boolean dashboardBuilt = false;
    private volatile DisplayDemoBlockEntity cachedController = null;
    private volatile boolean lookupScheduled = false;
    private volatile boolean pauseRequested = false;
    private boolean initialSyncDone = false;

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

            for (var el : gui.getElements()) {
                if (el instanceof Button btn && "Pause".equals(btn.getText())) {
                    btn.setOnFallingEdge(() -> pauseRequested = true);
                    break;
                }
            }
        }

        // Re-init after elements were added/screen was resized
        gui.init();

        // Sync server state into the screen
        Gui serverGui = getServerGui();
        if (serverGui != null) {
            GuiStateSync.syncState(serverGui, gui);
            initialSyncDone = true;
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics,
                       int mouseX, int mouseY, float partialTick) {
        // Sync server → client: get latest plot data, label updates
        syncCounter++;
        if (syncCounter >= 2) {
            syncCounter = 0;
            if (!initialSyncDone) {
                Gui serverGui = getServerGui();
                if (serverGui != null) {
                    GuiStateSync.syncState(serverGui, gui);
                    initialSyncDone = true;
                }
            } else {
                syncDisplayFromServer();
                syncInputToServer();
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void syncDisplayFromServer() {
        Gui serverGui = getServerGui();
        if (serverGui == null) return;
        GuiStateSync.syncDisplayState(serverGui, gui);
    }

    private void syncInputToServer() {
        DisplayDemoBlockEntity controller = getServerController();
        if (controller == null) return;

        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (server == null) return;

        double capturedSpeed = -1;
        String capturedText = null;
        for (var el : gui.getElements()) {
            if (el instanceof HorizontalSlider slider) {
                capturedSpeed = slider.getSliderValue();
            }
            if (el instanceof TextBox tb) {
                capturedText = tb.getText();
            }
        }

        boolean doPause = pauseRequested;
        if (doPause) pauseRequested = false;

        final double speed = capturedSpeed;
        final String text = capturedText;

        server.execute(() -> {
            boolean changed = false;
            Gui serverGui = controller.getGui();

            if (serverGui != null && speed >= 0) {
                for (var el : serverGui.getElements()) {
                    if (el instanceof HorizontalSlider slider) {
                        if (Math.abs(slider.getSliderValue() - speed) > 0.001) {
                            slider.setSliderValue(speed);
                            changed = true;
                        }
                        break;
                    }
                }
                controller.readStateFromGui();
                for (var el : serverGui.getElements()) {
                    if (el instanceof Label label && label.getText() != null && label.getText().startsWith("Speed:")) {
                        label.setText("Speed: " + (int)(speed * 100) + "%");
                        break;
                    }
                }
            }

            if (serverGui != null && text != null) {
                for (var el : serverGui.getElements()) {
                    if (el instanceof TextBox tb) {
                        if (!text.equals(tb.getText())) {
                            controller.handleTextInput(text);
                            changed = true;
                        }
                        break;
                    }
                }
            }

            if (doPause) {
                controller.togglePaused();
                changed = true;
            }

            if (changed) {
                controller.syncToClientPublic();
            }
        });
    }

    private DisplayDemoBlockEntity getServerController() {
        DisplayDemoBlockEntity cached = cachedController;
        if (cached != null) {
            if (!cached.isRemoved()) return cached;
            cachedController = null;
        }

        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (server == null || mc.level == null) return null;

        if (!lookupScheduled) {
            lookupScheduled = true;
            BlockPos pos = controllerPos;
            server.execute(() -> {
                var serverLevel = server.getLevel(mc.level.dimension());
                if (serverLevel != null) {
                    BlockEntity be = serverLevel.getBlockEntity(pos);
                    if (be instanceof DisplayDemoBlockEntity dbe) {
                        DisplayDemoBlockEntity ctrl = dbe.getControllerEntity();
                        if (ctrl != null && ctrl.getGui() != null) {
                            controllerPos = ctrl.getBlockPos();
                            cachedController = ctrl;
                        }
                    }
                }
                lookupScheduled = false;
            });
        }

        return cachedController;
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
