package net.kroia.modutilities.gui.display;

import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * Serializes and deserializes GUI element state to/from a {@link CompoundTag}.
 * <p>
 * Elements are keyed by their ID (if set) or index path in the element tree.
 * Each element's state is stored as a nested CompoundTag produced by
 * {@link GuiElement#serializeState()}, and restored via
 * {@link GuiElement#deserializeState(CompoundTag)}.
 */
public class GuiInputSerializer {

    /**
     * Serializes all element state from the GUI into a CompoundTag.
     *
     * @param gui the GUI whose state to serialize
     * @return a CompoundTag containing the serialized state
     */
    public static CompoundTag serializeInput(Gui gui) {
        CompoundTag tag = new CompoundTag();
        serializeElements(gui.getElements(), tag, "");
        return tag;
    }

    /**
     * Serializes only dirty element state from the GUI into a CompoundTag.
     * The resulting tag can be applied with {@link #applyInput(CompoundTag, Gui)}
     * since it only processes keys that are present.
     *
     * @param gui the GUI whose dirty state to serialize
     * @return a CompoundTag containing only the dirty elements' state
     */
    public static CompoundTag serializeDirtyInput(Gui gui) {
        CompoundTag tag = new CompoundTag();
        serializeDirtyElements(gui.getElements(), tag, "");
        return tag;
    }

    /**
     * Applies serialized state to the target GUI.
     * Only elements whose key is present in the tag are updated,
     * so this works for both full and delta (dirty-only) tags.
     *
     * @param tag the serialized state
     * @param gui the target GUI to apply state to
     */
    public static void applyInput(CompoundTag tag, Gui gui) {
        applyElements(gui.getElements(), tag, "");
    }

    private static String elementKey(GuiElement el, String prefix, int index) {
        String id = el.getId();
        if (id != null) return prefix + id;
        return prefix + index;
    }

    private static void serializeElements(List<GuiElement> elements, CompoundTag tag, String prefix) {
        for (int i = 0; i < elements.size(); i++) {
            GuiElement el = elements.get(i);
            String key = elementKey(el, prefix, i);
            if (el.getSyncCategory() == GuiElement.SyncCategory.INPUT) {
                tag.put(key, el.serializeState());
            }
            List<GuiElement> children = el.getChilds();
            if (!children.isEmpty()) {
                serializeElements(children, tag, key + "_");
            }
        }
    }

    private static void serializeDirtyElements(List<GuiElement> elements, CompoundTag tag, String prefix) {
        for (int i = 0; i < elements.size(); i++) {
            GuiElement el = elements.get(i);
            String key = elementKey(el, prefix, i);
            if (el.isDirty() && el.getSyncCategory() == GuiElement.SyncCategory.INPUT) {
                tag.put(key, el.serializeState());
            }
            List<GuiElement> children = el.getChilds();
            if (!children.isEmpty()) {
                serializeDirtyElements(children, tag, key + "_");
            }
        }
    }

    private static void applyElements(List<GuiElement> elements, CompoundTag tag, String prefix) {
        for (int i = 0; i < elements.size(); i++) {
            GuiElement el = elements.get(i);
            String key = elementKey(el, prefix, i);
            if (tag.contains(key)) {
                el.deserializeState(tag.getCompound(key));
            }
            List<GuiElement> children = el.getChilds();
            if (!children.isEmpty()) {
                applyElements(children, tag, key + "_");
            }
        }
    }
}
