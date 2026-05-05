# Utility Classes

## Overview

MC ModUtilities provides a comprehensive set of utility classes to simplify common Minecraft modding tasks. These utilities handle:

- **Item Management**: Creation, filtering, searching, and manipulation
- **Color Operations**: RGB/RGBA conversion, interpolation, brightness
- **Player Interactions**: Server and client player helper methods
- **JSON Handling**: Serialization and formatting
- **Timing**: Persistent timer with auto-restart capabilities
- **Platform Detection**: Mod loading and platform abstraction
- **Cross-Platform Support**: Unified API for Fabric/Forge/Quilt

**Location**: `net.kroia.modutilities`

---

## ItemUtilities

Comprehensive utilities for working with items, stacks, tags, and the creative inventory.

### Item Creation

```java
import net.kroia.modutilities.ItemUtilities;
import net.minecraft.world.item.ItemStack;

// Create item stack from ID
ItemStack diamond = ItemUtilities.createItemStackFromId("diamond");
ItemStack diamonds = ItemUtilities.createItemStackFromId("minecraft:diamond", 64);

// Normalize item IDs (adds "minecraft:" prefix if missing)
String fullId = ItemUtilities.getNormalizedItemID("diamond");
// Returns: "minecraft:diamond"

String invalid = ItemUtilities.getNormalizedItemID("invalid_item");
// Returns: null
```

### Item Information

```java
import net.minecraft.world.item.Item;

// Get item ID string
String itemId = ItemUtilities.getItemIDStr(item);

// Get item display name
String name = ItemUtilities.getItemName(item);
String nameById = ItemUtilities.getItemName("minecraft:diamond_sword");
```

### Item Retrieval and Filtering

```java
import java.util.ArrayList;

// Get all items
ArrayList<String> allItemIds = ItemUtilities.getAllItemIDStrs();
ArrayList<ItemStack> allItems = ItemUtilities.getAllItems();

// Filter by tag
ArrayList<String> oreIds = ItemUtilities.getAllItemIDStrs("minecraft:ores");
ArrayList<ItemStack> ores = ItemUtilities.getAllItems("minecraft:ores");

// Filter by multiple tags and ID patterns
ArrayList<String> tags = new ArrayList<>();
tags.add("ores");
tags.add("ingots");

ArrayList<String> containsInID = new ArrayList<>();
containsInID.add("minecraft:iron_ore");
containsInID.add("minecraft:gold_ingot");

ArrayList<ItemStack> filtered = ItemUtilities.getAllItems(tags, containsInID);
```

### Creative Inventory Search

```java
import java.util.List;

// Search creative inventory by name
List<ItemStack> results = ItemUtilities.getSearchCreativeItems("diamond");
// Returns items with "diamond" in their display name

// Extract search text from item ID
String searchText = ItemUtilities.getSearchTextFromItemID("minecraft:diamond_sword");
// Returns: "diamond sword" (replaces underscores with spaces)
```

### Player Inventory Operations

```java
import net.minecraft.server.level.ServerPlayer;

// Add item to player inventory
ItemStack stack = ItemUtilities.createItemStackFromId("diamond", 10);
int remaining = ItemUtilities.addToPlayerInventory(player, stack);
// Returns: number of items that didn't fit

// Drop item at player location
ItemUtilities.dropItemAtPlayer(player, stack);

// Check if slot is main inventory (not armor/offhand)
boolean isMainSlot = ItemUtilities.isMainInventorySlot(5); // true
boolean isArmorSlot = ItemUtilities.isMainInventorySlot(39); // false
```

### API Reference

| Method | Description |
|--------|-------------|
| `createItemStackFromId(String)` | Create item stack with amount 1 |
| `createItemStackFromId(String, int)` | Create item stack with specified amount |
| `getNormalizedItemID(String)` | Get full namespaced ID or null if invalid |
| `getItemName(Item)` | Get item display name |
| `getItemIDStr(Item)` | Get item's resource location string |
| `getAllItemIDStrs()` | Get all registered item IDs |
| `getAllItems()` | Get all registered items |
| `getAllItemIDStrs(String tag)` | Get items matching a tag |
| `getAllItems(ArrayList<String>, ArrayList<String>)` | Get items by tags and ID patterns |
| `getSearchCreativeItems(String)` | Search creative inventory |
| `getSearchTextFromItemID(String)` | Extract readable name from ID |
| `addToPlayerInventory(ServerPlayer, ItemStack)` | Add items to player inventory |
| `dropItemAtPlayer(ServerPlayer, ItemStack)` | Drop item at player position |
| `isMainInventorySlot(int)` | Check if slot index is main inventory |

