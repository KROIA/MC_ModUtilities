# Persistence System

## Overview

The Persistence system provides a comprehensive NBT-based data storage framework for Minecraft mods. It handles saving and loading data with support for large datasets, chunking, archiving, and both compressed and uncompressed formats.

**Key Features:**
- NBT-based serialization with ServerSaveable interface
- Automatic chunking for large datasets (exceeding 2MB NBT limit)
- JSON export/import capabilities
- Compressed and uncompressed storage formats
- Thread-safe file operations
- Extensive logging and error handling

---

## Core Concepts

### ServerSaveable Pattern

A simple interface for objects that can be saved to/loaded from NBT:

```java
public interface ServerSaveable {
    boolean save(CompoundTag tag);
    boolean load(CompoundTag tag);
}
```

Any class implementing this interface can be automatically persisted.

### Chunking

Minecraft has a 2MB limit for NBT data. The persistence system automatically:
- Detects when data exceeds this limit
- Splits data into multiple chunk files
- Manages chunk folders and file naming
- Reconstructs data from chunks on load

---

## Basic Usage

### Implementing ServerSaveable

```java
import net.kroia.modutilities.persistence.ServerSaveable;
import net.minecraft.nbt.CompoundTag;

public class PlayerStats implements ServerSaveable {
    private int level;
    private long experience;
    private String playerName;
    
    @Override
    public boolean save(CompoundTag tag) {
        tag.putInt("level", level);
        tag.putLong("experience", experience);
        tag.putString("playerName", playerName);
        return true;
    }
    
    @Override
    public boolean load(CompoundTag tag) {
        level = tag.getInt("level");
        experience = tag.getLong("experience");
        playerName = tag.getString("playerName");
        return true;
    }
}
```

### Using DataPersistence

```java
import net.kroia.modutilities.persistence.DataPersistence;
import net.minecraft.nbt.CompoundTag;
import java.nio.file.Path;

public class DataManager {
    private final DataPersistence persistence;
    
    public DataManager(Path savePath) {
        persistence = new DataPersistence(
            DataPersistence.JsonFormat.PRETTY,
            DataPersistence.NbtFormat.COMPRESSED,
            Path.of("mymod/data")
        );
        persistence.setLevelSavePath(savePath);
    }
    
    public void savePlayerStats(PlayerStats stats) {
        CompoundTag tag = new CompoundTag();
        stats.save(tag);
        
        Path filePath = persistence.getAbsoluteSavePath("player_stats.nbt");
        persistence.saveDataCompound(filePath, tag);
    }
    
    public PlayerStats loadPlayerStats() {
        Path filePath = persistence.getAbsoluteSavePath("player_stats.nbt");
        CompoundTag tag = persistence.readDataCompound(filePath);
        
        if (tag != null) {
            PlayerStats stats = new PlayerStats();
            stats.load(tag);
            return stats;
        }
        return null;
    }
}
```

---

## Chunked Data

### ServerSaveableChunked Interface

For large datasets that naturally organize into lists:

```java
public interface ServerSaveableChunked {
    boolean save(Map<String, ListTag> listTags);
    boolean load(Map<String, ListTag> listTags);
}
```

### Using Chunked Storage

```java
import net.kroia.modutilities.persistence.ServerSaveableChunked;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.Map;
import java.util.HashMap;

public class WorldEventLog implements ServerSaveableChunked {
    private final List<Event> events = new ArrayList<>();
    
    @Override
    public boolean save(Map<String, ListTag> listTags) {
        ListTag eventList = new ListTag();
        
        for (Event event : events) {
            CompoundTag eventTag = new CompoundTag();
            eventTag.putString("type", event.getType());
            eventTag.putLong("timestamp", event.getTimestamp());
            eventTag.putString("data", event.getData());
            eventList.add(eventTag);
        }
        
        listTags.put("events.nbt", eventList);
        return true;
    }
    
    @Override
    public boolean load(Map<String, ListTag> listTags) {
        ListTag eventList = listTags.get("events.nbt");
        if (eventList == null) return false;
        
        events.clear();
        for (int i = 0; i < eventList.size(); i++) {
            CompoundTag eventTag = eventList.getCompound(i);
            Event event = new Event(
                eventTag.getString("type"),
                eventTag.getLong("timestamp"),
                eventTag.getString("data")
            );
            events.add(event);
        }
        return true;
    }
}

// Saving with automatic chunking
WorldEventLog log = new WorldEventLog();
Map<String, ListTag> listTags = new HashMap<>();
log.save(listTags);

// DataPersistence automatically chunks if data exceeds 2MB
persistence.saveDataCompoundListMap(
    persistence.getAbsoluteSavePath("event_log"),
    listTags
);

// Loading reconstructs from all chunks
Map<String, ListTag> loadedTags = 
    persistence.readDataCompoundListMap(
        persistence.getAbsoluteSavePath("event_log")
    );
log.load(loadedTags);
```

