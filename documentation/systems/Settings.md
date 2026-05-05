# Settings System

## Overview

The Settings system provides a flexible, type-safe framework for managing mod configuration with JSON serialization support. It enables developers to create hierarchical settings structures with automatic save/load functionality, change listeners, and custom serialization for complex types like ItemStacks and NBT data.

**Key Features:**
- Type-safe settings with generic support
- Hierarchical organization via SettingsGroups
- Automatic JSON serialization/deserialization
- Change listeners for reactive programming
- Custom parsers for complex Minecraft types
- Default value management

---

## Core Concepts

### Setting&lt;T&gt;

A generic container that holds a value of type T with change tracking and listener support.

**Key Properties:**
- Stores current value and default value
- Type information for serialization
- Optional custom JSON parser
- Change event system

### SettingsGroup

A named collection of related settings that can be registered and managed together.

**Key Properties:**
- Named group for organization
- Settings registry
- Batch operations (reset all, iterate all)
- JSON serialization support

### SettingsStore

Handles JSON serialization and deserialization for settings groups.

**Key Features:**
- Pretty-printed or compact JSON output
- File-based persistence
- Type-safe deserialization
- Custom parser integration

### ModSettings

Top-level manager for all settings groups in a mod.

**Key Features:**
- Manages multiple SettingsGroups
- Centralized save/load operations
- Logging integration
- Error handling

---

## Basic Usage

### Creating a Simple Setting

```java
import net.kroia.modutilities.setting.Setting;

// Create a setting with name, default value, and type
Setting<Integer> maxItems = new Setting<>("maxItems", 64, Integer.class);

// Get the current value
int current = maxItems.get();

// Set a new value
maxItems.set(128);

// Reset to default
maxItems.setToDefaultValue();
```

### Creating a Settings Group

```java
import net.kroia.modutilities.setting.SettingsGroup;
import net.kroia.modutilities.setting.Setting;

public class GameplaySettings extends SettingsGroup {
    // Register settings in the constructor or as fields
    public final Setting<Integer> maxPlayers = 
        registerSetting("maxPlayers", 10, Integer.class);
    
    public final Setting<Boolean> enablePvP = 
        registerSetting("enablePvP", false, Boolean.class);
    
    public final Setting<Double> difficultyMultiplier = 
        registerSetting("difficultyMultiplier", 1.0, Double.class);
    
    public GameplaySettings() {
        super("GameplaySettings");
    }
}
```

### Using ModSettings

```java
import net.kroia.modutilities.setting.ModSettings;

public class MyModSettings extends ModSettings {
    // Create groups
    public final GameplaySettings gameplay = createGroup(new GameplaySettings());
    public final GraphicsSettings graphics = createGroup(new GraphicsSettings());
    
    public MyModSettings() {
        super("MyMod");
    }
}

// Usage
MyModSettings settings = new MyModSettings();

// Save to JSON file
settings.saveSettings("config/mymod.json");

// Load from JSON file
settings.loadSettings("config/mymod.json");

// Access settings
int maxPlayers = settings.gameplay.maxPlayers.get();
settings.gameplay.maxPlayers.set(20);
```

---

## Advanced Features

### Change Listeners

Listen for value changes to trigger reactive behavior:

```java
Setting<Integer> healthSetting = new Setting<>("health", 100, Integer.class);

// Add a listener
healthSetting.addListener(newValue -> {
    System.out.println("Health changed to: " + newValue);
    updatePlayerHealth(newValue);
});

// Listener is automatically notified when value changes
healthSetting.set(150); // Prints: "Health changed to: 150"
```

### Custom JSON Parsers

For complex types that need special serialization:

#### Using ItemStack Parser

```java
import net.kroia.modutilities.setting.parser.ItemStackJsonParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemSettings extends SettingsGroup {
    public final Setting<ItemStack> rewardItem = 
        registerSetting("rewardItem", 
                       new ItemStack(Items.DIAMOND, 5),
                       ItemStack.class,
                       new ItemStackJsonParser());
    
    public ItemSettings() {
        super("ItemSettings");
    }
}

// The ItemStack is automatically serialized to JSON:
// {
//   "id": "minecraft:diamond",
//   "count": 5,
//   "components": { ... }
// }
```

#### Using NBT Parser

```java
import net.kroia.modutilities.setting.parser.NBTJsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class DataSettings extends SettingsGroup {
    public final Setting<Tag> customData = 
        registerSetting("customData",
                       new CompoundTag(),
                       Tag.class,
                       new NBTJsonParser());
    
    public DataSettings() {
        super("DataSettings");
    }
}

// NBT data is converted to readable JSON format
```

