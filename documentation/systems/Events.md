# Event System

## Overview

The Event System provides a lightweight, type-safe event dispatching mechanism for decoupling components in your mod. It consists of two main classes:

- **Signal**: Simple event notifications without data
- **DataEvent<T>**: Events that carry a data payload of type T

Both classes support:
- Multiple subscribers
- Limited-call subscriptions (auto-unsubscribe after N calls)
- Manual subscription management
- Thread-safe listener invocation

**Location**: `net.kroia.modutilities.event`

---

## Signal Class

The `Signal` class provides basic event subscription and dispatching without any data payload.

### Key Features

- **Simple Notifications**: Fire events without passing data
- **Multiple Listeners**: Support for unlimited subscribers
- **Limited Calls**: Listeners can auto-unsubscribe after a specified number of invocations
- **Manual Management**: Add, remove, and configure listeners dynamically

### Basic Usage

```java
import net.kroia.modutilities.event.Signal;

// Create a signal
Signal onPlayerJoin = new Signal();

// Add a listener
Runnable listener = () -> {
    System.out.println("Player joined!");
};
onPlayerJoin.addListener(listener);

// Trigger the signal
onPlayerJoin.notifyListeners();
```

### Limited-Call Subscriptions

```java
// Listener will only be called 3 times, then auto-removed
onPlayerJoin.addListener(() -> {
    System.out.println("Welcome! (Limited greeting)");
}, 3);

// Fire multiple times
onPlayerJoin.notifyListeners(); // Prints
onPlayerJoin.notifyListeners(); // Prints
onPlayerJoin.notifyListeners(); // Prints
onPlayerJoin.notifyListeners(); // Does NOT print (listener removed)
```

### Managing Listeners

```java
// Remove a specific listener
boolean removed = onPlayerJoin.removeListener(listener);

// Clear all listeners
onPlayerJoin.removeListeners();

// Update remaining call count
onPlayerJoin.setListenerRemainingCallCount(listener, 5);

// Check remaining calls
int remaining = onPlayerJoin.getListenerRemainingCallCount(listener);
```

### API Reference

| Method | Description |
|--------|-------------|
| `addListener(Runnable listener)` | Add a listener with unlimited calls |
| `addListener(Runnable listener, int maxCalls)` | Add a listener with limited calls |
| `removeListener(Runnable listener)` | Remove a specific listener |
| `removeListeners()` | Clear all listeners |
| `notifyListeners()` | Trigger all listeners |
| `setListenerRemainingCallCount(Runnable, int)` | Update call limit |
| `getListenerRemainingCallCount(Runnable)` | Get remaining calls |

---

## DataEvent Class

The `DataEvent<T>` class provides event dispatching with a typed data payload.

### Key Features

- **Type-Safe Data**: Generic type parameter ensures type safety
- **All Signal Features**: Inherits all features from Signal (limited calls, management, etc.)
- **Data Passing**: Send data with each event notification

### Basic Usage

```java
import net.kroia.modutilities.event.DataEvent;
import net.minecraft.world.entity.player.Player;

// Create a data event
DataEvent<Player> onPlayerDamage = new DataEvent<>();

// Add a listener
onPlayerDamage.addListener(player -> {
    System.out.println(player.getName() + " took damage!");
});

// Trigger with data
onPlayerDamage.notifyListeners(player);
```

### Multiple Subscribers Example

```java
DataEvent<Integer> onScoreChange = new DataEvent<>();

// Add multiple listeners
onScoreChange.addListener(score -> {
    System.out.println("Score: " + score);
});

onScoreChange.addListener(score -> {
    if (score >= 100) {
        System.out.println("Achievement unlocked!");
    }
});

onScoreChange.addListener(score -> {
    // Save high score
    saveHighScore(score);
}, 1); // Only save once

// All listeners are called
onScoreChange.notifyListeners(150);
```

### Complex Data Types

```java
// Custom data class
public class BlockChangeData {
    public final BlockPos pos;
    public final BlockState oldState;
    public final BlockState newState;
    
    public BlockChangeData(BlockPos pos, BlockState oldState, BlockState newState) {
        this.pos = pos;
        this.oldState = oldState;
        this.newState = newState;
    }
}

// Create event
DataEvent<BlockChangeData> onBlockChange = new DataEvent<>();

// Subscribe
onBlockChange.addListener(data -> {
    System.out.println("Block at " + data.pos + " changed from " + 
                       data.oldState + " to " + data.newState);
});

// Trigger
onBlockChange.notifyListeners(new BlockChangeData(pos, old, newBlock));
```

### API Reference

