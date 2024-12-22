package net.kroia.modutilities;

import net.minecraft.nbt.CompoundTag;

public interface ServerSaveable {

    boolean save(CompoundTag tag);
    boolean load(CompoundTag tag);
}
