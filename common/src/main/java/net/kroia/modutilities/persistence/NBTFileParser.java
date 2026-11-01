package net.kroia.modutilities.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Reads and writes NBT data to/from individual files.
 * Supports both compressed (gzip) and uncompressed NBT formats and exposes a
 * configurable maximum NBT size (defaults to Mojang's 2 MB limit) used as a
 * sanity warning threshold when saving.
 *
 * @apiNote
 * On read, the file's actual format is detected via the gzip magic bytes; if
 * it differs from the configured {@link NbtFormat}, a warning is logged and
 * the actual format is used.
 */
public class NBTFileParser {
    /**
     * Specifies whether NBT data is stored using gzip compression or in raw form.
     */
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
    private final NbtFormat nbtFormat;
    private static long MAX_NBT_SIZE = 2_097_152L; // 2 MB


    /**
     * Creates a new NBT file parser configured with the given NBT format.
     *
     * @param nbtFormat the format used when writing files; on read, the actual
     *                  file format is detected automatically.
     */
    public NBTFileParser(NbtFormat nbtFormat) {
        this.nbtFormat = nbtFormat;
    }




    /**
     * Configures the logger callbacks used by this parser.
     *
     * @param errorLogger receives error messages.
     * @param debugLogger receives debug messages (e.g. timing information).
     * @param warnLogger  receives warning messages.
     */
    public void setLogger(Consumer<String> errorLogger, Consumer<String> debugLogger, Consumer<String> warnLogger) {
        this.errorLogger = errorLogger;
        this.debugLogger = debugLogger;
        this.warnLogger = warnLogger;
    }
    /**
     * Configures the logger callbacks used by this parser, including a
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
     * Sets the global maximum NBT size threshold (in bytes) used as a warning
     * trigger when saving and as a guard when chunked archives are written.
     *
     * @param maxNbtSize the new maximum NBT size in bytes; must be positive.
     *
     * @throws IllegalArgumentException if {@code maxNbtSize} is less than or equal to zero.
     */
    public static void setMaxNbtSize(long maxNbtSize) {
        if(maxNbtSize <= 0)
            throw new IllegalArgumentException("Maximum NBT size must be greater than 0");
        MAX_NBT_SIZE = maxNbtSize;
    }

    /**
     * Returns the current global maximum NBT size threshold in bytes.
     *
     * @return the maximum NBT size used as warning/guard threshold.
     */
    public static long getMaxNbtSize() {
        return MAX_NBT_SIZE;
    }


    /**
     * Reads a {@link CompoundTag} from the given absolute file path.
     * The file's actual compression state is detected from its gzip magic bytes;
     * if it disagrees with the configured {@link NbtFormat}, the actual format
     * is used and a warning is logged.
     *
     * @param absolutePath the absolute path of the NBT file to read.
     *
     * @apiNote
     * Returns {@code null} if the file does not exist or if reading fails;
     * errors are logged but no exception is thrown to the caller.
     *
     * @return the loaded compound tag, or {@code null} if reading failed.
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
                    data = NbtIo.read(Path.of(file.toURI()));

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
     * Logs a warning if the uncompressed payload exceeds the configured maximum
     * NBT size (the file is still written).
     *
     * @param absolutePath the absolute path of the NBT file to write.
     * @param data         the compound tag to serialize.
     *
     * @return {@code true} if the file was written successfully; {@code false} on I/O failure.
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

    /**
     * Computes the uncompressed serialized size in bytes of the given tag.
     * Non-{@link CompoundTag} tags are wrapped into a temporary compound under
     * key {@code "d"} prior to measurement.
     *
     * @param tag the tag whose serialized size should be measured.
     *
     * @apiNote
     * Returns {@code 0} on I/O failure (the exception is logged to standard error).
     *
     * @return the uncompressed size in bytes, or {@code 0} on failure.
     */
    public static long getUncompressedSize(Tag tag)  {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

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
}
