package net.kroia.modutilities.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Map;

/**
 * Contract for objects that participate in the server save/load lifecycle and
 * produce data large enough to require chunking across multiple files.
 * Implementations supply (or consume) a map of named {@link ListTag} instances,
 * each of which is split across files to circumvent Mojang's default 2 MB NBT
 * size limit.
 *
 * @apiNote
 * Each map key becomes a sub-folder name; the corresponding {@link ListTag} is
 * split into chunk files within that folder. Both methods are called
 * server-side only.
 */
public interface ServerSaveableChunked {

    /**
     * Writes this object's persistent state into the given map of list tags.
     * Each entry will be persisted as a chunked NBT structure.
     *
     * @param listTags the map to populate; keys become folder names, values hold the data.
     *
     * @return {@code true} if the state was saved successfully; {@code false} otherwise.
     */
    boolean save(Map<String, ListTag> listTags);

    /**
     * Restores this object's persistent state from the given map of list tags.
     *
     * @param listTags the map of previously saved list tags, keyed by folder name.
     *
     * @return {@code true} if the state was loaded successfully; {@code false} otherwise.
     */
    boolean load(Map<String, ListTag> listTags);
}
