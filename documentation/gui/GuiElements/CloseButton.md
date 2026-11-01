# CloseButton

## Overview

The `CloseButton` is a specialized button that displays an "X" symbol and is styled with red colors. It's specifically designed for closing windows, dialogs, and panels. The X is drawn using vertex buffers for crisp rendering at any size.

**When to use:**
- Window/dialog close buttons
- Panel dismiss buttons
- Modal close controls
- Any interface that needs a standard close action

## Constructor

```java
CloseButton(Runnable onFallingEdge)
```

**Parameters:**
- `onFallingEdge` - Callback invoked when the button is clicked

## Key Methods

The CloseButton inherits all methods from `Button`:

```java
void setOnFallingEdge(Runnable callback)    // Set click callback
void setSize(int width, int height)         // Resize button (updates X shape)
void setOutlineColor(int color)             // Set color of the X symbol
```

## Default Styling

```java
backgroundColor = 0xFFf55a42     // Light red
hoverColor = 0xFFe03d24          // Medium red
pressedColor = 0xFFde2b10        // Dark red
outlineColor = 0xFFde2510        // X symbol color (dark red)
defaultSize = 20x20 pixels       // Default dimensions
```

## Code Examples

### Basic Window Close
```java
Frame window = new Frame(100, 100, 400, 300);
Label title = new Label("Settings Window");

CloseButton closeBtn = new CloseButton(() -> {
    gui.removeChild(window);
});
closeBtn.setPosition(window.getWidth() - 25, 5);

window.addChild(title);
window.addChild(closeBtn);
gui.addChild(window);
```

### Dialog with Close
```java
Frame dialog = new Frame(0, 0, 300, 200);
dialog.setBackgroundColor(0xFF2a2a2a);

Label message = new Label("Are you sure?");
message.setAlignment(Alignment.CENTER);

CloseButton closeBtn = new CloseButton(() -> {
    closeDialog();
});
closeBtn.setPosition(275, 5);

dialog.addChild(message);
dialog.addChild(closeBtn);
```

### Custom Sized Close Button
```java
CloseButton largeCloseBtn = new CloseButton(() -> {
    handleClose();
});
largeCloseBtn.setSize(30, 30);  // Larger close button
largeCloseBtn.setPosition(windowWidth - 35, 5);
```

### Close with Confirmation
```java
CloseButton closeBtn = new CloseButton(() -> {
    if (hasUnsavedChanges()) {
        showConfirmDialog("Unsaved changes. Close anyway?", () -> {
            closeWindow();
        });
    } else {
        closeWindow();
    }
});
```

### Styled Close Button
```java
CloseButton customClose = new CloseButton(() -> {
    closePanel();
});

// Custom colors
customClose.setBackgroundColor(0xFF3366CC);
customClose.setHoverColor(0xFF2255BB);
customClose.setPressedColor(0xFF1144AA);
customClose.setOutlineColor(0xFFFFFFFF);  // White X
```

## Common Patterns

### Top-Right Corner Positioning
```java
// Position close button in top-right corner of a frame
CloseButton closeBtn = new CloseButton(this::close);
int padding = 5;
closeBtn.setPosition(
    parentFrame.getWidth() - closeBtn.getWidth() - padding,
    padding
);
```

### Panel with Header and Close
```java
Frame panel = new Frame(0, 0, 400, 300);
Frame header = new Frame(0, 0, 400, 30);
header.setBackgroundColor(0xFF1a1a1a);

Label headerTitle = new Label("Panel Title");
headerTitle.setAlignment(Alignment.LEFT);
headerTitle.setPosition(10, 7);

CloseButton closeBtn = new CloseButton(() -> {
    panel.setEnabled(false);
});
closeBtn.setPosition(375, 5);

header.addChild(headerTitle);
header.addChild(closeBtn);
panel.addChild(header);
```

### Close with Animation
```java
CloseButton animatedClose = new CloseButton(() -> {
    fadeOutAndClose(window);
});

void fadeOutAndClose(Frame window) {
    // Animate opacity from 255 to 0
    AnimationUtilities.fadeOut(window, 20, () -> {
        gui.removeChild(window);
    });
}
```

## Best Practices

1. **Positioning**: Place in the top-right corner for consistency with standard UIs
2. **Size**: Default 20x20 is good for most windows; use 15x15 for compact interfaces
3. **Padding**: Leave 5px padding from window edges
4. **Color Consistency**: The default red colors are recognizable; avoid changing unless needed
5. **Confirmation**: For destructive actions, confirm before closing
6. **Keyboard**: Also handle Escape key for closing the same window

## X Symbol Rendering

The X is drawn using two diagonal lines made of vertex quads:
- Line 1: Top-left to bottom-right
- Line 2: Top-right to bottom-left
- Thickness is determined by `outlineThickness` (default: 1px)
- The X automatically scales when the button is resized
- Corners have small caps for a polished appearance

## Technical Notes

- Extends `Button` class
- Default size set to 20x20 in the constructor
- The X shape is drawn using `VertexBuffer` objects for GPU-accelerated rendering
- `updateShape()` is called when size or outline color changes
- Outline thickness is inherited from the Button class
- The X symbol color matches the outline color
- Background color provides visual feedback for hover/press states
