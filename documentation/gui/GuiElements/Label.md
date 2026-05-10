# Label

## Overview

The `Label` is a text display element used to show static or dynamic text in your GUI. It supports text alignment, padding, and text styling. Labels have no background by default and are lightweight components for displaying information.

**When to use:**
- Static text labels for UI elements
- Dynamic text displays (scores, status, information)
- Headers and section titles
- Descriptive text next to input elements

## Constructor

```java
Label()                    // Empty label
Label(String text)         // Label with initial text
```

## Key Methods

### Text Management
```java
void setText(String text)              // Set the label text
String getText()                       // Get the current text
```

### Alignment
```java
void setAlignment(Alignment alignment) // Set text alignment
Alignment getAlignment()               // Get current alignment
```

### Styling
```java
void setPadding(int padding)           // Set padding around text
int getPadding()                       // Get padding value
void setTextColor(int color)           // Set text color
void setTextFontScale(float scale)     // Set font scale
```

## Constants

```java
public static final int DEFAULT_HEIGHT = 15;  // Default label height
```

## Code Examples

### Basic Label
```java
Label title = new Label("Game Settings");
title.setTextColor(0xFFFFFFFF);
title.setTextFontScale(1.5f);
title.setAlignment(Alignment.CENTER);
gui.addChild(title);
```

### Dynamic Status Label
```java
Label statusLabel = new Label("Status: Ready");
statusLabel.setAlignment(Alignment.LEFT);

void updateStatus(String status) {
    statusLabel.setText("Status: " + status);
}
```

### Aligned Labels
```java
Label leftLabel = new Label("Left Aligned");
leftLabel.setAlignment(Alignment.LEFT);

Label centerLabel = new Label("Center Aligned");
centerLabel.setAlignment(Alignment.CENTER);

Label rightLabel = new Label("Right Aligned");
rightLabel.setAlignment(Alignment.RIGHT);
```

## Best Practices

1. **Null Safety**: The label handles null text by converting it to an empty string
2. **Background**: Labels have background and outline disabled by default for performance
3. **Alignment**: Choose alignment based on the label's position and purpose
4. **Padding**: Use padding to prevent text from touching element borders
5. **Font Scale**: Use larger scales (1.2-2.0) for headers, smaller (0.7-0.9) for details

## Technical Notes

- Default height is 15 pixels (Label.DEFAULT_HEIGHT)
- Background and outline rendering are disabled by default
- Text position is calculated in `layoutChanged()` based on alignment
- Gizmo rendering shows corner markers when mouse hovers over the label
