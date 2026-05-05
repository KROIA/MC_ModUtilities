# CheckBox

## Overview

The `CheckBox` is a toggleable UI element that allows users to select or deselect an option. It displays a label and a checkbox frame on the right side, with a visual indicator when checked. CheckBoxes are ideal for binary choices, settings, and multi-select interfaces.

**When to use:**
- Boolean settings (enable/disable features)
- Multi-select lists where users can choose multiple options
- Filters and toggles in configuration interfaces
- Consent forms and agreements

## Constructor

```java
// Basic checkbox with text
CheckBox(String text)

// Positioned checkbox
CheckBox(int x, int y, int width, int height, String text)

// Checkbox with state change callback
CheckBox(String text, Consumer<Boolean> onStateChanged)

// Positioned checkbox with callback
CheckBox(int x, int y, int width, int height, String text, Consumer<Boolean> onStateChanged)
```

**Parameters:**
- `text` - The label text displayed next to the checkbox
- `x`, `y` - Position coordinates
- `width`, `height` - Size dimensions
- `onStateChanged` - Callback invoked when the checkbox state changes (receives the new checked state)

## Key Methods

### State Management
```java
void setChecked(boolean checked)           // Set the checked state
boolean isChecked()                        // Get the current checked state
void setCheckable(boolean checkable)       // Enable/disable interaction
boolean isCheckable()                      // Check if the checkbox is interactable
```

### Event Callbacks
```java
void setOnClick(Runnable onClick)                    // Called on any click
void setOnStateChanged(Consumer<Boolean> callback)   // Called when state changes
void setOnChecked(Runnable onChecked)                // Called when checked
void setOnUnchecked(Runnable onUnchecked)            // Called when unchecked
```

### Customization
```java
void setTextAlignment(Alignment alignment)        // Set label text alignment
void setHoverColor(int color)                     // Set hover background color
void setCheckBoxFrameColor(int color)             // Set checkbox frame color
void setCheckBoxCheckedColor(int color)           // Set check indicator color
void setTriggerButton(int triggerButton)          // Set mouse button (default: 0 = left)
int getTriggerButton()                            // Get the trigger button
```

### Text Styling
```java
void setTextColor(int color)                 // Set label text color
int getTextColor()                           // Get label text color
void setTextFontScale(float scale)           // Set label font scale
float getTextFontScale()                     // Get label font scale
```

## Events and Callbacks

The CheckBox provides multiple callback hooks for different interaction scenarios:

1. **onStateChanged** - Receives the new boolean state, ideal for most use cases
2. **onChecked** - Called only when the checkbox becomes checked
3. **onUnchecked** - Called only when the checkbox becomes unchecked
4. **onClick** - Called on every click, regardless of state change

## Styling

### Default Colors
- **Hover Color**: Semi-transparent white overlay
- **Frame Color**: Black outline around the checkbox
- **Checked Color**: Black fill for the check indicator

### Customization Example
```java
CheckBox checkbox = new CheckBox("Enable Feature");
checkbox.setHoverColor(0x803366FF);              // Blue hover
checkbox.setCheckBoxFrameColor(0xFF2244AA);      // Dark blue frame
checkbox.setCheckBoxCheckedColor(0xFF22AA44);    // Green check
checkbox.setTextColor(0xFFFFFFFF);               // White text
```

## Code Examples

### Basic Usage
```java
CheckBox enabledCheckbox = new CheckBox("Enable Sound Effects");
enabledCheckbox.setOnStateChanged(isChecked -> {
    SoundManager.setEnabled(isChecked);
    System.out.println("Sound effects: " + (isChecked ? "ON" : "OFF"));
});
gui.addChild(enabledCheckbox);
```

### Settings Panel
```java
// Create multiple checkboxes for settings
CheckBox autoSave = new CheckBox(10, 10, 200, 20, "Auto-save");
CheckBox showTooltips = new CheckBox(10, 35, 200, 20, "Show Tooltips");
CheckBox debugMode = new CheckBox(10, 60, 200, 20, "Debug Mode");

// Set initial states
autoSave.setChecked(config.getAutoSave());
showTooltips.setChecked(config.getShowTooltips());
debugMode.setChecked(config.getDebugMode());

// Add callbacks
autoSave.setOnStateChanged(checked -> config.setAutoSave(checked));
showTooltips.setOnStateChanged(checked -> config.setShowTooltips(checked));
debugMode.setOnStateChanged(checked -> config.setDebugMode(checked));

gui.addChild(autoSave);
gui.addChild(showTooltips);
gui.addChild(debugMode);
```

### Conditional Enabling
```java
CheckBox masterCheckbox = new CheckBox("Enable Advanced Features");
CheckBox featureA = new CheckBox("Feature A");
CheckBox featureB = new CheckBox("Feature B");

// Initially disable sub-features
featureA.setCheckable(false);
featureB.setCheckable(false);

masterCheckbox.setOnStateChanged(isChecked -> {
    featureA.setCheckable(isChecked);
    featureB.setCheckable(isChecked);
    if (!isChecked) {
        featureA.setChecked(false);
        featureB.setChecked(false);
    }
});
```

## Common Patterns

### Toggle Group (Radio Button Behavior)
```java
List<CheckBox> checkboxes = new ArrayList<>();
checkboxes.add(new CheckBox("Option 1"));
checkboxes.add(new CheckBox("Option 2"));
checkboxes.add(new CheckBox("Option 3"));

// Make them behave like radio buttons
for (CheckBox checkbox : checkboxes) {
    checkbox.setOnChecked(() -> {
        for (CheckBox other : checkboxes) {
            if (other != checkbox) {
                other.setChecked(false);
            }
        }
    });
}
```

### State Persistence
```java
CheckBox checkbox = new CheckBox("Remember Password");

// Load saved state
checkbox.setChecked(preferences.getBoolean("remember_password", false));

// Save on change
checkbox.setOnStateChanged(isChecked -> {
    preferences.putBoolean("remember_password", isChecked);
    preferences.save();
});
```

### Visual Feedback
```java
CheckBox checkbox = new CheckBox("Agree to Terms");
checkbox.setOnChecked(() -> {
    submitButton.setEnabled(true);
    submitButton.setBackgroundColor(0xFF44AA44);
});
checkbox.setOnUnchecked(() -> {
    submitButton.setEnabled(false);
    submitButton.setBackgroundColor(0xFFAA4444);
});
```

## Best Practices

1. **Clear Labels**: Use descriptive text that clearly indicates what the checkbox controls
2. **Default States**: Set reasonable default states based on the most common use case
3. **Callback Choice**: Use `setOnStateChanged` for most cases; use `onChecked`/`onUnchecked` when you need separate logic
4. **Visual Feedback**: Consider customizing colors to match your application theme
5. **Accessibility**: Ensure sufficient contrast between the checkbox and background
6. **Group Related Items**: Place related checkboxes close together in a logical order

## Technical Notes

- The checkbox frame is positioned on the right side of the element
- The check indicator is rendered as a filled square inside the checkbox frame
- Click detection works on the entire checkbox area by default
- The checkbox automatically plays a UI sound on interaction
- The component extends `GuiElement` and supports all standard GUI element features
