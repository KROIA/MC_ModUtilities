package net.kroia.modutilities.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * High-level NBT/JSON file-based persistence service.
 * Handles save/load of {@link CompoundTag} data (with optional gzip compression),
 * splits oversized {@link ListTag} data into multiple chunk files to circumvent
 * Mojang's 2 MB NBT limit, and offers convenience helpers for JSON serialization
 * via Gson.
 *
 * @apiNote
 * The save path is composed of {@code levelSavePath} (set per world) and
 * {@code relativeSavePath} (mod-specific subfolder). Calling
 * {@link #getAbsoluteSavePath()} before {@link #setLevelSavePath(Path)} throws
 * {@link IllegalStateException}. Internal directory listings use
 * try-with-resources to avoid leaking file handles, which is important on
 * Windows where leaked handles block subsequent directory operations.
 */
public class DataPersistence {
    /**
     * Output style used when serializing JSON.
     */
    public enum JsonFormat {
        PRETTY, COMPACT
    }
    /**
     * Specifies whether NBT data is stored using gzip compression or in raw form.
     */
    public enum NbtFormat {
        COMPRESSED, UNCOMPRESSED
    }

    // Mojang default max (2 MB)
    private static final long MAX_NBT_SIZE = 2_097_152L;


    public Consumer<String> errorLogger = System.err::println;
    public Consumer<String> warnLogger = System.out::println;
    public BiConsumer<String, Throwable> errorLoggerThrowable = (error, throwable) -> {
        if (errorLogger != null) {
            errorLogger.accept(error + "\nError: " + throwable.getMessage() + "\n" + Arrays.toString(throwable.getStackTrace()));
        }
    };
    public Consumer<String> debugLogger = System.out::println;

    private final Gson GSON;
    private Path relativeSavePath;
    private Path levelSavePath;


    private final JsonFormat jsonFormat;
    private final NbtFormat nbtFormat;

    /**
     * Creates a new persistence service.
     *
     * @param format           the JSON output format (pretty or compact).
     * @param nbtFormat        the NBT storage format (compressed or uncompressed).
     * @param relativeSavePath the path, relative to {@code levelSavePath}, where
     *                         this service stores its data.
     */
    public DataPersistence(JsonFormat format, NbtFormat nbtFormat, Path relativeSavePath) {
        this.jsonFormat = format;
        if (format == JsonFormat.PRETTY) {
            this.GSON = new GsonBuilder().setPrettyPrinting().create();
        } else {
            this.GSON = new GsonBuilder().create();
        }
        this.relativeSavePath = relativeSavePath;
        this.nbtFormat = nbtFormat;
    }

    /**
     * Configures the logger callbacks used by this service.
     *
     * @param errorLogger receives error messages.
     * @param debugLogger receives debug messages.
     * @param warnLogger  receives warning messages.
     */
    public void setLogger(Consumer<String> errorLogger, Consumer<String> debugLogger, Consumer<String> warnLogger) {
        this.errorLogger = errorLogger;
        this.debugLogger = debugLogger;
        this.warnLogger = warnLogger;
    }
    /**
     * Configures the logger callbacks used by this service, including a
     * dedicated handler for errors with an associated {@link Throwable}.
     *
     * @param errorLogger          receives plain error messages.
     * @param errorLoggerThrowable receives error messages paired with their causing throwable.
     * @param debugLogger          receives debug messages.
     * @param warnLogger           receives warning messages.
     */
    public void setLogger(Consumer<String> errorLogger, BiConsumer<String, Throwable>errorLoggerThrowable, Consumer<String> debugLogger, Consumer<String> warnLogger) {
        this.errorLogger = errorLogger;
        this.errorLoggerThrowable = errorLoggerThrowable;
        this.debugLogger = debugLogger;
        this.warnLogger = warnLogger;
    }



    /**
     * @return the configured JSON output format.
     */
    public JsonFormat getJsonFormat() {
        return jsonFormat;
    }
    /**
     * @return the configured NBT storage format.
     */
    public NbtFormat getNbtFormat() {
        return nbtFormat;
    }

    /**
     * Sets the per-world root save path. Must be called before any save/load
     * operation that resolves an absolute path.
     *
     * @param levelSavePath the absolute path of the current world's save directory.
     */
    public void setLevelSavePath(Path levelSavePath) {
        this.levelSavePath = levelSavePath;
    }
    /**
     * @return the per-world root save path, or {@code null} if not yet set.
     */
    public Path getLevelSavePath() {
        return levelSavePath;
    }
    /**
     * Sets the path, relative to {@link #getLevelSavePath()}, where this
     * service stores its data.
     *
     * @param relativeSavePath the new relative save path.
     */
    public void setRelativeSavePath(Path relativeSavePath) {
        this.relativeSavePath = relativeSavePath;
    }
    /**
     * @return the relative save path used by this service.
     */
    public Path getRelativeSavePath() {
        return relativeSavePath;
    }
    /**
     * Returns the absolute save path: {@code levelSavePath} resolved against
     * {@code relativeSavePath}.
     *
     * @apiNote
     * Throws {@link IllegalStateException} if {@link #setLevelSavePath(Path)}
     * has not been called yet (this used to surface as an NPE).
     *
     * @return the absolute save directory for this service.
     *
     * @throws IllegalStateException if {@code levelSavePath} has not been set.
     */
    public Path getAbsoluteSavePath() {
        Path level = getLevelSavePath();
        if (level == null) {
            throw new IllegalStateException("levelSavePath is not set; call setLevelSavePath() first");
        }
        return level.resolve(relativeSavePath);
    }
    /**
     * Returns the absolute save path with an additional sub-path appended.
     *
     * @param relativeAdded the additional relative path component to append.
     *
     * @return the resolved absolute path.
     *
     * @throws IllegalStateException if {@code levelSavePath} has not been set.
     */
    public Path getAbsoluteSavePath(String relativeAdded) {
        return getAbsoluteSavePath().resolve(relativeAdded);
    }

    protected boolean createSaveFolder() {
        if(folderExists(getAbsoluteSavePath())) {
            return true;
        }
        return createFolder(getAbsoluteSavePath());
    }
    /**
     * Creates the given folder (and any missing parents) if it does not exist.
     *
     * @param path the absolute path of the folder to create.
     *
     * @return {@code true} if the folder existed or was created successfully;
     *         {@code false} if creation failed.
     */
    public boolean createFolder(Path path) {
        File folder = new File(path.toUri());
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                debug("Created folder: " + folder.getAbsolutePath());
                return true;
            } else {
                error("Failed to create folder: " + folder.getAbsolutePath());
                return false;
            }
        }
        return true;
    }
    /**
     * Checks whether a regular file exists at the given path.
     *
     * @param path the path to test.
     *
     * @return {@code true} if a regular file exists at the path.
     */
    public boolean fileExists(Path path) {
        File file = new File(path.toUri());
        return file.exists() && file.isFile();
    }
    /**
     * Checks whether a directory exists at the given path.
     *
     * @param path the path to test.
     *
     * @return {@code true} if a directory exists at the path.
     */
    public boolean folderExists(Path path) {
        File folder = new File(path.toUri());
        return folder.exists() && folder.isDirectory();
    }





    protected void debug(String message) {
        if (debugLogger != null) {
            debugLogger.accept("[DataPersistence]: " + message);
        }
    }
    protected void error(String message) {
        if (errorLogger != null) {
            errorLogger.accept("[DataPersistence]: " + message);
        }
    }
    protected void error(String message, Throwable throwable) {
        if (errorLoggerThrowable != null) {
            errorLoggerThrowable.accept("[DataPersistence]: "+ message, throwable);
        } else if (errorLogger != null) {
            errorLogger.accept("[DataPersistence]: " + message + "\nError: " + throwable.getMessage() + "\n" + Arrays.toString(throwable.getStackTrace()));
        }
    }
    protected void warn(String message) {
        if (warnLogger != null) {
            warnLogger.accept("[DataPersistence]: "+message);
        }
    }




    /**
     * Lists all regular files ending in {@code .json} within the given directory.
     *
     * @param absolutePath the directory to scan.
     *
     * @apiNote
     * Uses try-with-resources on the underlying directory stream to avoid
     * leaking file handles. Returns an empty list (not {@code null}) on I/O
     * failure; the error is logged.
     *
     * @return the list of matching paths, or an empty list on failure.
     */
    public List<Path> getJsonFiles(Path absolutePath) {
        try (var stream = Files.list(absolutePath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(_path -> _path.toString().endsWith(".json"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            error("Failed to list JSON files in directory: " + absolutePath, e);
            return List.of();
        }
    }
    /**
     * Lists all regular files in the given directory whose name ends with the
     * specified extension.
     *
     * @param absolutePath the directory to scan.
     * @param extension    the suffix to filter by (e.g. {@code ".nbt"}).
     *
     * @apiNote
     * Uses try-with-resources to avoid leaking file handles; errors are
     * logged and an empty list is returned.
     *
     * @return the list of matching paths, or an empty list on failure.
     */
    public List<Path> getFiles(Path absolutePath, String extension) {
        try (var stream = Files.list(absolutePath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(_path -> _path.toString().endsWith(extension))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            error("Failed to list files in directory: " + absolutePath, e);
            return List.of();
        }
    }
    /**
     * Lists all sub-directories of the given directory.
     *
     * @param absolutePath the directory to scan.
     *
     * @apiNote
     * Uses try-with-resources to avoid leaking file handles; errors are
     * logged and an empty list is returned.
     *
     * @return the list of sub-directory paths, or an empty list on failure.
     *
     * @deprecated Use {@link #getFolders(Path)} instead.
     */
    @Deprecated
    public List<Path> getFoldes(Path absolutePath) {
        try (var stream = Files.list(absolutePath)) {
            return stream
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            error("Failed to list folders in directory: " + absolutePath, e);
            return List.of();
        }
    }

    /**
     * Lists all sub-directories of the given directory.
     *
     * @param absolutePath the directory to scan.
     *
     * @apiNote
     * Uses try-with-resources to avoid leaking file handles; errors are
     * logged and an empty list is returned.
     *
     * @return the list of sub-directory paths, or an empty list on failure.
     */
    public List<Path> getFolders(Path absolutePath) {
        return getFoldes(absolutePath);
    }



    /**
     * Reads a {@link CompoundTag} from the given absolute file path.
     * The file's actual compression state is detected via gzip magic bytes;
     * if it disagrees with the configured {@link NbtFormat}, the actual format
     * is used and a warning is logged.
     *
     * @param absolutePath the absolute path of the NBT file to read.
     *
     * @apiNote
     * Returns {@code null} if the file does not exist or if reading fails;
     * errors are logged but no exception is thrown.
     *
     * @return the loaded compound tag, or {@code null} on failure.
     */
    public CompoundTag readDataCompound(Path absolutePath)
    {
        CompoundTag dataOut;
        File file = new File(absolutePath.toUri());
        if (file.exists()) {
            try {
                CompoundTag data;
                boolean isFileCompressed = isCompressed(file);
                NbtFormat nbtFormatLocal = nbtFormat;
                if(isFileCompressed && nbtFormat == NbtFormat.UNCOMPRESSED)
                {
                    warn("File " + absolutePath + " is compressed, but NBT format is set to UNCOMPRESSED. Reading as compressed.");
                    nbtFormatLocal = NbtFormat.COMPRESSED;
                }
                if(!isFileCompressed && nbtFormat == NbtFormat.COMPRESSED)
                {
                    warn("File " + absolutePath + " is not compressed, but NBT format is set to COMPRESSED. Reading as uncompressed.");
                    nbtFormatLocal = NbtFormat.UNCOMPRESSED;
                }
                if(nbtFormatLocal == NbtFormat.COMPRESSED)
                    data = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
                else
                    data = NbtIo.read(file.toPath());

                dataOut = data;
                return dataOut;
            } catch(Exception e)
            {
                error("Failed to read data from file: " + absolutePath, e);
            }
        }
        return null;
    }
    /**
     * Saves the given {@link CompoundTag} to the specified absolute file path
     * using the configured {@link NbtFormat}.
     * Logs a warning if the uncompressed payload exceeds the maximum NBT size
     * (2 MB); the file is still written.
     *
     * @param absolutePath the absolute path of the NBT file to write.
     * @param data         the compound tag to serialize.
     *
     * @return {@code true} on success; {@code false} on I/O failure.
     */
    public boolean saveDataCompound(Path absolutePath, CompoundTag data) {
        long startMillis = System.currentTimeMillis();
        boolean success = true;

        long uncompressedSize = 0;
        uncompressedSize = ChunkedNBT.getUncompressedSize(data);
        if(uncompressedSize > MAX_NBT_SIZE)
        {
            warn("Data size exceeds maximum NBT size of " + MAX_NBT_SIZE + " bytes.\n" +
                    "Consider splitting the data into a TagList and use the saveDataCompoundList() function to store the data.");
        }


        File file = new File(absolutePath.toUri());
        try {
            if (nbtFormat == NbtFormat.COMPRESSED)
                NbtIo.writeCompressed(data, file.toPath());
            else
                NbtIo.write(data, file.toPath());
        } catch(Exception e)
        {
            error("Failed to save data to file: " + absolutePath, e);
            success = false;
        }
        long endMillis = System.currentTimeMillis();
        debug("Saving data to file: " + absolutePath + " took " + (endMillis - startMillis) + "ms");
        return success;
    }
    /**
     * Saves a potentially oversized {@link ListTag} by splitting it into
     * multiple chunk files. The chunks are written into a sub-folder whose
     * name is derived from {@code absolutePath} (extension stripped), and the
     * folder is purged of any stale chunk files first.
     *
     * @param absolutePath the path used to derive the chunk folder name; its
     *                     parent directory is the location where the chunk
     *                     folder is created.
     * @param dataList     the list tag to split and persist.
     *
     * @apiNote
     * The element type of the list is stored in the first chunk under
     * {@code "elementType"} so it can be restored when reading.
     * Returns {@code false} if a single tag exceeds the 2 MB chunk limit.
     *
     * @return {@code true} if all chunks were written successfully; {@code false} otherwise.
     */
    public boolean saveDataCompoundList(Path absolutePath, ListTag dataList)
    {
        long startMillis = System.currentTimeMillis();
        boolean success = true;
       /* long uncompressedSize = 0;
        try {
            CompoundTag data = new CompoundTag();
            data.put("data", dataList);
            uncompressedSize = getUncompressedSize(data);
        }catch (IOException e) {
            error("Failed to get uncompressed size of data: " + absolutePath, e);
            success = false;
        }*/
        // Save the data to a single file
        if(!createSaveFolder()) {
            error("Failed to create save folder: " + getAbsoluteSavePath());
            return false;
        }


        String fileName = absolutePath.getFileName().toString();
        if(fileName.lastIndexOf(".") != -1)
        {
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        Path chunkFolderPath = absolutePath.getParent().resolve(fileName);

        if(folderExists(chunkFolderPath))
        {
            // delete all files in the folder
            try (var stream = Files.list(chunkFolderPath)) {
                stream.forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        error("Failed to delete file: " + file, e);
                    }
                });
            } catch (IOException e) {
                error("Failed to list files in folder: " + chunkFolderPath, e);
                return false;
            }
        }
        else {
            if (!createFolder(chunkFolderPath)) {
                error("Failed to create folder for large NBT data: " + chunkFolderPath);
                return false;
            }
        }


        // Pre-compute individual element sizes to avoid O(N²) re-serialization
        long[] elementSizes = new long[dataList.size()];
        for (int i = 0; i < dataList.size(); i++) {
            elementSizes[i] = ChunkedNBT.getUncompressedSize(dataList.get(i));
        }

        // Split the data into chunks using a running size total
        List<CompoundTag> chunks = new ArrayList<>();
        int processedTagCount = 0;
        boolean firstChunk = true;
        while(processedTagCount < dataList.size())
        {
            CompoundTag chunk = new CompoundTag();
            ListTag chunkList = new ListTag();
            if(firstChunk)
            {
                byte dataType = dataList.getElementType();
                chunk.putByte("elementType", dataType);
                firstChunk = false;
            }
            chunk.put("data", chunkList);

            // Measure the empty chunk overhead (compound wrapper + "data" key + list header)
            long chunkBaseSize = ChunkedNBT.getUncompressedSize(chunk);
            long runningSize = chunkBaseSize;

            for(; processedTagCount<dataList.size(); ++processedTagCount)
            {
                long elementSize = elementSizes[processedTagCount];

                // Check if adding this element would likely exceed the limit
                if(runningSize + elementSize > MAX_NBT_SIZE && !chunkList.isEmpty())
                {
                    // Verify with a full serialization before splitting, in case
                    // the running total over-estimated due to per-element wrapper overhead
                    chunkList.add(dataList.get(processedTagCount));
                    long actualSize = ChunkedNBT.getUncompressedSize(chunk);
                    if(actualSize > MAX_NBT_SIZE) {
                        chunkList.remove(chunkList.size() - 1);
                        break;
                    }
                    // Actual size fits; update running total with the real value
                    runningSize = actualSize;
                    continue;
                }

                chunkList.add(dataList.get(processedTagCount));
                runningSize += elementSize;

                // If running total exceeds the limit and the list only has this one element,
                // then the single element is too large
                if(runningSize > MAX_NBT_SIZE)
                {
                    if(chunkList.size() == 1)
                    {
                        error("The Tag at index: "+processedTagCount+" in the dataList is larger than the maximum NBT size of " + MAX_NBT_SIZE + " bytes.\n" +
                                "Consider splitting the data into smaller ListTag elements or using a different data format.", new Throwable("Data too large"));
                        return false;
                    }
                    // Remove the last tag that pushed us over
                    chunkList.remove(chunkList.size() - 1);
                    break;
                }
            }
            chunks.add(chunk);
        }
        // Save each chunk to a file
        for(int i = 0; i < chunks.size(); ++i) {
            Path chunkPath = chunkFolderPath.resolve("chunk_" + i + ".nbt");
            if(!saveDataCompound(chunkPath, chunks.get(i))) {
                error("Failed to save chunk data to file: " + chunkPath);
                success = false;
            }
        }
        long endMillis = System.currentTimeMillis();
        debug("Saving data to file: " + absolutePath + " took " + (endMillis - startMillis) + "ms");
        return success;
    }

    /**
     * Reads a chunked {@link ListTag} previously saved via
     * {@link #saveDataCompoundList(Path, ListTag)}. Locates the chunk folder
     * derived from {@code absolutePath} (extension stripped), reads each
     * {@code .nbt} chunk in name order, and concatenates their contents.
     *
     * @param absolutePath the path used to derive the chunk folder name.
     *
     * @apiNote
     * The list's element type is recovered from the first chunk's
     * {@code "elementType"} byte tag (defaults to {@code TAG_COMPOUND}).
     *
     * @return the reassembled list tag (possibly empty); never {@code null}.
     */
    public ListTag readDataCompoundList(Path absolutePath) {
        ListTag dataOut;

        // Check if the path is a file or folder containing chunks
        String fileName = absolutePath.getFileName().toString();
        if(fileName.lastIndexOf(".") != -1)
        {
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        Path folderPath = absolutePath.getParent().resolve(fileName);

        dataOut = new ListTag();
        // If it's a folder, read all chunk files
        try {
            List<Path> chunkFiles = getFiles(folderPath, ".nbt");
            // Sort files
            chunkFiles.sort(Comparator.naturalOrder());
            boolean firstChunk = true;
            byte elementType = Tag.TAG_COMPOUND;
            for (Path chunkFile : chunkFiles) {
                CompoundTag chunkData = readDataCompound(chunkFile);
                if(firstChunk) {
                    if(chunkData != null && chunkData.contains("elementType", Tag.TAG_BYTE))
                    {
                        elementType = chunkData.getByte("elementType");
                        firstChunk = false;
                    }
                }
                if (chunkData != null && chunkData.contains("data", Tag.TAG_LIST)) {
                    dataOut.addAll(chunkData.getList("data", elementType));
                } else {
                    error("No valid 'data' list found in chunk file: " + chunkFile);
                }
            }
        } catch (Exception e) {
            error("Failed to read data from folder: " + absolutePath, e);
        }

        return dataOut;
    }


    /**
     * Saves a map of named {@link ListTag} instances. Each entry is written
     * via {@link #saveDataCompoundList(Path, ListTag)} into a sub-folder
     * named after the entry key.
     *
     * @param absolutePath the parent folder under which each entry's chunk
     *                     folder is created.
     * @param dataListMap  the map of list tags to persist; keys become folder names.
     *
     * @return {@code true} if all entries were saved successfully; {@code false} otherwise.
     */
    public boolean saveDataCompoundListMap(Path absolutePath, Map<String, ListTag> dataListMap)
    {
        if(!createSaveFolder()) {
            error("Failed to create save folder: " + getAbsoluteSavePath());
            return false;
        }
        if (!createFolder(absolutePath)) {
            error("Failed to create folder for large NBT data map: " + absolutePath);
            return false;
        }

        // Save each ListTag to a separate file
        boolean success = true;
        for (Map.Entry<String, ListTag> entry : dataListMap.entrySet()) {
            String fileName = entry.getKey();
            Path absoluteListPath = absolutePath.resolve(fileName);
            if (!saveDataCompoundList(absoluteListPath, entry.getValue())) {
                error("Failed to save ListTag to file: " + absoluteListPath);
                success = false;
            }
        }

        /*CompoundTag allDataCompound = new CompoundTag();
        // Create a ListTag to hold all ListTags
        for(Map.Entry<String, ListTag> entry : dataListMap.entrySet()) {
            String fileName = entry.getKey();
            ListTag listTag = entry.getValue();
            if (listTag != null) {
                allDataCompound.put(fileName, listTag);
            } else {
                error("No valid ListTag found for key: " + fileName);
            }
        }*/

        //saveDataCompoundChunked(absolutePath.resolve("all_data"), allDataCompound);

        //CompoundTag readed = readDataCompoundChunked(absolutePath.resolve("all_data"));

        /*CompoundTag analyzedCompound = ChunkedNBT.getSizeAnalysis(allDataCompound);
        // save the tag as json
        Path analysisPath = absolutePath.resolve("analysis.json");
        if(!saveAsJson(analyzedCompound, analysisPath)) {
            error("Failed to save analysis JSON to file: " + analysisPath);
            success = false;
        }*/

        return success;
    }

    /**
     * Reads a map of named {@link ListTag} instances previously saved via
     * {@link #saveDataCompoundListMap(Path, Map)}. Each direct sub-folder of
     * {@code absolutePath} is read as a chunked list and added to the result
     * map keyed by the folder name.
     *
     * @param absolutePath the parent folder containing per-entry sub-folders.
     *
     * @return the reassembled map (possibly empty); never {@code null}.
     */
    public Map<String, ListTag> readDataCompoundListMap(Path absolutePath) {
        Map<String, ListTag> dataMap = new HashMap<>();
        if(!folderExists(absolutePath)) {
            error("Folder does not exist: " + absolutePath);
            return dataMap;
        }

        // Read each ListTag from a separate file
        List<Path> folderList = getFoldes(absolutePath);
        for(Path folder : folderList) {
            String fileName = folder.getFileName().toString();
            if(fileName.lastIndexOf(".") != -1)
            {
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
            }
            Path absoluteListPath = absolutePath.resolve(fileName);
            ListTag listTag = readDataCompoundList(absoluteListPath);
            if (listTag != null) {
                dataMap.put(fileName, listTag);
            } else {
                error("No valid ListTag found in folder: " + absoluteListPath);
            }
        }
        return dataMap;
    }

    /*public boolean saveDataCompoundChunked(Path absolutePath, CompoundTag tag)
    {
        ChunkedNBT.FlatNBT flatNBT = null;
        try{
            flatNBT = ChunkedNBT.flatten(tag);
        }
        catch(IOException e) {
            error("Failed to flatten CompoundTag: " + absolutePath, e);
            return false;
        }
        return saveFlatNBT(absolutePath, flatNBT);
    }
    public CompoundTag readDataCompoundChunked(Path absolutePath) {
        ChunkedNBT.FlatNBT flatNBT = loadFlatNBT(absolutePath);
        if (flatNBT == null) {
            error("Failed to load FlatNBT data from file: " + absolutePath);
            return null;
        }
        return ChunkedNBT.rebuild(flatNBT);
    }


    public boolean saveFlatNBT(Path absolutePath, ChunkedNBT.FlatNBT flatNBT)
    {
    // delete existing files
        if(fileExists(absolutePath)) {
            try {
                Files.delete(absolutePath);
            } catch (IOException e) {
                error("Failed to delete existing file: " + absolutePath, e);
                return false;
            }
        }
        if(!createFolder(absolutePath))
            return false;

        Path chunksFolder = absolutePath.resolve("chunks");
        if(!createFolder(chunksFolder)) {
            error("Failed to create chunks folder: " + chunksFolder);
            return false;
        }

        CompoundTag root = new CompoundTag();
        root.putInt("root", flatNBT.rootId);
        boolean success = saveDataCompound(absolutePath.resolve("root"), root);
        success &= saveDataCompoundList(chunksFolder, flatNBT.chunks);
        if(!success) {
            error("Failed to save FlatNBT data to file: " + absolutePath);
            return false;
        }
        debug("Saved FlatNBT data to file: " + absolutePath);
        return true;
    }

    public ChunkedNBT.FlatNBT loadFlatNBT(Path absolutePath) {
        // Check if the path exists
        if (!folderExists(absolutePath)) {
            error("FlatNBT folder does not exist: " + absolutePath);
            return null;
        }
        // Read the structure data
        CompoundTag rootData = readDataCompound(absolutePath.resolve("root"));
        if (rootData == null) {
            error("Failed to read root data from file: " + absolutePath.resolve("root"));
            return null;
        }
        // Read the chunks data
        ListTag chunksData = readDataCompoundList(absolutePath.resolve("chunks"));
        if (chunksData == null) {
            error("Failed to read chunks data from folder: " + absolutePath.resolve("chunks"));
            return null;
        }
        ChunkedNBT.FlatNBT flatNBT = new ChunkedNBT.FlatNBT();
        flatNBT.rootId = rootData.getInt("root");
        flatNBT.chunks = chunksData;
        debug("Loaded FlatNBT data from file: " + absolutePath);
        return flatNBT;
    }*/




    /**
     * Serializes the given object to JSON using Gson and writes it to the
     * specified path. Parent directories are created as needed.
     *
     * @param o            the object to serialize.
     * @param absolutePath the absolute path of the JSON file to write.
     *
     * @return {@code true} on success; {@code false} on I/O failure.
     */
    public boolean saveAsJson(Object o, Path absolutePath)
    {
        String json = GSON.toJson(o);
        try {
            Files.createDirectories(absolutePath.getParent());
            Files.writeString(absolutePath, json);
        } catch (Exception e) {
            error("Failed to save JSON to file: " + absolutePath, e);
            return false;
        }
        return true;
    }
    /**
     * Writes a {@link JsonElement} tree to the specified path. Parent
     * directories are created as needed.
     *
     * @param json         the JSON tree to write.
     * @param absolutePath the absolute path of the JSON file to write.
     *
     * @return {@code true} on success; {@code false} on I/O failure.
     */
    public boolean saveJson(JsonElement json, Path absolutePath) {
        try {
            Files.createDirectories(absolutePath.getParent());
            Files.writeString(absolutePath, GSON.toJson(json));
        } catch (Exception e) {
            error("Failed to save JSON to file: " + absolutePath, e);
            return false;
        }
        return true;
    }
    /**
     * Loads and deserializes a JSON file into an instance of the given type
     * using Gson.
     *
     * @param absolutePath the absolute path of the JSON file to read.
     * @param typeOfT      the {@link Type} describing the expected return type.
     * @param <T>          the deserialized object type.
     *
     * @apiNote
     * Returns {@code null} on I/O failure (the error is logged); a malformed
     * JSON file will throw {@link JsonSyntaxException}.
     *
     * @return the deserialized instance, or {@code null} on I/O failure.
     *
     * @throws JsonSyntaxException if the file's content is not valid JSON.
     */
    public <T> T loadFromJson(Path absolutePath, Type typeOfT) throws JsonSyntaxException {
        try {
            // Read JSON content
            String json = Files.readString(absolutePath);
            return (T) GSON.fromJson(json, TypeToken.get(typeOfT));
        } catch (Exception e) {
            error("Failed to load JSON from file: " + absolutePath, e);
            return null;
        }
    }

    /**
     * Loads a JSON file and returns it as a parsed {@link JsonElement} tree.
     *
     * @param absolutePath the absolute path of the JSON file to read.
     *
     * @return the parsed JSON element, or {@code null} on failure.
     */
    public JsonElement loadJson(Path absolutePath) {
        try {
            // Read JSON content
            String json = Files.readString(absolutePath);
            return GSON.fromJson(json, JsonElement.class);
        } catch (Exception e) {
            error("Failed to load JSON from file: " + absolutePath, e);
            return null;
        }
    }
    /**
     * Detects whether the given file uses gzip compression by inspecting the
     * first two bytes for the gzip magic number {@code 0x1F 0x8B}.
     *
     * @param file the file to inspect.
     *
     * @return {@code true} if the file is gzip-compressed; {@code false} otherwise.
     *
     * @throws IOException if the file cannot be read.
     */
    public static boolean isCompressed(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            int b1 = in.readUnsignedByte();
            int b2 = in.readUnsignedByte();
            return (b1 == 0x1F && b2 == 0x8B);
        }
    }

}
