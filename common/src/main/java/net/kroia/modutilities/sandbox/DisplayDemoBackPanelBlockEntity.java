package net.kroia.modutilities.sandbox;

import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.display.AbstractDisplayBlockEntity;
import net.kroia.modutilities.gui.display.ContentBuilder;
import net.kroia.modutilities.gui.display.DisplayConfig;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class DisplayDemoBackPanelBlockEntity extends AbstractDisplayBlockEntity {

    private int tick = 0;

    public DisplayDemoBackPanelBlockEntity(BlockPos pos, BlockState blockState) {
        super(SandboxRegistration.DISPLAY_DEMO_BACK_PANEL_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public DisplayConfig getDisplayConfig() {
        return DisplayConfig.backPanel();
    }

    @Override
    public ContentBuilder getContentBuilder() {
        return DisplayDemoBackPanelBlockEntity::buildContent;
    }

    @Override
    public String getChannelId() {
        return "back_panel";
    }

    @Override
    public boolean opensSyncedScreenOnUse() {
        return false;
    }

    @Override
    protected void wireCallbacks(Gui gui) {}

    @Override
    protected void onControllerTick() {
        tick++;
        if (tick % 20 == 0 && gui != null) {
            for (var el : gui.getElements()) {
                if (el instanceof Label l && "timer".equals(l.getId())) {
                    l.setText("Uptime: " + (tick / 20) + "s");
                    syncToClientPublic();
                    break;
                }
            }
        }
    }

    @Override
    protected void saveCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("tick", tick);
    }

    @Override
    protected void loadCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        tick = tag.getInt("tick");
    }

    private static void buildContent(Gui gui, int w, int h) {
        int margin = 8;

        Label title = new Label("Back Panel Demo");
        title.setBounds(0, margin, w, 14);
        title.setAlignment(GuiElement.Alignment.CENTER);
        gui.addElement(title);

        Label info = new Label("No interaction screen");
        info.setBounds(0, margin + 20, w, 12);
        info.setAlignment(GuiElement.Alignment.CENTER);
        gui.addElement(info);

        Label timer = new Label("Uptime: 0s");
        timer.setId("timer");
        timer.setBounds(0, h / 2, w, 14);
        timer.setAlignment(GuiElement.Alignment.CENTER);
        gui.addElement(timer);
    }
}