---

## Load/Save Patterns

### Pattern 1: Single File Save/Load

```java
public class SingleFilePersistence {
    private final DataPersistence persistence;
    
    public void saveGameState(GameState state) {
        CompoundTag tag = new CompoundTag();
        state.save(tag);
        
        Path path = persistence.getAbsoluteSavePath("gamestate.nbt");
        if (!persistence.saveDataCompound(path, tag)) {
            System.err.println("Failed to save game state!");
        }
    }
    
    public GameState loadGameState() {
        Path path = persistence.getAbsoluteSavePath("gamestate.nbt");
        CompoundTag tag = persistence.readDataCompound(path);
        
        if (tag == null) {
            System.err.println("No saved game state found");
            return new GameState(); // Return default
        }
        
        GameState state = new GameState();
        if (!state.load(tag)) {
            System.err.println("Failed to load game state!");
            return new GameState();
        }
        return state;
    }
}
```

### Pattern 2: Multiple Files

```java
public class MultiFilePersistence {
    public void saveAll(World world) {
        // Save different aspects to different files
        savePlayers(world.getPlayers());
        saveEntities(world.getEntities());
        saveChunks(world.getChunks());
    }
    
    private void savePlayers(List<Player> players) {
        ListTag playerList = new ListTag();
        for (Player player : players) {
            CompoundTag playerTag = new CompoundTag();
            player.save(playerTag);
            playerList.add(playerTag);
        }
        
        persistence.saveDataCompoundList(
            persistence.getAbsoluteSavePath("players.nbt"),
            playerList
        );
    }
}
```

### Pattern 3: Lazy Loading

```java
public class LazyLoadManager {
    private Map<String, PlayerStats> cache = new HashMap<>();
    
    public PlayerStats getPlayerStats(String playerName) {
        // Check cache first
        if (cache.containsKey(playerName)) {
            return cache.get(playerName);
        }
        
        // Load from disk if not in cache
        Path path = persistence.getAbsoluteSavePath("players/" + playerName + ".nbt");
        CompoundTag tag = persistence.readDataCompound(path);
        
        if (tag != null) {
            PlayerStats stats = new PlayerStats();
            stats.load(tag);
            cache.put(playerName, stats);
            return stats;
        }
        
        return null;
    }
    
    public void saveAllCached() {
        for (Map.Entry<String, PlayerStats> entry : cache.entrySet()) {
            Path path = persistence.getAbsoluteSavePath("players/" + entry.getKey() + ".nbt");
            CompoundTag tag = new CompoundTag();
            entry.getValue().save(tag);
            persistence.saveDataCompound(path, tag);
        }
    }
}
```

---

## Best Practices

### Thread Safety

```java
public class ThreadSafePersistence {
    private final DataPersistence persistence;
    private final ReentrantLock lock = new ReentrantLock();
    
    public void saveData(CompoundTag data, String filename) {
        lock.lock();
        try {
            Path path = persistence.getAbsoluteSavePath(filename);
            persistence.saveDataCompound(path, data);
        } finally {
            lock.unlock();
        }
    }
    
    public CompoundTag loadData(String filename) {
        lock.lock();
        try {
            Path path = persistence.getAbsoluteSavePath(filename);
            return persistence.readDataCompound(path);
        } finally {
            lock.unlock();
        }
    }
}
```

### Data Migration

```java
public class VersionedData implements ServerSaveable {
    private static final int CURRENT_VERSION = 2;
    private int dataVersion;
    private Map<String, Object> data;
    
    @Override
    public boolean save(CompoundTag tag) {
        tag.putInt("version", CURRENT_VERSION);
        // Save current format
        return true;
    }
    
    @Override
    public boolean load(CompoundTag tag) {
        dataVersion = tag.getInt("version");
        
        switch (dataVersion) {
            case 1:
                loadVersion1(tag);
                migrateV1ToV2();
                break;
            case 2:
                loadVersion2(tag);
                break;
            default:
                System.err.println("Unknown data version: " + dataVersion);
                return false;
        }
        return true;
    }
    
    private void migrateV1ToV2() {
        // Perform migration logic
        System.out.println("Migrating data from v1 to v2");
    }
}
```

