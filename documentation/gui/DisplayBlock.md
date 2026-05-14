# Display Block System

## Overview

Display blocks are in-world Minecraft blocks that render GUI elements directly on their front face. They allow mods to create functional, interactive displays placed in the game world -- dashboards, status panels, control interfaces, and more -- without requiring players to open a separate screen.

**Key features:**

- **In-world GUI rendering**: GUI elements (labels, plots, sliders, buttons, text inputs) are rendered as a texture on the block face using an offscreen framebuffer.
- **Multi-block grouping**: Adjacent display blocks of the same type and facing automatically merge into a single, larger display. The top-left block becomes the controller.
- **Server-authoritative interaction**: The server owns the GUI state. Players interact by right-clicking; the server processes mouse events and syncs results to all clients.
- **Interaction screen**: Right-clicking opens a full-screen overlay that mirrors the display's content, enabling precise input (sliders, text fields, checkboxes) via `GuiStateSync`.
- **Channel-based merge prevention**: Different display block types can coexist without merging by using distinct channel IDs.
- **Three built-in shape variants**: Full block (solid cube with inset face), flat panel (thin glass-pane-like shape centered in the block), and back panel (thin panel flush against the back face).
- **Built-in textures and block models**: ModUtilities ships reusable block models (`display_full_block`, `display_flat_panel`, `display_back_panel`) with dark tech panel textures. Dependent mods can parent their models directly to these without providing custom textures.
- **Configurable interaction mode**: Open the synced interaction screen on right-click (default), or display-only with custom logic by overriding `opensSyncedScreenOnUse()`.
- **Runtime GUI swapping**: Call `rebuildGui()` to clear and rebuild the display content at runtime, enabling dynamic views that change based on block entity state.

---

## Architecture

### Class Hierarchy

```
HorizontalDirectionalBlock (Minecraft)
  +-- AbstractDisplayBlock              [common] Block class
        +-- YourCustomBlock

BlockEntity (Minecraft)
  +-- AbstractDisplayBlockEntity        [common] Block entity class
        +-- YourCustomBlockEntity

BlockEntityRenderer (Minecraft)
  +-- AbstractDisplayBlockEntityRenderer [client] Renderer (use directly, no subclass needed)

GuiScreen (MC_ModUtilities)
  +-- DisplayInteractionScreen          [client] Interaction overlay screen

ContentBuilder                          [common] Functional interface for GUI layout
DisplayConfig                           [common] Record holding display parameters
ShapeProvider                           [common] Functional interface for voxel shapes
GuiStateSync                            [common] Bidirectional state sync between Gui instances
GuiInputSerializer                      [common] Serializes GUI input state to/from CompoundTag
DisplayInputSyncPacket                  [common] C2S packet for input sync (dedicated server support)
DisplayNetworking                       [common] Packet registration (called at mod init)
DisplayClientHooks                      [client] Client-only hook entry points
DisplayRenderProfiler                   [client] Toggleable per-group render timing profiler
GuiElementRegistry                      [common] Element type factory registry (16 built-in types)
GuiStructuralChange                     [common] Structural change event record (ADDED/REMOVED)
```

### Server/Client Split

The display block system uses a server-authoritative model:

1. **Server side**: The `AbstractDisplayBlockEntity` owns a `Gui` instance. The `ContentBuilder` creates the visual layout, and `wireCallbacks()` attaches server-side behavior (button handlers, slider listeners, etc.). The server processes all mouse events (click, drag, release) and updates the GUI state. State is synced to clients via block entity NBT update packets.

2. **Client renderer**: `AbstractDisplayBlockEntityRenderer` reads the client-side copy of the `Gui` (rebuilt from synced NBT), renders it into an offscreen framebuffer, and projects the resulting texture onto the block face as a quad. Each display group is rendered once per frame, regardless of how many blocks are in the group. The renderer uses `glCopyTexSubImage2D` for GPU-to-GPU texture copy (no CPU round-trip), dirty-flag rendering that skips unchanged displays, and distance-based LOD and render interval throttling for distant or low-priority displays.

3. **Interaction screen**: When a player right-clicks, `DisplayInteractionScreen` opens. It uses the same `ContentBuilder` to create a local GUI copy, then synchronizes in two directions:
   - **Display state** (labels, plots) is read from the client-side block entity's Gui (kept up-to-date by the NBT sync above) via `GuiStateSync.syncDisplayState()`.
   - **Input state** (sliders, text boxes, checkboxes, button clicks) is serialized into a `CompoundTag` by `GuiInputSerializer` and sent to the server via `DisplayInputSyncPacket` (a C2S network packet). This works on both singleplayer and dedicated servers.

### Multi-Block Grouping

