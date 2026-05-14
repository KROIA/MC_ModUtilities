package net.kroia.modutilities.gui;

import net.minecraft.nbt.CompoundTag;

/**
 * Records a single structural change (element added or removed) on a {@link Gui}.
 * Used for future delta-sync support where only structural diffs are sent
 * instead of the full tree.
 */
public class GuiStructuralChange {
    public enum Type { ADDED, REMOVED }

    private final Type type;
    private final String parentId;
    private final int index;
    private final CompoundTag elementData;

    public GuiStructuralChange(Type type, String parentId, int index, CompoundTag elementData) {
        this.type = type;
        this.parentId = parentId;
        this.index = index;
        this.elementData = elementData;
    }

    public Type getType() { return type; }
    public String getParentId() { return parentId; }
    public int getIndex() { return index; }
    public CompoundTag getElementData() { return elementData; }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        if (parentId != null) tag.putString("parentId", parentId);
        tag.putInt("index", index);
        if (elementData != null) tag.put("data", elementData);
        return tag;
    }

    public static GuiStructuralChange deserialize(CompoundTag tag) {
        Type type = Type.valueOf(tag.getString("type"));
        String parentId = tag.contains("parentId") ? tag.getString("parentId") : null;
        int index = tag.getInt("index");
        CompoundTag data = tag.contains("data") ? tag.getCompound("data") : null;
        return new GuiStructuralChange(type, parentId, index, data);
    }
}
