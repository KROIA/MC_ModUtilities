# Tabbed Interface

A three-tab screen built with `TabElement`, where each tab hosts a different `Frame` of content.

## What this shows

- Hosting multiple unrelated panes in a single screen with `TabElement`.
- Adding tabs by string name via `tabElement.addTab("Name", contentFrame)`.
- Each tab's content is a normal `GuiElement` (a `Frame` here), so it can hold any combination of children.
- An interactive element inside a tab keeps its state across tab switches because the `Frame` lives across the screen lifecycle.

## How to run

```
/modutilities openExample tabs
```

## What you see

- A `TabElement` filling most of the screen with three tabs:
  - **Info**: a static title and description.
  - **Controls**: a counter button that increments a label - the count survives tab switches.
  - **About**: a short description of the example.

## Code walkthrough

The `TabElement` is created and added like any other element:

```java
tabElement = new TabElement();
addElement(tabElement);
```

Each tab content is built as a self-contained `Frame`:

```java
controlsTab = new Frame();
controlsButton = new Button("Click me", () -> {
    counter++;
    controlsCounterLabel.setText("Clicked: " + counter + " times");
});
controlsTab.addChild(controlsTitle);
controlsTab.addChild(controlsCounterLabel);
controlsTab.addChild(controlsButton);
```

Tabs are then registered by name:

```java
tabElement.addTab("Info", infoTab);
tabElement.addTab("Controls", controlsTab);
tabElement.addTab("About", aboutTab);
```

`updateLayout(Gui)` sizes the `TabElement` to the screen and then sizes each tab's children using the **content** width (`controlsTab.getWidth()`), which is the inner area available below the tab title bar.

## Layout note

`TabElement` automatically reserves its title bar height; you don't need to subtract it manually when laying out children inside the content frame.

## Key takeaways

- Use one `Frame` per tab content area as a clean container.
- Tab content state is preserved when switching - the elements stay in memory; only what is rendered changes.
- For icon-style tab titles you can pass a `GuiElement` as the title via `addTab(GuiElement, GuiElement)`; this example uses simple string labels.
