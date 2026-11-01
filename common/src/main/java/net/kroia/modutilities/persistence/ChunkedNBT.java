package net.kroia.modutilities.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.*;
import net.minecraft.util.Tuple;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Utility class for handling NBT data that exceeds Mojang's default 2 MB size limit.
 * Provides helpers for splitting large NBT structures across multiple files (chunks)
 * and measuring the uncompressed serialized size of arbitrary {@link Tag} instances.
 *
 * @apiNote
 * Most of the chunking/rebuilding logic in this class is currently disabled
 * (kept as commented reference). The active public surface is limited to
 * {@link #getUncompressedSize(Tag)}.
 */
public class ChunkedNBT {
/*
    private static final UUID sizeUUID = UUID.nameUUIDFromBytes("sizeUUID".getBytes());
    //private static final UUID treeIndexUUID = UUID.nameUUIDFromBytes("treeIndexUUID".getBytes());
    public static final String SIZE_KEY = "size_"+ sizeUUID.toString().replace("-", "_");
    public static final String TREE_INDEX_KEY = "i";//"treeindex_"+ treeIndexUUID.toString().replace("-", "_");
    public static final String TREE_DATA_KEY = "d";


    private static final long MAX_NBT_SIZE = 2_097_152L; // 2 MB

    public static class FlatNBT {
        ListTag chunks = new ListTag();
        int rootId;
    }

    public static FlatNBT flatten(CompoundTag root) throws IOException {
        FlatNBT flat = new FlatNBT();
        int rootId = splitTag(root, flat);
        flat.rootId = rootId;
        return flat;
    }

    public static CompoundTag rebuild(FlatNBT flat) {
        //return rebuildIterative(flat);
    }


    public static void collectCompounds(CompoundTag tag, List<CompoundTag> buffer)
    {
        if(tag == null || tag.isEmpty()) return;

        buffer.add(tag);

        for(String key : tag.getAllKeys())
        {
            Tag value = tag.get(key);
            if(value instanceof CompoundTag child)
            {
                collectCompounds(child, buffer);
            }else if(value instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND)
            {
                for(Tag element : list)
                {
                    collectCompounds((CompoundTag) element, buffer);
                }
            }
        }
    }
    public static void collectAndReplaceCompounds(CompoundTag tag, List<CompoundTag> buffer,
                                                  Map<Integer, CompoundTag> referencedRootsTags,
                                                  Map<Integer, Tuple<ListTag, Integer>> referencedRootsLists)
    {
        if(tag == null || tag.isEmpty()) return;

        class Data
        {
            public String key;
            public Tag value;
            public long size;
        }

        //buffer.add(tag);
        //Map<Tuple<String,Tag>, Long> sizeMap = new HashMap<>();
        List<Data> sizeList = new ArrayList<>(tag.getAllKeys().size());
        for(String key : tag.getAllKeys())
        {
            Tag value = tag.get(key);
            long size = getUncompressedSize(singleEntry(key, value));
            Data data = new Data();
            data.key = key;
            data.value = value;
            data.size = size;
            sizeList.add(data);
        }
        // Sort by size descending
        sizeList.sort((a, b) -> Long.compare(b.size, a.size));

        for(int i=0; i<sizeList.size(); ++i)
        {
            Data data = sizeList.get(i);
            Tag value = data.value;
            if(value instanceof CompoundTag child)
            {
                long size = getUncompressedSize(child);
                if(size > MAX_NBT_SIZE)
                    collectAndReplaceCompounds(child, buffer, referencedRootsTags, referencedRootsLists);
                else {
                    // Replace the compound with a reference
                    buffer.add(child);
                    CompoundTag refTag = new CompoundTag();
                    int ref = buffer.size() - 1; // Use the current size as the reference
                    refTag.putInt("__ref__", ref); // Use the current size as the reference
                    referencedRootsTags.put(ref, tag); // Store the reference for later use
                    tag.put(data.key, refTag); // Replace the original compound with the reference
                }


            }else if(value instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND)
            {
                for(int j=0; j<list.size(); j++)
                {
                    Tag element = list.get(j);
                    if(element instanceof CompoundTag compoundElement)
                    {
                        collectAndReplaceCompounds(compoundElement, buffer, referencedRootsTags, referencedRootsLists);


                        // Replace the compound with a reference
                        buffer.add(compoundElement);
                        CompoundTag refTag = new CompoundTag();
                        int ref = buffer.size() - 1; // Use the current size as the reference
                        refTag.putInt("__ref__", ref); // Use the current size as the reference
                        list.set(j, refTag); // Replace the original compound with the reference
                        referencedRootsLists.put(ref, new Tuple<>(list, j)); // Store the reference for later use

                    }
                }
            }
        }


        for(String key : tag.getAllKeys())
        {

        }
    }

    public static void replaceCompoundByRef(CompoundTag tag, CompoundTag oldValue, int ref) {
        if(tag == null || tag.isEmpty()) return;

        for(String key : tag.getAllKeys())
        {
            Tag value = tag.get(key);
            if(value instanceof CompoundTag child)
            {
                if(child.equals(oldValue)) {
                    CompoundTag refTag = new CompoundTag();
                    refTag.putInt("__ref__", ref);
                    tag.put(key, refTag);
                } else {
                    replaceCompoundByRef(child, oldValue, ref);
                }
            } else if(value instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND) {
                for(int i = 0; i < list.size(); i++) {
                    CompoundTag element = (CompoundTag) list.get(i);
                    if(element.equals(oldValue)) {
                        CompoundTag refTag = new CompoundTag();
                        refTag.putInt("__ref__", ref);
                        list.set(i, refTag);
                    } else {
                        replaceCompoundByRef(element, oldValue, ref);
                    }
                }
            }
        }
    }


    private static int splitTag(CompoundTag tag, FlatNBT flat) throws IOException {
        List<CompoundTag> extractedCompounds = new ArrayList<>();
        Map<Integer, CompoundTag> referencedRootsTags = new HashMap<>();
        Map<Integer, Tuple<ListTag, Integer>> referencedRootsLists = new HashMap<>();

        collectAndReplaceCompounds(tag, extractedCompounds, referencedRootsTags, referencedRootsLists);

        long tagSize = getUncompressedSize(tag);
        if(tagSize > MAX_NBT_SIZE) {
            // Needs to be split appart more than it already is
            System.out.println("Tag size exceeds maximum chunk size, further splitting required: " + tagSize);
            return -1;
        }




    }*/

/*
    private static CompoundTag singleEntry(String key, Tag value) {
        CompoundTag t = new CompoundTag();
        t.put(key, value);
        return t;
    }*/


    /*private static int splitTag(CompoundTag tag, FlatNBT flat) throws IOException {
        CompoundTag currentChunk = new CompoundTag();
        long currentSize = 0;

        int startIndex = flat.chunks.size(); // index of this compound’s first chunk
        Map<String, Tag> delayed = new LinkedHashMap<>();

        for (String key : tag.getAllKeys()) {
            Tag value = tag.get(key);

            long entrySize = getUncompressedSize(singleEntry(key, value));

            if (entrySize > MAX_NBT_SIZE) {
                // Needs recursive splitting
                if (value instanceof CompoundTag child) {
                    int childId = splitTag(child, flat);
                    CompoundTag ref = new CompoundTag();
                    ref.putInt("__ref__", childId);
                    delayed.put(key, ref);
                } else if (value instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND) {
                    ListTag newList = new ListTag();
                    for (Tag el : list) {
                        CompoundTag child = (CompoundTag) el;
                        int childId = splitTag(child, flat);
                        CompoundTag ref = new CompoundTag();
                        ref.putInt("__ref__", childId);
                        newList.add(ref);
                    }
                    delayed.put(key, newList);
                } else {
                    throw new IOException("Single NBT element too large to split further: " + key);
                }
            } else {
                if (currentSize + entrySize > MAX_NBT_SIZE) {
                    flat.chunks.add(currentChunk);
                    currentChunk = new CompoundTag();
                    currentSize = 0;
                }
                currentChunk.put(key, value.copy());
                currentSize += entrySize;
            }
        }

        if (!currentChunk.isEmpty()) {
            flat.chunks.add(currentChunk);
        }

        // If the compound was split into multiple chunks, create a "wrapper"
        if (flat.chunks.size() - startIndex > 1 || !delayed.isEmpty()) {
            CompoundTag wrapper = new CompoundTag();

            // Add references for the chunks
            ListTag refs = new ListTag();
            for (int i = startIndex; i < flat.chunks.size(); i++) {
                CompoundTag ref = new CompoundTag();
                ref.putInt("__ref__", i);
                refs.add(ref);
            }
            wrapper.put("__chunks__", refs);

            // Add delayed children (big ones that had to be split)
            for (Map.Entry<String, Tag> e : delayed.entrySet()) {
                wrapper.put(e.getKey(), e.getValue());
            }

            flat.chunks.add(wrapper);
            return flat.chunks.size() - 1;
        }

        // If only one chunk was made, return its index
        return flat.chunks.size() - 1;
    }

    private static CompoundTag singleEntry(String key, Tag value) {
        CompoundTag t = new CompoundTag();
        t.put(key, value.copy());
        return t;
    }




    // Build the full CompoundTag from chunk graph without recursion.
    public static CompoundTag rebuildIterative(FlatNBT flat) {
        final ListTag chunks = flat.chunks;
        final int n = chunks.size();

        // parents[u] = list of nodes that depend on u
        List<List<Integer>> parents = new ArrayList<>(n);
        for (int i = 0; i < n; i++) parents.add(new ArrayList<>());

        // indegree[v] = number of dependencies v needs before it can be built
        int[] indegree = new int[n];

        // Build dependency graph
        for (int v = 0; v < n; v++) {
            Set<Integer> deps = collectRefs(chunks.getCompound(v)); // all ids referenced by v
            indegree[v] = deps.size();
            for (int u : deps) parents.get(u).add(v);
        }

        // Start with nodes that have no deps
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (indegree[i] == 0) q.add(i);

        Map<Integer, CompoundTag> built = new HashMap<>(n);

        while (!q.isEmpty()) {
            int v = q.remove();
            CompoundTag builtV = assembleNode(v, chunks, built); // use already-built children
            built.put(v, builtV);

            for (int p : parents.get(v)) {
                if (--indegree[p] == 0) q.add(p);
            }
        }

        if (!built.containsKey(flat.rootId)) {
            throw new IllegalStateException("Could not rebuild root; cycle or missing chunks?");
        }
        return built.get(flat.rootId);
    }

    // Collect every referenced chunk id from a chunk (both __chunks__ pieces and inline refs).
    private static Set<Integer> collectRefs(CompoundTag base) {
        Set<Integer> refs = new HashSet<>();

        if (base.contains("__chunks__")) {
            ListTag list = base.getList("__chunks__", Tag.TAG_COMPOUND);
            for (Tag t : list) refs.add(((CompoundTag) t).getInt("__ref__"));
        }

        for (String k : base.getAllKeys()) {
            if ("__chunks__".equals(k)) continue; // assembly metadata, not real content

            Tag v = base.get(k);
            if (v instanceof CompoundTag ct && ct.contains("__ref__")) {
                refs.add(ct.getInt("__ref__"));
            } else if (v instanceof ListTag lt && lt.getElementType() == Tag.TAG_COMPOUND) {
                for (Tag e : lt) {
                    CompoundTag ct = (CompoundTag) e;
                    if (ct.contains("__ref__")) refs.add(ct.getInt("__ref__"));
                }
            }
        }
        return refs;
    }

    // Build one node from its children (already present in `built`).
    private static CompoundTag assembleNode(int id,
                                            ListTag chunks,
                                            Map<Integer, CompoundTag> built) {
        CompoundTag base = chunks.getCompound(id);

        // If this is a wrapper, create a fresh container for the merged children
        CompoundTag out = new CompoundTag();

        if (base.contains("__chunks__")) {
            ListTag pieceRefs = base.getList("__chunks__", Tag.TAG_COMPOUND);
            for (Tag t : pieceRefs) {
                int pieceId = ((CompoundTag) t).getInt("__ref__");
                CompoundTag piece = built.get(pieceId);
                if (piece == null)
                    throw new IllegalStateException("Piece " + pieceId + " not built before " + id);
                for (String k : piece.getAllKeys()) {
                    out.put(k, piece.get(k));
                }
            }
        } else {
            // Plain chunk, just copy all keys
            for (String k : base.getAllKeys()) {
                if (!"__chunks__".equals(k)) {
                    out.put(k, base.get(k));
                }
            }
        }

        // Now resolve inline refs inside out
        List<String> keys = new ArrayList<>(out.getAllKeys());
        for (String k : keys) {
            Tag v = out.get(k);

            if (v instanceof CompoundTag ct && ct.contains("__ref__")) {
                int childId = ct.getInt("__ref__");
                CompoundTag child = built.get(childId);
                if (child == null)
                    throw new IllegalStateException("Child " + childId + " not built before " + id);
                out.put(k, child);
            } else if (v instanceof ListTag lt && lt.getElementType() == Tag.TAG_COMPOUND) {
                ListTag rebuiltList = new ListTag();
                for (Tag e : lt) {
                    CompoundTag ct = (CompoundTag) e;
                    if (ct.contains("__ref__")) {
                        int childId = ct.getInt("__ref__");
                        CompoundTag child = built.get(childId);
                        if (child == null)
                            throw new IllegalStateException("Child " + childId + " not built before " + id);
                        rebuiltList.add(child);
                    } else {
                        rebuiltList.add(ct.copy());
                    }
                }
                out.put(k, rebuiltList);
            }
        }

        return out;
    }*/


    /*

    private static CompoundTag rebuildRec(int id, ListTag chunks, Map<Integer, CompoundTag> cache) {
        if (cache.containsKey(id)) return cache.get(id);

        CompoundTag base = chunks.getCompound(id);
        CompoundTag result = new CompoundTag();

        if (base.contains("__chunks__")) {
            ListTag refs = base.getList("__chunks__", Tag.TAG_COMPOUND);
            for (Tag ref : refs) {
                int cid = ((CompoundTag) ref).getInt("__ref__");
                CompoundTag childChunk = chunks.getCompound(cid);
                for (String key : childChunk.getAllKeys()) {
                    result.put(key, childChunk.get(key));
                }
            }
        } else {
            for (String key : base.getAllKeys()) {
                result.put(key, base.get(key));
            }
        }

        // Handle delayed children (recursive references)
        for (String key : base.getAllKeys()) {
            Tag value = base.get(key);
            if (value instanceof CompoundTag ref && ref.contains("__ref__")) {
                int childId = ref.getInt("__ref__");
                result.put(key, rebuildRec(childId, chunks, cache));
            } else if (value instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND) {
                ListTag rebuiltList = new ListTag();
                for (Tag el : list) {
                    CompoundTag ref = (CompoundTag) el;
                    int childId = ref.getInt("__ref__");
                    rebuiltList.add(rebuildRec(childId, chunks, cache));
                }
                result.put(key, rebuiltList);
            }
        }

        cache.put(id, result);
        return result;
    }*/

    /*

    public static class FlatNBT {
        public ListTag chunks = new ListTag(); // list of chunks, each chunk is a CompoundTag
        public int rootId;
    }



*/

    /**
     * Computes the uncompressed serialized size in bytes of the given tag.
     * Non-{@link CompoundTag} tags are wrapped into a temporary compound under
     * the key {@code "d"} before measurement, since {@link NbtIo#write} requires
     * a {@link CompoundTag} root.
     *
     * @param tag the tag whose serialized size should be measured.
     *
     * @apiNote
     * Uses try-with-resources for the underlying streams to avoid leaking
     * resources. Returns {@code 0} on I/O failure (the exception is logged to
     * standard error).
     *
     * @return the uncompressed size in bytes, or {@code 0} if measurement failed.
     */
    public static long getUncompressedSize(Tag tag)  {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // Write the NBT to a byte array to measure its raw uncompressed size
            if(tag instanceof CompoundTag compoundTag)
            {
                NbtIo.write(compoundTag, dos);
            }
            else
            {
                CompoundTag wrapper = new CompoundTag();
                wrapper.put("d", tag);
                NbtIo.write(wrapper, dos);
            }
            dos.flush();

            return baos.size();
        }catch(IOException e) {
            e.printStackTrace();
            return 0; // Return 0 if there was an error
        }
    }
}
