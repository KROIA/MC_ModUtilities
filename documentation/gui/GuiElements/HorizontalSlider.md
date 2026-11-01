# HorizontalSlider

## Overview

The `HorizontalSlider` is a draggable control that allows users to select a value along a horizontal axis. It displays a slider handle that can be dragged left to right, with values ranging from 0.0 to 1.0. HorizontalSliders are ideal for continuous value adjustment interfaces.

**When to use:**
- Volume controls and audio levels
- Brightness, opacity, or transparency adjustments
- Percentage inputs (0-100%)
- Any continuous value selection within a range

## Constructor

```java
// Default slider (positioned at 0,0)
HorizontalSlider()

// Positioned slider
HorizontalSlider(int x, int y, int width, int height)
```

**Parameters:**
- `x`, `y` - Position coordinates
- `width`, `height` - Size dimensions (height typically matches slider handle width)

## Key Methods

### Value Management
```java
void setSliderValue(double value)              // Set value (0.0 to 1.0)
double getSliderValue()                        // Get current value (0.0 to 1.0)
void setOnValueChanged(Consumer<Double> callback)   // Called when value changes
```

### Slider Handle
```java
void setSliderWidth(int width)                 // Set slider handle width
int getSliderWidth()                           // Get slider handle width
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
HorizontalSlider slider = new HorizontalSlider(10, 10, 200, 20);
slider.setSliderLineColor(0xFF444444);
slider.setIdleColor(0xFF2266AA);
slider.setHoverColor(0xFF3377BB);
slider.setPressedColor(0xFF1155AA);
slider.setSliderOutlineColor(0xFF000000);
slider.setSliderWidth(15);
```

## Code Examples

### Volume Control
```java
HorizontalSlider volumeSlider = new HorizontalSlider(10, 10, 200, 20);
Label volumeLabel = new Label("Volume: 50%");

volumeSlider.setSliderValue(0.5);  // 50%
volumeSlider.setOnValueChanged(value -> {
    int percentage = (int)(value * 100);
    volumeLabel.setText("Volume: " + percentage + "%");
    audioManager.setVolume((float)value);
});

volumeSlider.setTooltipSupplier(() -> {
    int percentage = (int)(volumeSlider.getSliderValue() * 100);
    return percentage + "%";
});

gui.addChild(volumeSlider);
gui.addChild(volumeLabel);
```

### Brightness Adjustment
```java
HorizontalSlider brightnessSlider = new HorizontalSlider(10, 40, 250, 18);
brightnessSlider.setSliderValue(0.75);

brightnessSlider.setOnValueChanged(value -> {
    float brightness = (float)value;
    screen.setBrightness(brightness);
});

// Visual feedback - lighter slider when brighter
brightnessSlider.setOnValueChanged(value -> {
    int brightness = (int)(value * 255);
    int color = 0xFF000000 | (brightness << 16) | (brightness << 8) | brightness;
    brightnessSlider.setIdleColor(color);
});
```

### Range Value Slider
```java
// Convert slider value (0-1) to custom range (min-max)
HorizontalSlider temperatureSlider = new HorizontalSlider(10, 70, 300, 20);
int minTemp = -20;
int maxTemp = 40;

Label tempLabel = new Label("Temperature: 10°C");

temperatureSlider.setSliderValue(0.5);  // Middle of range
temperatureSlider.setOnValueChanged(value -> {
    int temperature = (int)(minTemp + value * (maxTemp - minTemp));
    tempLabel.setText("Temperature: " + temperature + "°C");
});

temperatureSlider.setTooltipSupplier(() -> {
    int temp = (int)(minTemp + temperatureSlider.getSliderValue() * (maxTemp - minTemp));
    return temp + "°C";
});
```