### Backup Strategies

```java
public class BackupManager {
    private final DataPersistence persistence;
    
    public void saveWithBackup(CompoundTag data, String filename) {
        Path mainPath = persistence.getAbsoluteSavePath(filename);
        Path backupPath = persistence.getAbsoluteSavePath(filename + ".backup");
        
        // If main file exists, copy to backup
        if (persistence.fileExists(mainPath)) {
            CompoundTag backup = persistence.readDataCompound(mainPath);
            if (backup != null) {
                persistence.saveDataCompound(backupPath, backup);
            }
        }
        
        // Save new data
        if (!persistence.saveDataCompound(mainPath, data)) {
            System.err.println("Save failed! Backup preserved at: " + backupPath);
        }
    }
    
    public CompoundTag loadWithFallback(String filename) {
        Path mainPath = persistence.getAbsoluteSavePath(filename);
        CompoundTag data = persistence.readDataCompound(mainPath);
        
        if (data == null) {
            System.err.println("Main file corrupted, trying backup...");
            Path backupPath = persistence.getAbsoluteSavePath(filename + ".backup");
            data = persistence.readDataCompound(backupPath);
            
            if (data != null) {
                System.out.println("Restored from backup");
            }
        }
        
        return data;
    }
}
```

### Error Handling

```java
public class RobustPersistence {
    private final DataPersistence persistence;
    
    public RobustPersistence() {
        persistence = new DataPersistence(
            DataPersistence.JsonFormat.PRETTY,
            DataPersistence.NbtFormat.COMPRESSED,
            Path.of("mymod")
        );
        
        // Set up logging
        persistence.setLogger(
            error -> ModLogger.error(error),
            (error, throwable) -> ModLogger.error(error, throwable),
            debug -> ModLogger.debug(debug),
            warn -> ModLogger.warn(warn)
        );
    }
    
    public boolean trySave(CompoundTag data, String filename) {
        try {
            Path path = persistence.getAbsoluteSavePath(filename);
            
            // Ensure directory exists
            if (!persistence.folderExists(path.getParent())) {
                persistence.createFolder(path.getParent());
            }
            
            // Check data size
            long size = ChunkedNBT.getUncompressedSize(data);
            if (size > 2_097_152L) {
                System.err.println("Warning: Data exceeds 2MB, consider chunking");
            }
            
            return persistence.saveDataCompound(path, data);
            
        } catch (Exception e) {
            System.err.println("Save failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
```

---

## API Reference

### DataPersistence

| Method | Description |
|--------|-------------|
| `void setLevelSavePath(Path path)` | Set world save directory |
| `Path getAbsoluteSavePath()` | Get full save path |
| `boolean saveDataCompound(Path path, CompoundTag tag)` | Save single NBT file |
| `CompoundTag readDataCompound(Path path)` | Load single NBT file |
| `boolean saveDataCompoundList(Path path, ListTag list)` | Save with auto-chunking |
| `ListTag readDataCompoundList(Path path)` | Load from chunked files |
| `boolean saveAsJson(Object o, Path path)` | Export to JSON |
| `<T> T loadFromJson(Path path, Type type)` | Import from JSON |
| `boolean fileExists(Path path)` | Check if file exists |
| `boolean folderExists(Path path)` | Check if folder exists |

---

## Sandbox Example

See the persistence tests and sandbox code for working examples of NBT persistence with `DataPersistence` and `ChunkedNBT`.

---

## Performance Considerations

1. **Chunk Size**: Monitor chunk utilization and rotate at 80% to avoid hitting the 2MB limit
2. **Compression**: Use COMPRESSED format for production to save disk space
3. **Batch Operations**: Save multiple changes together rather than individually
4. **Lazy Loading**: Only load data when needed, not all at startup
5. **Cache Strategy**: Keep frequently accessed data in memory
6. **Archive Cleanup**: Periodically remove old archive chunks to manage disk usage

---

## Common Pitfalls

1. **Forgetting to Create Directories**: Always ensure parent directories exist before saving
2. **Not Checking Return Values**: Always verify save/load operations succeeded
3. **Exceeding NBT Limit**: Use chunking for large datasets
4. **Thread Safety**: Protect concurrent access with locks
5. **Missing Backups**: Always maintain backups before overwriting data
6. **Version Incompatibility**: Implement versioning for long-term data

---

## Related Systems

- **Settings System**: Use Settings for configuration, Persistence for game data
- **Networking**: Sync persisted data between client and server
- **Event System**: Trigger saves on specific events
