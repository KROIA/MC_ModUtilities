# GUI Layout System

## Overview

The Layout system provides automatic positioning and sizing of child elements within a container. It eliminates the need for manual element positioning and creates responsive, maintainable GUI layouts. The library includes several built-in layout managers for common layout patterns.

**Benefits:**
- Automatic child element positioning
- Responsive layouts that adapt to container size changes
- Consistent spacing and padding
- Simplified GUI code
- Easy to switch between layout strategies

## Layout Base Class

All layouts extend the abstract `Layout` class which provides common properties:

```java
public abstract class Layout {
    public boolean enabled = true;         // Enable/disable layout
    public int padding = DEFAULT_PADDING;  // Outer padding
    public int spacing = DEFAULT_PADDING;  // Spacing between elements
    public boolean stretchX = false;       // Stretch children horizontally
    public boolean stretchY = false;       // Stretch children vertically
    
    public abstract void apply(GuiElement element);
}
```

### Common Properties

- **enabled**: When false, the layout is not applied
- **padding**: Space between container edges and first/last child
- **spacing**: Space between adjacent children
- **stretchX**: If true, children are resized to fill available width
- **stretchY**: If true, children are resized to fill available height

## Layout Types

### 1. LayoutHorizontal

Arranges child elements in a horizontal row from left to right.

**Constructor:**
```java
LayoutHorizontal()
LayoutHorizontal(int padding, int spacing, boolean stretchX, boolean stretchY)
```

**Behavior:**
- Elements are positioned left to right
- Y position is set to `padding`
- If `stretchX` is true, all children get equal width: `(containerWidth - 2*padding + spacing) / childCount - spacing`
- If `stretchY` is true, all children get height: `containerHeight - 2*padding`

**Example:**
```java
Frame toolbar = new Frame(0, 0, 400, 40);
LayoutHorizontal layout = new LayoutHorizontal();
layout.padding = 5;
layout.spacing = 10;
layout.stretchY = true;  // Buttons fill height

toolbar.setLayout(layout);
toolbar.addChild(new Button("File"));
toolbar.addChild(new Button("Edit"));
toolbar.addChild(new Button("View"));
// Buttons are automatically arranged horizontally
```

### 2. LayoutVertical

Arranges child elements in a vertical column from top to bottom.

**Constructor:**
```java
LayoutVertical()
LayoutVertical(int padding, int spacing, boolean stretchX, boolean stretchY)
```

**Behavior:**
- Elements are positioned top to bottom
- X position is set to `padding`
- If `stretchX` is true, all children get width: `containerWidth - 2*padding`
- If `stretchY` is true, all children get equal height: `(containerHeight - 2*padding + spacing) / childCount - spacing`

**Example:**
```java
Frame sidebar = new Frame(0, 0, 150, 600);
LayoutVertical layout = new LayoutVertical();
layout.padding = 10;
layout.spacing = 5;
layout.stretchX = true;  // Buttons fill width

sidebar.setLayout(layout);
sidebar.addChild(new Button("Home"));
sidebar.addChild(new Button("Settings"));
sidebar.addChild(new Button("About"));
// Buttons are automatically stacked vertically
```

### 3. LayoutGrid

Arranges child elements in a grid with configurable rows and columns.

**Constructor:**
```java
LayoutGrid()
LayoutGrid(int padding, int spacing, boolean stretchX, boolean stretchY, 
           int rows, int columns, Alignment alignment)
```

**Properties:**
- **rows**: Number of rows (0 = auto-calculate)
- **columns**: Number of columns (0 = auto-calculate)
- **alignment**: Alignment of elements within their grid cells

**Behavior:**
- If both rows and columns are 0, it creates a square-ish grid
- If only rows is 0, columns is used and rows are auto-calculated
- If only columns is 0, rows is used and columns are auto-calculated
- Elements are placed left-to-right, top-to-bottom
- If `stretchX` or `stretchY` is true, elements are resized to fill their cell

**Auto-Calculation Logic:**
- When both are 0: `columns = ceil(sqrt(childCount))`, `rows = ceil(childCount / columns)`
- When columns is 0: `columns = ceil(childCount / rows)`
- When rows is 0: `rows = ceil(childCount / columns)`

