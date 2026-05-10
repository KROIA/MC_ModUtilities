# VerticalSlider

## Overview

The `VerticalSlider` is a draggable control that allows users to select a value along a vertical axis. It displays a slider handle that can be dragged top to bottom, with values ranging from 0.0 (top) to 1.0 (bottom). VerticalSliders are ideal for vertical value adjustments and space-efficient layouts.

**When to use:**
- Level indicators and progress displays
- Vertical mixing boards and audio controls
- Height, altitude, or elevation adjustments
- Any continuous value selection where vertical orientation is preferred

## Constructor

```java
// Default slider (positioned at 0,0)
VerticalSlider()

// Positioned slider
VerticalSlider(int x, int y, int width, int height)
```

**Parameters:**
- `x`, `y` - Position coordinates
- `width`, `height` - Size dimensions (width typically matches slider handle height)

## Key Methods

### Value Management
```java
void setSliderValue(double value)              // Set value (0.0 to 1.0)
double getSliderValue()                        // Get current value (0.0 to 1.0)
void setOnValueChanged(Consumer<Double> callback)   // Called when value changes
```

### Slider Handle
```java
void setSliderHeight(int height)               // Set slider handle height
int getSliderHeight()                          // Get slider handle height
```

### Interaction Control
```java
void setMovable(boolean movable)               // Enable/disable dragging
boolean isMovable()                            // Check if draggable
void setTriggerButton(int button)              // Set mouse button for dragging
int getTriggerButton()                         // Get trigger button
```

### Customization
```java
void setSliderLineColor(int color)             // Set background line color
void setIdleColor(int color)                   // Set handle idle color
void setHoverColor(int color)                  // Set handle hover color
void setPressedColor(int color)                // Set handle pressed color
void setSliderOutlineColor(int color)          // Set handle outline color
void setTooltipSupplier(Supplier<String> supplier)  // Set tooltip text provider
```

## Styling

### Default Colors
The slider inherits colors from the base `Slider` class:
- **Line Color**: Default outline color
- **Idle Color**: Default background color
- **Hover Color**: Default hover background color
- **Pressed Color**: Default focused background color
- **Outline Color**: Default outline color

### Customization Example
```java
VerticalSlider slider = new VerticalSlider(10, 10, 20, 200);
slider.setSliderLineColor(0xFF444444);
slider.setIdleColor(0xFF22AA66);
slider.setHoverColor(0xFF33BB77);
slider.setPressedColor(0xFF11AA55);
slider.setSliderOutlineColor(0xFF000000);
slider.setSliderHeight(15);
```

## Code Examples

### Audio Level Meter
```java
VerticalSlider levelMeter = new VerticalSlider(10, 10, 25, 200);
Label levelLabel = new Label("0 dB");

levelMeter.setMovable(false);  // Display only, not interactive
levelMeter.setSliderValue(0.0);

// Update from audio input
void updateAudioLevel(float level) {
    double normalizedLevel = Math.min(1.0, level);
    levelMeter.setSliderValue(normalizedLevel);
    
    int db = (int)(level * 100);
    levelLabel.setText(db + " dB");
    
    // Color changes based on level
    if (level > 0.9) {
        levelMeter.setIdleColor(0xFFAA2222);  // Red for clipping
    } else if (level > 0.7) {
        levelMeter.setIdleColor(0xFFAAAA22);  // Yellow for high
    } else {
        levelMeter.setIdleColor(0xFF22AA22);  // Green for normal
    }
}
```

### Elevator Control
```java
VerticalSlider elevatorSlider = new VerticalSlider(50, 50, 30, 300);
int maxFloor = 10;
int minFloor = 0;

Label floorLabel = new Label("Floor: 0");

elevatorSlider.setSliderValue(0.0);  // Start at ground floor
elevatorSlider.setOnValueChanged(value -> {
    // Invert value (0.0 = top = max floor, 1.0 = bottom = min floor)
    int floor = maxFloor - (int)(value * maxFloor);
    floorLabel.setText("Floor: " + floor);
    elevator.moveTo(floor);
});

elevatorSlider.setTooltipSupplier(() -> {
    int floor = maxFloor - (int)(elevatorSlider.getSliderValue() * maxFloor);
    return "Floor " + floor;
});
```

### Water Level Indicator
```java
VerticalSlider waterLevel = new VerticalSlider(10, 10, 40, 250);
Frame tankBackground = new Frame(10, 10, 40, 250);

tankBackground.setBackgroundColor(0xFF333333);
waterLevel.setBackgroundColor(0x00000000);  // Transparent
waterLevel.setMovable(false);

// Update water level (0 = empty, 1 = full)
void setWaterLevel(double percentage) {
    waterLevel.setSliderValue(1.0 - percentage);  // Invert: full = top
    
    // Blue tint for water
    int alpha = (int)(percentage * 128);
    waterLevel.setIdleColor(0x3366FF | (alpha << 24));
}
```

