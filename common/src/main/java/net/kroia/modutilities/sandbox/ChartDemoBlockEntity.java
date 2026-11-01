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

/**
 * Sandbox block entity hosting a {@link SandboxLineChart} with dummy data.
 * Tests scissor clipping in the offscreen display block renderer.
 * <p>
 * The chart generates sine/cosine/square wave data and supports pan/zoom.
 * Scissor is used to clip lines to the chart canvas area — this should
 * clip correctly in both the interaction screen and on the block face.
 */
public class ChartDemoBlockEntity extends AbstractDisplayBlockEntity {

    public ChartDemoBlockEntity(BlockPos pos, BlockState blockState) {
        super(SandboxRegistration.CHART_DEMO_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public DisplayConfig getDisplayConfig() {
        return DisplayConfig.fullBlock();
    }

    @Override
    public ContentBuilder getContentBuilder() {
        return ChartDemoBlockEntity::buildContent;
    }

    @Override
    public String getChannelId() {
        return "chart_demo";
    }

    @Override
    protected void wireCallbacks(Gui gui) {}

    @Override
    protected void onControllerTick() {}

    @Override
    protected void saveCustomData(CompoundTag tag, HolderLookup.Provider registries) {}

    @Override
    protected void loadCustomData(CompoundTag tag, HolderLookup.Provider registries) {}

    private static void buildContent(Gui gui, int w, int h) {
        int margin = 8;

        Label title = new Label("Chart Scissor Test");
        title.setBounds(0, margin, w, 14);
        title.setAlignment(GuiElement.Alignment.CENTER);
        gui.addElement(title);

        SandboxLineChart chart = new SandboxLineChart();
        chart.setId("chart");
        chart.setBounds(margin, margin + 18, w - margin * 2, h - margin * 2 - 18);
        gui.addElement(chart);
    }
}
