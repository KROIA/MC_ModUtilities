# MC_ModUtilities

## About
This is a comprehensive utilities library mod that provides common functionality for Minecraft mod development.
This mod is only needed for the Quilt platform since it does not include the jar inside the mod which uses this dependency.
Fabric and NeoForge both include this mod, so you don't need it as a separate dependency.

**Current Version**: v2.0.0_ALPHA  
**Minecraft Version**: 1.21.1  
**Supported Platforms**: Fabric, NeoForge, Quilt

## Features
- **GUI Library**: Create GUI screens with a modern, component-based architecture ([documentation](documentation/gui/GuiLibrary.md))
- **Settings Library**: JSON-based configuration system with type-safe parsers
- **Networking Library**: Easy client-server communication and data transfer ([documentation](documentation/networking/Networking.md))
  - Asynchronous Request Response System (ARRS)
  - Streaming system for continuous data transfer
  - Multi-Server networking with TCP Master/Slave architecture
- **Persistence System**: NBT storage and chunked data archives for efficient data management
- **Event System**: Signal and DataEvent classes for event-driven programming
- **Utility Classes**: Helper functions for common tasks
  - ItemUtilities: Item stack manipulation
  - ColorUtilities: RGB/RGBA color manipulation
  - PlayerUtilities: Client and server player utilities
  - JsonUtilities: JSON parsing and serialization
- **GUI Layout System**: Flexible layout managers for GUI components

## Dependencies
- [Architectury API](https://www.curseforge.com/minecraft/mc-mods/architectury-api) v13.0.8
- [Fabric Loader](https://fabricmc.net/use/) v0.16.9
- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) v0.114.0+1.21.1
- [NeoForge](https://neoforged.net/) v21.1.84

## Documentation

**[Complete Documentation Index](documentation/README.md)** - Start here for organized access to all documentation

### Quick Links
- [GUI Library](documentation/gui/GuiLibrary.md) - Component-based UI system
- [Networking Library](documentation/networking/Networking.md) - Client-server communication
- [Sandbox System](documentation/development/Sandbox.md) - Development testing framework

## Downloads

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/kroiautilities)

### Current Development Version
| Minecraft | Version | Platform |
|-----------|---------|----------|
| ![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-green) | ![Version](https://img.shields.io/badge/v2.0.0__ALPHA-orange) | Fabric, NeoForge, Quilt |

### Legacy Versions
| Minecraft | Quilt |
|-----------|-------|
| ![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.4-lightgrey)    | [![Version](https://img.shields.io/badge/v1.2.0-lightgrey)][1.2.0-quilt-1.20.4]  |
| ![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.2-lightgrey)    | [![Version](https://img.shields.io/badge/v1.2.0-lightgrey)][1.2.0-quilt-1.20.2]  | 
| ![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-lightgrey)    | [![Version](https://img.shields.io/badge/v1.2.0-lightgrey)][1.2.0-quilt-1.20.1]  |
| ![Minecraft Version](https://img.shields.io/badge/Minecraft-1.19.3-lightgrey)    | [![Version](https://img.shields.io/badge/v1.2.0-lightgrey)][1.2.0-quilt-1.19.3]  |
| ![Minecraft Version](https://img.shields.io/badge/Minecraft-1.19.2-lightgrey)    | [![Version](https://img.shields.io/badge/v1.2.0-lightgrey)][1.2.0-quilt-1.19.2]  |


<!--	Links to Curseforge:	-->
[1.2.0-quilt-1.20.4]:https://www.curseforge.com/minecraft/mc-mods/kroiautilities/download/6158338
[1.2.0-quilt-1.20.2]:https://www.curseforge.com/minecraft/mc-mods/kroiautilities/download/6158336
[1.2.0-quilt-1.20.1]:https://www.curseforge.com/minecraft/mc-mods/kroiautilities/download/6158335
[1.2.0-quilt-1.19.3]:https://www.curseforge.com/minecraft/mc-mods/kroiautilities/download/6158327
[1.2.0-quilt-1.19.2]:https://www.curseforge.com/minecraft/mc-mods/kroiautilities/download/6158325