| Method | Description |
|--------|-------------|
| `addListener(Consumer<T> listener)` | Add a listener with unlimited calls |
| `addListener(Consumer<T> listener, int maxCalls)` | Add a listener with limited calls |
| `removeListener(Consumer<T> listener)` | Remove a specific listener |
| `removeListeners()` | Clear all listeners |
| `notifyListeners(T value)` | Trigger all listeners with data |
| `setListenerRemainingCallCount(Consumer<T>, int)` | Update call limit |
| `getListenerRemainingCallCount(Consumer<T>)` | Get remaining calls |

---

## Code Examples

### Example 1: World Loading Event

```java
public class MyMod {
    public static final Signal ON_WORLD_LOAD = new Signal();
    
    public void onServerStarting() {
        ON_WORLD_LOAD.addListener(() -> {
            System.out.println("World loaded!");
            initializeWorldData();
        });
    }
}

// In another class
MyMod.ON_WORLD_LOAD.addListener(() -> {
    System.out.println("Starting background tasks...");
});
```

### Example 2: Item Collection Event

```java
public class InventoryTracker {
    public final DataEvent<ItemStack> onItemCollected = new DataEvent<>();
    
    public void collectItem(ItemStack stack) {
        // Game logic here...
        
        // Notify listeners
        onItemCollected.notifyListeners(stack);
    }
}

// Usage
tracker.onItemCollected.addListener(stack -> {
    if (stack.is(Items.DIAMOND)) {
        player.sendSystemMessage(Component.literal("You found a diamond!"));
    }
});
```

### Example 3: Temporary Event Subscription

```java
// Show tutorial message only first 3 times
gameEvents.onCrafting.addListener(item -> {
    showTutorialMessage("You crafted: " + item.getHoverName());
}, 3);

// One-time initialization
gameEvents.onFirstJoin.addListener(player -> {
    setupPlayerData(player);
}, 1);
```

### Example 4: Unsubscribing from Events

```java
public class QuestSystem {
    private Consumer<Player> questListener;
    
    public void startQuest() {
        // Create listener
        questListener = player -> {
            checkQuestCompletion(player);
        };
        
        // Subscribe
        gameEvents.onPlayerAction.addListener(questListener);
    }
    
    public void endQuest() {
        // Unsubscribe when quest completes
        gameEvents.onPlayerAction.removeListener(questListener);
        questListener = null;
    }
}
```

### Example 5: Event Chain

```java
public class EventChain {
    public final Signal onPhase1Complete = new Signal();
    public final Signal onPhase2Complete = new Signal();
    public final Signal onAllComplete = new Signal();
    
    public void setup() {
        onPhase1Complete.addListener(() -> {
            System.out.println("Phase 1 done, starting Phase 2");
            startPhase2();
        }, 1);
        
        onPhase2Complete.addListener(() -> {
            System.out.println("Phase 2 done, all complete!");
            onAllComplete.notifyListeners();
        }, 1);
    }
}
```

---

## Use Cases

### When to Use Events

Events are ideal when:

1. **Decoupling Components**: Multiple systems need to react to the same action
2. **Optional Listeners**: Not all code paths require the notification
3. **Dynamic Subscriptions**: Listeners are added/removed at runtime
4. **Unknown Subscriber Count**: You don't know how many components will listen
5. **Asynchronous Notifications**: Triggering actions without blocking
6. **Plugin Architecture**: Allowing other mods to hook into your events

### When to Use Direct Calls

Direct method calls are better when:

1. **Single Recipient**: Only one component needs to handle the action
2. **Return Values Required**: You need feedback from the handler
3. **Error Handling Critical**: You must handle exceptions immediately
4. **Performance Critical**: Avoiding overhead of event dispatch
5. **Strong Coupling Acceptable**: Components are tightly integrated

### Event vs Callback Comparison

| Aspect | Event System | Direct Callback |
|--------|-------------|-----------------|
| Multiple handlers | Yes | Requires collection |
| Dynamic subscription | Easy | Manual management |
| Decoupling | High | Low |
| Return values | No | Yes |
| Exception handling | Isolated | Direct |
| Performance | Slight overhead | Fastest |

---

## Best Practices

### 1. Memory Leak Prevention

```java
// BAD: Storing reference to listener that won't be cleaned up
public class MyClass {
    public MyClass() {
        globalEvent.addListener(() -> {
            // 'this' is captured, preventing garbage collection
            this.doSomething();
        });
    }
}

// GOOD: Store listener reference and remove it
public class MyClass {
    private final Runnable listener;
    
    public MyClass() {
        listener = this::doSomething;
        globalEvent.addListener(listener);
    }
    
    public void cleanup() {
        globalEvent.removeListener(listener);
    }
}
```

### 2. Thread Safety Considerations

