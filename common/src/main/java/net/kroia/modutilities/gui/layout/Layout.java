package net.kroia.modutilities.gui.layout;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.elements.base.GuiElement;

/**
 * Base class for layout strategies that arrange the children of a {@link GuiElement}.
 * <p>
 * Layouts are attached via {@link GuiElement#setLayout(Layout)}. Whenever the parent's bounds
 * or child list change, the layout's {@link #apply(GuiElement)} method runs from the parent's
 * {@code layoutChangedInternal()} pipeline. Concrete subclasses include {@link LayoutHorizontal},
 * {@link LayoutVertical} and {@link LayoutGrid}.
 */
@Environment(EnvType.CLIENT)
public abstract class Layout {
    /** When false the layout is skipped and child positions/sizes are left untouched. */
    public boolean enabled = true;
    /** Padding (in pixels) reserved between the parent's edges and its children. */
    public int padding = GuiElement.DEFAULT_PADDING;
    /** Spacing (in pixels) between adjacent children. */
    public int spacing = GuiElement.DEFAULT_PADDING;
    /** When true children are stretched to fill the parent on the X axis. */
    public boolean stretchX = false;
    /** When true children are stretched to fill the parent on the Y axis. */
    public boolean stretchY = false;

    /**
     * Creates a layout with default padding/spacing and no stretching.
     */
    public Layout(){

    }

    /**
     * Creates a layout with the given padding, spacing and stretch flags.
     * @param padding padding in pixels around the children
     * @param spacing spacing in pixels between adjacent children
     * @param stretchX whether to stretch children on the X axis
     * @param stretchY whether to stretch children on the Y axis
     */
    public Layout(int padding, int spacing, boolean stretchX, boolean stretchY) {
        this.padding = padding;
        this.spacing = spacing;
        this.stretchX = stretchX;
        this.stretchY = stretchY;
    }

    /**
     * Re-positions and (optionally) re-sizes the children of {@code element} according to this layout.
     * @param element the parent whose children will be arranged
     */
    public abstract void apply(GuiElement element);
}

