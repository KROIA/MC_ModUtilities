# TabElement

## Overview

The `TabElement` is a tabbed interface component that displays multiple content panels with tab titles. It allows users to switch between different views while maintaining a compact layout. Tabs can have custom title elements and support features like reordering, tooltips, and dynamic styling.

**When to use:**
- Multi-page settings interfaces
- Content organization with categories
- Wizard-style interfaces
- Dashboard sections

## Constructor

```java
TabElement()
```

## Key Methods

### Tab Management
```java
int addTab(String tabName, GuiElement tabElement)                    // Add tab with text title
int addTab(GuiElement titleElement, GuiElement tabElement)           // Add tab with custom title
boolean removeTab(int index)                                         // Remove tab at index
boolean reorderTab(int fromIndex, int toIndex)                       // Move tab to new position
void clearTabs()                                                     // Remove all tabs
int getTabCount()                                                    // Get number of tabs
```

### Tab Selection
```java
void selectTab(int index)                                            // Select tab by index
int getSelectedTab()                                                 // Get selected tab index
GuiElement getSelectedTabElement()                                   // Get selected content element
GuiElement getSelectedTabTitleElement()                              // Get selected title element
GuiElement getTabElement(int index)                                  // Get content at index
GuiElement getTabTitleElement(int index)                             // Get title at index
```

### Title Bar Styling
```java
void setTitleHeight(int height)                                      // Set title bar height
int getTitleHeight()                                                 // Get title bar height
void setSelectedTitleHeight(int height)                              // Set selected title height
int getSelectedTitleHeight()                                         // Get selected title height
void setUnselectedTitleHeight(int height)                            // Set unselected title height
int getUnselectedTitleHeight()                                       // Get unselected title height
void setSelectOutlineThickness(int thickness)                        // Set selected outline thickness
int getSelectOutlineThickness()                                      // Get outline thickness
```

### Title Styling
```java
void setTitleSelectColor(int color)                                  // Set selected title color
int getTitleSelectColor()                                            // Get selected title color
void setTitleHoverColor(int color)                                   // Set hover overlay color
int getTitleHoverColor()                                             // Get hover color
void setSelectedTitleLabelAlignment(Alignment alignment)             // Set selected title alignment
Alignment getSelectedTitleLabelAlignment()                           // Get selected alignment
void setUnselectedTitleLabelAlignment(Alignment alignment)           // Set unselected title alignment
Alignment getUnselectedTitleLabelAlignment()                         // Get unselected alignment
```

### Tooltips
```java
void setTitleElementHoverTooltipSupplier(Function<Integer, String> supplier)    // Set tooltip supplier
void setTitleElementHoverTooltipMouseAlignment(Alignment alignment)             // Set tooltip alignment
```

## Code Examples

### Basic Tabbed Interface
```java
TabElement tabs = new TabElement();
tabs.setSize(600, 400);

Frame generalSettings = new Frame();
generalSettings.addChild(new Label("General Settings Content"));

Frame advancedSettings = new Frame();
advancedSettings.addChild(new Label("Advanced Settings Content"));

tabs.addTab("General", generalSettings);
tabs.addTab("Advanced", advancedSettings);

gui.addChild(tabs);
```

### Multi-Tab Settings Panel
```java
TabElement settingsTabs = new TabElement();
settingsTabs.setSize(700, 500);
settingsTabs.setTitleHeight(30);

// Create content for each tab
Frame videoTab = createVideoSettings();
Frame audioTab = createAudioSettings();
Frame controlsTab = createControlsSettings();
Frame gameplayTab = createGameplaySettings();

settingsTabs.addTab("Video", videoTab);
settingsTabs.addTab("Audio", audioTab);
settingsTabs.addTab("Controls", controlsTab);
settingsTabs.addTab("Gameplay", gameplayTab);

// Style the tabs
settingsTabs.setTitleSelectColor(0x803366FF);
settingsTabs.setTitleHoverColor(0x402266CC);
```

### Tabs with Icons
```java
TabElement iconTabs = new TabElement();

for (Category category : categories) {
    Frame titleFrame = new Frame();
    titleFrame.setLayout(new LayoutHorizontal(2, 2, false, false));
    
    TextureElement icon = new TextureElement(
        "mymod",
        "textures/gui/" + category.getIconName(),
        16, 16
    );
    Label titleLabel = new Label(category.getName());
    
    titleFrame.addChild(icon);
    titleFrame.addChild(titleLabel);
    
    Frame contentFrame = createCategoryContent(category);
    
    iconTabs.addTab(titleFrame, contentFrame);
}
```