---

## ColorUtilities

Utilities for RGB/RGBA color manipulation and conversion.

### Color Extraction

```java
import net.kroia.modutilities.ColorUtilities;

int color = 0xFFAABBCC; // ARGB format

// Extract individual channels
int alpha = ColorUtilities.getAlpha(color);  // 255
int red = ColorUtilities.getRed(color);      // 170
int green = ColorUtilities.getGreen(color);  // 187
int blue = ColorUtilities.getBlue(color);    // 204
```

### Color Creation

```java
// Create RGB color (alpha = 255)
int rgb = ColorUtilities.getRGB(255, 128, 0);

// Create RGBA color
int rgba = ColorUtilities.getRGB(255, 128, 0, 200);

// Add alpha to existing RGB color
int withAlpha = ColorUtilities.getRGB(rgb, 128);
int withAlphaFloat = ColorUtilities.getRGB(rgb, 0.5f);
```

### Color Modification

```java
// Set individual channels (int 0-255)
int newColor = ColorUtilities.setRed(color, 255);
newColor = ColorUtilities.setGreen(newColor, 128);
newColor = ColorUtilities.setBlue(newColor, 64);
newColor = ColorUtilities.setAlpha(newColor, 200);

// Set channels using float (0.0-1.0)
newColor = ColorUtilities.setRed(color, 1.0f);
newColor = ColorUtilities.setAlpha(color, 0.5f);

// Adjust brightness
int darker = ColorUtilities.setBrightness(color, 0.5f);  // 50% brightness
int brighter = ColorUtilities.setBrightness(color, 1.5f); // 150% brightness
```

### Color Interpolation

```java
int color1 = 0xFFFF0000; // Red
int color2 = 0xFF0000FF; // Blue

// Interpolate between colors
int purple = ColorUtilities.interpolate(color1, color2, 0.5f);

// Create color gradients
for (float t = 0; t <= 1.0f; t += 0.1f) {
    int gradientColor = ColorUtilities.interpolate(color1, color2, t);
}
```

### Practical Examples

```java
// Create semi-transparent overlay
int overlayColor = ColorUtilities.getRGB(0, 0, 0, 128);

// Fade effect
public int getFadeColor(int baseColor, float fadeProgress) {
    return ColorUtilities.setAlpha(baseColor, 1.0f - fadeProgress);
}

// Health bar color (green to red)
public int getHealthColor(float healthPercent) {
    int green = 0xFF00FF00;
    int red = 0xFFFF0000;
    return ColorUtilities.interpolate(red, green, healthPercent);
}

// Darken on hover
public int getDarkenedColor(int color, boolean isHovered) {
    return isHovered ? ColorUtilities.setBrightness(color, 0.8f) : color;
}
```

### API Reference

| Method | Description |
|--------|-------------|
| `getRed(int)` | Extract red channel (0-255) |
| `getGreen(int)` | Extract green channel (0-255) |
| `getBlue(int)` | Extract blue channel (0-255) |
| `getAlpha(int)` | Extract alpha channel (0-255) |
| `getRGB(int, int, int)` | Create RGB color (alpha=255) |
| `getRGB(int, int, int, int)` | Create RGBA color |
| `getRGB(int, int)` | Set alpha on RGB color (int) |
| `getRGB(int, float)` | Set alpha on RGB color (float) |
| `setRed(int, int)` | Set red channel (int) |
| `setRed(int, float)` | Set red channel (float) |
| `setGreen(int, int)` | Set green channel (int) |
| `setGreen(int, float)` | Set green channel (float) |
| `setBlue(int, int)` | Set blue channel (int) |
| `setBlue(int, float)` | Set blue channel (float) |
| `setAlpha(int, int)` | Set alpha channel (int) |
| `setAlpha(int, float)` | Set alpha channel (float) |
| `setBrightness(int, float)` | Adjust color brightness |
| `interpolate(int, int, float)` | Interpolate between colors |

