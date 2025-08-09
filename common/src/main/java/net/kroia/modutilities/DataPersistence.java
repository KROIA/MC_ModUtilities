package net.kroia.modutilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DataPersistence {
    public enum JsonFormat {
        PRETTY, COMPACT
    }
    public enum NbtFormat {
        COMPRESSED, UNCOMPRESSED
    }


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
                if(nbtFormat == NbtFormat.COMPRESSED)
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
}