### Dynamic Tabs
```java
TabElement dynamicTabs = new TabElement();

void addNewTab() {
    int index = dynamicTabs.addTab("New Tab " + nextId++, new Frame());
    dynamicTabs.selectTab(index);
}

void closeCurrentTab() {
    int currentIndex = dynamicTabs.getSelectedTab();
    if (currentIndex >= 0) {
        dynamicTabs.removeTab(currentIndex);
        if (dynamicTabs.getTabCount() > 0) {
            dynamicTabs.selectTab(Math.max(0, currentIndex - 1));
        }
    }
}
```

### Tabs with Tooltips
```java
TabElement tooltipTabs = new TabElement();

tooltipTabs.setTitleElementHoverTooltipSupplier(index -> {
    switch (index) {
        case 0: return "General game settings";
        case 1: return "Configure video options";
        case 2: return "Adjust audio settings";
        default: return null;
    }
});

tooltipTabs.setTitleElementHoverTooltipMouseAlignment(Alignment.TOP_LEFT);
```

### Custom Styled Tabs
```java
TabElement styledTabs = new TabElement();
styledTabs.setSize(800, 600);

// Title bar styling
styledTabs.setTitleHeight(35);
styledTabs.setSelectedTitleHeight(35);
styledTabs.setUnselectedTitleHeight(30);
styledTabs.setSelectOutlineThickness(3);

// Colors
styledTabs.setBackgroundColor(0xFF1a1a1a);
styledTabs.setTitleSelectColor(0x804488FF);
styledTabs.setTitleHoverColor(0x403377CC);
styledTabs.setOutlineColor(0xFF4488FF);

// Title alignment
styledTabs.setSelectedTitleLabelAlignment(Alignment.BOTTOM);
styledTabs.setUnselectedTitleLabelAlignment(Alignment.CENTER);

// Font scaling
styledTabs.setTextFontScale(1.2f);
```

## Common Patterns

### Wizard-Style Interface
```java
TabElement wizard = new TabElement();
int currentStep = 0;

Button nextBtn = new Button("Next", () -> {
    if (currentStep < wizard.getTabCount() - 1) {
        currentStep++;
        wizard.selectTab(currentStep);
    }
});

Button prevBtn = new Button("Previous", () -> {
    if (currentStep > 0) {
        currentStep--;
        wizard.selectTab(currentStep);
    }
});
```

### Closeable Tabs
```java
// Add close button to each tab title
for (int i = 0; i < tabCount; i++) {
    Frame titleFrame = new Frame();
    titleFrame.setLayout(new LayoutHorizontal());
    
    Label tabName = new Label("Tab " + i);
    CloseButton closeBtn = new CloseButton(() -> {
        int tabIndex = findTabIndex(titleFrame);
        tabs.removeTab(tabIndex);
    });
    
    titleFrame.addChild(tabName);
    titleFrame.addChild(closeBtn);
    
    tabs.addTab(titleFrame, createTabContent(i));
}
```

### Save Tab State
```java
TabElement statefulTabs = new TabElement();

// Load last selected tab
int savedTabIndex = config.getInt("lastSelectedTab", 0);
statefulTabs.selectTab(savedTabIndex);

// Save on change
// (You'll need to track changes via a wrapper or override)
void onTabChanged(int newIndex) {
    config.setInt("lastSelectedTab", newIndex);
    config.save();
}
```

## Best Practices

1. **Tab Count**: Keep the number of tabs reasonable (3-8 tabs for best UX)
2. **Title Width**: Ensure tab titles have appropriate width for their content
3. **Visual Hierarchy**: Selected tabs should be clearly distinguishable
4. **Height Difference**: Make selected tabs slightly taller than unselected ones
5. **First Tab**: Always have at least one tab selected by default
6. **Reordering**: Use `reorderTab()` carefully as it affects indices
7. **Font Scale**: Call `setTextFontScale()` to propagate to all tab titles

## Tab Title Behavior

- **Selected Tab**: Renders with `selectedTitleHeight` and `selectedTitleLabelAlignment`
- **Unselected Tabs**: Render with `unselectedTitleHeight` and `unselectedTitleLabelAlignment`
- **Hover Effect**: Shows `titleHoverColor` overlay when mouse is over a tab title
- **Selection Indicator**: Selected tab has `titleSelectColor` overlay and outline

## Technical Notes

- Tab titles are positioned horizontally at the top
- Tab content fills the remaining space below the title bar
- Only the selected tab's content is added as a child (others are removed)
- Title elements must have their width set before adding to tabs
- The internal tab list uses a `Tab` class storing title and content elements
- Clicking a tab title selects that tab
- `addChild()`, `removeChild()`, `removeChilds()`, and `getChilds()` are overridden to prevent direct manipulation
- Tab indices are stable until `removeTab()` or `reorderTab()` is called