---

## ServerPlayerUtilities

Server-side utilities for interacting with players.

### Sending Messages

```java
import net.kroia.modutilities.ServerPlayerUtilities;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

// Send message to specific player
ServerPlayerUtilities.printToClientConsole(player, "Hello, player!");

// Send message by UUID
ServerPlayerUtilities.printToClientConsole(playerUUID, "Message by UUID");

// Send message by username
ServerPlayerUtilities.printToClientConsole("PlayerName", "Message by name");

// Broadcast to all online players
ServerPlayerUtilities.printToClientConsole("Server announcement!");
```

### Player Lookup

```java
// Get player by UUID
ServerPlayer player = ServerPlayerUtilities.getOnlinePlayer(playerUUID);

// Get player by username
ServerPlayer player = ServerPlayerUtilities.getOnlinePlayer("Notch");

// Get all online players
ArrayList<ServerPlayer> players = ServerPlayerUtilities.getOnlinePlayers();

// Get all online player names
ArrayList<String> names = ServerPlayerUtilities.getOnlinePlayerNames();

// Get UUID to name mapping
Map<UUID, String> uuidMap = ServerPlayerUtilities.getUUIDToNameMap();
```

### Inventory Management

```java
// Add items to player inventory
ItemStack diamonds = new ItemStack(Items.DIAMOND, 64);
int leftover = ServerPlayerUtilities.addToPlayerInventory(player, diamonds);

if (leftover > 0) {
    ServerPlayerUtilities.printToClientConsole(player, 
        "Your inventory is full! " + leftover + " items dropped.");
}

// Check if slot is main inventory
boolean isMainSlot = ServerPlayerUtilities.isMainInventorySlot(10); // true
boolean isOffhand = ServerPlayerUtilities.isMainInventorySlot(40); // false
```

### Practical Examples

```java
// Reward system
public void giveReward(UUID playerUUID, ItemStack reward) {
    ServerPlayer player = ServerPlayerUtilities.getOnlinePlayer(playerUUID);
    if (player != null) {
        int remaining = ServerPlayerUtilities.addToPlayerInventory(player, reward);
        if (remaining == 0) {
            ServerPlayerUtilities.printToClientConsole(player, 
                "Reward received: " + reward.getHoverName().getString());
        } else {
            ServerPlayerUtilities.printToClientConsole(player, 
                "Inventory full! Some items were not received.");
        }
    }
}

// Broadcast to specific players
public void notifyTeam(List<UUID> teamMembers, String message) {
    for (UUID uuid : teamMembers) {
        ServerPlayerUtilities.printToClientConsole(uuid, "[TEAM] " + message);
    }
}

// Check if player is online
public boolean isPlayerOnline(String username) {
    return ServerPlayerUtilities.getOnlinePlayer(username) != null;
}
```

### API Reference

| Method | Description |
|--------|-------------|
| `printToClientConsole(ServerPlayer, String)` | Send message to player |
| `printToClientConsole(UUID, String)` | Send message by UUID |
| `printToClientConsole(String, String)` | Send message by username |
| `printToClientConsole(String)` | Broadcast to all players |
| `getOnlinePlayer(UUID)` | Get player by UUID |
| `getOnlinePlayer(String)` | Get player by username |
| `getOnlinePlayers()` | Get all online players |
| `getOnlinePlayerNames()` | Get all online player names |
| `getUUIDToNameMap()` | Get UUID to name mapping |
| `addToPlayerInventory(ServerPlayer, ItemStack)` | Add items to inventory |
| `isMainInventorySlot(int)` | Check if slot is main inventory |

---

## ClientPlayerUtilities

Client-side utilities for player interactions.

**Note**: This class is annotated with `@Environment(EnvType.CLIENT)` and only available on the client side.

### Item Information

```java
import net.kroia.modutilities.ClientPlayerUtilities;
import net.minecraft.world.item.ItemStack;

// Get full item tooltip text
ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
String tooltip = ClientPlayerUtilities.getItemDisplayText(stack);
// Returns multi-line string with item name and all tooltip lines
```

### Console Output

```java
// Print to client console (or stdout if no player)
ClientPlayerUtilities.printToConsole("Debug message");
```

