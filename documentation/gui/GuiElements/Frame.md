# Frame

## Overview

The `Frame` is a simple container element that serves as a blank canvas for grouping and organizing other GUI elements. It provides no special functionality beyond the base `GuiElement` features, making it perfect for layout organization and visual grouping.

**When to use:**
- Container for grouping related elements
- Layout organization with layout managers
- Visual panels and sections
- Background panels with borders

## Constructor

```java
Frame()                                // Default frame at (0,0)
Frame(int x, int y, int width, int height)    // Positioned frame
```

**Parameters:**
- `x`, `y` - Position coordinates
- `width`, `height` - Size dimensions

## Key Methods

Frame inherits all methods from `GuiElement`:
```java
void addChild(GuiElement element)      // Add child element
void removeChild(GuiElement element)   // Remove child element
void setLayout(Layout layout)          // Apply layout manager
void setBackgroundColor(int color)     // Set background color
void setOutlineColor(int color)        // Set outline/border color
```

## Code Examples

### Simple Container
```java
Frame panel = new Frame(10, 10, 300, 200);
panel.setBackgroundColor(0xFF333333);
panel.setOutlineColor(0xFF666666);

Label title = new Label("Settings");
CheckBox option1 = new CheckBox("Option 1");
CheckBox option2 = new CheckBox("Option 2");

panel.addChild(title);
panel.addChild(option1);
panel.addChild(option2);

gui.addChild(panel);
```

### Frame with Layout
```java
Frame verticalPanel = new Frame(0, 0, 200, 300);
verticalPanel.setLayout(new LayoutVertical(5, 5, true, false));

verticalPanel.addChild(new Button("Button 1"));
verticalPanel.addChild(new Button("Button 2"));
verticalPanel.addChild(new Button("Button 3"));
// Children are automatically arranged vertically
```

### Nested Frames
```java
Frame mainFrame = new Frame(0, 0, 600, 400);
mainFrame.setBackgroundColor(0xFF1a1a1a);
mainFrame.setLayout(new LayoutHorizontal());

Frame leftPanel = new Frame();
leftPanel.setBackgroundColor(0xFF2a2a2a);
Frame rightPanel = new Frame();
rightPanel.setBackgroundColor(0xFF3a3a3a);

mainFrame.addChild(leftPanel);
mainFrame.addChild(rightPanel);
```

### Card-Style Frame
```java
Frame card = new Frame(10, 10, 250, 150);
card.setBackgroundColor(0xFFFFFFFF);
card.setOutlineColor(0xFFCCCCCC);
card.setEnableOutline(true);
card.setOutlineThickness(2);

Label cardTitle = new Label("Information Card");
cardTitle.setTextColor(0xFF000000);
cardTitle.setAlignment(Alignment.CENTER);

card.addChild(cardTitle);
```

### Dashboard Layout
```java
Frame dashboard = new Frame(0, 0, 800, 600);
dashboard.setLayout(new LayoutGrid(2, 2, true, true, 5, 5, Alignment.CENTER));

Frame metric1 = createMetricFrame("CPU", "45%");
Frame metric2 = createMetricFrame("Memory", "2.1 GB");
Frame metric3 = createMetricFrame("Network", "15 MB/s");
Frame metric4 = createMetricFrame("Disk", "120 GB");

dashboard.addChild(metric1);
dashboard.addChild(metric2);
dashboard.addChild(metric3);
dashboard.addChild(metric4);
```

## Common Patterns

### Scrollable Content Container
```java
Frame scrollContainer = new Frame(0, 0, 300, 400);
VerticalListView scrollList = new VerticalListView();
scrollContainer.addChild(scrollList);

for (int i = 0; i < 50; i++) {
    scrollList.addChild(new Label("Item " + i));
}
```

### Section with Header
```java
Frame section = new Frame(0, 0, 400, 250);
section.setBackgroundColor(0xFF2a2a2a);
section.setLayout(new LayoutVertical(0, 0, true, false));

Frame header = new Frame(0, 0, 400, 30);
header.setBackgroundColor(0xFF1a1a1a);
Label headerLabel = new Label("Section Title");
headerLabel.setAlignment(Alignment.CENTER);
header.addChild(headerLabel);

Frame content = new Frame();
// Add content elements to content frame

section.addChild(header);
section.addChild(content);
```

### Border Frame
```java
Frame borderFrame = new Frame(10, 10, 300, 200);
borderFrame.setEnableBackground(false);
borderFrame.setEnableOutline(true);
borderFrame.setOutlineColor(0xFF4488FF);
borderFrame.setOutlineThickness(3);
// Transparent background with colored border
```

## Best Practices

1. **Semantic Naming**: Use descriptive variable names (panel, container, card) for clarity
2. **Layout Managers**: Combine frames with layout managers for responsive designs
3. **Nesting**: Use nested frames to create complex hierarchical layouts
4. **Background Colors**: Use subtle color differences to visually separate sections
5. **Padding**: Apply padding through layouts or manual positioning for clean spacing

## Technical Notes

- Frame has no custom rendering beyond what GuiElement provides
- The `render()` and `layoutChanged()` methods are empty (can be overridden)
- Frames are lightweight and have minimal performance impact
- Ideal base class for creating custom container elements
- Background and outline rendering are inherited from GuiElement