#### Creating a Custom Parser

```java
import net.kroia.modutilities.setting.parser.CustomJsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ColorParser implements CustomJsonParser<Color> {
    @Override
    public JsonElement toJson(Color color) {
        JsonObject json = new JsonObject();
        json.addProperty("r", color.getRed());
        json.addProperty("g", color.getGreen());
        json.addProperty("b", color.getBlue());
        json.addProperty("a", color.getAlpha());
        return json;
    }
    
    @Override
    public Color fromJson(JsonElement json) {
        JsonObject obj = json.getAsJsonObject();
        return new Color(
            obj.get("r").getAsInt(),
            obj.get("g").getAsInt(),
            obj.get("b").getAsInt(),
            obj.get("a").getAsInt()
        );
    }
}

// Usage
Setting<Color> accentColor = new Setting<>("accentColor",
                                           new Color(255, 0, 0, 255),
                                           Color.class,
                                           new ColorParser());
```

### Nested Settings

Organize complex configurations hierarchically:

```java
public class ServerSettings extends SettingsGroup {
    public final Setting<Integer> port = registerSetting("port", 25565, Integer.class);
    public final Setting<String> motd = registerSetting("motd", "A Minecraft Server", String.class);
    
    public ServerSettings() {
        super("ServerSettings");
    }
}

public class WorldSettings extends SettingsGroup {
    public final Setting<String> seed = registerSetting("seed", "", String.class);
    public final Setting<Boolean> generateStructures = registerSetting("generateStructures", true, Boolean.class);
    
    public WorldSettings() {
        super("WorldSettings");
    }
}

public class CompleteConfig extends ModSettings {
    public final ServerSettings server = createGroup(new ServerSettings());
    public final WorldSettings world = createGroup(new WorldSettings());
    
    public CompleteConfig() {
        super("CompleteConfig");
    }
}

// Saves as hierarchical JSON:
// {
//   "ServerSettings": {
//     "port": 25565,
//     "motd": "A Minecraft Server"
//   },
//   "WorldSettings": {
//     "seed": "",
//     "generateStructures": true
//   }
// }
```

### Custom Logging

```java
MyModSettings settings = new MyModSettings();

// Set custom loggers
settings.setLogger(
    error -> ModLogger.error(error),           // Error logger
    (error, throwable) -> ModLogger.error(error, throwable), // Error with throwable
    debug -> ModLogger.debug(debug)            // Debug logger
);
```

---

## API Reference

### Setting&lt;T&gt;

| Method | Description |
|--------|-------------|
| `T get()` | Get current value |
| `void set(T newValue)` | Set new value (notifies listeners if changed) |
| `String getName()` | Get setting name |
| `Type getType()` | Get setting type |
| `T getDefaultValue()` | Get default value |
| `void setToDefaultValue()` | Reset to default value |
| `void addListener(Consumer<T> listener)` | Add change listener |
| `void removeListener(Consumer<T> listener)` | Remove change listener |
| `CustomJsonParser<T> getCustomJsonParser()` | Get custom parser (if any) |

### SettingsGroup

| Method | Description |
|--------|-------------|
| `String getName()` | Get group name |
| `Setting<?> getSetting(String name)` | Get setting by name |
| `List<Setting<?>> getAllSettings()` | Get all settings |
| `void forEachSetting(Consumer<Setting<?>> action)` | Iterate all settings |
| `void setToDefaultValue()` | Reset all settings to defaults |
| `String toString()` | Get JSON representation |

### SettingsStore

| Method | Description |
|--------|-------------|
| `void saveToFile(List<SettingsGroup> groups, String filePath)` | Save groups to JSON file |
| `void loadFromFile(List<SettingsGroup> groups, String filePath)` | Load groups from JSON file |
| `String toJsonString(List<SettingsGroup> groups)` | Convert groups to JSON string |
| `JsonElement toJson(SettingsGroup group)` | Convert group to JsonElement |
| `SettingsGroup fromJson(SettingsGroup loader, JsonElement json)` | Load group from JsonElement |

### ModSettings

| Method | Description |
|--------|-------------|
| `<T extends SettingsGroup> T createGroup(T group)` | Register a settings group |
| `boolean saveSettings(String filePath)` | Save all groups to file |
| `boolean loadSettings(String filePath)` | Load all groups from file |
| `void setLogger(...)` | Set custom loggers |

---

## Sandbox Example

Here's a complete example demonstrating the settings system:

