package net.kroia.modutilities.gui;

import net.kroia.modutilities.gui.elements.CheckBox;
import net.kroia.modutilities.gui.elements.EmptyButton;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.Plot;
import net.kroia.modutilities.gui.elements.TextBox;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.Slider;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Generic utility to synchronize interactive state between two {@link Gui}
 * instances that share the same element structure (built by the same method).
 * <p>
 * The sync walks both element trees in parallel by index and copies
 * type-specific interactive state (slider values, text content, checkbox
 * state, label text/color, enabled state) from the source to the target.
 * <p>
 * Layout properties (position, size, font scale, colors, outline) are
 * <b>not</b> copied. Focus and hover state are also excluded.
 */
public class GuiStateSync {

    /**
     * Copies all interactive state from source to target.
     */
    public static void syncState(Gui source, Gui target) {
        walkElements(source, target, true, true);
    }

    /**
     * Copies only computed/display state (Plot data, Label text) from source to target.
     * Does NOT copy interactive state (slider values, textbox text, checkbox).
     */
    public static void syncDisplayState(Gui source, Gui target) {
        walkElements(source, target, false, true);
    }

    /**
     * Copies only interactive input state (slider values, textbox, checkbox) from source to target.
     * Does NOT copy display state (Plot data, server-driven labels).
     */
    public static void syncInputState(Gui source, Gui target) {
        walkElements(source, target, true, false);
    }

    private static void walkElements(Gui source, Gui target, boolean syncInput, boolean syncDisplay) {
        List<GuiElement> srcElements = source.getElements();
        List<GuiElement> tgtElements = target.getElements();
        int count = Math.min(srcElements.size(), tgtElements.size());
        for (int i = 0; i < count; i++) {
            syncElementState(srcElements.get(i), tgtElements.get(i), syncInput, syncDisplay);
        }
    }

    /**
     * Recursively syncs the interactive state of a single element pair.
     * Elements must be of the same type for type-specific state to be copied.
     *
     * @param src the source element
     * @param tgt the target element
     */
    private static void syncElementState(GuiElement src, GuiElement tgt,
                                            boolean syncInput, boolean syncDisplay) {
        // Display state: Labels, Plot data (server → client)
        if (syncDisplay) {
            if (src instanceof Label s && tgt instanceof Label t
                    && !(src instanceof TextBox)) {
                if (!s.getText().equals(t.getText())) t.setText(s.getText());
                if (s.getTextColor() != t.getTextColor()) t.setTextColor(s.getTextColor());
            }
            if (src instanceof Plot s && tgt instanceof Plot t) {
                try {
                    List<Plot.PlotData> snapshot = new ArrayList<>();
                    for (var series : s.getPlotDataList()) {
                        Plot.PlotData copy = new Plot.PlotData();
                        copy.color = series.color;
                        copy.thickness = series.thickness;
                        copy.yValues.addAll(series.yValues);
                        snapshot.add(copy);
                    }
                    t.clearPlotData();
                    for (var copy : snapshot) {
                        t.addPlotData(copy);
                    }
                } catch (ConcurrentModificationException ignored) {
                }
            }
        }

        // Input state: Sliders, TextBoxes, CheckBoxes (client → server)
        if (syncInput) {
            if (src instanceof Slider s && tgt instanceof Slider t) {
                if (Math.abs(s.getSliderValue() - t.getSliderValue()) > 0.001) {
                    t.setSliderValue(s.getSliderValue());
                }
            }
            if (src instanceof TextBox s && tgt instanceof TextBox t) {
                if (!s.getText().equals(t.getText())) t.setText(s.getText());
            }
            if (src instanceof CheckBox s && tgt instanceof CheckBox t) {
                if (s.isChecked() != t.isChecked()) t.setChecked(s.isChecked());
            }
            if (src instanceof EmptyButton s && tgt instanceof EmptyButton t) {
                t.syncClickCount(s.getClickCount());
            }
        }

        // Always sync enabled state
        if (src.isEnabled() != tgt.isEnabled()) {
            tgt.setEnabled(src.isEnabled());
        }

        // Recurse into children
        List<GuiElement> srcChildren = src.getChilds();
        List<GuiElement> tgtChildren = tgt.getChilds();
        int childCount = Math.min(srcChildren.size(), tgtChildren.size());
        for (int i = 0; i < childCount; i++) {
            syncElementState(srcChildren.get(i), tgtChildren.get(i), syncInput, syncDisplay);
        }
    }
}