### Practical Examples

```java
// Display item information in GUI
public void showItemInfo(ItemStack stack) {
    String fullInfo = ClientPlayerUtilities.getItemDisplayText(stack);
    String[] lines = fullInfo.split("\n");
    for (String line : lines) {
        // Render each line in GUI
        renderTooltipLine(line);
    }
}

// Debug logging
@Environment(EnvType.CLIENT)
public void debugLog(String message) {
    ClientPlayerUtilities.printToConsole("[DEBUG] " + message);
}
```

### API Reference

| Method | Description |
|--------|-------------|
| `getItemDisplayText(ItemStack)` | Get full tooltip text for item |
| `printToConsole(String)` | Print to client console |

---

## JsonUtilities

Simple utilities for JSON serialization and formatting.

### JSON Formatting

```java
import net.kroia.modutilities.JsonUtilities;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

// Create JSON object
JsonObject obj = new JsonObject();
obj.addProperty("name", "Test");
obj.addProperty("value", 42);

// Convert to pretty-printed string
String pretty = JsonUtilities.toPrettyString(obj);
// Output:
// {
//   "name": "Test",
//   "value": 42
// }

// Convert to compact string
String compact = JsonUtilities.toString(obj);
// Output: {"name":"Test","value":42}
```

### JSON Parsing

```java
// Parse string to JsonElement
String jsonString = "{\"name\":\"Test\",\"value\":42}";
JsonElement element = JsonUtilities.fromString(jsonString);

// Use the parsed element
if (element.isJsonObject()) {
    JsonObject obj = element.getAsJsonObject();
    String name = obj.get("name").getAsString();
}
```

### Practical Examples

```java
// Save configuration
public void saveConfig(JsonObject config) {
    String json = JsonUtilities.toPrettyString(config);
    Files.writeString(configPath, json);
}

// Network packet serialization
public String serializePacket(JsonElement data) {
    return JsonUtilities.toString(data); // Compact for network efficiency
}

// Debug output
public void debugJson(JsonElement element) {
    System.out.println(JsonUtilities.toPrettyString(element));
}
```

### API Reference

| Method | Description |
|--------|-------------|
| `toPrettyString(JsonElement)` | Convert to formatted JSON string |
| `toString(JsonElement)` | Convert to compact JSON string |
| `fromString(String)` | Parse JSON string to JsonElement |

---

## TimerMillis

A persistent timer with millisecond precision, auto-restart capability, and NBT serialization support.

### Basic Usage

```java
import net.kroia.modutilities.TimerMillis;

// Create timer (autoRestart = false)
TimerMillis timer = new TimerMillis(false);

// Start timer for 5 seconds
timer.start(5000);

// Check if timer is running
if (timer.isRunning()) {
    System.out.println("Timer is running...");
}

// Check if finished
if (timer.isFinished()) {
    System.out.println("Timer finished!");
}
```

### Auto-Restart Timers

```java
// Create auto-restart timer
TimerMillis autoTimer = new TimerMillis(true);
autoTimer.start(1000); // 1 second

// In game tick loop
public void tick() {
    if (autoTimer.check()) {
        // Executes every 1 second automatically
        performPeriodicTask();
    }
}
```

### Timer Information

```java
// Get elapsed time
long elapsed = timer.getElapsedTime();

// Get remaining time
long remaining = timer.getRemainingTime();

// Get timer configuration
long duration = timer.getDuration();
long startTime = timer.getStartTime();
boolean isAutoRestart = timer.isAutoRestart();

// Modify timer
timer.setDuration(10000); // Change to 10 seconds
timer.setAutoRestart(true);

// Stop timer
timer.stop();
```

### Persistence

```java
import net.minecraft.nbt.CompoundTag;

// Save timer state
CompoundTag tag = new CompoundTag();
timer.save(tag);

// Load timer state
TimerMillis loadedTimer = new TimerMillis(false);
loadedTimer.load(tag);
```

### Practical Examples