**Example:**
```java
Frame iconGrid = new Frame(0, 0, 400, 400);
LayoutGrid layout = new LayoutGrid();
layout.columns = 4;  // 4 columns, auto rows
layout.rows = 0;
layout.padding = 10;
layout.spacing = 5;
layout.alignment = Alignment.CENTER;

iconGrid.setLayout(layout);
for (int i = 0; i < 12; i++) {
    iconGrid.addChild(new Button("Item " + i));
}
// Creates a 4x3 grid of buttons
```

## Code Examples

### 1. Horizontal Toolbar
```java
Frame toolbar = new Frame(0, 0, 600, 40);
toolbar.setBackgroundColor(0xFF333333);

LayoutHorizontal layout = new LayoutHorizontal(5, 10, false, true);
toolbar.setLayout(layout);

toolbar.addChild(new Button("New"));
toolbar.addChild(new Button("Open"));
toolbar.addChild(new Button("Save"));
toolbar.addChild(new Button("Export"));

gui.addChild(toolbar);
```

### 2. Vertical Settings Menu
```java
Frame settingsPanel = new Frame(10, 10, 200, 400);
settingsPanel.setBackgroundColor(0xFF2a2a2a);

LayoutVertical layout = new LayoutVertical();
layout.padding = 10;
layout.spacing = 5;
layout.stretchX = true;
settingsPanel.setLayout(layout);

settingsPanel.addChild(new Label("Settings"));
settingsPanel.addChild(new CheckBox("Enable Sound"));
settingsPanel.addChild(new CheckBox("Show FPS"));
settingsPanel.addChild(new CheckBox("V-Sync"));
settingsPanel.addChild(new Button("Apply"));
```

### 3. Icon Grid
```java
Frame iconPanel = new Frame(0, 0, 320, 320);

LayoutGrid gridLayout = new LayoutGrid();
gridLayout.columns = 5;
gridLayout.rows = 0;  // Auto-calculate rows
gridLayout.padding = 5;
gridLayout.spacing = 5;
gridLayout.stretchX = true;
gridLayout.stretchY = true;

iconPanel.setLayout(gridLayout);

for (ItemStack item : items) {
    ItemView itemView = new ItemView(item);
    iconPanel.addChild(itemView);
}
```

### 4. Nested Layouts
```java
// Main container with horizontal layout
Frame mainPanel = new Frame(0, 0, 800, 600);
mainPanel.setLayout(new LayoutHorizontal(0, 0, true, true));

// Left sidebar with vertical layout
Frame leftSidebar = new Frame();
leftSidebar.setBackgroundColor(0xFF1a1a1a);
LayoutVertical leftLayout = new LayoutVertical(10, 5, true, false);
leftSidebar.setLayout(leftLayout);

leftSidebar.addChild(new Button("Home"));
leftSidebar.addChild(new Button("Profile"));
leftSidebar.addChild(new Button("Settings"));

// Right content area with grid layout
Frame contentArea = new Frame();
contentArea.setBackgroundColor(0xFF2a2a2a);
LayoutGrid contentLayout = new LayoutGrid(10, 10, true, true, 3, 3, Alignment.CENTER);
contentArea.setLayout(contentLayout);

for (int i = 0; i < 9; i++) {
    contentArea.addChild(createContentCard(i));
}

mainPanel.addChild(leftSidebar);
mainPanel.addChild(contentArea);
```

### 5. Form Layout
```java
Frame formPanel = new Frame(0, 0, 400, 300);
formPanel.setLayout(new LayoutVertical(20, 10, true, false));

// Each row has a horizontal layout
Frame nameRow = new Frame();
nameRow.setLayout(new LayoutHorizontal(0, 10, true, false));
nameRow.addChild(new Label("Name:"));
nameRow.addChild(new TextBox());

Frame emailRow = new Frame();
emailRow.setLayout(new LayoutHorizontal(0, 10, true, false));
emailRow.addChild(new Label("Email:"));
emailRow.addChild(new TextBox());

Frame buttonRow = new Frame();
buttonRow.setLayout(new LayoutHorizontal(0, 10, false, false));
buttonRow.addChild(new Button("Submit"));
buttonRow.addChild(new Button("Cancel"));

formPanel.addChild(nameRow);
formPanel.addChild(emailRow);
formPanel.addChild(buttonRow);
```

