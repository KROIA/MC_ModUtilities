package net.kroia.modutilities.sandbox;

import dev.architectury.event.events.common.TickEvent;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.persistence.NBTFileParser;
import net.kroia.modutilities.persistence.archive.DataArchiveChunk;
import net.kroia.modutilities.persistence.archive.DataArchiveManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public class SandboxDataArchiveManager extends DataArchiveManager<SandboxDataArchiveManager.SandboxDataArchiveChunk> {


    public static class SandboxDataArchiveChunk extends DataArchiveChunk
    {

        public final List<String> dataEntries = new java.util.ArrayList<>();

        public SandboxDataArchiveChunk() {
            super();
        }
        public SandboxDataArchiveChunk(long startTime) {
            super(startTime);
        }

        @Override
        protected boolean save(CompoundTag dataTag) {
            ListTag dataList = new ListTag();
            for (String entry : dataEntries) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putString("dataEntry", entry);
                dataList.add(entryTag);
            }
            dataTag.put("dataEntries", dataList);
            return true;
        }

        @Override
        protected boolean load(CompoundTag dataTag) {
            ListTag dataList = dataTag.getList("dataEntries", ListTag.TAG_COMPOUND);
            dataEntries.clear();
            for (int i = 0; i < dataList.size(); i++) {
                CompoundTag entryTag = dataList.getCompound(i);
                dataEntries.add(entryTag.getString("dataEntry"));
            }
            return true;
        }
    }


    private SandboxDataArchiveChunk currentChunk;

    private static SandboxDataArchiveManager instance;
    public SandboxDataArchiveManager(Path archiveFolderPath) {
        super(archiveFolderPath, NBTFileParser.NbtFormat.UNCOMPRESSED, SandboxDataArchiveChunk::new);
        this.currentChunk = new SandboxDataArchiveChunk();
        instance = this;
        TickEvent.SERVER_POST.register(SandboxDataArchiveManager::onServerTick);

    }

    public static void onServerTick(MinecraftServer server)
    {
        instance.currentChunk.dataEntries.add("Server tick at " + server.getTickCount());
        instance.onChunkChanged();
    }

    private void onChunkChanged()
    {
        if(getChunkSizeUtilisationPercentage(currentChunk) > 80)
        {
            long endTime = currentChunk.updateEndTime();
            saveChunk(currentChunk);
            currentChunk = new SandboxDataArchiveChunk(endTime+1);
            System.out.println("SandboxDataArchiveManager: Chunk size utilisation exceeded 80%, creating new chunk.");
        }
    }

    public static void loadAndSave()
    {
        if(instance != null)
            instance.loadAndSaveInternal();
    }
    private void loadAndSaveInternal()
    {
        Path outputPath = Path.of("data/sandbox_data_archive_2");

        List<SandboxDataArchiveChunk> chunks = loadChunks();
        if(chunks.isEmpty())
        {
            System.out.println("No chunks found, creating new chunk.");
            currentChunk = new SandboxDataArchiveChunk();
        }
        else
        {
            for(SandboxDataArchiveChunk chunk : chunks)
            {
                System.out.println("Loaded chunk with start time: " + chunk.getStartTime() + ", end time: " + chunk.getEndTime());
                saveChunk(chunk, outputPath);
            }
            System.out.println("done");
        }

        Path outputPath2 = Path.of("data/sandbox_data_archive_3");
        if (chunks.size() > 5) {
            saveChunk(chunks.get(5), outputPath2);
        }

    }
}
