# Persistence System

## Overview

The Persistence system provides a comprehensive NBT-based data storage framework for Minecraft mods. It handles saving and loading data with support for large datasets, chunking, archiving, and both compressed and uncompressed formats.

**Key Features:**
- NBT-based serialization with ServerSaveable interface
- Automatic chunking for large datasets (exceeding 2MB NBT limit)
- Data archive management with time-based organization
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

### Data Archives

For time-series or large datasets, the archive system provides:
- Time-based chunk organization
- Automatic chunk rotation based on size
- Efficient querying by time range
- Archive cleanup and management

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

## Data Archives

### Creating a DataArchiveChunk

```java
import net.kroia.modutilities.persistence.archive.DataArchiveChunk;
import net.minecraft.nbt.CompoundTag;

public class PlayerActivityChunk extends DataArchiveChunk {
    private final List<PlayerAction> actions = new ArrayList<>();
    
    public PlayerActivityChunk() {
        super(); // Uses current time as start
    }
    
    public PlayerActivityChunk(long startTime) {
        super(startTime);
    }
    
    public void addAction(PlayerAction action) {
        actions.add(action);
        updateEndTime(); // Update chunk end time
    }
    
    @Override
    protected boolean save(CompoundTag dataTag) {
        ListTag actionList = new ListTag();
        for (PlayerAction action : actions) {
            CompoundTag actionTag = new CompoundTag();
            actionTag.putString("player", action.getPlayerName());
            actionTag.putString("action", action.getActionType());
            actionTag.putLong("time", action.getTimestamp());
            actionList.add(actionTag);
        }
        dataTag.put("actions", actionList);
        return true;
    }
    
    @Override
    protected boolean load(CompoundTag dataTag) {
        ListTag actionList = dataTag.getList("actions", CompoundTag.TAG_COMPOUND);
        actions.clear();
        
        for (int i = 0; i < actionList.size(); i++) {
            CompoundTag actionTag = actionList.getCompound(i);
            PlayerAction action = new PlayerAction(
                actionTag.getString("player"),
                actionTag.getString("action"),
                actionTag.getLong("time")
            );
            actions.add(action);
        }
        return true;
    }
    
    public List<PlayerAction> getActions() {
        return actions;
    }
}
```

### Creating a DataArchiveManager

```java
import net.kroia.modutilities.persistence.archive.DataArchiveManager;
import net.kroia.modutilities.persistence.NBTFileParser;
import java.nio.file.Path;

public class PlayerActivityArchive extends DataArchiveManager<PlayerActivityChunk> {
    private PlayerActivityChunk currentChunk;
    
    public PlayerActivityArchive(Path archivePath) {
        super(archivePath, 
              NBTFileParser.NbtFormat.COMPRESSED, 
              PlayerActivityChunk::new); // Chunk factory
        
        currentChunk = new PlayerActivityChunk();
    }
    
    public void logAction(PlayerAction action) {
        currentChunk.addAction(action);
        
        // Check if chunk is getting too large (>80% of max size)
        if (getChunkSizeUtilisationPercentage(currentChunk) > 80) {
            rotateChunk();
        }
    }
    
    private void rotateChunk() {
        // Save current chunk
        long endTime = currentChunk.updateEndTime();
        saveChunk(currentChunk);
        
        // Create new chunk starting after previous one
        currentChunk = new PlayerActivityChunk(endTime + 1);
        System.out.println("Rotated to new chunk");
    }
    
    public List<PlayerAction> getActionsInRange(long startTime, long endTime) {
        List<PlayerAction> result = new ArrayList<>();
        
        // Create time interval for query
        DataArchiveChunk.TimeInterval range = 
            new DataArchiveChunk.TimeInterval(startTime, endTime);
        
        // Load all chunks that overlap with this range
        List<PlayerActivityChunk> chunks = loadChunks(range);
        
        for (PlayerActivityChunk chunk : chunks) {
            for (PlayerAction action : chunk.getActions()) {
                if (action.getTimestamp() >= startTime && 
                    action.getTimestamp() <= endTime) {
                    result.add(action);
                }
            }
        }
        
        return result;
    }
    
    public void saveCurrentChunk() {
        currentChunk.updateEndTime();
        saveChunk(currentChunk);
    }
}
```

### Using the Archive

```java
// Initialize archive
Path archivePath = Path.of("world/data/player_activity");
PlayerActivityArchive archive = new PlayerActivityArchive(archivePath);

// Log actions during gameplay
archive.logAction(new PlayerAction("Steve", "LOGIN", System.currentTimeMillis()));
archive.logAction(new PlayerAction("Alex", "BREAK_BLOCK", System.currentTimeMillis()));

// Query historical data
long dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
long now = System.currentTimeMillis();
List<PlayerAction> recentActions = archive.getActionsInRange(dayAgo, now);

// Save when server stops
archive.saveCurrentChunk();
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

### DataArchiveChunk

| Method | Description |
|--------|-------------|
| `long getStartTime()` | Get chunk start timestamp |
| `long getEndTime()` | Get chunk end timestamp |
| `long updateEndTime()` | Update end to current time |
| `TimeInterval getTimeInterval()` | Get time range |
| `long getUncompressedSize()` | Get data size in bytes |

### DataArchiveManager&lt;T&gt;

| Method | Description |
|--------|-------------|
| `boolean saveChunk(T chunk)` | Save chunk to archive |
| `List<T> loadChunks()` | Load all chunks |
| `List<T> loadChunks(TimeInterval range)` | Load chunks in time range |
| `T loadChunk(long startTime)` | Load specific chunk |
| `boolean clearArchive()` | Delete all chunks |
| `long getStartTime()` | Get earliest chunk time |
| `List<TimeInterval> getStoredIntervals()` | Get all chunk time ranges |

---

## Sandbox Example

Complete implementation demonstrating the persistence system:

```java
package net.kroia.modutilities.sandbox.persistence;

