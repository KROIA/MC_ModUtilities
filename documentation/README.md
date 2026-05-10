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
- [ListView](gui/GuiElements/ListView.md) - Scrollable list views

**See Also:**
- GUI Layout System (documentation pending)
- Additional GUI elements documentation (in progress)

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

**See Also:**
- Multi-Server Networking (documentation pending)

### Utility Systems

#### Settings & Configuration
- Settings Library (documentation pending) - JSON-based configuration system

#### Persistence
- Persistence System (documentation pending) - NBT storage and data archives

#### Events
- Event System (documentation pending) - Signal and DataEvent classes

#### Utility Classes
- Utility Classes Reference (documentation pending)
  - ItemUtilities
  - ColorUtilities (RGB/RGBA manipulation)
  - PlayerUtilities (Client and Server)
  - JsonUtilities

### Development

#### Testing & Examples
- [Sandbox System](development/Sandbox.md) - Development testing framework

#### Contributing
- JavaDoc documentation (in progress)
- Code examples and use cases (in progress)

## Navigation Tips

- **New to MC_ModUtilities?** Start with the [Project README](../README.md) and [GUI Library Overview](gui/GuiLibrary.md)
- **Need to send data?** Check the [Networking Overview](networking/Networking.md)
- **Building UIs?** See the [GUI Library Overview](gui/GuiLibrary.md) and [GuiScreen](gui/GuiScreen.md)
- **Testing features?** Use the [Sandbox System](development/Sandbox.md)

## Documentation Status

Legend:
- **Complete** - Documentation is comprehensive and up-to-date
- **Partial** - Basic documentation exists but needs expansion
- **Pending** - Documentation not yet created

| System | Status | Notes |
|--------|--------|-------|
| GUI Library | Partial | Core docs complete, 15+ components need documentation |
| Networking (Client-Server) | Complete | All major features documented |
| Networking (Multi-Server) | Pending | TCP Master/Slave system needs docs |
| Settings Library | Pending | JSON parser system needs docs |
| Persistence System | Pending | NBT and archive system needs docs |
| Event System | Pending | Signal and DataEvent need docs |
| Utility Classes | Pending | Helper functions need API reference |
| Sandbox System | Complete | Development testing framework documented |

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

**Version:** 2.0.0_ALPHA  
**Minecraft Version:** 1.21.1  
**Last Updated:** May 2026
