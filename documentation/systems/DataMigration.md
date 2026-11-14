# Data Migration

Versions all world data of one mod (SQLite databases, NBT and JSON files) with a single integer and upgrades old saves
step by step at server start.

**Package**: `net.kroia.modutilities.persistence`
**Class**: `DataMigration`

---

## Prerequisites

- Server-side code only.
- The mod knows every file and folder in the world that holds its data.

---

## How it works

The data version is stored in a small marker file that the mod names, e.g. `<world>/data/<modid>/data_version.json`:
```json
{ "dataVersion": 3, "modVersion": "2.2.0" }
```
`modVersion` is the mod version that last wrote the marker. It is informational only.

At start, `DataMigration.migrate(...)` compares the saved version with the version of the newest step:

| Save | Result |
|---|---|
| No marker, no file under the data paths (fresh world; missing paths and empty folders count as no data) | Writes the current version. No step runs, no backup |
| No marker, but at least one file under the data paths (save from before versioning) | Treated as version 0: every step runs |
| Saved version lower than current | Backup, then every step with a higher version, in order |
| Saved version equals current | Nothing happens, nothing is logged |
| Saved version higher than current | Throws `DataVersionTooNewException`. No file is touched |

Each server in a [multi-server setup](../networking/MultiServerNetworking.md) migrates its own data.

### Backup

Before the first step runs, all data paths are zipped into
`<modid>-data-v<oldVersion>-backup-<yyyyMMdd-HHmmss>.zip`, in the marker's folder or the given backup folder.
- Folders are added recursively. For a file, its SQLite `-wal`, `-shm` and `-journal` siblings are added if they exist.
- Paths inside the zip are relative to the world folder. Earlier backup zips are not included.
- If the backup fails, no step runs and `migrate` throws an `IOException`.

To restore, stop the server, delete the mod's data paths and unzip the backup into the world folder.

### Crash safety

The marker is rewritten (atomically) after **every** finished step. If the server crashes or a step throws, the marker
holds the last finished step, and the next start runs only the unfinished step and the ones after it.

> [!IMPORTANT]
> Every step must be safe to run again on data it already partly converted. For example, check whether a database
> column already exists before adding it, and write converted files under a new name before deleting the old ones.

---

## API

| Member | Description |
|---|---|
| `migrate(String modId, String modVersion, Path worldRoot, Path markerFile, Path backupDir, List<Path> dataPaths, List<Step> steps)` | Runs the logic above. `backupDir` may be `null` (marker's folder). Every data path must be inside `worldRoot`. Throws `IOException` if the marker is unreadable, the backup fails or a step fails |
| `record Step(int version, String modVersion, Action action)` | `version`: the data version after this step (1, 2, 3, ...). `modVersion`: the mod version that added it, for logs. Steps must be in ascending version order; the last one is the current version |
| `interface Action { void run() throws Exception; }` | The conversion. May read, write, move or delete any of the mod's data files |
| `DataVersionTooNewException` | Extends `IOException`. Fields `modId`, `savedVersion`, `supportedVersion`; the message names all three |

Logged at INFO: the old and new version and the backup path, then one line per finished step (`from -> to`, mod version).

---

## Example

Call it once, before the mod opens any of its databases or data files:

```java
LifecycleEvent.SERVER_STARTING.register(server -> {
    Path world = server.getWorldPath(LevelResource.ROOT);
    Path dir = world.resolve("data/mymod");
    try {
        DataMigration.migrate("mymod", MyMod.VERSION, world, dir.resolve("data_version.json"), null,
                List.of(dir, world.resolve("mymod.db")),
                List.of(
                        new DataMigration.Step(1, "2.2.0", () -> renameLegacyFiles(dir)),
                        new DataMigration.Step(2, "2.3.0", () -> addCurrencyColumn(world.resolve("mymod.db")))));
    } catch (IOException e) {
        throw new IllegalStateException("MyMod cannot start on this save: " + e.getMessage(), e);
    }
    openDatabase();
});
```

A new step is added at the end of the list with the next version number. Steps are never changed or removed once released.

---

## See Also

- [Persistence System](Persistence.md) - `writeJsonAtomically` and crash-safe saves
- [Documentation Index](../README.md)
