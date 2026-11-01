# TextureElement

## Overview

The `TextureElement` displays a texture/image in your GUI. It supports different rendering modes (stretch, fill) and can be rendered in different layers (background, foreground, tooltip, gizmo). Perfect for icons, backgrounds, decorative elements, and image displays.

**When to use:**
- Icons and image displays
- Background textures
- Decorative elements
- Custom graphics and sprites

## Constructor

```java
TextureElement()                                              // Empty texture element
TextureElement(GuiTexture texture)                            // Element with texture
TextureElement(String modID, String path, int width, int height)  // Element with texture from path
```

**Parameters:**
- `texture` - The GuiTexture to display
- `modID` - Mod ID for resource location
- `path` - Path to texture file
- `width`, `height` - Texture dimensions

## Enums

### Mode
```java
STRETCH   // Stretch texture to fill element bounds
FILL      // Tile texture to fill element bounds
```

### Layer
```java
BACKGROUND  // Render in background layer (before element content)
FOREGROUND  // Render in foreground layer (after element content)
TOOLTIP     // Render in tooltip layer
GIZMO       // Render in gizmo layer (debug/editor)
```

## Key Methods

### Texture Management
```java
void setTexture(GuiTexture texture)      // Set the texture
GuiTexture getTexture()                  // Get the texture
```

### Display Mode
```java
void setMode(Mode mode)                  // Set rendering mode (STRETCH or FILL)
Mode getMode()                           // Get current mode
```

### Layer Control
```java
void setLayer(Layer layer)               // Set rendering layer
Layer getLayer()                         // Get current layer
```

## Code Examples

### Basic Icon Display
```java
TextureElement icon = new TextureElement(
    "modutilities",
    "textures/gui/icon.png",
    16, 16
);
icon.setSize(32, 32);  // Display at 2x size
icon.setMode(Mode.STRETCH);
gui.addChild(icon);
```

### Background Texture
```java
TextureElement background = new TextureElement(
    "modutilities",
    "textures/gui/panel_background.png",
    256, 256
);
background.setSize(500, 400);
background.setMode(Mode.FILL);  // Tile the texture
background.setLayer(Layer.BACKGROUND);
panel.addChild(background);
```

### Button Icon
```java
Button button = new Button("Settings");
TextureElement gearIcon = new TextureElement(
    "modutilities",
    "textures/gui/gear.png",
    16, 16
);
gearIcon.setSize(16, 16);
gearIcon.setLayer(Layer.FOREGROUND);
button.addChild(gearIcon);
```

### Tiled Pattern
```java
TextureElement pattern = new TextureElement(
    "minecraft",
    "textures/block/stone.png",
    16, 16
);
pattern.setSize(200, 150);
pattern.setMode(Mode.FILL);  // Tiles the 16x16 texture
```

### Overlay Icon
```java
Frame itemFrame = new Frame(0, 0, 32, 32);
ItemView item = new ItemView(itemStack);

TextureElement badge = new TextureElement(
    "modutilities",
    "textures/gui/star.png",
    8, 8
);
badge.setPosition(24, 0);  // Top-right corner
badge.setLayer(Layer.FOREGROUND);

itemFrame.addChild(item);
itemFrame.addChild(badge);
```

## Common Patterns

### Stretched Logo
```java
TextureElement logo = new TextureElement(
    "mymod",
    "textures/gui/logo.png",
    256, 64
);
logo.setSize(512, 128);  // 2x scale
logo.setMode(Mode.STRETCH);
logo.setLayer(Layer.FOREGROUND);
```

### Status Indicator
```java
TextureElement statusIcon = new TextureElement();
statusIcon.setSize(16, 16);

void updateStatus(boolean online) {
    if (online) {
        statusIcon.setTexture(new GuiTexture("mymod", "textures/gui/online.png", 16, 16));
    } else {
        statusIcon.setTexture(new GuiTexture("mymod", "textures/gui/offline.png", 16, 16));
    }
}
```

### Animated Texture
```java
List<GuiTexture> frames = new ArrayList<>();
frames.add(new GuiTexture("mymod", "textures/gui/anim_0.png", 32, 32));
frames.add(new GuiTexture("mymod", "textures/gui/anim_1.png", 32, 32));
frames.add(new GuiTexture("mymod", "textures/gui/anim_2.png", 32, 32));

TextureElement animatedElement = new TextureElement(frames.get(0));
int currentFrame = 0;
int tickCounter = 0;

// In your update loop
void update() {
    tickCounter++;
    if (tickCounter >= 10) {  // Change frame every 10 ticks
        tickCounter = 0;
        currentFrame = (currentFrame + 1) % frames.size();
        animatedElement.setTexture(frames.get(currentFrame));
    }
}
```

## Best Practices

1. **Texture Size**: Ensure texture dimensions match the actual image file size
2. **Mode Selection**: Use STRETCH for scaling single images, FILL for repeating patterns
3. **Layer Order**: Use BACKGROUND for backgrounds, FOREGROUND for overlays
4. **Resource Location**: Textures must be in the mod's resources folder
5. **Transparency**: PNG files with alpha channels are supported
6. **Background/Outline**: Disabled by default for performance (no unnecessary rectangles)

## Technical Notes

- Background and outline rendering are disabled by default
- The element size is set to the texture dimensions by default
- STRETCH mode scales the entire texture to fit the element bounds
- FILL mode repeats the texture using `drawTextureFillArea()`
- Texture rendering respects the element's scissor bounds
- The layer determines when the texture is rendered in the rendering pipeline
- UV offsets from GuiTexture are applied during rendering