```java
// Cooldown system
public class Ability {
    private final TimerMillis cooldown = new TimerMillis(false);
    
    public boolean use() {
        if (!cooldown.isRunning()) {
            performAbility();
            cooldown.start(30000); // 30 second cooldown
            return true;
        }
        return false;
    }
    
    public long getCooldownRemaining() {
        return cooldown.getRemainingTime();
    }
}

// Periodic task
public class AutoSave {
    private final TimerMillis saveTimer = new TimerMillis(true);
    
    public AutoSave() {
        saveTimer.start(300000); // Auto-save every 5 minutes
    }
    
    public void tick() {
        if (saveTimer.check()) {
            performAutoSave();
        }
    }
}

// Temporary effect
public class BuffEffect {
    private final TimerMillis duration = new TimerMillis(false);
    
    public void apply(long durationMs) {
        duration.start(durationMs);
    }
    
    public boolean isActive() {
        return duration.isRunning() && !duration.isFinished();
    }
    
    public float getProgress() {
        return (float) duration.getElapsedTime() / duration.getDuration();
    }
}
```

### Global Time Offset

```java
// Adjust global timer offset (affects all TimerMillis instances)
TimerMillis.TIMER_OFFSET_MS = 1000; // Add 1 second offset

// Get current time with offset
long currentTime = timer.currentTimeMillis();
```

### API Reference

| Method | Description |
|--------|-------------|
| `TimerMillis(boolean)` | Create timer with auto-restart option |
| `start(long)` | Start timer with duration in milliseconds |
| `isRunning()` | Check if timer is currently running |
| `isFinished()` | Check if timer has completed |
| `check()` | Check and auto-restart if enabled |
| `stop()` | Stop the timer |
| `getElapsedTime()` | Get milliseconds elapsed |
| `getRemainingTime()` | Get milliseconds remaining |
| `getDuration()` | Get configured duration |
| `getStartTime()` | Get start timestamp |
| `setDuration(long)` | Change timer duration |
| `setAutoRestart(boolean)` | Enable/disable auto-restart |
| `isAutoRestart()` | Check if auto-restart is enabled |
| `save(CompoundTag)` | Serialize to NBT |
| `load(CompoundTag)` | Deserialize from NBT |
| `currentTimeMillis()` | Get current time with offset |

---

## ModChecker

Platform-independent mod detection.

### Basic Usage

```java
import net.kroia.modutilities.ModChecker;

// Check if mod is loaded
boolean hasJEI = ModChecker.isModLoaded("jei");
boolean hasIronChests = ModChecker.isModLoaded("ironchest");

// Conditional features
if (ModChecker.isModLoaded("create")) {
    enableCreateIntegration();
}
```

### Practical Examples

```java
// Optional integration
public void registerCompatibility() {
    if (ModChecker.isModLoaded("appleskin")) {
        registerAppleSkinCompat();
    }
    
    if (ModChecker.isModLoaded("waila")) {
        registerWailaCompat();
    }
}

// Feature gating
public boolean canUseAdvancedFeatures() {
    return ModChecker.isModLoaded("required_dependency");
}

// Conflict detection
public void checkConflicts() {
    if (ModChecker.isModLoaded("conflicting_mod")) {
        LOGGER.warn("Conflicting mod detected, disabling feature X");
        disableFeatureX();
    }
}
```

### API Reference

| Method | Description |
|--------|-------------|
| `isModLoaded(String)` | Check if mod with given ID is loaded |

---

## PlatformAbstraction

Interface for platform-specific implementations (Fabric/Forge/Quilt).

**Note**: This is an internal interface used by `UtilitiesPlatform`. Most mods should use the higher-level utility classes instead.

### Interface Definition

```java
public interface PlatformAbstraction {
    ItemStack getItemStack(String itemIDStr);
    String getItemIDStr(Item item);
    ArrayList<ItemStack> getAllItems();
    MinecraftServer getServer();
    UtilitiesPlatform.Type getPlatformType();
}
```

### Usage Through UtilitiesPlatform

```java
// Get platform type
UtilitiesPlatform.Type platform = UtilitiesPlatform.getPlatformType();

switch (platform) {
    case FABRIC -> System.out.println("Running on Fabric");
    case FORGE -> System.out.println("Running on Forge");
    case QUILT -> System.out.println("Running on Quilt");
}

// Access server instance
MinecraftServer server = UtilitiesPlatform.getServer();

// Get item by ID (platform-independent)
ItemStack stack = UtilitiesPlatform.getItemStack("minecraft:diamond");
```