### Inverted Vertical Control
```java
// Value 0.0 = bottom, 1.0 = top (inverted from default)
VerticalSlider heightSlider = new VerticalSlider(100, 50, 25, 200);
double minHeight = 0;
double maxHeight = 256;

heightSlider.setSliderValue(0.5);
heightSlider.setOnValueChanged(value -> {
    // Invert the value
    double invertedValue = 1.0 - value;
    double height = minHeight + invertedValue * (maxHeight - minHeight);
    
    player.setY(height);
    System.out.println("Height: " + (int)height);
});
```

### Volume Fader Array
```java
// Create a mixing board with multiple vertical faders
List<VerticalSlider> faders = new ArrayList<>();
int faderCount = 8;

for (int i = 0; i < faderCount; i++) {
    int x = 10 + i * 35;
    VerticalSlider fader = new VerticalSlider(x, 10, 30, 200);
    fader.setSliderValue(0.7);  // Default to 70%
    
    int channelIndex = i;
    fader.setOnValueChanged(value -> {
        audioMixer.setChannelVolume(channelIndex, 1.0 - value);
    });
    
    Label channelLabel = new Label("Ch" + (i + 1));
    channelLabel.setPosition(x, 215);
    
    faders.add(fader);
    gui.addChild(fader);
    gui.addChild(channelLabel);
}
```

## Common Patterns

### Synchronized Sliders
```java
VerticalSlider masterSlider = new VerticalSlider(10, 10, 30, 200);
VerticalSlider slaveSlider = new VerticalSlider(50, 10, 30, 200);

CheckBox linkCheckbox = new CheckBox("Link Sliders");

masterSlider.setOnValueChanged(value -> {
    if (linkCheckbox.isChecked()) {
        slaveSlider.setSliderValue(value);
    }
});
```

### Progress Indicator
```java
VerticalSlider progressBar = new VerticalSlider(10, 10, 50, 300);
progressBar.setMovable(false);
progressBar.setSliderValue(1.0);  // Start at bottom (0%)

void updateProgress(double percentage) {
    progressBar.setSliderValue(1.0 - percentage);  // Inverted
    
    // Gradient color based on progress
    int r = (int)(255 * (1 - percentage));
    int g = (int)(255 * percentage);
    int color = 0xFF000000 | (r << 16) | (g << 8);
    progressBar.setIdleColor(color);
}
```

### Snap-to-Detents Slider
```java
VerticalSlider quantizedSlider = new VerticalSlider(10, 10, 25, 200);
int detents = 5;  // 5 discrete positions

quantizedSlider.setOnValueChanged(value -> {
    // Snap to nearest detent
    double snapped = Math.round(value * (detents - 1)) / (double)(detents - 1);
    quantizedSlider.setSliderValue(snapped);
});
```

## Best Practices

1. **Value Orientation**: Remember that 0.0 = top, 1.0 = bottom by default; invert if needed
2. **Handle Size**: Set slider height to 10-20 pixels for comfortable dragging
3. **Width**: Keep width between 20-50 pixels for good visibility and interaction
4. **Read-Only Indicators**: Use `setMovable(false)` for display-only level meters
5. **Visual Feedback**: Color-code sliders to indicate function (red = danger, green = safe)
6. **Labels**: Add labels above and/or below the slider to indicate min/max values
7. **Tooltips**: Provide tooltips with exact values when hovering over the handle

## Value Interpretation

By default, VerticalSlider maps values with top = 0.0 and bottom = 1.0. For most intuitive interfaces, you may want to invert this:

```java
// Standard (0 = top, 1 = bottom)
double topValue = 0.0;
double bottomValue = 1.0;

// Inverted (0 = bottom, 1 = top)
slider.setOnValueChanged(value -> {
    double invertedValue = 1.0 - value;
    // Use invertedValue for calculations
});
```

## Technical Notes

- The slider value is clamped to the range [0.0, 1.0]
- The slider handle is centered horizontally
- The background line is 1 pixel wide and centered horizontally
- Dragging is relative to where the user initially clicks on the handle
- The slider plays a UI click sound when grabbed
- The component extends the abstract `Slider` base class
- Mouse drag events update the value continuously while dragging
- Value 0.0 corresponds to the top position, 1.0 to the bottom position
