# Networking Library

## Overview

The Networking library provides a comprehensive system for communication between client and server, as well as server-to-server communication. It simplifies packet handling and provides advanced features like request-response patterns and continuous data streaming.

## Prerequisites

Before using the Networking library, you should understand:
- Minecraft's client-server architecture
- Basic networking concepts (packets, serialization)
- Java functional interfaces (lambdas, callbacks)
- Asynchronous programming patterns (for ARRS)

## Features

- **Simple Packet Communication:** Send and receive custom data packets between server and client
- **Asynchronous Request Response System (ARRS):** Request-response pattern for client-server communication
- **Streaming System:** Continuous data transfer with automatic lifecycle management
- **Multi-Server Networking:** TCP-based Master/Slave server architecture (documentation pending)

## Content

### Core Networking
- [NetworkManager](NetworkManager.md) - Core network packet manager
  - [NetworkPacket](NetworkPacket.md) - Creating custom packets

### Advanced Features

#### Request-Response Pattern
- [Asynchronous Request Response System (ARRS)](ARRS.md) - Overview and concepts
  - [ARRS Generic Request](ARRSGenericRequest.md) - Creating custom requests

#### Streaming System
- [Stream System](StreamSystem.md) - Continuous data streaming
  - [GenericStream](GenericStream.md) - Creating custom stream implementations

### Multi-Server (Documentation Pending)
- TCP Master/Slave architecture
- Cross-server communication
- Server discovery and connection management

## Common Use Cases

1. **Simple Data Transfer:** Use NetworkPacket for one-way communication
2. **Data Queries:** Use ARRS to request data and receive responses
3. **Real-time Updates:** Use Stream System for continuous data flow
4. **Opening GUIs:** Send packet from server to client to trigger screen opening

## See Also

- [Documentation Index](../README.md) - Main documentation index
- [Sandbox System](../development/Sandbox.md) - Network testing examples
- [GUI Library](../gui/GuiLibrary.md) - For client-side GUI integration


