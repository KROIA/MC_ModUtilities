package net.kroia.modutilities.gui.display.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiStateSync;
import net.kroia.modutilities.gui.client.GuiScreen;
import net.kroia.modutilities.gui.display.AbstractDisplayBlockEntity;
import net.kroia.modutilities.gui.display.ContentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

@Environment(EnvType.CLIENT)
public class DisplayInteractionScreen extends GuiScreen {

    private volatile BlockPos controllerPos;
    private int syncCounter = 0;
    private boolean contentBuilt = false;
    private volatile AbstractDisplayBlockEntity cachedController = null;
    private volatile boolean lookupScheduled = false;
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
        if (!contentBuilt) {
            ContentBuilder builder = getContentBuilderFromClient();
            if (builder != null) {
                builder.build(gui, getWidth(), getHeight());
                contentBuilt = true;
            }
        }

        gui.init();

        Gui serverGui = getServerGui();
        if (serverGui != null) {
            GuiStateSync.syncState(serverGui, gui);
            initialSyncDone = true;
        }
    }

    private ContentBuilder getContentBuilderFromClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        BlockEntity be = mc.level.getBlockEntity(controllerPos);
        if (be instanceof AbstractDisplayBlockEntity dbe) {
            return dbe.getContentBuilder();
        }
        return null;
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics,
                       int mouseX, int mouseY, float partialTick) {
        syncCounter++;
        if (syncCounter >= 2) {
            syncCounter = 0;
            if (!initialSyncDone) {
                Gui serverGui = getServerGui();
                if (serverGui != null && gui != null) {
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
        if (serverGui == null || gui == null) return;
        GuiStateSync.syncDisplayState(serverGui, gui);
    }

    private void syncInputToServer() {
        AbstractDisplayBlockEntity controller = getServerController();
        if (controller == null || gui == null) return;

        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (server == null) return;

        final Gui clientGui = this.gui;

        server.execute(() -> {
            Gui serverGui = controller.getGui();
            if (serverGui != null) {
                GuiStateSync.syncInputState(clientGui, serverGui);
                controller.onInputSynced();
                controller.syncToClientPublic();
            }
        });
    }

    private AbstractDisplayBlockEntity getServerController() {
        AbstractDisplayBlockEntity cached = cachedController;
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
                    if (be instanceof AbstractDisplayBlockEntity dbe) {
                        AbstractDisplayBlockEntity ctrl = dbe.getControllerEntity();
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
        AbstractDisplayBlockEntity ctrl = getServerController();
        return ctrl != null ? ctrl.getGui() : null;
    }

    @Override
    public void onClose() {
        syncInputToServer();

        AbstractDisplayBlockEntity controller = getServerController();
        if (controller != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                controller.releaseEditor(mc.player.getUUID());
            }
        }

        super.onClose();
    }
}