import net.kroia.modutilities.persistence.*;
import net.kroia.modutilities.persistence.archive.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.nio.file.Path;
import java.util.*;

/**
 * Example: Player achievement tracking with archive
 */
public class AchievementTracker {
    
    // Achievement data structure
    public static class Achievement implements ServerSaveable {
        private String id;
        private String playerName;
        private long unlockTime;
        private Map<String, Integer> criteria;
        
        public Achievement(String id, String playerName) {
            this.id = id;
            this.playerName = playerName;
            this.unlockTime = System.currentTimeMillis();
            this.criteria = new HashMap<>();
        }
        
        public Achievement() {
            this.criteria = new HashMap<>();
        }
        
        @Override
        public boolean save(CompoundTag tag) {
            tag.putString("id", id);
            tag.putString("player", playerName);
            tag.putLong("unlockTime", unlockTime);
            
            CompoundTag criteriaTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : criteria.entrySet()) {
                criteriaTag.putInt(entry.getKey(), entry.getValue());
            }
            tag.put("criteria", criteriaTag);
            return true;
        }
        
        @Override
        public boolean load(CompoundTag tag) {
            id = tag.getString("id");
            playerName = tag.getString("player");
            unlockTime = tag.getLong("unlockTime");
            
            CompoundTag criteriaTag = tag.getCompound("criteria");
            criteria.clear();
            for (String key : criteriaTag.getAllKeys()) {
                criteria.put(key, criteriaTag.getInt(key));
            }
            return true;
        }
        
        public String getId() { return id; }
        public String getPlayerName() { return playerName; }
        public long getUnlockTime() { return unlockTime; }
    }
    
    // Archive chunk for achievements
    public static class AchievementChunk extends DataArchiveChunk {
        private final List<Achievement> achievements = new ArrayList<>();
        
        public AchievementChunk() {
            super();
        }
        
        public AchievementChunk(long startTime) {
            super(startTime);
        }
        
        public void addAchievement(Achievement achievement) {
            achievements.add(achievement);
            updateEndTime();
        }
        
        @Override
        protected boolean save(CompoundTag dataTag) {
            ListTag achievementList = new ListTag();
            for (Achievement achievement : achievements) {
                CompoundTag achTag = new CompoundTag();
                achievement.save(achTag);
                achievementList.add(achTag);
            }
            dataTag.put("achievements", achievementList);
            return true;
        }
        
        @Override
        protected boolean load(CompoundTag dataTag) {
            ListTag achievementList = dataTag.getList("achievements", CompoundTag.TAG_COMPOUND);
            achievements.clear();
            
            for (int i = 0; i < achievementList.size(); i++) {
                CompoundTag achTag = achievementList.getCompound(i);
                Achievement achievement = new Achievement();
                achievement.load(achTag);
                achievements.add(achievement);
            }
            return true;
        }
        
        public List<Achievement> getAchievements() {
            return achievements;
        }
    }
    
    // Archive manager
    public static class AchievementArchive extends DataArchiveManager<AchievementChunk> {
        private AchievementChunk currentChunk;
        
        public AchievementArchive(Path archivePath) {
            super(archivePath, NBTFileParser.NbtFormat.COMPRESSED, AchievementChunk::new);
            currentChunk = new AchievementChunk();
        }
        
        public void unlockAchievement(String playerId, String achievementId) {
            Achievement achievement = new Achievement(achievementId, playerId);
            currentChunk.addAchievement(achievement);
            
            // Rotate chunk if getting full
            if (getChunkSizeUtilisationPercentage(currentChunk) > 80) {
                System.out.println("Rotating achievement chunk (>80% full)");
                long endTime = currentChunk.updateEndTime();
                saveChunk(currentChunk);
                currentChunk = new AchievementChunk(endTime + 1);
            }
        }
        
        public List<Achievement> getPlayerAchievements(String playerName, long since) {
            List<Achievement> result = new ArrayList<>();
            long now = System.currentTimeMillis();
            
            DataArchiveChunk.TimeInterval range = 
                new DataArchiveChunk.TimeInterval(since, now);
            
            List<AchievementChunk> chunks = loadChunks(range);
            for (AchievementChunk chunk : chunks) {
                for (Achievement achievement : chunk.getAchievements()) {
                    if (achievement.getPlayerName().equals(playerName)) {
                        result.add(achievement);
                    }
                }
            }
            
            return result;
        }
        
        public void shutdown() {
            currentChunk.updateEndTime();
            saveChunk(currentChunk);
        }
    }
    
    // Main example usage
    public static void main(String[] args) {
        Path archivePath = Path.of("world/achievements");
        AchievementArchive archive = new AchievementArchive(archivePath);
        
        // Simulate unlocking achievements
        archive.unlockAchievement("Steve", "first_tree");
        archive.unlockAchievement("Alex", "craft_pickaxe");
        archive.unlockAchievement("Steve", "mine_diamond");
        
        // Query achievements
        long dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        List<Achievement> steveAchievements = 
            archive.getPlayerAchievements("Steve", dayAgo);
        
        System.out.println("Steve's recent achievements:");
        for (Achievement ach : steveAchievements) {
            System.out.println("- " + ach.getId());
        }
        
        // Shutdown properly
        archive.shutdown();
    }
}
```

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
