# MC_ModUtilities

## About
This is a comprehensive utilities library mod that provides common functionality for Minecraft mod development.
It is a **required dependency** of BankSystem and StockMarket: install it alongside those mods on the client and the server.

**Current Version**: v2.2.0 (in development)  
**Supported Platforms**: Fabric, NeoForge  
**Supported Minecraft Versions**: 1.21.1 – 1.21.11, 26.1 (26.1.2), 26.2, 26.3

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

## Requirements

Every Minecraft version has its own build; install the matching Architectury API and loader.

| Minecraft | Architectury API | NeoForge | Fabric Loader | Fabric API | Java |
|---|---|---|---|---|---|
| 1.21.1  | 13.0.8  | 21.1.84       | 0.16.9  | 0.114.0+1.21.1  | 21 |
| 1.21.2  | 14.0.4  | 21.2.1-beta   | 0.16.9  | 0.106.1+1.21.2  | 21 |
| 1.21.3  | 14.0.4  | 21.3.100      | 0.16.9  | 0.114.1+1.21.3  | 21 |
| 1.21.4  | 15.0.3  | 21.4.161      | 0.16.14 | 0.119.4+1.21.4  | 21 |
| 1.21.5  | 16.1.4  | 21.5.101      | 0.16.14 | 0.128.2+1.21.5  | 21 |
| 1.21.6  | 17.0.6  | 21.6.20-beta  | 0.16.14 | 0.128.2+1.21.6  | 21 |
| 1.21.7  | 17.0.8  | 21.7.25-beta  | 0.16.14 | 0.129.0+1.21.7  | 21 |
| 1.21.8  | 17.0.8  | 21.8.57       | 0.16.14 | 0.136.1+1.21.8  | 21 |
| 1.21.9  | 18.0.5  | 21.9.16-beta  | 0.17.3  | 0.134.1+1.21.9  | 21 |
| 1.21.10 | 18.0.8  | 21.10.64      | 0.17.3  | 0.138.4+1.21.10 | 21 |
| 1.21.11 | 19.0.1  | 21.11.48      | 0.18.6  | 0.141.6+1.21.11 | 21 |
| 26.1.2  | 20.1.17 | 26.1.2.126    | 0.19.5  | 0.155.3+26.1.2  | 25 |
| 26.2    | 21.1.12 | 26.2.0.93     | 0.19.5  | 0.161.0+26.2    | 25 |
| 26.3    | 22.0.5  | 26.3.0.101    | 0.19.5  | 0.165.0+26.3    | 25 |

Versions are the ones each build is compiled against; newer patch releases of the same line usually work.

- [Architectury API](https://www.curseforge.com/minecraft/mc-mods/architectury-api)
- [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
- [NeoForge](https://neoforged.net/)

## Documentation

**[Complete Documentation Index](documentation/README.md)** - Start here for organized access to all documentation

### Quick Links
- [GUI Library](documentation/gui/GuiLibrary.md) - Component-based UI system
- [Networking Library](documentation/networking/Networking.md) - Client-server communication
- [Multi-Version Compatibility](documentation/development/MultiVersionCompat.md) - Compat helpers for code that builds on every supported MC version
- [Sandbox System](documentation/development/Sandbox.md) - Development testing framework

## Downloads

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/kroiautilities)

### Current Development Version
v2.2.0 for Minecraft 1.21.1 – 1.21.11, 26.1, 26.2 and 26.3 (Fabric, NeoForge) is **in development**; download links follow with the release.

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

## Changelog

All notable changes are documented in version-specific files under `changelog/`.

| Version | Date | Notes |
|---|---|---|
| [2.2.0](changelog/v2.2.0.md) | In Development | Multi-version support (1.21.1 – 26.3), compat helpers, placeholder registry, automated in-game tests |
| [2.1.2](changelog/v2.1.2.md) | 2026-10-25 | Test runs written to log files, setup/teardown failure handling |
| [2.1.1](changelog/v2.1.1.md) | 2026-08-20 | `ListView` viewport culling, `shouldRenderChild` hook |
| [2.1.0](changelog/v2.1.0.md) | 2026-08-15 | `MultiLineTextBox`, auto-scrolling `Label`, `TextBox` improvements |
| [2.0.7](changelog/v2.0.7.md) | 2026-07-29 | Maintenance release |
| [2.0.6](changelog/v2.0.6.md) | 2026-07-27 | Display renderer fixes, JEI overlay hiding, `TextBox` crash fix |
| [2.0.5](changelog/v2.0.5.md) | 2026-07-25 | Fabric dedicated server crash fix |
| [2.0.4](changelog/v2.0.4.md) | 2026-07-21 | Multi-server sender identity hardening (wire format change), `ExpandablePanel` |
| [2.0.3](changelog/v2.0.3.md) | 2026-07-14 | Maintenance release — version sync |
| [2.0.2](changelog/v2.0.2.md) | 2026-06-07 | Maintenance release — version sync |
| [2.0.1](changelog/v2.0.1.md) | 2026-05-21 | |
| [2.0.0_ALPHA](changelog/v2.0.0_ALPHA.md) | 2026-05-10 | Test suite framework, documentation, 80+ bug fixes |

Previous versions (pre-1.21.1) were not documented in this format.