### 6. Responsive Dashboard
```java
Frame dashboard = new Frame(0, 0, 1000, 800);
LayoutGrid dashLayout = new LayoutGrid();
dashLayout.rows = 2;
dashLayout.columns = 3;
dashLayout.padding = 10;
dashLayout.spacing = 10;
dashLayout.stretchX = true;
dashLayout.stretchY = true;

dashboard.setLayout(dashLayout);

dashboard.addChild(createMetricCard("CPU", "45%"));
dashboard.addChild(createMetricCard("Memory", "2.1 GB"));
dashboard.addChild(createMetricCard("Disk", "120 GB"));
dashboard.addChild(createMetricCard("Network", "15 MB/s"));
dashboard.addChild(createMetricCard("FPS", "60"));
dashboard.addChild(createMetricCard("Ping", "45 ms"));
```

## Comparison Table

| Layout Type | Use Case | Key Features | Best For |
|------------|----------|--------------|----------|
| **LayoutHorizontal** | Toolbars, button rows | Left-to-right arrangement | Horizontal menus, toolbars |
| **LayoutVertical** | Sidebars, lists | Top-to-bottom arrangement | Vertical menus, forms, lists |
| **LayoutGrid** | Icon grids, dashboards | Rows and columns | Item grids, dashboards, galleries |

## Best Practices

### 1. Choose the Right Layout
- Use **LayoutHorizontal** for navigation bars and button groups
- Use **LayoutVertical** for menus and stacked content
- Use **LayoutGrid** for uniform collections of items

### 2. Stretching Strategy
```java
// Stretch only when children should fill the container
layout.stretchX = true;  // Children fill width
layout.stretchY = true;  // Children fill height
```

### 3. Padding and Spacing
```java
// Consistent spacing creates clean layouts
layout.padding = 10;   // Space from container edges
layout.spacing = 5;    // Space between elements
```

### 4. Nested Layouts
```java
// Combine layouts for complex interfaces
Frame mainContainer = new Frame();
mainContainer.setLayout(new LayoutVertical());

Frame topSection = new Frame();
topSection.setLayout(new LayoutHorizontal());

Frame bottomSection = new Frame();
bottomSection.setLayout(new LayoutGrid());

mainContainer.addChild(topSection);
mainContainer.addChild(bottomSection);
```

### 5. Dynamic Content
```java
// Layouts automatically adjust when children are added/removed
void addItem(GuiElement item) {
    container.addChild(item);
    // Layout is automatically reapplied
}

void removeItem(GuiElement item) {
    container.removeChild(item);
    // Layout is automatically reapplied
}
```

### 6. Temporary Layout Disable
```java
// Disable layout for bulk operations
layout.enabled = false;

for (GuiElement item : manyItems) {
    container.addChild(item);
}

layout.enabled = true;
container.layoutChangedInternal();  // Apply layout once
```

## Common Patterns

### Equal-Width Columns
```java
LayoutHorizontal layout = new LayoutHorizontal();
layout.stretchX = true;  // All columns get equal width
layout.stretchY = true;  // Columns fill height
```

### Fixed-Size Grid
```java
LayoutGrid layout = new LayoutGrid();
layout.stretchX = false;  // Children keep their original width
layout.stretchY = false;  // Children keep their original height
layout.columns = 4;
layout.alignment = Alignment.CENTER;  // Center in cells
```

### Auto-Sizing Grid
```java
LayoutGrid layout = new LayoutGrid();
layout.rows = 0;     // Auto-calculate
layout.columns = 0;  // Auto-calculate (creates square grid)
```

## Technical Notes

- Layouts are applied automatically when children are added/removed
- The layout's `apply()` method is called from the parent element's `layoutChanged()` callback
- Setting `enabled = false` allows manual positioning
- Layouts only affect direct children, not nested descendants
- Layout calculations happen during the layout phase, not rendering
- Stretch modes override child element sizes
- Grid layout uses alignment when stretch is disabled
