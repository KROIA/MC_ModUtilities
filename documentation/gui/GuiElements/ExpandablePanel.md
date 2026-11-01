# ExpandablePanel

## Overview

The `ExpandablePanel` is a collapsible/expandable container element. It is composed of a clickable **header** (a title `Label` plus an expand/collapse arrow indicator) and a **content area** that hosts arbitrary child `GuiElement`s stacked vertically. Clicking the header (or calling `setExpanded(boolean)` / `toggle()`) shows or hides the content area and changes the panel's effective height accordingly. ExpandablePanels are ideal for tucking away secondary or advanced options until the user chooses to reveal them.

**When to use:**
- Grouping optional or advanced settings behind a single header
- Progressive disclosure of extra detail in dense forms
- Collapsible sections inside a scrollable list
- Reducing visual clutter while keeping content one click away

## Constructor

```java
// Collapsed panel with the given title
ExpandablePanel(String title)

// Panel with an explicit initial expansion state
ExpandablePanel(String title, boolean initiallyExpanded)
```

**Parameters:**
- `title` - The header title text
- `initiallyExpanded` - `true` to start expanded, `false` to start collapsed

## Key Methods

### Expansion Control
```java
void setExpanded(boolean expanded)                 // Expand or collapse (no-op if unchanged)
boolean isExpanded()                               // Check if currently expanded
void toggle()                                      // Flip between expanded and collapsed
void setOnToggle(Consumer<Boolean> onToggle)       // Register a toggle listener (null clears)
Consumer<Boolean> getOnToggle()                    // Get the registered toggle listener
```

### Title Management
```java
void setTitle(String title)                        // Set the header title text
String getTitle()                                  // Get the header title text
```

### Child Management
```java
void addChild(GuiElement el)                        // Add a child to the content area (null ignored)
void removeChild(GuiElement el)                     // Remove a child from the content area
void removeChilds()                                 // Remove all content children
List<GuiElement> getChilds()                        // Get the live list of content children
```

### Layout Tuning
```java
void setHeaderHeight(int headerHeight)              // Set the clickable header row height
int getHeaderHeight()                              // Get the header row height
void setContentPadding(int contentPadding)         // Set inner padding around the content
int getContentPadding()                            // Get the content padding
void setContentSpacing(int contentSpacing)         // Set vertical spacing between children
int getContentSpacing()                            // Get the content spacing
int getContentHeight()                             // Get the expanded content area height
```

### Styling
```java
void setHeaderColor(int color)                     // Set header background (idle)
int getHeaderColor()                               // Get header background (idle)
void setHeaderHoverColor(int color)                // Set header background while hovered
void setHeaderPressedColor(int color)              // Set header background while pressed
void setHeaderTextColor(int color)                 // Set header title text color
int getHeaderTextColor()                           // Get header title text color
void setContentBackgroundColor(int color)          // Set the content area background color
void setContentBackgroundEnabled(boolean enabled)  // Toggle the content background fill (off by default)
```

## Styling

The panel frame draws an outline only (transparent fill) so the header and content provide the visuals. The header behaves like a button and exposes idle, hover and pressed colors, while the content background is disabled by default and must be turned on explicitly.

### Default Values
```java
ExpandablePanel.DEFAULT_HEADER_HEIGHT    // 20 - header row height in pixels
ExpandablePanel.DEFAULT_CONTENT_PADDING  // 2  - inner padding around the content
ExpandablePanel.DEFAULT_CONTENT_SPACING  // 2  - spacing between adjacent children
ExpandablePanel.DEFAULT_HEADER_TEXT_PADDING // 4 - horizontal padding before the title text
```

### Customization Example
```java
ExpandablePanel panel = new ExpandablePanel("Options");
panel.setHeaderColor(0xFF333333);
panel.setHeaderHoverColor(0xFF444444);
panel.setHeaderPressedColor(0xFF222222);
panel.setHeaderTextColor(0xFFFFFFFF);
panel.setContentBackgroundColor(0xFF1E1E1E);
panel.setContentBackgroundEnabled(true);
```

## Code Examples

