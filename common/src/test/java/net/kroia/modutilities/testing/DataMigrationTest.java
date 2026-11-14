package net.kroia.modutilities.testing;

import net.kroia.modutilities.persistence.DataMigration;
import net.kroia.modutilities.persistence.DataMigration.Step;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/** Offline tests for {@link DataMigration}; lives in the testing package so release-mode test builds include it. */
class DataMigrationTest {
    @TempDir Path world;

    Path dataDir()  { return world.resolve("data/mymod"); }
    Path marker()   { return dataDir().resolve("data_version.json"); }
    Path db()       { return world.resolve("mymod.db"); }
    List<Path> dataPaths() { return List.of(dataDir(), db()); }

    final List<Integer> ran = new ArrayList<>();

    List<Step> steps(int count, int failAt) {
        List<Step> steps = new ArrayList<>();
        for (int v = 1; v <= count; v++) {
            int version = v;
            steps.add(new Step(version, "2." + version + ".0", () -> {
                if (version == failAt) throw new IllegalStateException("boom");
                ran.add(version);
            }));
        }
        return steps;
    }

    void migrate(List<Step> steps) throws IOException {
        DataMigration.migrate("mymod", "2.2.0", world, marker(), null, dataPaths(), steps);
    }

    void writeMarker(int version) throws IOException {
        Files.createDirectories(dataDir());
        Files.writeString(marker(), "{\"dataVersion\": " + version + ", \"modVersion\": \"x\"}");
    }

    void writeData() throws IOException {
        Files.createDirectories(dataDir().resolve("sub"));
        Files.writeString(dataDir().resolve("bank.nbt"), "nbt");
        Files.writeString(dataDir().resolve("sub/x.json"), "{}");
        Files.writeString(db(), "db");
        Files.writeString(world.resolve("mymod.db-wal"), "wal");
        Files.writeString(world.resolve("unrelated.dat"), "other mod");
    }

    int markerVersion() throws IOException {
        String s = Files.readString(marker());
        return Integer.parseInt(s.replaceAll("(?s).*\"dataVersion\":\\s*(\\d+).*", "$1"));
    }

    List<Path> backups() throws IOException {
        if (!Files.isDirectory(dataDir())) return List.of();
        try (Stream<Path> s = Files.list(dataDir())) {
            return s.filter(p -> p.getFileName().toString().matches("mymod-data-v\\d+-backup-\\d{8}-\\d{6}\\.zip")).toList();
        }
    }

    @Test
    void freshWorldWritesMarkerOnly() throws IOException {
        migrate(steps(4, -1));
        assertEquals(4, markerVersion());
        assertTrue(Files.readString(marker()).contains("\"modVersion\": \"2.2.0\""));
        assertTrue(ran.isEmpty());
        assertTrue(backups().isEmpty());
    }

    @Test
    void emptyFoldersCountAsFreshWorld() throws IOException {
        Files.createDirectories(dataDir().resolve("sub/deeper"));
        migrate(steps(4, -1));
        assertEquals(4, markerVersion());
        assertTrue(ran.isEmpty());
        assertTrue(backups().isEmpty());
    }

    @Test
    void preVersioningSaveRunsAllStepsWithBackup() throws IOException {
        writeData();
        migrate(steps(4, -1));
        assertEquals(List.of(1, 2, 3, 4), ran);
        assertEquals(4, markerVersion());
        assertEquals(1, backups().size());
        assertTrue(backups().get(0).getFileName().toString().startsWith("mymod-data-v0-backup-"));

        ran.clear();
        migrate(steps(4, -1)); // up to date: nothing runs, no new backup
        assertTrue(ran.isEmpty());
        assertEquals(1, backups().size());
    }

    @Test
    void runsOnlyNewerSteps() throws IOException {
        writeData();
        writeMarker(2);
        migrate(steps(4, -1));
        assertEquals(List.of(3, 4), ran);
        assertEquals(4, markerVersion());
        assertTrue(backups().get(0).getFileName().toString().startsWith("mymod-data-v2-backup-"));
    }

    @Test
    void newerSaveThrowsAndTouchesNothing() throws IOException {
        writeData();
        writeMarker(5);
        byte[] markerBefore = Files.readAllBytes(marker());
        byte[] nbtBefore = Files.readAllBytes(dataDir().resolve("bank.nbt"));
        byte[] dbBefore = Files.readAllBytes(db());

        var e = assertThrows(DataMigration.DataVersionTooNewException.class, () -> migrate(steps(4, -1)));
        assertEquals(5, e.savedVersion);
        assertEquals(4, e.supportedVersion);
        assertTrue(e.getMessage().contains("mymod"));

        assertArrayEquals(markerBefore, Files.readAllBytes(marker()));
        assertArrayEquals(nbtBefore, Files.readAllBytes(dataDir().resolve("bank.nbt")));
        assertArrayEquals(dbBefore, Files.readAllBytes(db()));
        assertTrue(ran.isEmpty());
        assertTrue(backups().isEmpty());
    }

    @Test
    void failedStepKeepsLastFinishedVersionAndRerunContinues() throws IOException {
        writeData();
        assertThrows(IOException.class, () -> migrate(steps(4, 3)));
        assertEquals(List.of(1, 2), ran);
        assertEquals(2, markerVersion());

        ran.clear();
        migrate(steps(4, -1));
        assertEquals(List.of(3, 4), ran);
        assertEquals(4, markerVersion());
    }

    @Test
    void backupContainsEveryDataFile() throws IOException {
        writeData();
        writeMarker(1);
        Files.writeString(dataDir().resolve("mymod-data-v0-backup-20260101-000000.zip"), "old backup");
        migrate(steps(2, -1));

        Set<String> entries = new TreeSet<>();
        try (ZipFile zip = new ZipFile(backups().stream()
                .filter(p -> p.getFileName().toString().startsWith("mymod-data-v1-")).findFirst().orElseThrow().toFile())) {
            zip.stream().forEach(z -> entries.add(z.getName()));
        }
        assertEquals(new TreeSet<>(List.of(
                "data/mymod/data_version.json", "data/mymod/bank.nbt", "data/mymod/sub/x.json",
                "mymod.db", "mymod.db-wal")), entries);
    }
}
