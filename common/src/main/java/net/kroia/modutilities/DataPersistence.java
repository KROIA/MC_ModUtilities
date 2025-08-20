package net.kroia.modutilities;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class DataPersistence {
    public enum JsonFormat {
        PRETTY, COMPACT
    }
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

    public void setLogger(Consumer<String> errorLogger, Consumer<String> debugLogger, Consumer<String> warnLogger) {
        this.errorLogger = errorLogger;
        this.debugLogger = debugLogger;
        this.warnLogger = warnLogger;
    }
    public void setLogger(Consumer<String> errorLogger, BiConsumer<String, Throwable>errorLoggerThrowable, Consumer<String> debugLogger, Consumer<String> warnLogger) {
        this.errorLogger = errorLogger;
        this.errorLoggerThrowable = errorLoggerThrowable;
        this.debugLogger = debugLogger;
        this.warnLogger = warnLogger;
    }



    public JsonFormat getJsonFormat() {
        return jsonFormat;
    }
    public NbtFormat getNbtFormat() {
        return nbtFormat;
    }

    public void setLevelSavePath(Path levelSavePath) {
        this.levelSavePath = levelSavePath;
    }
    public Path getLevelSavePath() {
        return levelSavePath;
    }
    public void setRelativeSavePath(Path relativeSavePath) {
        this.relativeSavePath = relativeSavePath;
    }
    public Path getRelativeSavePath() {
        return relativeSavePath;
    }
    public Path getAbsoluteSavePath() {
        return getLevelSavePath().resolve(relativeSavePath);
    }
    public Path getAbsoluteSavePath(String relativeAdded) {
        return getAbsoluteSavePath().resolve(relativeAdded);
    }

    protected boolean createSaveFolder() {
        if(folderExists(getAbsoluteSavePath())) {
            return true;
        }
        return createFolder(getAbsoluteSavePath());
    }
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
    public boolean fileExists(Path path) {
        File file = new File(path.toUri());
        return file.exists() && file.isFile();
    }
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




    public List<Path> getJsonFiles(Path absolutePath) {
        try {
            return Files.list(absolutePath) // List files
                    .filter(Files::isRegularFile)       // Keep only regular files
                    .filter(_path -> _path.toString().endsWith(".json")) // Keep only .json files
                    .collect(Collectors.toList()); // Convert to List
        } catch (IOException e) {
            error("Failed to list JSON files in directory: " + absolutePath, e);
            return List.of(); // Return empty list on error
        }
    }
    public List<Path> getFiles(Path absolutePath, String extension) {
        try {
            return Files.list(absolutePath) // List files
                    .filter(Files::isRegularFile)       // Keep only regular files
                    .filter(_path -> _path.toString().endsWith(extension)) // Keep only files with specified extension
                    .collect(Collectors.toList()); // Convert to List
        } catch (IOException e) {
            error("Failed to list files in directory: " + absolutePath, e);
            return List.of(); // Return empty list on error
        }
    }



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
                    data = NbtIo.readCompressed(file);
                else
                    data = NbtIo.read(file);

                dataOut = data;
                return dataOut;
            } catch(Exception e)
            {
                error("Failed to read data from file: " + absolutePath, e);
            }
        }
        return null;
    }
    public boolean saveDataCompound(Path absolutePath, CompoundTag data) {
        long startMillis = System.currentTimeMillis();
        boolean success = true;

        long uncompressedSize = 0;
        try {
            uncompressedSize = getUncompressedSize(data);
        }catch (IOException e) {
            error("Failed to get uncompressed size of data: " + absolutePath, e);
            success = false;
        }
        if(uncompressedSize > MAX_NBT_SIZE)
        {
            warn("Data size exceeds maximum NBT size of " + MAX_NBT_SIZE + " bytes.\nThis will cause problems in specific minecraft versions.\n" +
                    "Consider splitting the data into a TagList and use the saveDataCompoundList() function to store the data.");
        }


        File file = new File(absolutePath.toUri());
        try {
            if (nbtFormat == NbtFormat.COMPRESSED)
                NbtIo.writeCompressed(data, file);
            else
                NbtIo.write(data, file);
        } catch(Exception e)
        {
            error("Failed to save data to file: " + absolutePath, e);
            success = false;
        }
        long endMillis = System.currentTimeMillis();
        debug("Saving data to file: " + absolutePath + " took " + (endMillis - startMillis) + "ms");
        return success;
    }
    public boolean saveDataCompoundList(Path absolutePath, ListTag dataList)
    {
        long startMillis = System.currentTimeMillis();
        boolean success = true;
        long uncompressedSize = 0;
        try {
            CompoundTag data = new CompoundTag();
            data.put("data", dataList);
            uncompressedSize = getUncompressedSize(data);
        }catch (IOException e) {
            error("Failed to get uncompressed size of data: " + absolutePath, e);
            success = false;
        }
        if(uncompressedSize > MAX_NBT_SIZE)
        {
            // Split the data into chunks and store them in a folder with the name of the file
            String fileName = absolutePath.getFileName().toString();
            if(fileName.lastIndexOf(".") != -1)
            {
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
            }
            Path folderPath = absolutePath.getParent().resolve(fileName);
            if(!createFolder(folderPath)) {
                error("Failed to create folder for large NBT data: " + folderPath);
                return false;
            }
            // Split the data into chunks
            List<CompoundTag> chunks = new ArrayList<>();
            int processedTagCount = 0;
            while(processedTagCount < dataList.size())
            {
                CompoundTag chunk = new CompoundTag();
                ListTag chunkList = new ListTag();
                chunk.put("data", chunkList);
                for(; processedTagCount<dataList.size(); ++processedTagCount)
                {
                    chunkList.add(dataList.get(processedTagCount));
                    long chunkUncompressedSize = 0;
                    try {
                        chunkUncompressedSize = getUncompressedSize(chunk);
                    } catch (IOException e) {
                        error("Failed to get uncompressed size of chunk data: " + absolutePath, e);
                        success = false;
                        break;
                    }
                    if(chunkUncompressedSize > MAX_NBT_SIZE) {
                        // If the chunk is too large, stop adding more tags
                        // remove the last tag
                        chunkList.remove(chunkList.size() - 1);
                        break;
                    }
                }
                chunks.add(chunk);
            }
            // Save each chunk to a file
            for(int i = 0; i < chunks.size(); ++i) {
                Path chunkPath = folderPath.resolve("chunk_" + i + ".nbt");
                if(!saveDataCompound(chunkPath, chunks.get(i))) {
                    error("Failed to save chunk data to file: " + chunkPath);
                    success = false;
                }
            }
        }
        else {
            // Save the data to a single file
            if(!createSaveFolder()) {
                error("Failed to create save folder: " + getAbsoluteSavePath());
                return false;
            }
            CompoundTag dataTag = new CompoundTag();
            dataTag.put("data", dataList);
            if(!saveDataCompound(absolutePath, dataTag)) {
                error("Failed to save data to file: " + absolutePath);
                success = false;
            }
        }
        long endMillis = System.currentTimeMillis();
        debug("Saving data to file: " + absolutePath + " took " + (endMillis - startMillis) + "ms");
        return success;
    }

    public ListTag readDataCompoundList(Path absolutePath) {
        ListTag dataOut;

        // Check if the path is a file or folder containing chunks
        String fileName = absolutePath.getFileName().toString();
        if(fileName.lastIndexOf(".") != -1)
        {
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        Path folderPath = absolutePath.getParent().resolve(fileName);
        boolean isFile = Files.isRegularFile(absolutePath) && !Files.isDirectory(folderPath);
        if(isFile)
        {
            CompoundTag data = readDataCompound(absolutePath);
            if(data != null && data.contains("data", Tag.TAG_LIST)) {
                dataOut = data.getList("data", Tag.TAG_COMPOUND); // 10 is the type ID for CompoundTag
            } else {
                error("No valid 'data' list found in file: " + absolutePath);
                dataOut = new ListTag();
            }
        } else {
            dataOut = new ListTag();
            // If it's a folder, read all chunk files
            try {
                List<Path> chunkFiles = getFiles(folderPath, ".nbt");
                for (Path chunkFile : chunkFiles) {
                    CompoundTag chunkData = readDataCompound(chunkFile);
                    if (chunkData != null && chunkData.contains("data", Tag.TAG_LIST)) {
                        dataOut.addAll(chunkData.getList("data", Tag.TAG_COMPOUND));
                    } else {
                        error("No valid 'data' list found in chunk file: " + chunkFile);
                    }
                }
            } catch (Exception e) {
                error("Failed to read data from folder: " + absolutePath, e);
            }
        }
        return dataOut;
    }

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
    public static boolean isCompressed(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            int b1 = in.readUnsignedByte();
            int b2 = in.readUnsignedByte();
            return (b1 == 0x1F && b2 == 0x8B);
        }
    }
    public static long getUncompressedSize(CompoundTag tag) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Write the NBT to a byte array to measure its raw uncompressed size
        NbtIo.write(tag, dos);
        dos.flush();

        return baos.size();
    }
}
