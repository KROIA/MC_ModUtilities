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

public class NBTFileParser {
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


    public NBTFileParser(NbtFormat nbtFormat) {
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

    public static void setMaxNbtSize(long maxNbtSize) {
        if(maxNbtSize <= 0)
            throw new IllegalArgumentException("Maximum NBT size must be greater than 0");
        MAX_NBT_SIZE = maxNbtSize;
    }

    public static long getMaxNbtSize() {
        return MAX_NBT_SIZE;
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

    public static boolean isCompressed(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            int b1 = in.readUnsignedByte();
            int b2 = in.readUnsignedByte();
            return (b1 == 0x1F && b2 == 0x8B);
        }
    }

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