```java
package net.kroia.modutilities.sandbox.settings;

import net.kroia.modutilities.setting.*;
import net.kroia.modutilities.setting.parser.ItemStackJsonParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Example settings for a custom game mode
 */
public class SandboxGameSettings extends SettingsGroup {
    // Basic settings
    public final Setting<Integer> roundDuration = 
        registerSetting("roundDuration", 300, Integer.class);
    
    public final Setting<Integer> teamSize = 
        registerSetting("teamSize", 4, Integer.class);
    
    public final Setting<Boolean> friendlyFire = 
        registerSetting("friendlyFire", false, Boolean.class);
    
    public final Setting<Double> experienceMultiplier = 
        registerSetting("experienceMultiplier", 1.5, Double.class);
    
    // Complex type with custom parser
    public final Setting<ItemStack> startingWeapon = 
        registerSetting("startingWeapon",
                       new ItemStack(Items.WOODEN_SWORD),
                       ItemStack.class,
                       new ItemStackJsonParser());
    
    public SandboxGameSettings() {
        super("GameMode");
        
        // Add listeners for reactive behavior
        roundDuration.addListener(duration -> {
            System.out.println("Round duration changed to: " + duration + " seconds");
        });
        
        teamSize.addListener(size -> {
            if (size < 1 || size > 10) {
                System.err.println("Warning: Team size should be between 1 and 10");
            }
        });
    }
}

/**
 * Main settings manager for the sandbox mod
 */
public class SandboxModSettings extends ModSettings {
    public final SandboxGameSettings gameMode = createGroup(new SandboxGameSettings());
    
    public SandboxModSettings() {
        super("SandboxMod");
        
        // Set up logging
        setLogger(
            System.err::println,
            System.out::println
        );
    }
    
    /**
     * Example usage method
     */
    public static void main(String[] args) {
        SandboxModSettings settings = new SandboxModSettings();
        
        // Configure settings
        settings.gameMode.roundDuration.set(600);
        settings.gameMode.teamSize.set(5);
        settings.gameMode.friendlyFire.set(true);
        settings.gameMode.startingWeapon.set(new ItemStack(Items.IRON_SWORD));
        
        // Save to file
        if (settings.saveSettings("config/sandbox.json")) {
            System.out.println("Settings saved successfully!");
        }
        
        // Later, load from file
        SandboxModSettings loadedSettings = new SandboxModSettings();
        if (loadedSettings.loadSettings("config/sandbox.json")) {
            System.out.println("Settings loaded successfully!");
            System.out.println("Round duration: " + loadedSettings.gameMode.roundDuration.get());
        }
        
        // Reset to defaults
        loadedSettings.gameMode.setToDefaultValue();
        System.out.println("Settings reset to defaults");
    }
}
```

### Expected JSON Output

```json
{
  "GameMode": {
    "roundDuration": 600,
    "teamSize": 5,
    "friendlyFire": true,
    "experienceMultiplier": 1.5,
    "startingWeapon": {
      "id": "minecraft:iron_sword",
      "count": 1
    }
  }
}
```

---

## Best Practices

1. **Use Meaningful Names**: Setting names should be clear and descriptive
2. **Set Reasonable Defaults**: Choose default values that work well out-of-the-box
3. **Add Validation**: Use listeners to validate values when they change
4. **Group Related Settings**: Organize settings into logical groups
5. **Document Your Settings**: Add comments explaining what each setting does
6. **Handle Load Failures**: Always check return values from load operations
7. **Use Custom Parsers**: For complex types, implement proper serialization
8. **Version Your Config**: Consider adding a version field for migration support

---

## Common Patterns

### Singleton Pattern

```java
public class ConfigManager {
    private static ConfigManager instance;
    private final MyModSettings settings;
    
    private ConfigManager() {
        settings = new MyModSettings();
        settings.loadSettings("config/mymod.json");
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    public MyModSettings getSettings() {
        return settings;
    }
    
    public void save() {
        settings.saveSettings("config/mymod.json");
    }
}
```

### Auto-Save on Change

```java
public class AutoSaveSettings extends ModSettings {
    public AutoSaveSettings() {
        super("AutoSave");
        
        // Add auto-save listener to all settings
        for (SettingsGroup group : allGroups) {
            group.forEachSetting(setting -> {
                setting.addListener(value -> saveSettings("config/autosave.json"));
            });
        }
    }
}
```

---

## Related Systems

- **Persistence System**: For NBT-based data storage
- **Event System**: Settings use DataEvent for change notifications
- **Networking**: Sync settings between client and server using the networking system
