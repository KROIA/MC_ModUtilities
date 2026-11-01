package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.nbt.CompoundTag;

/**
 * Invisible grouping container for {@link GuiElement} children.
 * <p>
 * A {@code Frame} performs no rendering of its own and has no layout logic; it is
 * primarily used as a logical parent that lets you treat a collection of child
 * elements as a single unit (for positioning, visibility toggling, etc.).
 *
 * @apiNote Background and outline are inherited from {@link GuiElement} and are
 *          disabled by default styling expectations of a transparent grouping container,
 *          but can be re-enabled via the inherited {@code setEnableBackground} and
 *          {@code setEnableOutline} setters when a visible panel is desired.
 */
public class Frame extends GuiElement {




    /**
     * Creates a new {@code Frame} with the default size and position
     * inherited from {@link GuiElement}.
     */
    public Frame() {
        super();
    }

    /**
     * Creates a new {@code Frame} at the given position with the given size.
     *
     * @param x      the x-coordinate of the frame, relative to its parent
     * @param y      the y-coordinate of the frame, relative to its parent
     * @param width  the width of the frame in pixels
     * @param height the height of the frame in pixels
     */
    public Frame(int x, int y, int width, int height) {
        super(x, y, width, height);
    }


    @Override
    public SyncCategory getSyncCategory() {
        return SyncCategory.DISPLAY;
    }

    @Override
    public CompoundTag serializeState() {
        CompoundTag tag = super.serializeState();
        tag.putInt("bgColor", getBackgroundColor());
        tag.putInt("outColor", getOutlineColor());
        tag.putBoolean("bgOn", isBackgroundEnabled());
        tag.putBoolean("outOn", isOutlineEnabled());
        return tag;
    }

    @Override
    public void deserializeState(CompoundTag tag) {
        super.deserializeState(tag);
        if (tag.contains("bgColor")) setBackgroundColor(tag.getInt("bgColor"));
        if (tag.contains("outColor")) setOutlineColor(tag.getInt("outColor"));
        if (tag.contains("bgOn")) setEnableBackground(tag.getBoolean("bgOn"));
        if (tag.contains("outOn")) setEnableOutline(tag.getBoolean("outOn"));
    }

    @Override
    protected void render() {

    }

    @Override
    protected void layoutChanged() {

    }
}