### Linked Sliders
```java
HorizontalSlider redSlider = new HorizontalSlider(10, 10, 200, 15);
HorizontalSlider greenSlider = new HorizontalSlider(10, 30, 200, 15);
HorizontalSlider blueSlider = new HorizontalSlider(10, 50, 200, 15);
Frame colorPreview = new Frame(220, 10, 50, 50);

Runnable updateColor = () -> {
    int r = (int)(redSlider.getSliderValue() * 255);
    int g = (int)(greenSlider.getSliderValue() * 255);
    int b = (int)(blueSlider.getSliderValue() * 255);
    int color = 0xFF000000 | (r << 16) | (g << 8) | b;
    colorPreview.setBackgroundColor(color);
};

redSlider.setOnValueChanged(v -> updateColor.run());
greenSlider.setOnValueChanged(v -> updateColor.run());
blueSlider.setOnValueChanged(v -> updateColor.run());
```

### Snap-to-Grid Slider
```java
HorizontalSlider quantizedSlider = new HorizontalSlider(10, 10, 200, 20);
int steps = 10;  // 10 discrete values

quantizedSlider.setOnValueChanged(value -> {
    // Round to nearest step
    double snapped = Math.round(value * steps) / (double)steps;
    quantizedSlider.setSliderValue(snapped);
    
    int stepValue = (int)(snapped * steps);
    System.out.println("Step: " + stepValue + "/" + steps);
});
```

## Common Patterns

### Two-Thumb Range Slider
```java
HorizontalSlider minSlider = new HorizontalSlider(10, 10, 200, 20);
HorizontalSlider maxSlider = new HorizontalSlider(10, 10, 200, 20);

minSlider.setSliderValue(0.2);
maxSlider.setSliderValue(0.8);

minSlider.setOnValueChanged(value -> {
    if (value > maxSlider.getSliderValue()) {
        minSlider.setSliderValue(maxSlider.getSliderValue());
    }
    updateRange(minSlider.getSliderValue(), maxSlider.getSliderValue());
});

maxSlider.setOnValueChanged(value -> {
    if (value < minSlider.getSliderValue()) {
        maxSlider.setSliderValue(minSlider.getSliderValue());
    }
    updateRange(minSlider.getSliderValue(), maxSlider.getSliderValue());
});
```

### Logarithmic Scale
```java
HorizontalSlider logSlider = new HorizontalSlider(10, 10, 250, 20);
double minValue = 1;
double maxValue = 10000;

logSlider.setOnValueChanged(value -> {
    // Convert linear slider to logarithmic scale
    double logMin = Math.log10(minValue);
    double logMax = Math.log10(maxValue);
    double logValue = logMin + value * (logMax - logMin);
    double actualValue = Math.pow(10, logValue);
    
    System.out.println("Value: " + (int)actualValue);
});
```

### Disabled State Slider
```java
HorizontalSlider slider = new HorizontalSlider(10, 10, 200, 20);
CheckBox enableCheckbox = new CheckBox("Enable Adjustment");

slider.setMovable(false);
slider.setIdleColor(0xFF666666);

enableCheckbox.setOnStateChanged(enabled -> {
    slider.setMovable(enabled);
    slider.setIdleColor(enabled ? 0xFF2266AA : 0xFF666666);
});
```

## Best Practices

1. **Value Range**: The slider uses 0.0-1.0 internally; map to your actual range in the callback
2. **Tooltip**: Provide a tooltip showing the current value for better user feedback
3. **Handle Size**: Set slider width to 10-20 pixels for comfortable dragging
4. **Visual Feedback**: Update colors based on the value for intuitive understanding
5. **Accessibility**: Ensure sufficient contrast between the handle and background
6. **Label Pairing**: Always pair sliders with labels showing the current value
7. **Precision**: For precise values, consider adding number input boxes alongside sliders

## Technical Notes

- The slider value is clamped to the range [0.0, 1.0]
- The slider handle is centered vertically
- The background line is 1 pixel tall and centered vertically
- Dragging is relative to where the user initially clicks on the handle
- The slider plays a UI click sound when grabbed
- The component extends the abstract `Slider` base class
- Mouse drag events update the value continuously while dragging
