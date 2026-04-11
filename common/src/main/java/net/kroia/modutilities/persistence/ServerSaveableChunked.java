package net.kroia.modutilities.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Map;

public interface ServerSaveableChunked {
    boolean save(Map<String, ListTag> listTags);
    boolean load(Map<String, ListTag> listTags);
}
