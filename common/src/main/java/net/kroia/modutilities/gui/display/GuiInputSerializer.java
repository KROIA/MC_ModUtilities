package net.kroia.modutilities.gui.display;

import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.elements.CheckBox;
import net.kroia.modutilities.gui.elements.EmptyButton;
import net.kroia.modutilities.gui.elements.TextBox;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.Slider;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * Serializes and deserializes GUI input element state to/from a {@link CompoundTag}.
 * <p>
 * Elements are keyed by their index path in the element tree, using the same
 * traversal order as {@link net.kroia.modutilities.gui.GuiStateSync}. This allows
 * the client interaction screen to send input state to the server without needing
 * direct access to the server-side GUI.
 */
public class GuiInputSerializer {

    /**
     * Serializes all input element state from the GUI into a CompoundTag.
     * Captures slider values, textbox text, checkbox state, and button click counts.
     *
     * @param gui the GUI whose input state to serialize
     * @return a CompoundTag containing the serialized input state
     */
    public static CompoundTag serializeInput(Gui gui) {
        CompoundTag tag = new CompoundTag();
        serializeElements(gui.getElements(), tag, "");
        return tag;
    }

    /**
     * Applies serialized input state to the target GUI.
     * For sliders/textboxes/checkboxes: sets value directly.
     * For buttons: uses {@link EmptyButton#syncClickCount(int)} to replay missed clicks.
     *
     * @param tag the serialized input state
     * @param gui the target GUI to apply state to
     */
    public static void applyInput(CompoundTag tag, Gui gui) {
        applyElements(gui.getElements(), tag, "");
    }

    private static void serializeElements(List<GuiElement> elements, CompoundTag tag, String prefix) {
        for (int i = 0; i < elements.size(); i++) {
            GuiElement el = elements.get(i);
            String key = prefix + i;
            if (el instanceof Slider s) {
                tag.putDouble("s" + key, s.getSliderValue());
            }
            if (el instanceof TextBox t) {
                tag.putString("t" + key, t.getText());
            }
            if (el instanceof CheckBox c) {
                tag.putBoolean("c" + key, c.isChecked());
            }
            if (el instanceof EmptyButton b) {
                tag.putInt("b" + key, b.getClickCount());
            }
            // Recurse into children
            List<GuiElement> children = el.getChilds();
            if (!children.isEmpty()) {
                serializeElements(children, tag, key + "_");
            }
        }
    }

    private static void applyElements(List<GuiElement> elements, CompoundTag tag, String prefix) {
        for (int i = 0; i < elements.size(); i++) {
            GuiElement el = elements.get(i);
            String key = prefix + i;
            if (el instanceof Slider s && tag.contains("s" + key)) {
                double val = tag.getDouble("s" + key);
                if (Math.abs(s.getSliderValue() - val) > 0.001) {
                    s.setSliderValue(val);
                }
            }
            if (el instanceof TextBox t && tag.contains("t" + key)) {
                String text = tag.getString("t" + key);
                if (!text.equals(t.getText())) {
                    t.setText(text);
                }
            }
            if (el instanceof CheckBox c && tag.contains("c" + key)) {
                boolean checked = tag.getBoolean("c" + key);
                if (c.isChecked() != checked) {
                    c.setChecked(checked);
                }
            }
            if (el instanceof EmptyButton b && tag.contains("b" + key)) {
                int count = tag.getInt("b" + key);
                b.syncClickCount(count);
            }
            // Recurse into children
            List<GuiElement> children = el.getChilds();
            if (!children.isEmpty()) {
                applyElements(children, tag, key + "_");
            }
        }
    }
}
