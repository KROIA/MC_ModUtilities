package net.kroia.modutilities.persistence.archive;

import net.kroia.modutilities.persistence.DataPersistence;
import net.kroia.modutilities.persistence.NBTFileParser;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class DataArchiveManager<T extends DataArchiveChunk> {

    private final NBTFileParser nbtFileParser;
    private final Path archiveFolderPath;
    private final String archiveName;
    private final Supplier<T> chunkFactory;
    private Consumer<String> errorLogger = System.err::println;
    private Consumer<String> warnLogger = System.out::println;
    private BiConsumer<String, Throwable> errorLoggerThrowable = (error, throwable) -> {
        if (errorLogger != null) {
            errorLogger.accept(error + "\nError: " + throwable.getMessage() + "\n" + Arrays.toString(throwable.getStackTrace()));
        }
    };
    private Consumer<String> debugLogger = System.out::println;


    public DataArchiveManager(Path archiveFolderPath, NBTFileParser.NbtFormat format, Supplier<T> chunkFactory)
    {
        this.archiveFolderPath = archiveFolderPath;
        this.archiveName = archiveFolderPath.getFileName().toString();
        this.nbtFileParser = new NBTFileParser(format);
        this.chunkFactory = chunkFactory;
    }

    public Path getArchiveFolderPath() {
        return archiveFolderPath;
    }

    protected boolean saveChunk(T chunk)
    {
        if(createArchiveFolder())
        {
            String chunkName = chunk.getTimeInterval().createFileName()+".nbt";
            String chunkStartName = chunk.getTimeInterval().getStartTime()+"_";
            // check if a file is present that starts with the name chunkStartName, if so, delete it
            List<Path> existingFiles = getArchiveFiles();
            for (Path existingFile : existingFiles) {
                if (existingFile.getFileName().toString().startsWith(chunkStartName)) {
                    try {
                        Files.delete(existingFile); // Delete the existing file
                        debug("Deleted existing chunk file: " + existingFile);
                    } catch (IOException e) {
                        error("Failed to delete existing chunk file: " + existingFile, e);
                        return false; // Return false if deletion fails
                    }
                }
            }


            Path chunkPath = archiveFolderPath.resolve(chunkName);
            CompoundTag dataTag = new CompoundTag();
            if(!chunk.saveInternal(dataTag))
            {
                error("Failed to save chunk data for: " + chunkName);
                return false;
            }
            if(NBTFileParser.getUncompressedSize(dataTag) > NBTFileParser.getMaxNbtSize())
            {
                warn("Chunk data for " + chunkName + " exceeds maximum NBT size, skipping save.");
                return false; // Skip saving if the data is too large
            }
            return nbtFileParser.saveDataCompound(chunkPath, dataTag);
        }
        error("Failed to create archive folder: " + archiveFolderPath);
        return false;
    }
    protected boolean saveChunk(T chunk, Path destinationFolder)
    {
        try {
            if (!Files.exists(destinationFolder)) {
                Files.createDirectories(destinationFolder); // Create the directory if it doesn't exist
                debug("Created archive directory: " + destinationFolder);
            }
        } catch (IOException e) {
            error("Failed to create archive directory: " + destinationFolder, e);
            return false; // Return false if directory creation fails
        }

        if(Files.exists(destinationFolder))
        {
            String chunkName = chunk.getTimeInterval().createFileName()+".nbt";
            Path chunkPath = destinationFolder.resolve(chunkName);
            CompoundTag dataTag = new CompoundTag();
            if(!chunk.saveInternal(dataTag))
            {
                error("Failed to save chunk data for: " + chunkName);
                return false;
            }
            if(NBTFileParser.getUncompressedSize(dataTag) > NBTFileParser.getMaxNbtSize())
            {
                warn("Chunk data for " + chunkName + " exceeds maximum NBT size, skipping save.");
                return false; // Skip saving if the data is too large
            }
            return nbtFileParser.saveDataCompound(chunkPath, dataTag);
        }
        error("Failed to create archive folder: " + destinationFolder);
        return false;
    }

    protected List<T> loadChunks(DataArchiveChunk.TimeInterval specificRange)
    {
        List<DataArchiveChunk.TimeInterval> availableIntervals = getStoredIntervals();
        // find a overlapping interval set
        List<DataArchiveChunk.TimeInterval> overlappingIntervals = availableIntervals.stream()
                .filter(interval -> interval.overlapsWith(specificRange))
                .toList();
        if(overlappingIntervals.isEmpty())
        {
            error("No overlapping intervals found for: " + specificRange);
            return new ArrayList<>(); // Return null if no overlapping intervals are found
        }
        List<T> loadedChunks = new ArrayList<>();
        for (DataArchiveChunk.TimeInterval interval : overlappingIntervals) {
            String fileName = interval.createFileName()+".nbt";
            Path chunkPath = archiveFolderPath.resolve(fileName);
            if (Files.exists(chunkPath)) {
                CompoundTag dataTag = nbtFileParser.readDataCompound(chunkPath);
                if (dataTag != null) {
                    T chunk = chunkFactory.get();
                    if (chunk.loadInternal(dataTag)) {
                        loadedChunks.add(chunk); // Add the loaded chunk to the list
                        debug("Loaded chunk: " + fileName + " with size: " + getChunkSizeUtilisationPercentage(chunk) + "%");
                    } else {
                        error("Failed to load chunk from file: " + fileName);
                    }
                } else {
                    error("Failed to read data from file: " + fileName);
                }
            } else {
                warn("Chunk file does not exist: " + fileName);
            }
        }
        if(loadedChunks.isEmpty())
        {
            error("No chunks loaded for the specified range: " + specificRange);
        }
        return loadedChunks; // Return the list of loaded chunks
    }
    protected List<T> loadChunks()
    {
        List<DataArchiveChunk.TimeInterval> availableIntervals = getStoredIntervals();
        // find a overlapping interval set
        if(availableIntervals.isEmpty())
        {
            error("No chunks to load");
            return new ArrayList<>(); // Return null if no overlapping intervals are found
        }
        List<T> loadedChunks = new ArrayList<>();
        for (DataArchiveChunk.TimeInterval interval : availableIntervals) {
            String fileName = interval.createFileName()+".nbt"; // Ensure the file name ends with .nbt
            Path chunkPath = archiveFolderPath.resolve(fileName);
            if (Files.exists(chunkPath)) {
                CompoundTag dataTag = nbtFileParser.readDataCompound(chunkPath);
                if (dataTag != null) {
                    T chunk = chunkFactory.get();
                    if (chunk.loadInternal(dataTag)) {
                        loadedChunks.add(chunk); // Add the loaded chunk to the list
                        debug("Loaded chunk: " + fileName + " with size: " + getChunkSizeUtilisationPercentage(chunk) + "%");
                    } else {
                        error("Failed to load chunk from file: " + fileName);
                    }
                } else {
                    error("Failed to read data from file: " + fileName);
                }
            } else {
                warn("Chunk file does not exist: " + fileName);
            }
        }
        if(loadedChunks.isEmpty())
        {
            error("No chunks loaded");
        }
        return loadedChunks; // Return the list of loaded chunks
    }

    protected float getChunkSizeUtilisationPercentage(T chunk)
    {
        long uncompressedSize = chunk.getUncompressedSize();
        if(uncompressedSize < 0)
        {
            error("Failed to get uncompressed size for chunk: " + chunk);
            return 0.0f; // Return 0% if the size cannot be determined
        }
        long maxSize = NBTFileParser.getMaxNbtSize();
        return (float) uncompressedSize / maxSize * 100.0f; // Calculate percentage
    }


    public boolean clearArchive()
    {
        List<Path> files = getFiles(archiveFolderPath);
        for(Path file : files) {
            try {
                Files.delete(file); // Delete each file
                debug("Deleted archive file: " + file);
            } catch (IOException e) {
                error("Failed to delete archive file: " + file, e);
                return false; // Return false if any deletion fails
            }
        }
        return getFiles(archiveFolderPath).isEmpty();
    }
    public boolean removeArchive()
    {
        if(!clearArchive()) // Clear the archive first
            return false; // If clearing fails, return false
        try {
            Files.delete(archiveFolderPath); // Delete the directory itself
            debug("Deleted archive directory: " + archiveFolderPath);
            return true; // Return true if all deletions were successful
        } catch (IOException e) {
            error("Failed to delete archive directory: " + archiveFolderPath, e);
            return false; // Return false if directory deletion fails
        }
    }
    public boolean createArchiveFolder()
    {
        try {
            if (Files.exists(archiveFolderPath))
                return true; // Return true if the directory already exists
            else {
                Files.createDirectories(archiveFolderPath); // Create the directory if it doesn't exist
                debug("Created archive directory: " + archiveFolderPath);
            }
            return true; // Return true if the directory exists or was created successfully
        } catch (IOException e) {
            error("Failed to create archive directory: " + archiveFolderPath, e);
            return false; // Return false if directory creation fails
        }
    }


    protected List<Path> getArchiveFiles()
    {
        return getFiles(archiveFolderPath); // Get all files in the archive directory
    }



    protected @Nullable DataArchiveChunk.TimeInterval getIntervalFromTimePoint(long timePoint)
    {
        List<DataArchiveChunk.TimeInterval> intervals = getStoredIntervals();
        for (DataArchiveChunk.TimeInterval interval : intervals) {
            if (interval.isInInterval(timePoint)) {
                return interval; // Return the first matching interval
            }
        }
        return null; // Return null if no matching interval is found
    }

    public long getStartTime()
    {
        List<DataArchiveChunk.TimeInterval> intervals = getStoredIntervals();
        if (intervals.isEmpty()) {
            return -1; // Return -1 if no intervals are found
        }
        long smallestStartTime = intervals.get(0).getStartTime();
        for (DataArchiveChunk.TimeInterval interval : intervals) {
            if (interval.getStartTime() < smallestStartTime) {
                smallestStartTime = interval.getStartTime(); // Find the smallest start time
            }
        }
        return smallestStartTime; // Return the smallest start time found
    }
    protected List<DataArchiveChunk.TimeInterval> getStoredIntervals()
    {
        List<DataArchiveChunk.TimeInterval> intervals = new ArrayList<>();
        List<Path> files = getArchiveFiles();
        for(Path file : files) {
            String fileName = file.getFileName().toString();
            DataArchiveChunk.TimeInterval interval = DataArchiveChunk.TimeInterval.fromFileName(fileName);
            if (interval != null) {
                intervals.add(interval); // Add valid intervals to the list
            } else {
                warn("Invalid file name format for interval extraction: " + fileName);
            }
        }
        // sort the list by start time
        intervals.sort(Comparator.comparingLong(DataArchiveChunk.TimeInterval::getStartTime));
        return intervals; // Return the list of valid intervals
    }
    protected List<Path> getFiles(Path absolutePath) {
        try {
            return Files.list(absolutePath) // List files
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList()); // Convert to List
        } catch (IOException e) {
            error("Failed to list JSON files in directory: " + absolutePath, e);
            return List.of(); // Return empty list on error
        }
    }


    public void setLogger(Consumer<String> errorLogger, Consumer<String> debugLogger, Consumer<String> warnLogger) {
        this.errorLogger = errorLogger;
        this.debugLogger = debugLogger;
        this.warnLogger = warnLogger;
        nbtFileParser.setLogger(errorLogger, debugLogger, warnLogger); // Set logger for NBTFileParser as well
    }
    public void setLogger(Consumer<String> errorLogger, BiConsumer<String, Throwable>errorLoggerThrowable, Consumer<String> debugLogger, Consumer<String> warnLogger) {
        this.errorLogger = errorLogger;
        this.errorLoggerThrowable = errorLoggerThrowable;
        this.debugLogger = debugLogger;
        this.warnLogger = warnLogger;
        nbtFileParser.setLogger(errorLogger, errorLoggerThrowable, debugLogger, warnLogger); // Set logger for NBTFileParser as well
    }

    protected void debug(String message) {
        if (debugLogger != null) {
            debugLogger.accept("["+archiveName+"]: " + message);
        }
    }
    protected void error(String message) {
        if (errorLogger != null) {
            errorLogger.accept("["+archiveName+"]: " + message);
        }
    }
    protected void error(String message, Throwable throwable) {
        if (errorLoggerThrowable != null) {
            errorLoggerThrowable.accept("["+archiveName+"]: "+ message, throwable);
        } else if (errorLogger != null) {
            errorLogger.accept("["+archiveName+"]: " + message + "\nError: " + throwable.getMessage() + "\n" + Arrays.toString(throwable.getStackTrace()));
        }
    }
    protected void warn(String message) {
        if (warnLogger != null) {
            warnLogger.accept("["+archiveName+"]: "+message);
        }
    }
}
