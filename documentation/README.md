# MC_ModUtilities Documentation

Welcome to the MC_ModUtilities documentation. This library provides a comprehensive set of tools and systems for Minecraft mod development.

## Documentation Structure

This documentation is organized into logical sections to help you find what you need quickly:

### Getting Started
- [Project README](../README.md) - Project overview, dependencies, and downloads
- [Sandbox System](development/Sandbox.md) - Testing framework for library features

### Core Systems

#### GUI System
The GUI library provides a modern, component-based system for creating user interfaces.

**Main Documentation:**
- [GUI Library Overview](gui/GuiLibrary.md) - Introduction and component list
- [GuiScreen](gui/GuiScreen.md) - Base screen implementation
- [GuiContainerScreen](gui/GuiContainerScreen.md) - Inventory-based screens
- [GuiElement](gui/GuiElement.md) - Base widget class

**Components:**
- [Button](gui/GuiElements/Button.md) - Interactive button elements
- [CheckBox](gui/GuiElements/CheckBox.md) - Toggle checkbox elements
- [CloseButton](gui/GuiElements/CloseButton.md) - Close button for screens
- [ContainerView](gui/GuiElements/ContainerView.md) - Container-based views
- [DropDownMenu](gui/GuiElements/DropDownMenu.md) - Drop-down selection menus
- [EmptyButton](gui/GuiElements/EmptyButton.md) - Unstyled button base
- [Frame](gui/GuiElements/Frame.md) - Bordered frame container
- [HorizontalSlider](gui/GuiElements/HorizontalSlider.md) - Horizontal slider control
- [InventoryView](gui/GuiElements/InventoryView.md) - Player/container inventory display
- [ItemSelectionView](gui/GuiElements/ItemSelectionView.md) - Item picker interface
- [ItemView](gui/GuiElements/ItemView.md) - Single item display
- [Label](gui/GuiElements/Label.md) - Text label elements
- [ListView](gui/GuiElements/ListView.md) - Scrollable list views
- [Plot](gui/GuiElements/Plot.md) - Data plotting and charts
- [TabElement](gui/GuiElements/TabElement.md) - Tab-based navigation
- [TextBox](gui/GuiElements/TextBox.md) - Text input fields
- [TextureElement](gui/GuiElements/TextureElement.md) - Texture rendering element
- [VerticalSlider](gui/GuiElements/VerticalSlider.md) - Vertical slider control

**Layout:**
- [Layout System](gui/Layout.md) - Automatic layout management for GUI elements

**Use Cases:**
- [Dashboard](gui/usecases/Dashboard.md) - Building dashboard-style interfaces
- [Dialog](gui/usecases/Dialog.md) - Creating dialog and confirmation screens
- [Form](gui/usecases/Form.md) - Building input forms
- [Settings](gui/usecases/Settings.md) - Settings screen patterns
- [Tabs](gui/usecases/Tabs.md) - Tabbed interface patterns

**Display Blocks:**
- [Display Block System](gui/DisplayBlock.md) - In-world blocks that render GUI elements on their face

#### Networking System
Comprehensive networking library for client-server communication.

**Main Documentation:**
- [Networking Overview](networking/Networking.md) - Introduction to networking features
- [NetworkManager](networking/NetworkManager.md) - Core network manager
- [NetworkPacket](networking/NetworkPacket.md) - Custom packet creation

**Advanced Features:**
- [ARRS (Asynchronous Request Response System)](networking/ARRS.md) - Request/response pattern
- [ARRS Generic Request](networking/ARRSGenericRequest.md) - Creating custom requests
- [Stream System](networking/StreamSystem.md) - Continuous data streaming
- [GenericStream](networking/GenericStream.md) - Creating custom streams

**Multi-Server:**
- [Multi-Server Networking](networking/MultiServerNetworking.md) - TCP-based Master/Slave multi-server communication

### Utility Systems

#### Settings & Configuration
- [Settings Library](systems/Settings.md) - JSON-based configuration system

#### Persistence
- [Persistence System](systems/Persistence.md) - NBT storage and data archives

#### Events
- [Event System](systems/Events.md) - Signal and DataEvent classes

#### Utility Classes
- [Utility Classes Reference](systems/Utilities.md)
  - ItemUtilities
  - ColorUtilities (RGB/RGBA manipulation)
  - PlayerUtilities (Client and Server)
  - JsonUtilities

