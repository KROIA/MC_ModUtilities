package net.kroia.modutilities.persistence;

import net.minecraft.nbt.CompoundTag;

/**
 * Contract for objects that participate in the server save/load lifecycle.
 * Implementations are expected to serialize their state into the supplied
 * {@link CompoundTag} on save and restore it from the same tag on load.
 *
 * @apiNote
 * Save is invoked when the world is saved; load is invoked when the world is
 * loaded. Both methods are called server-side only.
 */
public interface ServerSaveable {

    /**
     * Writes this object's persistent state into the given tag.
     *
     * @param tag the tag to populate with this object's state.
     *
     * @return {@code true} if the state was saved successfully; {@code false} otherwise.
     */
    boolean save(CompoundTag tag);

    /**
     * Restores this object's persistent state from the given tag.
     *
     * @param tag the tag containing previously saved state.
     *
     * @return {@code true} if the state was loaded successfully; {@code false} otherwise.
     */
    boolean load(CompoundTag tag);
}