```java
// The event system is NOT inherently thread-safe
// If notifyListeners() may be called from multiple threads:

public class ThreadSafeEvent<T> {
    private final DataEvent<T> event = new DataEvent<>();
    private final Object lock = new Object();
    
    public void addListener(Consumer<T> listener) {
        synchronized (lock) {
            event.addListener(listener);
        }
    }
    
    public void notifyListeners(T data) {
        synchronized (lock) {
            event.notifyListeners(data);
        }
    }
}
```

### 3. Listener Execution Order

```java
// Listeners are executed in the order they were added
// If order matters, document it clearly

public class OrderedEvents {
    public final Signal onTick = new Signal();
    
    public void setup() {
        // Phase 1: Update game state
        onTick.addListener(this::updateGameState);
        
        // Phase 2: Update UI (depends on game state)
        onTick.addListener(this::updateUI);
        
        // Phase 3: Network sync (depends on both)
        onTick.addListener(this::syncNetwork);
    }
}
```

### 4. Exception Handling

```java
// Wrap listener calls to prevent one failing listener from stopping others
public void safeNotifyListeners() {
    for (Runnable listener : listeners) {
        try {
            listener.run();
        } catch (Exception e) {
            ModUtilitiesMod.LOGGER.error("Event listener failed", e);
        }
    }
}
```

### 5. Event Naming Conventions

```java
// Use clear, action-based names with "on" prefix
public static final Signal onServerStarted = new Signal();
public static final Signal onWorldSaved = new Signal();
public static final DataEvent<Player> onPlayerJoin = new DataEvent<>();
public static final DataEvent<BlockPos> onBlockDestroyed = new DataEvent<>();

// Group related events in classes
public class PlayerEvents {
    public static final DataEvent<Player> onJoin = new DataEvent<>();
    public static final DataEvent<Player> onLeave = new DataEvent<>();
    public static final DataEvent<Player> onDeath = new DataEvent<>();
}
```

### 6. Documentation

```java
/**
 * Fired when a player completes a quest.
 * 
 * <p>Listeners receive the quest ID and should handle
 * rewards, achievements, or logging.
 * 
 * <p>Called on: Server thread
 * <p>Timing: After quest validation, before reward distribution
 */
public static final DataEvent<String> onQuestComplete = new DataEvent<>();
```

### 7. Limited Subscriptions for Initialization

```java
// Use maxCalls=1 for one-time setup
public void initialize() {
    gameEvents.onWorldLoad.addListener(() -> {
        loadConfiguration();
        registerCommands();
        initializeDatabase();
    }, 1); // Only run once
}
```

---

## Common Patterns

### Observer Pattern

```java
public class HealthMonitor {
    private final DataEvent<Integer> onHealthChanged = new DataEvent<>();
    private int health;
    
    public void setHealth(int newHealth) {
        int oldHealth = this.health;
        this.health = newHealth;
        
        if (oldHealth != newHealth) {
            onHealthChanged.notifyListeners(newHealth);
        }
    }
    
    public DataEvent<Integer> getHealthChangedEvent() {
        return onHealthChanged;
    }
}
```

### Event Aggregation

```java
public class EventAggregator {
    public static final DataEvent<Object> onAnyEvent = new DataEvent<>();
    public static final DataEvent<Player> onPlayerEvent = new DataEvent<>();
    public static final DataEvent<Block> onBlockEvent = new DataEvent<>();
    
    static {
        // Forward specific events to general event
        onPlayerEvent.addListener(player -> onAnyEvent.notifyListeners(player));
        onBlockEvent.addListener(block -> onAnyEvent.notifyListeners(block));
    }
}
```

### State Machine Events

```java
public enum GameState { WAITING, RUNNING, FINISHED }

public class GameStateMachine {
    private GameState state = GameState.WAITING;
    public final DataEvent<GameState> onStateChange = new DataEvent<>();
    
    public void setState(GameState newState) {
        if (this.state != newState) {
            this.state = newState;
            onStateChange.notifyListeners(newState);
        }
    }
}

// Usage
stateMachine.onStateChange.addListener(state -> {
    switch (state) {
        case WAITING -> prepareGame();
        case RUNNING -> startGame();
        case FINISHED -> cleanupGame();
    }
});
```

---

## Performance Notes

1. **Listener Iteration**: O(n) where n is the number of listeners
2. **Limited Calls**: Adds minimal overhead for call count tracking
3. **Removal**: O(n) search to find listener to remove
4. **Memory**: Each listener stores a Pair<Listener, Integer>

For high-frequency events (e.g., every tick), consider:
- Minimizing listener count
- Using direct method calls instead
- Batching events when possible

---

## See Also

- [Persistence System](../systems/Persistence.md) - For saving event-driven state
- [Networking](../networking/README.md) - For network event handling
- [Sandbox System](../development/Sandbox.md) - For testing events in isolation
