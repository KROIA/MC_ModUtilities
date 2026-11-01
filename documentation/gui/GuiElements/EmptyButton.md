# EmptyButton

## Overview

The `EmptyButton` is a minimal button implementation that provides click detection and visual feedback without any text or icon content. It serves as a base for creating custom button types and interactive areas. Unlike the standard Button, it has no label and provides hooks for pressed/released/held states.

**When to use:**
- Base class for custom buttons
- Clickable areas without visible content
- Interactive image buttons (combine with TextureElement)
- Touch-sensitive regions

## Constructor

```java
EmptyButton()                                    // Default empty button
EmptyButton(int x, int y, int width, int height) // Positioned empty button
EmptyButton(Runnable onFallingEdge)              // Button with click callback
EmptyButton(int x, int y, int width, int height, Runnable onFallingEdge)
```

**Parameters:**
- `x`, `y` - Position coordinates
- `width`, `height` - Size dimensions
- `onFallingEdge` - Callback when button is clicked (mouse down)

## Key Methods

### Event Callbacks
```java
void setOnFallingEdge(Runnable callback)    // Called when button is pressed
void setOnRisingEdge(Runnable callback)     // Called when button is released
void setOnDown(Runnable callback)           // Called while button is held down
```

### State Management
```java
void setClickable(boolean clickable)        // Enable/disable button interaction
boolean isClickable()                       // Check if button is clickable
boolean isPressed()                         // Check if button is currently pressed
void setTriggerButton(int button)           // Set mouse button (0=left, 1=right, 2=middle)
int getTriggerButton()                      // Get trigger button
```

### Styling
```java
void setHoverColor(int color)               // Set color when mouse hovers
void setPressedColor(int color)             // Set color when pressed
void setBackgroundColor(int color)          // Set idle background color
int getHoverColor()                         // Get hover color
int getPressedColor()                       // Get pressed color
```

## Default Colors

```java
backgroundColor = setBrightness(DEFAULT_BACKGROUND_COLOR, 0.8f)
hoverColor = setBrightness(backgroundColor, 0.8f)
pressedColor = setBrightness(backgroundColor, 0.6f)
outlineColor = setBrightness(backgroundColor, 0.4f)
```

## Code Examples

### Basic Click Handler
```java
EmptyButton clickArea = new EmptyButton(10, 10, 100, 50);
clickArea.setOnFallingEdge(() -> {
    System.out.println("Clicked!");
});
gui.addChild(clickArea);
```

### Image Button
```java
EmptyButton imageButton = new EmptyButton(0, 0, 64, 64);
TextureElement icon = new TextureElement("mymod", "textures/gui/play.png", 64, 64);

imageButton.setOnFallingEdge(() -> {
    startGame();
});

imageButton.addChild(icon);
gui.addChild(imageButton);
```

### Hold Button
```java
EmptyButton holdButton = new EmptyButton(10, 10, 80, 80);
int[] holdCounter = {0};

holdButton.setOnFallingEdge(() -> {
    holdCounter[0] = 0;
});

holdButton.setOnDown(() -> {
    holdCounter[0]++;
    if (holdCounter[0] >= 20) {  // Held for 1 second (20 ticks)
        performAction();
        holdCounter[0] = 0;
    }
});

holdButton.setOnRisingEdge(() -> {
    holdCounter[0] = 0;
});
```

### Toggle Button
```java
EmptyButton toggleButton = new EmptyButton(10, 10, 60, 30);
boolean[] isOn = {false};

toggleButton.setOnFallingEdge(() -> {
    isOn[0] = !isOn[0];
    if (isOn[0]) {
        toggleButton.setBackgroundColor(0xFF44AA44);
    } else {
        toggleButton.setBackgroundColor(0xFFAA4444);
    }
});
```

### Right-Click Button
```java
EmptyButton rightClickButton = new EmptyButton(10, 10, 100, 40);
rightClickButton.setTriggerButton(1);  // Right mouse button

rightClickButton.setOnFallingEdge(() -> {
    showContextMenu();
});
```

### Disabled Button
```java
EmptyButton button = new EmptyButton(10, 10, 120, 40);
CheckBox enableCheckbox = new CheckBox("Enable Button");

button.setClickable(false);
button.setBackgroundColor(0xFF666666);

enableCheckbox.setOnStateChanged(enabled -> {
    button.setClickable(enabled);
    button.setBackgroundColor(enabled ? 0xFF2266AA : 0xFF666666);
});
```

## Common Patterns

### Custom Styled Button
```java
public class ColoredButton extends EmptyButton {
    public ColoredButton(String label, int color) {
        super();
        Label text = new Label(label);
        text.setAlignment(Alignment.CENTER);
        addChild(text);
        
        setBackgroundColor(color);
        setHoverColor(ColorUtilities.setBrightness(color, 0.8f));
        setPressedColor(ColorUtilities.setBrightness(color, 0.6f));
    }
}
```

### Interactive Area
```java
// Create an invisible clickable area
EmptyButton invisibleArea = new EmptyButton(100, 100, 200, 150);
invisibleArea.setEnableBackground(false);
invisibleArea.setEnableOutline(false);

invisibleArea.setOnFallingEdge(() -> {
    System.out.println("Secret area clicked!");
});
```

### Multi-State Button
```java
EmptyButton stateButton = new EmptyButton(10, 10, 80, 80);
int[] state = {0};
String[] states = {"Off", "Low", "High"};

stateButton.setOnFallingEdge(() -> {
    state[0] = (state[0] + 1) % states.length;
    updateButtonAppearance(stateButton, state[0]);
});
```

## Best Practices

1. **Event Choice**: Use `onFallingEdge` for clicks, `onDown` for held, `onRisingEdge` for release
2. **Visual Feedback**: Always provide visual feedback (color changes) for button states
3. **Clickable State**: Disable buttons (setClickable(false)) rather than hiding them when unavailable
4. **Sound Feedback**: The button plays UI_BUTTON_CLICK sound automatically on press
5. **Trigger Button**: Default is left-click (0); set explicitly if you need right/middle-click
6. **Custom Content**: Add child elements (TextureElement, Label) for visual content

## Event Sequence

When a user interacts with the button:
1. **Mouse Down**: `onFallingEdge` is called, `isPressed` becomes true
2. **While Held**: `onDown` is called each frame
3. **Mouse Up**: `onRisingEdge` is called, `isPressed` becomes false

## Technical Notes

- The button only responds to the configured trigger button (default: left mouse)
- Sound (UI_BUTTON_CLICK) is played automatically when pressed
- The `isPressed` state is automatically reset when mouse is released
- The button tracks mouse button state and only responds when clickable
- Background color changes automatically based on state (idle/hover/pressed)
- The button extends GuiElement and inherits all its capabilities
