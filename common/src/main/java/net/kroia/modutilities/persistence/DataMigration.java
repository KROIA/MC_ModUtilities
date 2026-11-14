package net.kroia.modutilities.persistence;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Versions all world data of one mod with a single integer and upgrades old saves step by step.
 * Call {@link #migrate} once at server start, before the mod opens any of its data files or databases.
 *
 * <ul>
 *   <li>The data version is stored in a JSON marker file: {@code {"dataVersion": N, "modVersion": "x.y.z"}}.</li>
 *   <li>No marker but at least one file under the data paths: version 0 (a save from before versioning). No marker and
 *       no file (missing or only empty folders): a fresh world, the current version is written and nothing runs.</li>
 *   <li>Saved version newer than the newest step: {@link DataVersionTooNewException}, nothing is touched.</li>
 *   <li>Before the first step runs, all data paths are zipped into one backup. If that fails, no step runs.</li>
 *   <li>The marker is rewritten after every finished step. A crash re-runs only the unfinished step, so every
 *       step must be safe to run again on data it already partly converted.</li>
 * </ul>
 */
public final class DataMigration {
    private static final Logger LOGGER = LogManager.getLogger("DataMigration");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String[] SQLITE_SIBLINGS = {"-wal", "-shm", "-journal"};

    private DataMigration() {}

    /** The work of one step. May read, write, move or delete any of the mod's data files. Must be re-runnable. */
    @FunctionalInterface
    public interface Action {
        void run() throws Exception;
    }

    /**
     * One migration step.
     *
     * @param version    the data version the save has after this step (1, 2, 3, ...).
     * @param modVersion the mod version that introduced this step; used in log lines only.
     * @param action     the conversion.
     */
    public record Step(int version, String modVersion, Action action) {}

    /** The save was written by a newer mod version. The mod must not start on it. */
    public static final class DataVersionTooNewException extends IOException {
        public final String modId;
        public final int savedVersion;
        public final int supportedVersion;

        public DataVersionTooNewException(String modId, int savedVersion, int supportedVersion) {
            super(modId + ": the save data has data version " + savedVersion + ", but this mod version supports only up to "
                    + supportedVersion + ". It was saved by a newer version of " + modId
                    + ". Update the mod or restore an older save; the data was not changed.");
            this.modId = modId;
            this.savedVersion = savedVersion;
            this.supportedVersion = supportedVersion;
        }
    }

    /**
     * Brings the mod's save data to the newest step version.
     *
     * @param modId      the mod id, used in the backup name and in messages.
     * @param modVersion the running mod version, written into the marker.
     * @param worldRoot  the world folder; zip entries are relative to it, and every data path must be inside it.
     * @param markerFile the marker JSON file, e.g. {@code <world>/data/<modid>/data_version.json}.
     * @param backupDir  folder for the backup zip; {@code null} = the marker's folder.
     * @param dataPaths  every file and folder that holds the mod's world data. A folder is backed up recursively,
     *                   a file together with its SQLite {@code -wal}, {@code -shm} and {@code -journal} siblings.
     * @param steps      the steps, in ascending version order (versions &gt;= 1). The last one is the current version.
     *
     * @throws DataVersionTooNewException if the save is newer than the last step.
     * @throws IOException                if the marker is unreadable, the backup fails or a step fails. After a failed
     *                                    step the marker holds the last finished step.
     */
    public static void migrate(String modId, String modVersion, Path worldRoot, Path markerFile, Path backupDir,
                               List<Path> dataPaths, List<Step> steps) throws IOException {
        int current = 0;
        for (Step s : steps) {
            if (s.version() <= current)
                throw new IllegalArgumentException(modId + ": step versions must be ascending and >= 1, got v" + s.version() + " after v" + current);
            current = s.version();
        }
        for (Path p : dataPaths)
            relative(worldRoot, p); // reject paths outside the world before touching anything

        Integer saved = readMarker(markerFile);
        if (saved == null) {
            if (!hasDataFile(dataPaths)) { // fresh world
                writeMarker(markerFile, current, modVersion);
                return;
            }
            saved = 0;
        }
        if (saved > current)
            throw new DataVersionTooNewException(modId, saved, current);
        if (saved == current)
            return;

        Path backup;
        try {
            backup = backup(modId, saved, worldRoot, backupDir != null ? backupDir : markerFile.getParent(), dataPaths);
        } catch (IOException | RuntimeException e) {
            throw new IOException(modId + ": backup of the save data failed, no migration step was run", e);
        }
        LOGGER.info("[{}] Save data is at data version {}, migrating to {}. Backup: {}", modId, saved, current, backup);

        int done = saved;
        for (Step s : steps) {
            if (s.version() <= saved)
                continue;
            try {
                s.action().run();
            } catch (Exception e) {
                throw new IOException(modId + ": data migration step to version " + s.version() + " (mod " + s.modVersion()
                        + ") failed; the save stays at data version " + done + ". Backup: " + backup, e);
            }
            writeMarker(markerFile, s.version(), modVersion);
            LOGGER.info("[{}] Migrated save data {} -> {} (mod {})", modId, done, s.version(), s.modVersion());
            done = s.version();
        }
    }

    /** {@code true} if a data path is a file or a folder with at least one file somewhere below it. */
    private static boolean hasDataFile(List<Path> dataPaths) throws IOException {
        for (Path p : dataPaths) {
            if (Files.isRegularFile(p))
                return true;
            if (Files.isDirectory(p)) {
                try (Stream<Path> walk = Files.walk(p)) {
                    if (walk.anyMatch(Files::isRegularFile))
                        return true;
                }
            }
        }
        return false;
    }

    private static Integer readMarker(Path markerFile) throws IOException {
        if (!Files.exists(markerFile))
            return null;
        try {
            int v = JsonParser.parseString(Files.readString(markerFile)).getAsJsonObject().get("dataVersion").getAsInt();
            if (v < 0)
                throw new IllegalStateException("negative dataVersion " + v);
            return v;
        } catch (RuntimeException e) {
            throw new IOException("Unreadable data version marker " + markerFile, e);
        }
    }

    private static void writeMarker(Path markerFile, int version, String modVersion) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("dataVersion", version);
        o.addProperty("modVersion", modVersion);
        Files.createDirectories(markerFile.getParent());
        DataPersistence.writeJsonAtomically(markerFile, new GsonBuilder().setPrettyPrinting().create().toJson(o));
    }

    private static Path backup(String modId, int oldVersion, Path worldRoot, Path dir, List<Path> dataPaths) throws IOException {
        Set<Path> files = new LinkedHashSet<>(); // set: overlapping data paths add a file only once
        for (Path p : dataPaths) {
            if (Files.isDirectory(p)) {
                try (Stream<Path> walk = Files.walk(p)) {
                    walk.filter(Files::isRegularFile).filter(f -> !isBackup(modId, f))
                            .forEach(f -> files.add(f.toAbsolutePath().normalize()));
                }
            } else if (Files.isRegularFile(p)) {
                files.add(p.toAbsolutePath().normalize());
                for (String suffix : SQLITE_SIBLINGS) {
                    Path sibling = p.resolveSibling(p.getFileName() + suffix);
                    if (Files.isRegularFile(sibling))
                        files.add(sibling.toAbsolutePath().normalize());
                }
            }
        }
        Files.createDirectories(dir);
        Path zip = dir.resolve(modId + "-data-v" + oldVersion + "-backup-" + LocalDateTime.now().format(STAMP) + ".zip");
        FileChannel channel = FileChannel.open(zip, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Channels.newOutputStream(channel)))) {
            for (Path f : files) {
                out.putNextEntry(new ZipEntry(relative(worldRoot, f)));
                Files.copy(f, out);
                out.closeEntry();
            }
            out.finish();
            out.flush();
            channel.force(true);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(zip); // a partial zip is no backup
            throw e;
        }
        return zip;
    }

    private static boolean isBackup(String modId, Path f) {
        String name = f.getFileName().toString();
        return name.startsWith(modId + "-data-v") && name.contains("-backup-") && name.endsWith(".zip");
    }

    private static String relative(Path worldRoot, Path p) {
        Path r = worldRoot.toAbsolutePath().normalize().relativize(p.toAbsolutePath().normalize());
        if (r.startsWith(".."))
            throw new IllegalArgumentException("Data path " + p + " is not inside the world folder " + worldRoot);
        return r.toString().replace('\\', '/');
    }
}
