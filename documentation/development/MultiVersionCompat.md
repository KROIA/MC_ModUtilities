# Multi-Version Compatibility

## Overview

ModUtilities is built for every Minecraft version from 1.21.1 to 26.3 (one project folder per version, `mc_<version>_MC_ModUtilities`). Between these versions Minecraft renamed, moved or removed many APIs: registry IDs on block/item properties (1.21.2), `Optional` NBT getters (1.21.5), input event records (1.21.9), `ResourceLocation` → `Identifier` (1.21.11), screen handling and `Tuple` (26.2), GLFW → SDL3 (26.3), and more.

The compat helpers below hide these differences. Code in dependent mods (BankSystem, StockMarket, your mod) that goes through them compiles **unchanged** on every supported version, so the dependent mod needs fewer version toggles of its own.

| Class | Package | Side | Covers |
|---|---|---|---|
| [`RegistryCompat`](#registrycompat) | `net.kroia.modutilities` | common | Block/item properties, block entity types |
| [`NbtCompat`](#nbtcompat) | `net.kroia.modutilities` | common | `CompoundTag` / `ListTag` reads, UUIDs, `ValueInput`/`ValueOutput` |
| [`PlayerCompat`](#playercompat) | `net.kroia.modutilities` | common | `GameProfile` name/id, messages, dropping items |
| [`PermissionCompat`](#permissioncompat) | `net.kroia.modutilities` | common | Op-level permission checks |
| [`Tuple`](#tuple) | `net.kroia.modutilities` | common | Replacement for `net.minecraft.util.Tuple` |
| [`InputCompat`](#inputcompat) | `net.kroia.modutilities.gui.client` | client | Modifier keys, key/mouse state, key mappings, cursor |
| [`ClientCompat`](#clientcompat) | `net.kroia.modutilities.gui.client` | client | Current screen, main render target |
| [`InputConstants`](../gui/GuiLibrary.md#inputconstants) | `net.kroia.modutilities.gui` | common | Key/mouse constants and layout-aware Ctrl shortcuts |

## Prerequisites

- Familiarity with Minecraft block/item registration and NBT
- Knowing which Minecraft versions your mod targets

---

## RegistryCompat

MC 1.21.2 requires the registry ID to be set on `BlockBehaviour.Properties` / `Item.Properties` **before** the block or item is constructed, and removed `BlockEntityType.Builder`. `RegistryCompat` does the right thing on every version (on 1.21.1 the ID is ignored).

| Method | Description |
|---|---|
| `blockProperties(BlockBehaviour.Properties props, id)` | Returns `props` with the block registry ID set |
| `itemProperties(Item.Properties props, id)` | Returns `props` with the item registry ID set |
| `blockItemProperties(Item.Properties props, id)` | Like `itemProperties`, but the `BlockItem` keeps the block's translation key (1.21.1 behaviour) |
| `createBlockEntityType(BlockEntityFactory<T> factory, Block... blocks)` | Creates a `BlockEntityType` (replaces `BlockEntityType.Builder.of(...).build(null)`) |
| `BlockEntityFactory<T>` | Public functional interface `(BlockPos, BlockState) -> T`, e.g. `MyBlockEntity::new` |

The `id` parameter is a `ResourceLocation` up to 1.21.10 and an `Identifier` from 1.21.11 (Minecraft renamed the class); keep one small `id(String)` helper per mod for that.

```java
public class MyBlock extends Block {
    public MyBlock() {
        super(RegistryCompat.blockProperties(
                BlockBehaviour.Properties.of().strength(2.0f),
                MyMod.id("my_block")));
    }
}

public static final RegistrySupplier<Block> MY_BLOCK =
        BLOCKS.register("my_block", MyBlock::new);

public static final RegistrySupplier<Item> MY_BLOCK_ITEM =
        ITEMS.register("my_block", () -> new BlockItem(MY_BLOCK.get(),
                RegistryCompat.blockItemProperties(new Item.Properties(), MyMod.id("my_block"))));

public static final RegistrySupplier<Item> MY_ITEM =
        ITEMS.register("my_item", () -> new Item(
                RegistryCompat.itemProperties(new Item.Properties().stacksTo(16), MyMod.id("my_item"))));

public static final RegistrySupplier<BlockEntityType<MyBlockEntity>> MY_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("my_block_entity", () ->
                RegistryCompat.createBlockEntityType(MyBlockEntity::new, MY_BLOCK.get()));
```

See [Display Block System](../gui/DisplayBlock.md) for a complete registration example.

---

## NbtCompat

MC 1.21.5 changed most `CompoundTag` / `ListTag` getters to return `Optional`, renamed the numeric tag accessors and removed the UUID helpers. `NbtCompat` behaves the same on every version: a missing or wrong-typed key returns the 1.21.1 default (`0`, `false`, `""`, an empty compound/list/array) or the explicit default you pass. All ModUtilities NBT reads go through it.

| Method group | Methods |
|---|---|
| Readers with 1.21.1 defaults | `getByte`, `getShort`, `getInt`, `getLong`, `getFloat`, `getDouble`, `getBoolean`, `getString`, `getCompound`, `getIntArray`, `getByteArray`, `getLongArray` — all `(CompoundTag tag, String key)` |
| Readers with explicit default | `getByte/getShort/getInt/getLong/getFloat/getDouble/getBoolean/getString(CompoundTag tag, String key, def)` |
| Lists | `getList(CompoundTag tag, String key, int elementType)`, `getCompound(ListTag list, int index)`, `getString(ListTag list, int index)`, `elementType(ListTag list)` |
| Keys / types | `keys(CompoundTag)`, `contains(CompoundTag tag, String key, int tagType)` |
| UUIDs | `hasUUID`, `getUUID`, `putUUID` |
| Tag values | `asByte`, `asShort`, `asInt`, `asLong`, `asFloat`, `asDouble`, `asNumber` (`NumericTag`), `asString(Tag)` |
| 1.21.6+ only | `readAll(ValueInput)`, `writeAll(ValueOutput, CompoundTag)` |

`CompoundTag.contains(String)` and the `put*` methods exist on every version and need no wrapper.

```java
void readFrom(CompoundTag tag) {
    balance = NbtCompat.getLong(tag, "balance");
    name = NbtCompat.getString(tag, "name", "unnamed");
    if (NbtCompat.hasUUID(tag, "owner"))
        owner = NbtCompat.getUUID(tag, "owner");
    ListTag entries = NbtCompat.getList(tag, "entries", Tag.TAG_COMPOUND);
    for (int i = 0; i < entries.size(); i++)
        readEntry(NbtCompat.getCompound(entries, i));
}
```

**Block entities on 1.21.6+:** `BlockEntity.saveAdditional` / `loadAdditional` take `ValueOutput` / `ValueInput` instead of a `CompoundTag`. `NbtCompat.readAll(input)` and `NbtCompat.writeAll(output, tag)` let existing `CompoundTag`-based code keep its on-disk format (all keys at the same level). These two methods only exist in the 1.21.6+ builds, so the calling code needs a version toggle.

---

## PlayerCompat

| Method | Description |
|---|---|
| `profileName(GameProfile)` | Player name (`GameProfile` became a record in 1.21.9: `getName()` → `name()`) |
| `profileId(GameProfile)` | Player UUID (`getId()` → `id()`) |
| `sendMessage(Player, Component message, boolean actionBar)` | Chat message, or above the hotbar if `actionBar` (`displayClientMessage` was removed in 26.1) |
| `giveOrDrop(Player, ItemStack)` | Adds the stack to the inventory, drops it at the player's feet if full |
| `drop(Player, ItemStack)` | Drops the stack in front of the player like the drop key (26.3 added a `Prediction` argument) |

```java
String name = PlayerCompat.profileName(player.getGameProfile());
PlayerCompat.sendMessage(player, Component.literal("Saved"), true);
PlayerCompat.giveOrDrop(player, new ItemStack(Items.DIAMOND, 3));
```

---

## PermissionCompat

MC 1.21.11 replaced the numeric permission levels (`CommandSourceStack.hasPermission(int)`, `Player.hasPermissions(int)`) with permission sets. `PermissionCompat` keeps the vanilla level semantics (0 = all, 2 = gamemasters/OP, 4 = owners):

| Method | Description |
|---|---|
| `hasPermission(CommandSourceStack source, int level)` | `true` if the command source has at least `level` |
| `hasPermission(Player player, int level)` | `true` if the player has at least `level` |

```java
dispatcher.register(Commands.literal("mymod")
        .requires(source -> PermissionCompat.hasPermission(source, 2))
        .executes(ctx -> 1));
```

---

## Tuple

`net.kroia.modutilities.Tuple<A, B>` is a mutable pair with the same API as `net.minecraft.util.Tuple` (`getA`, `setA`, `getB`, `setB`), which Minecraft 26.2 removed.

> **Breaking change (2.2.0):** `StreamManager` and `StreamSystem.getActive*Streams()` return `List<net.kroia.modutilities.Tuple<UUID, String>>`. Code that stored the result as the vanilla `Tuple` must change its import. See [Stream System](../networking/StreamSystem.md#listing-active-streams).

```java
import net.kroia.modutilities.Tuple; // not net.minecraft.util.Tuple

Tuple<UUID, String> entry = new Tuple<>(id, "my_stream");
```

---

## InputCompat

Client-only (`net.kroia.modutilities.gui.client`). 1.21.9 moved keyboard/mouse input to event records and the shift/control/alt queries from `Screen` to `Minecraft`; 26.3 replaced GLFW with SDL3.

| Method | Description |
|---|---|
| `hasShiftDown()`, `hasControlDown()`, `hasAltDown()` | Modifier key state (control = command on macOS) |
| `currentModifiers()` | Modifier bits of the keys held right now |
| `isKeyDown(int key)` | Key held (`InputConstants.KEY_*`) |
| `isMouseButtonDown(int button)` | Mouse button held (`InputConstants.MOUSE_BUTTON_*`) |
| `setCursorPos(double x, double y)` | Moves the cursor (window coordinates) |
| `windowHandle()` | Native window handle (GLFW, or SDL3 from 26.3) |
| `matchesKey(KeyMapping, int key, int scanCode)` | `true` if the key mapping is bound to the key |
| `matchesMouse(KeyMapping, int button)` | `true` if the key mapping is bound to the mouse button |
| `containerClickButton(int button)` | Converts a Minecraft mouse button to the container click index (0 left, 1 right) that `AbstractContainerMenu` and the click packet expect (SDL3 numbers buttons 1/2/3) |
| `touchscreen()` | Touchscreen mode (always `false` from 26.2, where the option was removed) |

```java
@Override
public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (InputCompat.matchesKey(MyKeys.OPEN_MENU, keyCode, scanCode)) {
        openMenu(InputCompat.hasShiftDown());
        return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
}
```

Compare key and button values against [`InputConstants`](../gui/GuiLibrary.md#inputconstants), never raw numbers or `GLFW.*` constants: all key codes changed with SDL3 in 26.3. In `GuiScreen` / `GuiContainerScreen`, override the int-based input methods; see [GuiScreen](../gui/GuiScreen.md#overriding-input-and-render-methods-across-minecraft-versions).

---

## ClientCompat

Client-only (`net.kroia.modutilities.gui.client`). 26.2 moved screen handling to `Minecraft.gui` and the main render target to `GameRenderer`.

| Method | Description |
|---|---|
| `screen()` | The open screen, or `null` (replaces `Minecraft.getInstance().screen`) |
| `setScreen(Screen screen)` | Opens `screen`, or closes the open screen if `null` |
| `mainRenderTarget()` | The render target the game and GUI draw into |
| `setMainRenderTarget(RenderTarget)` | Replaces the main render target (used internally for offscreen GUI rendering) |

```java
if (ClientCompat.screen() instanceof MyScreen screen)
    screen.refresh();
ClientCompat.setScreen(null); // close
```

`GuiScreen.setScreen(...)` uses the same mechanism.

---

## Source toggles: `// mc<=X` / `// mc>=X`

Where an API differs between versions and no helper hides it, the ModUtilities sources (and BankSystem / StockMarket) keep **all** variants side by side and mark each line with the versions it belongs to. In each `mc_<version>_...` folder, exactly the lines whose markers match that folder's Minecraft version are active; all others are commented out with a leading `//`.

```java
return Screen.hasShiftDown(); // mc<=1.21.8
//return Minecraft.getInstance().hasShiftDown(); // mc>=1.21.9
```

Markers:

- `// mc<=X` — active on version X and older
- `// mc>=X` — active on version X and newer
- `// mc>=X mc<=Y` — active from X to Y (both inclusive)

Rules:

- Every line of a version-specific block carries the marker (including imports, braces and `@Override` lines), so a block can be switched by commenting/uncommenting line by line.
- The same convention is used in `build.gradle` (Java 21 `// mc<=1.21.11` vs. Java 25 `// mc>=26.1`, mappings).
- Files should differ between version folders only in which marked lines are commented; unmarked lines stay identical in every folder, which keeps merging fixes forward from 1.21.1 simple.
- Prefer a compat helper over a toggle in dependent mods: one toggle inside ModUtilities replaces one in every caller.

---

## See Also

- [GUI Library](../gui/GuiLibrary.md) - `InputConstants`, `GuiElementRegistry`
- [GuiScreen](../gui/GuiScreen.md) - Input/render extension points across versions
- [Display Block System](../gui/DisplayBlock.md) - Registration example using `RegistryCompat`
- [Stream System](../networking/StreamSystem.md) - `Tuple` return types
- [Automated in-game tests](../../tools/autotest/README.md) - Running the test suites on every version
- [Documentation Index](../README.md)