---

## Quick Reference Table

### ItemUtilities
| Task | Method |
|------|--------|
| Create item | `createItemStackFromId(id, amount)` |
| Get all items | `getAllItems()` |
| Search items | `getSearchCreativeItems(text)` |
| Add to inventory | `addToPlayerInventory(player, stack)` |

### ColorUtilities
| Task | Method |
|------|--------|
| Create color | `getRGB(r, g, b, a)` |
| Extract channel | `getRed(color)`, `getAlpha(color)` |
| Modify channel | `setRed(color, value)` |
| Interpolate | `interpolate(c1, c2, ratio)` |
| Adjust brightness | `setBrightness(color, factor)` |

### ServerPlayerUtilities
| Task | Method |
|------|--------|
| Send message | `printToClientConsole(player, msg)` |
| Get player | `getOnlinePlayer(uuid/name)` |
| Get all players | `getOnlinePlayers()` |
| Add to inventory | `addToPlayerInventory(player, stack)` |

### ClientPlayerUtilities
| Task | Method |
|------|--------|
| Get tooltip | `getItemDisplayText(stack)` |
| Print message | `printToConsole(msg)` |

### JsonUtilities
| Task | Method |
|------|--------|
| Format JSON | `toPrettyString(json)` |
| Compact JSON | `toString(json)` |
| Parse JSON | `fromString(string)` |

### TimerMillis
| Task | Method |
|------|--------|
| Start timer | `start(durationMs)` |
| Check finished | `isFinished()` / `check()` |
| Get time info | `getElapsedTime()`, `getRemainingTime()` |
| Persistence | `save(tag)`, `load(tag)` |

### ModChecker
| Task | Method |
|------|--------|
| Check mod | `isModLoaded(modId)` |

---

## Common Use Cases

### 1. Custom Item Drops

```java
public void dropCustomLoot(ServerPlayer player, String itemId, int amount) {
    ItemStack stack = ItemUtilities.createItemStackFromId(itemId, amount);
    if (!stack.isEmpty()) {
        ItemUtilities.dropItemAtPlayer(player, stack);
    }
}
```

### 2. Colored GUI Elements

```java
public int getButtonColor(boolean isHovered, boolean isPressed) {
    int baseColor = 0xFF4080FF;
    
    if (isPressed) {
        return ColorUtilities.setBrightness(baseColor, 0.7f);
    } else if (isHovered) {
        return ColorUtilities.setBrightness(baseColor, 1.2f);
    }
    return baseColor;
}
```

### 3. Player Notification System

```java
public void notifyPlayer(UUID playerUUID, String message) {
    ServerPlayer player = ServerPlayerUtilities.getOnlinePlayer(playerUUID);
    if (player != null) {
        ServerPlayerUtilities.printToClientConsole(player, 
            "[Notification] " + message);
    }
}
```

### 4. Periodic Task with Timer

```java
public class PeriodicCleaner {
    private final TimerMillis cleanupTimer = new TimerMillis(true);
    
    public PeriodicCleaner() {
        cleanupTimer.start(60000); // Every minute
    }
    
    public void tick() {
        if (cleanupTimer.check()) {
            performCleanup();
        }
    }
}
```

### 5. Configuration Management

```java
public void saveSettings(JsonObject settings) {
    String json = JsonUtilities.toPrettyString(settings);
    // Write to file...
}

public JsonObject loadSettings(String jsonString) {
    JsonElement element = JsonUtilities.fromString(jsonString);
    return element.getAsJsonObject();
}
```

---

## Performance Tips

1. **ItemUtilities**: Cache `getAllItems()` results if calling frequently
2. **ColorUtilities**: Pre-calculate colors instead of computing per-frame
3. **ServerPlayerUtilities**: Batch messages when notifying multiple players
4. **TimerMillis**: Use auto-restart for periodic tasks instead of manual restarts
5. **JsonUtilities**: Use compact format for network traffic, pretty format for files

---

## See Also

- [Event System](Events.md) - For event-driven programming
- [Persistence System](Persistence.md) - For saving game state
- [GUI System](../gui/README.md) - For user interface utilities
- [Networking](../networking/README.md) - For client-server communication
