# Sandbox System

## Overview

The Sandbox system is a testing and development framework built into MC_ModUtilities. Since this mod is a library that provides no gameplay content on its own, the Sandbox serves as a testbed for demonstrating and verifying library features during development.

## Dev Builds and Release Builds

The Sandbox is development-only. It is never disabled by editing Java code; it is **not compiled** into release builds.

The single switch is the Gradle property `enable_dev_features` in `gradle.properties` (default `false`):

```properties
# Dev-only code (sandbox, unit tests, autotest). false = release: those packages are not compiled.
enable_dev_features = false
```

- **Release build** (`enable_dev_features=false`): the root `build.gradle` excludes `net/kroia/modutilities/sandbox/**` and `net/kroia/modutilities/testing/**` from compilation. The jar contains no sandbox or test code, and any release class that still references it is a compile error.
- **Dev build**: set the property to `true`, or pass it on the command line:

  ```
  ./gradlew build -Penable_dev_features=true
  ```

At runtime `ModUtilitiesMod.ENABLE_DEV_FEATURES` is `true` exactly when the dev classes were compiled in. `ModUtilitiesMod.init()` then loads `net.kroia.modutilities.testing.DevBootstrap` via reflection, whose `init()` calls `Sandbox.init()` and `AutoTest.init()`. Do not call `Sandbox.init()` directly from release code: it would no longer compile with `enable_dev_features=false`.

Sandbox assets (blockstates, models, lang entries under `assets/modutilities`) are data, not code, and are not excluded.

## Location

All Sandbox-related code is located in:
```
common/src/main/java/net/kroia/modutilities/sandbox/
```

The Sandbox also registers the ModUtilities [test suites](../testing/TestSuite.md) and the `/modutilities test` command. To run all suites automatically on every loader and server configuration, use the [in-game autotest harness](../../tools/autotest/README.md).

## Available Test Components

The Sandbox provides several test implementations demonstrating various library features:

### 1. Networking System (`SandboxNetwork`)

A complete network packet manager demonstrating:
- Client-to-server and server-to-client packet registration
- Asynchronous Request Response System (ARRS)
- Streaming system setup
- Custom packet handlers

**Example packets:**
- `SandboxOpenGuiPacket`: Opens test GUIs on the client
- `SimpleDataPacketToClient`: Demonstrates simple data transfer

### 2. Streaming System (`SineStream`)

A demonstration of the continuous data streaming system:
- Extends `GenericStream<Float, Double>`
- Sends sine wave data from server to client
- Shows context data encoding/decoding
- Demonstrates stream lifecycle (start, update, stop)

**Usage:**
```java
UUID streamID = StreamSystem.startServerToClientStream(
    Sandbox.SandboxNetwork.SINUS_STREAM, 
    0.0f,  // Context data
    (value) -> {
        // Callback for receiving stream data
        plotData.yValues.add((float)value.doubleValue());
    },
    () -> {
        // Callback when stream stops
        ModUtilitiesMod.LOGGER.info("Stream stopped");
    }
);
```

### 3. Test GUI (`TestScreen`)

A comprehensive GUI demonstration screen showing:
- TextBox elements with regex validation
- ListView with LayoutGrid
- ItemSelectionView with sorting
- Tab elements (commented examples)
- Plot elements for data visualization
- Custom element creation

**Open via command:**
```
/modutilities openTestScreen
```

### 4. Commands (`SandboxCommand`)

A set of development commands registered under `/modutilities`:

| Command | Description |
|---------|-------------|
| `/modutilities openTestScreen` | Opens the test GUI screen |
| `/modutilities saveItemInHand` | Saves held item to JSON file |
| `/modutilities loadItemToHand` | Loads item from JSON file to hand |

All commands require OP permission level 2.

### 5. Block and Block Entity (Commented)

Example implementations for testing block-related features:
- `MyBlock`: Custom block implementation
- `MyBlockEntity`: Block entity with container support
- `MyContainerMenu`: Container menu integration
- Creative tab registration

These are currently commented out but available for testing block-based features.

## Adding Your Own Test Implementations

To add custom test implementations to the Sandbox:

### 1. Create Test Files

Add your test classes to the `sandbox` package:
```
common/src/main/java/net/kroia/modutilities/sandbox/YourTestClass.java
```

### 2. Register Network Packets (if needed)

Add packet registration in `SandboxNetwork`:

```java
@Override
public void setupClientReceiverPackets() {
    registerS2C(YourPacket.TYPE, YourPacket.STREAM_CODEC, YourPacket.HANDLER);
}

@Override
public void setupServerReceiverPackets() {
    registerC2S(YourPacket.TYPE, YourPacket.STREAM_CODEC, YourPacket.HANDLER);
}
```

### 3. Add Commands (if needed)

Extend the `SandboxCommand.register()` method:

```java
dispatcher.register(
    Commands.literal("modutilities")
        .then(Commands.literal("yourCommand")
            .executes(context -> {
                // Your command logic
                return 1;
            }))
);
```

### 4. Initialize in Sandbox.init()

If your test requires initialization, add it to `Sandbox.init()` (called by `DevBootstrap` in dev builds only):

```java
public static void init()
{
    CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
        SandboxCommand.register(dispatcher);
    });
    network = new SandboxNetwork();
    
    // Add your initialization here
    YourTestClass.init();
}
```

## Best Practices

1. **Keep Sandbox Self-Contained**: All test code should remain in the `sandbox` package
2. **Document New Tests**: Add comments explaining what each test demonstrates
3. **Use Logging**: Log important events to help debug issues
4. **Clean Exit**: Ensure resources are properly cleaned up (close streams, save data)
5. **Release builds use `enable_dev_features=false`**: Never ship a jar built with dev features; release code must not reference `sandbox` classes

## See Also

- [Networking Library](../networking/Networking.md)
- [GUI Library](../gui/GuiLibrary.md)
- [Streaming System](../networking/StreamSystem.md)
- [Test Suite](../testing/TestSuite.md)
- [Automated in-game tests](../../tools/autotest/README.md)