### Advanced Options Section
```java
// Collapsible "Advanced" section holding extra option rows.
ExpandablePanel advancedPanel = new ExpandablePanel("Advanced", false);
advancedPanel.addChild(new CheckBox("Reduce particles"));
advancedPanel.addChild(new CheckBox("Show debug overlay"));
Label hint = new Label("These options require a restart.");
hint.setHeight(16);
advancedPanel.addChild(hint);
advancedPanel.setOnToggle(expanded -> System.out.println("Advanced expanded: " + expanded));
addElement(advancedPanel);
```

### Starting Expanded
```java
ExpandablePanel panel = new ExpandablePanel("Details", true);
panel.addChild(new Label("Line one"));
panel.addChild(new Label("Line two"));
gui.addChild(panel);
```

### Reacting to Toggle
```java
ExpandablePanel filters = new ExpandablePanel("Filters");
filters.addChild(new CheckBox("Weapons"));
filters.addChild(new CheckBox("Armor"));

filters.setOnToggle(expanded -> {
    if (expanded) {
        loadFilterState();
    } else {
        applyFilters();
    }
});
```

### Programmatic Expansion
```java
ExpandablePanel panel = new ExpandablePanel("Log Output");
panel.addChild(new Label("Nothing yet."));

// Reveal the panel when new output arrives.
void onNewLogLine(String line) {
    panel.addChild(new Label(line));
    panel.setExpanded(true);
}
```

## Common Patterns

### Grouping Options in a List
```java
VerticalListView list = new VerticalListView();

ExpandablePanel general = new ExpandablePanel("General", true);
general.addChild(new CheckBox("Enable feature"));
list.addChild(general);

ExpandablePanel advanced = new ExpandablePanel("Advanced", false);
advanced.addChild(new CheckBox("Verbose logging"));
advanced.addChild(new CheckBox("Experimental mode"));
list.addChild(advanced);
```

### Accordion-Style Sections
```java
List<ExpandablePanel> sections = List.of(sectionA, sectionB, sectionC);

// Collapse the others whenever one expands.
for (ExpandablePanel section : sections) {
    section.setOnToggle(expanded -> {
        if (expanded) {
            for (ExpandablePanel other : sections) {
                if (other != section) other.setExpanded(false);
            }
        }
    });
}
```

### Tighter or Roomier Layout
```java
ExpandablePanel panel = new ExpandablePanel("Compact");
panel.setHeaderHeight(16);
panel.setContentPadding(4);
panel.setContentSpacing(1);
```

## Best Practices

1. **Start Collapsed**: Default advanced or secondary sections to collapsed to keep the initial view clean
2. **Reflow-Aware Containers**: Place the panel inside a container that reflows (such as a `VerticalListView`) so siblings shift as it expands
3. **Fixed-Height Children**: Give content children an explicit height so the computed content height is accurate
4. **Clear Titles**: Use a title that communicates what is hidden, so users know whether to expand
5. **Content Background**: Enable `setContentBackgroundEnabled(true)` when the panel needs to visually separate its content from surrounding elements
6. **Toggle Side Effects**: Use `setOnToggle` for lazy loading or applying changes only when the section is opened or closed

## Events and Callbacks

The toggle listener receives the new expansion state whenever it changes (via header click, `setExpanded`, or `toggle`):

```java
panel.setOnToggle(expanded -> {
    if (expanded) {
        System.out.println("Panel opened");
    } else {
        System.out.println("Panel closed");
    }
});
```

Calling `setExpanded` with the current state is a no-op and does not fire the listener.

## Technical Notes

- Children are routed to an inner content container carrying a `LayoutVertical`; the panel itself only manages the header and that container (mirroring how `DropDownMenu` and `ListView` expose their children)
- While collapsed the content area is disabled, so hidden children are neither rendered nor receive mouse/keyboard input
- On a state change the panel recomputes its own height and triggers a layout pass on its root parent (`layoutChangedInternal()`), so an enclosing list reflows and pushes sibling elements
- The panel grows downward when expanded; if the surrounding container does not reflow, the expanded content overlaps following siblings (the same caveat as `DropDownMenu`)
- The arrow indicator reuses the shared ModUtilities dropdown textures (`arrow_down.png` when collapsed, `arrow_up.png` when expanded)
- Only the `expanded` flag is persisted via `serializeState()` / `deserializeState()`; content children are not serialized through the GUI state-sync system (`getSerializableChildren()` returns an empty list, mirroring `DropDownMenu`)
- The `gui/` package is client-only (`@Environment(EnvType.CLIENT)`); expandable panels must only be used on the client
