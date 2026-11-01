# GUI Library

## Overview

The GUI library is a fast and easy way to create screens in Minecraft. It uses a hierarchical tree structure to organize elements displayed on the screen. The library is designed to be compatible across multiple Minecraft versions, abstracting away version-specific GUI code changes.

Starting with v2.0.1, the GUI library core (`Gui`, `GuiElement`, all element subclasses) is **common code** that runs on both server and client. This enables rendering GUI elements on block faces via the DisplayBlock system, in addition to traditional Minecraft screens.

<tr>
<td>
<div align="center">
    <img src="images/GUI.png" width="600"> 
</div>
</td>

## Prerequisites

Before using the GUI library, you should understand:
- Basic Minecraft client-side screen concepts
- Component-based UI design patterns
- How to create and register network packets (if opening GUIs from server)

## Key Features

- Hierarchical element tree structure
- Cross-version compatibility
- Built-in layout management
- Debug visualization tools (F3-F6 keys)
- Rich set of pre-built components
- Custom element support
- **Render on block faces** via DisplayBlock (same elements work on screens and blocks)

## Migration Guide (v2.0.1)

### Package changes

Several classes have been moved to the `gui.client` package. **Update your imports** if you use any of these:

| Class | Old package | New package |
|---|---|---|
| `GuiScreen` | `net.kroia.modutilities.gui` | `net.kroia.modutilities.gui.client` |
| `GuiContainerScreen` | `net.kroia.modutilities.gui` | `net.kroia.modutilities.gui.client` |
| `ContainerView` | `net.kroia.modutilities.gui.elements` | `net.kroia.modutilities.gui.client` |
| `InventoryView` | `net.kroia.modutilities.gui.elements` | `net.kroia.modutilities.gui.client` |
| `Graphics` | `net.kroia.modutilities.gui` | Renamed to `ClientGraphics` in `net.kroia.modutilities.gui.client` |

### Removed classes

| Class | Replacement |
|---|---|
| `Graphics` | Use `ClientGraphics` from `gui.client` package |

### New classes

| Class | Package | Purpose |
|---|---|---|
| `IGraphics` | `net.kroia.modutilities.gui` | Rendering abstraction interface (common code) |
| `IInputProvider` | `net.kroia.modutilities.gui` | Input polling interface (common code) |
| `InputConstants` | `net.kroia.modutilities.gui` | GLFW-free key/mouse constants (common code) |
| `ClientGraphics` | `net.kroia.modutilities.gui.client` | Screen rendering backend (replaces `Graphics`) |
| `ClientInputProvider` | `net.kroia.modutilities.gui.client` | GLFW input backend |
| `WorldGraphics` | `net.kroia.modutilities.gui.client` | Block-face rendering backend |

### What stays the same

- `GuiScreen` API is unchanged (just update the import)
- `GuiContainerScreen` API is unchanged (just update the import)
- All element classes (`Button`, `Label`, `Slider`, `Plot`, `Frame`, etc.) remain in their original packages
- The `Gui` class API is unchanged for screen usage
- `updateLayout(Gui gui)` pattern is unchanged
- Element lifecycle (`init`, `render`, `layoutChanged`) is unchanged

### Quick fix for dependent mods

In most cases, a find-and-replace on imports is sufficient:

```
// Old imports
import net.kroia.modutilities.gui.GuiScreen;
import net.kroia.modutilities.gui.GuiContainerScreen;
import net.kroia.modutilities.gui.elements.ContainerView;
import net.kroia.modutilities.gui.elements.InventoryView;

// New imports
import net.kroia.modutilities.gui.client.GuiScreen;
import net.kroia.modutilities.gui.client.GuiContainerScreen;
import net.kroia.modutilities.gui.client.ContainerView;
import net.kroia.modutilities.gui.client.InventoryView;
```

## Content

### Core Classes
- [GuiScreen](GuiScreen.md) - Base screen implementation for standalone GUIs
- [GuiContainerScreen](GuiContainerScreen.md) - Screen implementation for inventory-based GUIs
- [GuiElement](GuiElement.md) - Base widget class for all GUI components

### Available GUI Elements

**Interactive Elements:**
  - [Button](GuiElements/Button.md) - Standard clickable button
  - [EmptyButton](GuiElements/EmptyButton.md) - Button without visual representation
  - [CloseButton](GuiElements/CloseButton.md) - Pre-configured close button
  - [CheckBox](GuiElements/CheckBox.md) - Toggle checkbox element
  - [HorizontalSlider](GuiElements/HorizontalSlider.md) / [VerticalSlider](GuiElements/VerticalSlider.md) - Value slider controls
  - [TextBox](GuiElements/TextBox.md) - Text input field
  - [DropDownMenu](GuiElements/DropDownMenu.md) - Selection dropdown

**Display Elements:**
  - [Label](GuiElements/Label.md) - Text display
  - [TextureElement](GuiElements/TextureElement.md) - Image/texture display
  - [Plot](GuiElements/Plot.md) - Data visualization chart
  - [ItemView](GuiElements/ItemView.md) - Single item display
  - [ContainerView](GuiElements/ContainerView.md) - Container/inventory display

**Container Elements:**
  - [Frame](GuiElements/Frame.md) - Generic container for grouping elements
  - [ListView](GuiElements/ListView.md) - Scrollable list container
  - [InventoryView](GuiElements/InventoryView.md) - Player inventory display
  - [ItemSelectionView](GuiElements/ItemSelectionView.md) - Item selection grid
  - [TabElement](GuiElements/TabElement.md) - Tabbed container
  - [ExpandablePanel](GuiElements/ExpandablePanel.md) - Collapsible container with a header

## Architecture

### Rendering backends

The GUI library uses an `IGraphics` interface to abstract rendering. Two backends are provided:

- **`ClientGraphics`** - Wraps Minecraft's `GuiGraphics` for screen rendering. Used automatically by `GuiScreen` and `GuiContainerScreen`.
- **`WorldGraphics`** - Renders to a framebuffer texture for block-face display. Used by the DisplayBlock system.

Both backends support the same GUI elements. Code that creates elements and builds layouts works identically for screens and blocks.

### Common vs client code

The GUI library is split into common code (runs on server + client) and client-only code:

**Common** (`net.kroia.modutilities.gui` and subpackages):
- `Gui`, `GuiElement`, all element subclasses
- `IGraphics`, `IInputProvider`, `InputConstants`
- Layout classes, geometry classes

**Client-only** (`net.kroia.modutilities.gui.client`):
- `GuiScreen`, `GuiContainerScreen`
- `ClientGraphics`, `ClientInputProvider`, `WorldGraphics`
- `ContainerView`, `InventoryView`

## See Also

- [Documentation Index](../README.md) - Main documentation index
- [Sandbox System](../development/Sandbox.md) - GUI testing examples
- [Networking Library](../networking/Networking.md) - For opening GUIs from server