#### Client Utilities
- RecipeImageExporter - Off-screen framebuffer renderer for crafting recipe PNG export (documentation pending)

### Development

#### Testing
- [Sandbox System](development/Sandbox.md) - Development testing framework
- [Test Suite](testing/TestSuite.md) - Automated test suite for library features

#### Contributing
- JavaDoc documentation (in progress)
- Code examples and use cases (in progress)

## Navigation Tips

- **New to MC_ModUtilities?** Start with the [Project README](../README.md) and [GUI Library Overview](gui/GuiLibrary.md)
- **Need to send data?** Check the [Networking Overview](networking/Networking.md)
- **Building UIs?** See the [GUI Library Overview](gui/GuiLibrary.md) and [GuiScreen](gui/GuiScreen.md)
- **Multi-server setup?** See [Multi-Server Networking](networking/MultiServerNetworking.md)
- **Testing features?** Use the [Sandbox System](development/Sandbox.md) and [Test Suite](testing/TestSuite.md)

## Documentation Status

Legend:
- **Complete** - Documentation is comprehensive and up-to-date
- **Partial** - Basic documentation exists but needs expansion
- **Pending** - Documentation not yet created

| System | Status | Notes |
|--------|--------|-------|
| GUI Library | Complete | 18 component docs, layout, 5 use cases, display block |
| Networking (Client-Server) | Complete | All major features documented |
| Networking (Multi-Server) | Complete | TCP Master/Slave system documented |
| Settings Library | Complete | JSON parser system documented |
| Persistence System | Complete | NBT and archive system documented |
| Event System | Complete | Signal and DataEvent documented |
| Utility Classes | Complete | Helper functions API reference documented |
| Sandbox System | Complete | Development testing framework documented |
| Test Suite | Complete | Automated test suite documented |
| RecipeImageExporter | Pending | Crafting recipe PNG export needs docs |

## Prerequisites by System

### GUI System
- Basic understanding of Minecraft screens
- Familiarity with component-based UI patterns
- Understanding of client-side rendering

### Networking System
- Knowledge of client-server architecture
- Understanding of packet-based communication
- Familiarity with asynchronous patterns (for ARRS)

### Settings Library
- Basic JSON knowledge
- Understanding of configuration patterns

### Persistence System
- Familiarity with NBT format
- Understanding of data serialization

## Common Patterns

### Opening a GUI from Server
1. Create a network packet to trigger GUI opening
2. Send packet from server to specific client
3. Client receives packet and opens screen using `GuiScreen.setScreen()`

**Example:** See [SandboxOpenGuiPacket](development/Sandbox.md) in Sandbox system

### Request-Response Pattern
1. Define a request class extending `ARRSGenericRequest`
2. Register the request in your NetworkManager
3. Send request from client/server
4. Handle response asynchronously

**Documentation:** [ARRS System](networking/ARRS.md)

### Continuous Data Updates
1. Create a stream class extending `GenericStream`
2. Register the stream in your NetworkManager
3. Start stream with context data and callbacks
4. Stream automatically manages lifecycle

**Documentation:** [Stream System](networking/StreamSystem.md)

## See Also

- [Main Project Repository](https://github.com/KROIA/MC_ModUtilities) (if applicable)
- [CurseForge Page](https://www.curseforge.com/minecraft/mc-mods/kroiautilities)
- [Architectury API Documentation](https://docs.architectury.dev/)

## Contributing to Documentation

When adding new documentation:

1. **Use consistent formatting:**
   - H1 (`#`) for page title
   - H2 (`##`) for major sections
   - H3 (`###`) for subsections
   - Horizontal rules (`---`) to separate major sections

2. **Include these sections:**
   - Prerequisites (what developers should know first)
   - See Also (related documentation)
   - Code examples with syntax highlighting
   - Common use cases

3. **Add to this index:**
   - Update the appropriate category above
   - Update the Documentation Status table
   - Add cross-references from related pages

4. **Use relative links:**
   - Link to other documentation using relative paths
   - Example: `[GUI Library](gui/GuiLibrary.md)`

---

**Version:** 2.0.1  
**Minecraft Version:** 1.21.1  
**Last Updated:** May 2026