When a display block is placed or removed, `recalculateGroups()` performs a flood-fill across adjacent blocks that share the same facing direction and channel ID. The system finds the largest axis-aligned rectangle within the connected set. The **top-left** block (highest Y, then leftmost along the facing's right direction) becomes the **controller**. Only the controller holds a `Gui` instance; all other members in the group store a reference to the controller's position.

---

## Quick Start -- Creating a Custom Display Block

This guide walks through creating a custom display block from scratch.

### Step 1: Create the Block Entity

Extend `AbstractDisplayBlockEntity` and implement the required abstract methods and optional hooks.

```java
package com.example.mymod;

import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.display.AbstractDisplayBlockEntity;
import net.kroia.modutilities.gui.display.ContentBuilder;
import net.kroia.modutilities.gui.display.DisplayConfig;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class MyDisplayBlockEntity extends AbstractDisplayBlockEntity {

    // Custom state
    private int clickCount = 0;

    // Server-side GUI element references
    private Label countLabel;

    public MyDisplayBlockEntity(BlockPos pos, BlockState blockState) {
        super(MyModRegistration.MY_DISPLAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    // --- Required: Display configuration ---

    @Override
    public DisplayConfig getDisplayConfig() {
        return DisplayConfig.fullBlock();
    }

    // --- Required: Content builder ---
    // Returns a functional interface that builds the GUI layout.
    // This same builder is used both server-side and by the interaction screen.
    // It must NOT attach server callbacks -- use wireCallbacks() for that.

    @Override
    public ContentBuilder getContentBuilder() {
        return MyDisplayBlockEntity::buildContent;
    }

    // --- Optional: Wire server-side callbacks ---
    // Called after the GUI is built on the server. Use this to find elements
    // by type/content and attach callbacks (button press handlers, slider
    // listeners, etc.).

    @Override
    protected void wireCallbacks(Gui gui) {
        countLabel = null;

        for (var el : gui.getElements()) {
            if (el instanceof Label l && l.getText() != null
                    && l.getText().startsWith("Count:")) {
                countLabel = l;
            }
            if (el instanceof Button btn && "Increment".equals(btn.getText())) {
                btn.setOnFallingEdge(() -> {
                    clickCount++;
                    updateCountLabel();
                    syncToClientPublic();
                });
            }
        }

        updateCountLabel();
    }

    // --- Optional: Persistence ---

    @Override
    protected void saveCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("clickCount", clickCount);
    }

    @Override
    protected void loadCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        clickCount = tag.getInt("clickCount");
        updateCountLabel();
    }

    // --- Optional: Interaction screen sync ---
    // Called on the server after the interaction screen syncs input state
    // into this entity's GUI. Read GUI element values back into BE fields.

    @Override
    public void onInputSynced() {
        // Read any input element values back into fields if needed.
        // For this example, button clicks are handled via wireCallbacks.
    }

    // --- Optional: Controller tick ---
    // Called every server tick on the controller entity only.
    // Use for animations, periodic updates, etc.

    @Override
    protected void onControllerTick() {
        // No periodic updates needed for this example.
    }

    // --- Optional: Channel ID ---
    // Override to return a unique channel so this block type won't merge
    // with other display block types. Defaults to "default".

    // @Override
    // public String getChannelId() { return "my_channel"; }

    // --- Content builder (static) ---

    private static void buildContent(Gui gui, int w, int h) {
        int margin = 10;

        Label title = new Label("My Display");
        title.setBounds(0, margin, w, 16);
        title.setAlignment(GuiElement.Alignment.CENTER);
        gui.addElement(title);

        Label countLabel = new Label("Count: 0");
        countLabel.setBounds(margin, margin + 24, w - margin * 2, 14);
        countLabel.setAlignment(GuiElement.Alignment.LEFT);
        gui.addElement(countLabel);

        Button button = new Button("Increment");
        button.setBounds(margin, margin + 46, w - margin * 2, 18);
        gui.addElement(button);
    }

    // --- Helper ---

    private void updateCountLabel() {
        if (countLabel != null) {
            countLabel.setText("Count: " + clickCount);
        }
    }
}
```

### Step 2: Create the Block

Extend `AbstractDisplayBlock`. The base class handles facing, voxel shape, placement, removal, interaction, group recalculation, and ticking. You only need to provide the codec and `newBlockEntity`.

```java
package com.example.mymod;

import com.mojang.serialization.MapCodec;
import net.kroia.modutilities.gui.display.AbstractDisplayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MyDisplayBlock extends AbstractDisplayBlock {

    public static final MapCodec<MyDisplayBlock> CODEC =
            simpleCodec(p -> new MyDisplayBlock());

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public MyDisplayBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MyDisplayBlockEntity(pos, state);
    }
}
```

### Step 3: Register

Use Architectury's `DeferredRegister` pattern to register the block, block item, block entity type, and client-side renderer.

```java
package com.example.mymod;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.display.client.AbstractDisplayBlockEntityRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class MyModRegistration {

    public static final String MOD_ID = "mymod";

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(MOD_ID, Registries.BLOCK);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MOD_ID, Registries.ITEM);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    // Block
    public static final RegistrySupplier<Block> MY_DISPLAY_BLOCK =
            BLOCKS.register("my_display_block", MyDisplayBlock::new);

    // Block item
    public static final RegistrySupplier<Item> MY_DISPLAY_BLOCK_ITEM =
            ITEMS.register("my_display_block", () ->
                    new BlockItem(MY_DISPLAY_BLOCK.get(), new Item.Properties()));

    // Block entity type
    @SuppressWarnings("unchecked")
    public static final RegistrySupplier<BlockEntityType<MyDisplayBlockEntity>>
            MY_DISPLAY_BLOCK_ENTITY =
            (RegistrySupplier<BlockEntityType<MyDisplayBlockEntity>>) (RegistrySupplier<?>)
            BLOCK_ENTITIES.register("my_display_block_entity", () ->
                    BlockEntityType.Builder.of(
                            MyDisplayBlockEntity::new,
                            MY_DISPLAY_BLOCK.get()
                    ).build(null));

    /** Call during mod initialization (before registry freeze). */
    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
    }

    /** Call from client-only initialization code. */
    @Environment(EnvType.CLIENT)
    public static void registerClient() {
        MY_DISPLAY_BLOCK_ENTITY.listen(blockEntityType ->
                BlockEntityRendererRegistry.register(blockEntityType,
                        AbstractDisplayBlockEntityRenderer::new));
    }
}
```

Note: `AbstractDisplayBlockEntityRenderer` is used directly. There is no need to subclass it -- just register it for your block entity type.

### Step 4: Add Resource Files

You need standard Minecraft resource files for the block to appear in-game. The display is rendered as a dynamic texture overlay; the block model itself can use any base texture.

**Blockstate JSON** (`assets/mymod/blockstates/my_display_block.json`):

```json
{
  "variants": {
    "facing=north": { "model": "mymod:block/my_display_block" },
    "facing=south": { "model": "mymod:block/my_display_block", "y": 180 },
    "facing=west":  { "model": "mymod:block/my_display_block", "y": 270 },
    "facing=east":  { "model": "mymod:block/my_display_block", "y": 90 }
  }
}
```

**Block model JSON -- Option A: Use ModUtilities' built-in models (recommended)**

ModUtilities ships three reusable block models with dark tech panel textures. Parent your block model to one of these to avoid creating custom textures:

`assets/mymod/models/block/my_display_block.json`:

```json
{
  "parent": "modutilities:block/display_full_block"
}
```

`assets/mymod/models/item/my_display_block.json`:

```json
{
  "parent": "mymod:block/my_display_block"
}
```

Available built-in models:

| Model | Description |
|-------|-------------|
| `modutilities:block/display_full_block` | Solid cube with dark tech panel texture. The north (front) face uses a darker texture (`display_block_front`); all other faces use `display_block_side` |
| `modutilities:block/display_flat_panel` | Thin center panel (2 pixels wide). Front/back faces use `display_panel` texture; edges use `display_block_side` |
| `modutilities:block/display_back_panel` | Thin back-face panel (2 pixels wide, flush against the back). Same textures as the flat panel |

These models reference the following textures (all shipped with ModUtilities):
- `modutilities:block/display_block_side` -- dark tech panel side texture
- `modutilities:block/display_block_front` -- darker front face texture
- `modutilities:block/display_panel` -- dark glass-like panel texture

**Block model JSON -- Option B: Custom texture**

If you want a custom look, create your own model and texture:

`assets/mymod/models/block/my_display_block.json`:

```json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "mymod:block/my_display_block"
  }
}
```

`assets/mymod/models/item/my_display_block.json`:

```json
{
  "parent": "mymod:block/my_display_block"
}
```

You will need a block texture at `assets/mymod/textures/block/my_display_block.png`. The GUI is rendered as an overlay on top of this texture.

**Language file entry** (`assets/mymod/lang/en_us.json`):

```json
{
  "block.mymod.my_display_block": "My Display Block"
}
```

---

## Display-Only Blocks (No Interaction Screen)

By default, right-clicking a display block opens the synced interaction screen. For blocks that should only display information -- dashboards, status boards, info panels -- override `opensSyncedScreenOnUse()` to return `false`.

```java
public class StatusBoardBlockEntity extends AbstractDisplayBlockEntity {

    public StatusBoardBlockEntity(BlockPos pos, BlockState blockState) {
        super(MyModRegistration.STATUS_BOARD_ENTITY.get(), pos, blockState);
    }

    @Override
    public DisplayConfig getDisplayConfig() {
        return DisplayConfig.backPanel();
    }

    @Override
    public ContentBuilder getContentBuilder() {
        return StatusBoardBlockEntity::buildContent;
    }

    @Override
    public boolean opensSyncedScreenOnUse() {
        return false;
    }

    @Override
    protected void onControllerTick() {
        // Update display content every second
        if (gui == null || level == null) return;
        if (level.getGameTime() % 20 != 0) return;
        
        for (var el : gui.getElements()) {
            if (el instanceof Label l && "time".equals(l.getId())) {
                l.setText("Time: " + level.getDayTime());
                syncToClientPublic();
            }
        }
    }

    private static void buildContent(Gui gui, int w, int h) {
        Label title = new Label("Status Board");
        title.setBounds(0, 10, w, 16);
        title.setAlignment(GuiElement.Alignment.CENTER);
        gui.addElement(title);

        Label time = new Label("Time: 0");
        time.setId("time");
        time.setBounds(0, h / 2, w, 14);
        time.setAlignment(GuiElement.Alignment.CENTER);
        gui.addElement(time);
    }
}
```

**Key points:**

- `opensSyncedScreenOnUse()` returns `false` -- right-clicking does nothing (returns `PASS` to Minecraft, allowing other interactions like placing blocks against it).
- `onControllerTick()` runs every server tick on the controller only. Use it to update GUI elements based on game state or block entity data.
- `syncToClientPublic()` pushes the current GUI state to all clients, triggering a re-render on the block face. Call it after modifying any display elements.

---

## Dynamic Content (GUI Swapping)

Use `rebuildGui()` to swap the displayed GUI at runtime. The `ContentBuilder` returned by `getContentBuilder()` can vary based on block entity state, and calling `rebuildGui()` clears the current GUI and rebuilds it using the current builder.

```java
// In your block entity
private boolean showDetailView = false;

@Override
public ContentBuilder getContentBuilder() {
    return showDetailView 
        ? MyBlockEntity::buildDetailView 
        : MyBlockEntity::buildOverview;
}

// Call this when you want to switch views
public void switchView() {
    showDetailView = !showDetailView;
    rebuildGui();  // Clears current GUI, calls getContentBuilder().build(), syncs to clients
}
```

**Key points:**

- `rebuildGui()` clears the current GUI, calls `getContentBuilder().build()` with the current group dimensions, then runs `wireCallbacks()` again. The rebuilt state is automatically synced to clients.
- The `ContentBuilder` returned by `getContentBuilder()` can change dynamically based on block entity state -- each call to `rebuildGui()` re-evaluates which builder to use.
- Automatically syncs to clients after rebuilding.
- Only works on the controller entity. Calling on a non-controller member has no effect.

---

## API Reference

### DisplayConfig

A Java record that holds all configuration parameters for a display block's rendering.

```java
public record DisplayConfig(
    int virtualWidth,
    int virtualHeight,
    int renderScale,
    int maxTextureDim,
    float faceOffset,
    ShapeProvider shapeProvider,
    int renderInterval,
    int maxRenderDistance
)
```

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `virtualWidth` | `int` | Width of the GUI coordinate space per block (default: 256) |
| `virtualHeight` | `int` | Height of the GUI coordinate space per block (default: 256) |
| `renderScale` | `int` | Multiplier from virtual pixels to texture pixels (default: 2) |
| `maxTextureDim` | `int` | Maximum texture dimension in pixels. Render scale is reduced if the group texture would exceed this (default: 4096) |
| `faceOffset` | `float` | Offset from the block face in block units. Positive moves the display outward; negative moves it inward |
| `shapeProvider` | `ShapeProvider` | Provides the collision/outline VoxelShape for each facing direction |
| `renderInterval` | `int` | Ticks between GUI re-renders. 1 = every tick (default). Higher values throttle animated displays |
| `maxRenderDistance` | `int` | Maximum distance in blocks for GUI rendering. 0 = unlimited (default). Beyond this distance, the cached texture is reused without re-rendering |

**Factory methods:**

| Method | Description |
|--------|-------------|
| `fullBlock()` | 256x256 virtual, scale 2, slightly inset face (0.005), solid cube shape |
| `fullBlock(int w, int h)` | Same as above with custom virtual dimensions |
| `fullBlock(int w, int h, int interval, int maxDist)` | Custom virtual dimensions with render throttling and distance LOD |
| `flatPanel()` | 256x256 virtual, scale 2, recessed face offset (-7/16), thin 2-pixel-wide center panel shape |
| `flatPanel(int w, int h)` | Same as above with custom virtual dimensions |
| `flatPanel(int w, int h, int interval, int maxDist)` | Same for flat panel variant with render throttling and distance LOD |
| `backPanel()` | 256x256 virtual, scale 2, recessed face offset (-14/16), thin 2-pixel panel on the back face |
| `backPanel(int w, int h)` | Same as above with custom virtual dimensions |
| `backPanel(int w, int h, int interval, int maxDist)` | Same for back panel variant with render throttling and distance LOD |

For a 2x3 multi-block group with `fullBlock()`, the total GUI resolution is 512x768 virtual pixels (rendered at 1024x1536 texture pixels with scale 2).

### ContentBuilder

Functional interface for building the GUI layout.

```java
@FunctionalInterface
public interface ContentBuilder {
    void build(Gui gui, int width, int height);
}
```

| Parameter | Description |
|-----------|-------------|
| `gui` | The `Gui` instance to add elements to |
| `width` | Total virtual width of the display (accounts for multi-block group size) |
| `height` | Total virtual height of the display |

The content builder must create only the visual structure -- elements with positions, sizes, labels, and default values. It must **not** attach server-side callbacks (use `wireCallbacks()` for that). This separation is critical because the same builder runs on both the server (for the authoritative GUI) and the client (for the interaction screen).

### ShapeProvider

Functional interface for providing VoxelShapes based on block facing.

```java
@FunctionalInterface
public interface ShapeProvider {
    VoxelShape getShape(Direction facing);
}
```

Used by `AbstractDisplayBlock.getShape()` to determine the block's collision and outline shape. Typically returns the same shape for all facings (full block) or direction-dependent shapes (panel).

### AbstractDisplayBlockEntity

The core block entity class. Extend this to create custom display block entities.

**Abstract methods (must implement):**

| Method | Description |
|--------|-------------|
| `DisplayConfig getDisplayConfig()` | Return the display configuration for this block type |
| `ContentBuilder getContentBuilder()` | Return the content builder that creates the GUI layout |

**Overridable hooks:**

| Method | Default | Description |
|--------|---------|-------------|
| `wireCallbacks(Gui gui)` | No-op | Attach server-side callbacks (button handlers, slider listeners) to GUI elements after the content builder runs |
| `onControllerTick()` | No-op | Called every server tick on the controller entity only. Use for animations, data updates |
| `saveCustomData(CompoundTag, Provider)` | No-op | Save custom block entity state to NBT |
| `loadCustomData(CompoundTag, Provider)` | No-op | Load custom block entity state from NBT |
| `onInputSynced()` | No-op | Called after the interaction screen syncs input state into the server GUI. Read element values back into BE fields |
| `getChannelId()` | `"default"` | Return a channel identifier. Blocks only merge with adjacent blocks that share the same channel ID and facing |
| `opensSyncedScreenOnUse()` | `true` | Whether right-clicking opens the built-in synced interaction screen. Override to return `false` for display-only blocks or blocks with custom interaction logic |

**Public API -- group state:**

| Method | Return | Description |
|--------|--------|-------------|
| `isController()` | `boolean` | True if this block is the controller (top-left) of its group |
| `isActive()` | `boolean` | True if this block is part of an active group |
| `getControllerPos()` | `BlockPos` | Position of the group's controller block |
| `getControllerEntity()` | `AbstractDisplayBlockEntity` | The controller block entity, or `this` if already the controller |
| `getGroupWidth()` | `int` | Width of the group in blocks |
| `getGroupHeight()` | `int` | Height of the group in blocks |
| `getGridX()` | `int` | This block's column index within the group (0 = leftmost) |
| `getGridY()` | `int` | This block's row index within the group (0 = top) |
| `getGui()` | `Gui` | The GUI instance (non-null only on the controller) |

**Public API -- sync, interaction, and GUI management:**

| Method | Description |
|--------|-------------|
| `syncToClientPublic()` | Marks the entity for client sync on the next tick. Call after modifying state that should be visible to clients. This is the standard way to push updates to clients and trigger a re-render on the block face |
| `rebuildGui()` | Clears the current GUI and rebuilds it using the `ContentBuilder` returned by `getContentBuilder()`. Call when the display content needs to change at runtime (e.g., switching views, updating after major data changes). Only works on the controller. Automatically syncs to clients |
| `handleInteraction(Player, double, double)` | Process a mouse click at the given GUI coordinates (server-side) |
| `handleMouseRelease(Player, double, double)` | Process a mouse release (server-side) |
| `tryAcquireEditor(UUID)` | Attempt to lock the display for exclusive editing by a player. Returns false if another player holds the lock |
| `releaseEditor(UUID)` | Release the editor lock |

**Static methods -- group recalculation:**

| Method | Description |
|--------|-------------|
| `recalculateGroups(Level, BlockPos, Direction)` | Flood-fill from the given position to recalculate the group containing it |
| `recalculateNeighborGroups(Level, BlockPos, Direction)` | Recalculate groups for all neighbors of a removed block |

**State transfer:**

| Method | Description |
|--------|-------------|
| `captureTransferState()` | Captures custom data via `saveCustomData()` into a CompoundTag for transfer to a new controller |
| `applyTransferState(CompoundTag)` | Applies previously captured state via `loadCustomData()` to a new controller after group recalculation |

### AbstractDisplayBlock

The base block class. Extend this to create custom display blocks.

**Abstract methods (must implement):**

| Method | Description |
|--------|-------------|
| `codec()` | Return the `MapCodec` for this block type |
| `newBlockEntity(BlockPos, BlockState)` | Create and return a new block entity instance |

**Inherited behavior (do not override unless you have a specific reason):**

- Horizontal facing via `FACING` block state property.
- Voxel shape delegated to the block entity's `DisplayConfig.shapeProvider()`.
- Player interaction: checks `opensSyncedScreenOnUse()` -- if true, acquires editor lock on server and opens interaction screen on client; if false, returns `PASS`.
- Block entity ticker: calls `serverTick()` on the block entity every tick.
- Placement/removal: triggers group recalculation with state transfer.

### AbstractDisplayBlockEntityRenderer

The client-side renderer. Use directly by registering it for your block entity type -- no subclass needed.

```java
BlockEntityRendererRegistry.register(blockEntityType,
        AbstractDisplayBlockEntityRenderer::new);
```

The renderer:

- Maintains a per-group render data cache (dynamic texture, framebuffer).
- Renders the controller's `Gui` into an offscreen framebuffer once per frame per group.
- Projects the appropriate UV sub-region of the group texture onto each member block's face.
- Automatically adjusts render scale downward if the group texture would exceed `maxTextureDim`.
- Updates the client-side mouse position on the Gui based on the player's crosshair raycast.
- Uses `glCopyTexSubImage2D` for GPU-to-GPU texture copy (no CPU round-trip).
- Skips `renderGuiToTexture()` when no elements are dirty and mouse position unchanged (dirty-flag optimization).
- Caches `GuiGraphics` instance across frames to avoid per-render allocation.
- Respects `DisplayConfig.renderInterval()` to throttle re-renders for animated displays.
- Respects `DisplayConfig.maxRenderDistance()` to skip GUI rendering for distant displays.
- Saves/restores all GL state (texture binding, render target, viewport, projection, fog, shader color) to prevent rendering artifacts.

### DisplayInteractionScreen

Automatically opened when a player right-clicks a display block. Extends `GuiScreen`.

- Uses the block entity's `ContentBuilder` to create a local GUI copy sized to the screen.
- On initial open: performs a full `syncState()` from the server GUI to set initial values.
- Every 2 render frames: syncs display state (server to client) and input state (client to server).
- On close: performs a final input sync and releases the editor lock.

No subclassing or configuration is needed. The screen is opened via `DisplayClientHooks.openInteractionScreen(pos)`, which is called automatically by `AbstractDisplayBlock`.

### DisplayClientHooks

Client-only static utility class. Contains:

| Method | Description |
|--------|-------------|
| `openInteractionScreen(BlockPos)` | Opens a `DisplayInteractionScreen` for the block at the given position |
| `ensureGraphics()` | Initializes the fallback `ClientGraphics` with Minecraft's font. Called automatically before GUI initialization on the client so text width computation works outside the renderer |

Called from `AbstractDisplayBlock.openInteractionScreen()` when `level.isClientSide()` is true. This indirection keeps client-only imports out of common code.

### GuiStateSync

Utility class for synchronizing state between two `Gui` instances that share the same element structure (built by the same `ContentBuilder`).

**Methods:**

| Method | Direction | Description |
|--------|-----------|-------------|
| `syncState(source, target)` | Both | Copies all interactive and display state |
| `syncDisplayState(source, target)` | Server to client | Copies Label text/color and Plot data |
| `syncInputState(source, target)` | Client to server | Copies Slider values, TextBox text, CheckBox state, and EmptyButton click counts |
| `syncDirtyState(source, target)` | Both | Copies only elements where `isDirty()` is true; children still visited recursively |

**Element types synced:**

| Element | Display sync | Input sync |
|---------|-------------|------------|
| `Label` (not TextBox) | Text, text color | -- |
| `Plot` | Plot data series (color, thickness, y-values) | -- |
| `Slider` (HorizontalSlider, VerticalSlider) | -- | Slider value |
| `TextBox` | -- | Text content |
| `CheckBox` | -- | Checked state |
| `EmptyButton` / `Button` | -- | Click count (fires `onFallingEdge` for each missed click) |

**Always synced:** `enabled` state on all elements.

Elements are matched by ID first (via `GuiElement.getId()`), falling back to index position. This enables stable sync even when element order changes slightly.

Each element declares a `SyncCategory` (NONE, INPUT, or DISPLAY). `syncDisplayState` only syncs DISPLAY elements, `syncInputState` only syncs INPUT elements. `syncState` and `syncDirtyState` sync all categories.

#### Button Click Propagation

Buttons cannot be "clicked" over the network directly. Instead, `EmptyButton` maintains a `clickCount` that increments on each press. `GuiStateSync.syncInputState()` compares click counts between client and server: if the client has more clicks, it calls `syncClickCount()` on the server-side button, which fires `onFallingEdge` once for each delta. This replays missed button presses reliably.

---

## Multi-Block Grouping

### How Blocks Auto-Merge

When a display block is placed or removed, `AbstractDisplayBlock.onPlace()`/`onRemove()` calls `AbstractDisplayBlockEntity.recalculateGroups()` on the server. The algorithm:

1. **Flood-fill**: Starting from the changed position, explore horizontally (along the facing's right direction) and vertically (up/down) to find all connected display block entities of the same type, facing, and channel ID.
2. **Find top-left**: Among the connected set, find the block with the highest Y coordinate. If tied, choose the one furthest in the negative-right direction (leftmost when looking at the face).
3. **Find largest rectangle**: Starting from the top-left, expand rightward to find the maximum width, then expand downward row-by-row as long as every position in that row contains a connected block.
4. **Assign roles**: Blocks inside the rectangle receive group info (controller position, group dimensions, grid coordinates). The controller builds and owns the GUI. Blocks outside the rectangle are disabled.
5. **State transfer**: Custom state from the old controller is captured via `captureTransferState()` and applied to the new controller via `applyTransferState()`, so data survives group reshuffling.

### Controller Pattern

Only the controller (top-left block) holds a `Gui` instance. All other group members store the controller's `BlockPos` and their own grid coordinates. The renderer on each member block samples the appropriate UV sub-region of the controller's texture.

When the controller changes (e.g., the top-left block is removed), the system captures the old controller's custom data and applies it to the new controller.

### Channel IDs

Override `getChannelId()` to return a unique string for each display block type that should not merge with others. For example, a full-block display returning `"default"` and a panel display returning `"panel"` will never merge even when placed adjacent to each other.

```java
@Override
public String getChannelId() {
    return "my_custom_channel";
}
```

### GUI Resolution Scaling

The total GUI resolution scales with group size:

- Virtual dimensions: `groupWidth * virtualWidth` by `groupHeight * virtualHeight`
- Texture dimensions: virtual dimensions * `renderScale`
- If texture dimensions exceed `maxTextureDim`, the render scale is automatically reduced

For example, a 3x2 group with `fullBlock()` (256x256, scale 2):
- Virtual: 768 x 512
- Texture: 1536 x 1024

---

## Content Builder Pattern

### Why Content Is Split from Callbacks

The display block system separates GUI layout (`ContentBuilder`) from server behavior (`wireCallbacks`) for a critical reason: the same layout must be buildable on both the server and the client.

- **Server**: `ContentBuilder.build()` creates the layout, then `wireCallbacks()` attaches server-side logic (callbacks that modify BE fields, trigger syncs, etc.).
- **Client interaction screen**: `ContentBuilder.build()` creates an identical layout for the interaction screen overlay. No server callbacks are attached -- instead, display state is synced via `GuiStateSync` (from the client-side block entity) and input state is sent to the server via `DisplayInputSyncPacket`.
- **Client renderer**: The renderer uses the client-side copy of the Gui (reconstructed from NBT sync) and does not call the content builder directly.

### Rules for Content Builders

1. The builder **must be a static method or a stateless lambda**. It receives `(Gui gui, int width, int height)` and adds elements to `gui`.
2. **Do not** reference instance fields or attach callbacks in the builder. The builder runs on both sides.
3. **Do** set default values for interactive elements (slider positions, checkbox states, text content) -- these serve as initial values before sync occurs.
4. **Do** add all elements in a deterministic order. `GuiStateSync` matches elements by index.

### wireCallbacks for Server Behavior

`wireCallbacks(Gui gui)` runs only on the server, after the content builder. Use it to:

- Find elements by type or content (e.g., iterate `gui.getElements()` to find a `Button` with text `"Pause"`).
- Attach callbacks (`setOnFallingEdge`, `setOnValueChanged`, `setOnTextChanged`, etc.).
- Store references to elements you need to update later (e.g., labels showing computed values).
- Call `syncToClientPublic()` from callbacks to push changes to clients.

---

## State Synchronization

### Server to Client: Block Entity Sync

Display block entities use Minecraft's standard block entity sync mechanism:

1. When state changes, `syncToClient()` (called internally or via `syncToClientPublic()`) sets a `needsSync` flag.
2. On the next `serverTick()`, if `needsSync` is true, the entity calls `level.sendBlockUpdated()`.
3. Minecraft calls `getUpdateTag()` which serializes the entity's state (including custom data) to NBT.
4. The client receives the NBT via `getUpdatePacket()` (`ClientboundBlockEntityDataPacket`), and `loadAdditional()` rebuilds the GUI from the synced state.

### Interaction Screen: Packet-Based Sync

The interaction screen works on both singleplayer and dedicated servers using a packet-based architecture:

1. **Initial sync**: On screen open, `GuiStateSync.syncState()` copies all state from the client-side block entity's Gui to the screen's Gui.
2. **Every 2 frames**:
   - **Display (server to client)**: `GuiStateSync.syncDisplayState()` reads labels and plots from the client-side block entity's Gui (which is kept up-to-date by Minecraft's standard block entity NBT sync).
   - **Input (client to server)**: `GuiInputSerializer.serializeInput()` captures all input element values (slider positions, text content, checkbox states, button click counts) into a `CompoundTag`. This is sent to the server via `DisplayInputSyncPacket` (a C2S packet registered through Architectury's `NetworkManager`).
3. **Server-side handling**: The packet handler calls `GuiInputSerializer.applyInput()` on the server-side Gui, then calls `onInputSynced()` on the block entity so it can read GUI values back into its fields, then calls `syncToClientPublic()` to broadcast the update.
4. **Screen close**: A final `DisplayInputSyncPacket` is sent with `closing=true`, which triggers `releaseEditor()` on the server to free the editor lock.

### GuiInputSerializer

Serializes GUI input state to/from `CompoundTag` by walking the element tree. Elements are keyed by their ID (if set via `setId()`) or index path. Each element's state is stored as a nested CompoundTag via `serializeState()`.

| Method | Description |
|--------|-------------|
| `serializeInput(Gui)` | Serializes all INPUT-category element state into a CompoundTag |
| `serializeDirtyInput(Gui)` | Serializes only dirty INPUT-category elements (for bandwidth-efficient delta sync) |
| `applyInput(CompoundTag, Gui)` | Applies serialized state. Works for both full and delta tags (only processes keys present in the tag) |

### GuiElement ISyncable API

All GUI elements support per-element state serialization and dirty tracking:

| Method | Description |
|--------|-------------|
| `getId()` / `setId(String)` | Optional element ID for stable matching during sync |
| `serializeState()` | Serializes element-specific state to CompoundTag. Base implementation includes `enabled` |
| `deserializeState(CompoundTag)` | Restores state from CompoundTag |
| `isDirty()` | Returns true if state changed since last `clearDirty()` |
| `clearDirty()` | Resets the dirty flag |
| `getSyncCategory()` | Returns `NONE`, `INPUT`, or `DISPLAY` -- determines which sync paths include this element |
| `getSerializableChildren()` | Returns children for tree serialization (excludes internal children like TextBox's label) |

**SyncCategory assignments:**

| Category | Elements |
|----------|----------|
| INPUT | Slider, TextBox, CheckBox, EmptyButton |
| DISPLAY | Label, Plot |
| NONE | Frame, TabElement, DropDownMenu, and all others |

State-changing setters (e.g., `setSliderValue`, `setText`, `setChecked`) automatically call `markDirty()`.

### GuiElementRegistry

Maps string type keys to element factory functions for tree serialization/deserialization.

```java
// Register a custom element type
GuiElementRegistry.register("my_widget", MyWidget.class, MyWidget::new);

// Create an element from a type key
GuiElement element = GuiElementRegistry.create("my_widget");

// Get the type key for serialization
String key = GuiElementRegistry.getTypeKey(element);
```

**Built-in registrations:** `label`, `textbox`, `checkbox`, `empty_button`, `button`, `close_button`, `horizontal_slider`, `vertical_slider`, `frame`, `plot`, `texture_element`, `tab_element`, `dropdown_menu`, `item_view`, `horizontal_list_view`, `vertical_list_view`

### Dynamic Element Sync

The display block system supports dynamic GUI trees where elements are added or removed at runtime on the server.

**Full tree serialization:**
```java
CompoundTag tree = gui.serializeTree();   // Serialize structure + state
gui.deserializeTree(tree);                // Reconstruct from tag
```

**Structural change tracking:**

When `gui.setTrackStructuralChanges(true)` is enabled, `addElement()` and `removeElement()` log `GuiStructuralChange` events and increment `gui.getStructureVersion()`.

The `DisplayInteractionScreen` detects version mismatches and performs a full tree resync automatically. Block entities serialize the GUI tree into NBT when structural changes have occurred (`structureVersion > 0`).

### DisplayRenderProfiler

Toggleable profiler that measures per-group render timing.

**Toggle:** `/modutilities displayProfiler` command or `DisplayRenderProfiler.setEnabled(true/false)`

**Categories measured:**

| Category | What it measures |
|----------|-----------------|
| TOTAL | Entire `render()` method per block per frame |
| GUI_RENDER | `gui.renderBackground()` + `gui.render()` (once per group per tick) |
| TEXTURE_TRANSFER | `glCopyTexSubImage2D` GPU copy (once per group per tick) |
| QUAD_RENDER | Textured quad draw on block face (per block per frame) |

**Output format:**
```
[DisplayProfiler] group@(x,y,z) WxH samples=gui/total | total: min/avg/max us | gui: min/avg/max us | transfer: min/avg/max us | quad: min/avg/max us
```

Reports every 100 GUI_RENDER samples, and flushes remaining data when disabled.

### DisplayInputSyncPacket

C2S packet carrying input state from the interaction screen to the server.

| Field | Type | Description |
|-------|------|-------------|
| `controllerPos` | `BlockPos` | Position of the display group's controller block entity |
| `inputState` | `CompoundTag` | Serialized GUI input state from `GuiInputSerializer` |
| `closing` | `boolean` | If true, releases the editor lock after applying input |

### DisplayNetworking

Registers display block packets. Called automatically during mod initialization (`ModUtilitiesMod.init()`). No action needed from consuming mods.

### onInputSynced Hook

Override `onInputSynced()` to read interactive element values back into block entity fields when the interaction screen syncs input:

```java
@Override
public void onInputSynced() {
    if (gui == null) return;
    for (var el : gui.getElements()) {
        if (el instanceof HorizontalSlider slider) {
            speed = slider.getSliderValue();
        }
        if (el instanceof TextBox tb) {
            titleText = tb.getText();
        }
        if (el instanceof CheckBox cb) {
            notificationsEnabled = cb.isChecked();
        }
    }
    // Update dependent elements
    syncStateToGui();
}
```

### Button Click Propagation

Buttons use a click-count mechanism rather than direct event forwarding:

1. When a button is clicked in the interaction screen, `EmptyButton.clickCount` increments on the client.
2. `syncInputState()` calls `syncClickCount(sourceCount)` on the server-side button.
3. If the server's count is behind, `syncClickCount` fires `onFallingEdge` once for each missed click, then updates the count.

This means button callbacks set in `wireCallbacks()` fire on the server even though the click originated on the client.

---

## Examples

### Full Block Example

The sandbox `DisplayDemoBlock` / `DisplayDemoBlockEntity` demonstrates a full-block display with:

- A live sine/cosine plot that animates via `onControllerTick()`
- A speed slider with reactive label updates
- A pause button using `onFallingEdge` callback
- A text input that updates the title label
- Custom data persistence (`saveCustomData` / `loadCustomData`)
- Interaction screen sync via `onInputSynced()`

Source files:
- `common/src/main/java/net/kroia/modutilities/sandbox/DisplayDemoBlock.java`
- `common/src/main/java/net/kroia/modutilities/sandbox/DisplayDemoBlockEntity.java`

### Flat Panel Example

The sandbox `DisplayDemoPanelBlock` / `DisplayDemoPanelBlockEntity` demonstrates a flat-panel display with:

- A thin, glass-pane-like collision shape
- A different channel ID (`"panel"`) to prevent merging with full-block displays
- A click counter button
- A checkbox for toggling notifications
- A text input for a status message

Source files:
- `common/src/main/java/net/kroia/modutilities/sandbox/DisplayDemoPanelBlock.java`
- `common/src/main/java/net/kroia/modutilities/sandbox/DisplayDemoPanelBlockEntity.java`

### Back Panel Example

The sandbox `DisplayDemoBackPanelBlock` / `DisplayDemoBackPanelBlockEntity` demonstrates a back-panel display with:

- A thin panel flush against the back face of the block
- Display-only mode (`opensSyncedScreenOnUse()` returns `false`) -- no interaction screen
- A different channel ID (`"back_panel"`) to prevent merging with other display types
- An uptime timer updated every second via `onControllerTick()`
- Server-side label updates pushed to clients via `syncToClientPublic()`

Source files:
- `common/src/main/java/net/kroia/modutilities/sandbox/DisplayDemoBackPanelBlock.java`
- `common/src/main/java/net/kroia/modutilities/sandbox/DisplayDemoBackPanelBlockEntity.java`

### Chart Demo Example

The sandbox `ChartDemoBlock` / `ChartDemoBlockEntity` demonstrates a display block hosting a `SandboxLineChart` with:

- Scissor clipping test in the offscreen display block renderer
- Sine, cosine, and square wave data series
- Interactive pan and zoom on the chart
- A different channel ID (`"chart_demo"`) to prevent merging with other display types

Source files:
- `common/src/main/java/net/kroia/modutilities/sandbox/ChartDemoBlock.java`
- `common/src/main/java/net/kroia/modutilities/sandbox/ChartDemoBlockEntity.java`

### Minimal Example: Simplest Possible Display Block

A display block that shows a single centered label with no interactivity:

**Block entity:**

```java
package com.example.mymod;

import net.kroia.modutilities.gui.display.AbstractDisplayBlockEntity;
import net.kroia.modutilities.gui.display.ContentBuilder;
import net.kroia.modutilities.gui.display.DisplayConfig;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class InfoDisplayBlockEntity extends AbstractDisplayBlockEntity {

    public InfoDisplayBlockEntity(BlockPos pos, BlockState blockState) {
        super(MyModRegistration.INFO_DISPLAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public DisplayConfig getDisplayConfig() {
        return DisplayConfig.fullBlock();
    }

    @Override
    public ContentBuilder getContentBuilder() {
        return (gui, w, h) -> {
            Label label = new Label("Hello, World!");
            label.setBounds(0, h / 2 - 8, w, 16);
            label.setAlignment(GuiElement.Alignment.CENTER);
            gui.addElement(label);
        };
    }
}
```

**Block:**

```java
package com.example.mymod;

import com.mojang.serialization.MapCodec;
import net.kroia.modutilities.gui.display.AbstractDisplayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class InfoDisplayBlock extends AbstractDisplayBlock {

    public static final MapCodec<InfoDisplayBlock> CODEC =
            simpleCodec(p -> new InfoDisplayBlock());

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public InfoDisplayBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfoDisplayBlockEntity(pos, state);
    }
}
```

This is the absolute minimum: two classes, two method overrides on the block entity, two on the block. Registration follows the same pattern as the Quick Start.

---

## Prerequisites

- Familiarity with Minecraft block entities and the block entity renderer system
- Understanding of Architectury's DeferredRegister pattern
- Basic knowledge of the MC_ModUtilities GUI element system (see [GUI Library Overview](GuiLibrary.md))

## See Also

- [GUI Library Overview](GuiLibrary.md) -- GUI element system documentation
- [GuiScreen](GuiScreen.md) -- Base screen class that `DisplayInteractionScreen` extends
- [GuiElement](GuiElement.md) -- Base widget class for all GUI elements

---

**Version:** 2.0.1
**Minecraft Version:** 1.21.1
