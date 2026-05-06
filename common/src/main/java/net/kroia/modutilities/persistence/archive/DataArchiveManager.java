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

/**
 * Manages a collection of {@link DataArchiveChunk}s persisted as individual
 * NBT files inside a single archive folder. Files are named after their
 * {@link DataArchiveChunk.TimeInterval} so that chunks can be located by
 * start time or queried by interval overlap.
 *
 * @param <T> the concrete chunk subtype managed by this archive.
 *
 * @apiNote
 * {@link #clearArchive()} uses {@link Files#walk(Path, java.nio.file.FileVisitOption...)}
 * with reverse ordering to avoid {@code DirectoryNotEmptyException} when
 * subdirectories are present.
 */
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


    /**
     * Creates a new archive manager rooted at the given folder.
     *
     * @param archiveFolderPath the absolute path of the archive folder; the
     *                          folder name is used as the archive's identifier
     *                          for log messages.
     * @param format            the NBT format used when reading and writing chunk files.
     * @param chunkFactory      supplier that produces fresh chunk instances when loading.
     */
    public DataArchiveManager(Path archiveFolderPath, NBTFileParser.NbtFormat format, Supplier<T> chunkFactory)
    {
        this.archiveFolderPath = archiveFolderPath;
        this.archiveName = archiveFolderPath.getFileName().toString();
        this.nbtFileParser = new NBTFileParser(format);
        this.chunkFactory = chunkFactory;
    }

    /**
     * @return the absolute path of this archive's folder.
     */
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


    /**
     * Loads a chunk that start time is equal to the given startTime.
     * This start time must be equal to the first part of the file name of the chunk.
     * The filename looks like "{startTime}_{duration}.nbt", where startTime is the first part.
     * @param startTime the start time of the chunk to load, which is the first part of the file name
     * @return the loaded chunk, or null if no chunk is found with the specified start time
     */
    protected @Nullable T loadChunk(long startTime)
    {
        String fileNameStartsWith = startTime + "_";
        List<Path> files = getArchiveFiles();
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            if (fileName.startsWith(fileNameStartsWith)) {
                CompoundTag dataTag = nbtFileParser.readDataCompound(file);
                if (dataTag != null) {
                    T chunk = chunkFactory.get();
                    if (chunk.loadInternal(dataTag)) {
                        debug("Loaded chunk: " + fileName + " with size: " + getChunkSizeUtilisationPercentage(chunk) + "%");
                        return chunk; // Return the loaded chunk
                    } else {
                        error("Failed to load chunk from file: " + fileName);
                    }
                } else {
                    error("Failed to read data from file: " + fileName);
                }
            }
        }
        error("No chunk found starting with: " + fileNameStartsWith);
        return null; // Return null if no chunk is found
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


    /**
     * Recursively deletes all files and sub-directories inside the archive
     * folder, leaving the folder itself intact.
     *
     * @apiNote
     * Uses {@link Files#walk(Path, java.nio.file.FileVisitOption...)} with
     * reverse-order deletion so non-empty directories are not encountered
     * before their children, avoiding {@code DirectoryNotEmptyException}.
     *
     * @return {@code true} if the archive folder is empty afterwards;
     *         {@code false} if walking or deletion failed.
     */
    public boolean clearArchive()
    {
        if (!Files.isDirectory(archiveFolderPath)) {
            return true;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(archiveFolderPath)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .filter(p -> !p.equals(archiveFolderPath))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            debug("Deleted archive entry: " + p);
                        } catch (IOException e) {
                            error("Failed to delete archive entry: " + p, e);
                        }
                    });
        } catch (IOException e) {
            error("Failed to walk archive folder: " + archiveFolderPath, e);
            return false;
        }
        return getFiles(archiveFolderPath).isEmpty();
    }
    /**
     * Clears the archive (see {@link #clearArchive()}) and then deletes the
     * archive folder itself.
     *
     * @return {@code true} if the archive was fully removed; {@code false}
     *         if clearing or directory deletion failed.
     */
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
    /**
     * Creates the archive folder (and any missing parents) if it does not
     * already exist.
     *
     * @return {@code true} if the folder existed or was created successfully;
     *         {@code false} on I/O failure.
     */
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

    /**
     * Returns the smallest start time across all stored chunk intervals.
     *
     * @return the smallest start time in milliseconds since the epoch, or
     *         {@code -1} if the archive contains no chunks.
     */
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
            // check if folder exists
            if(!Files.exists(absolutePath)) {
                error("Archive folder does not exist: " + absolutePath);
                return new ArrayList<>(); // Return empty list if folder does not exist
            }

            return Files.list(absolutePath) // List files
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList()); // Convert to List
        } catch (IOException e) {
            error("Failed to list files in directory: " + absolutePath, e);
            return List.of(); // Return empty list on error
        }
    }


    /**
     * Configures the logger callbacks used by this manager and its underlying
     * {@link NBTFileParser}.
     *
     * @param errorLogger receives error messages.
     * @param debugLogger receives debug messages.
     * @param warnLogger  receives warning messages.
     */
    public void setLogger(Consumer<String> errorLogger, Consumer<String> debugLogger, Consumer<String> warnLogger) {
        this.errorLogger = errorLogger;
        this.debugLogger = debugLogger;
        this.warnLogger = warnLogger;
        nbtFileParser.setLogger(errorLogger, debugLogger, warnLogger); // Set logger for NBTFileParser as well
    }
    /**
     * Configures the logger callbacks used by this manager and its underlying
     * {@link NBTFileParser}, including a dedicated handler for errors with an
     * associated {@link Throwable}.
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
