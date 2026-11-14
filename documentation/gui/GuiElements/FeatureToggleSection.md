# FeatureToggleSection

## Overview

`FeatureToggleSection` is an [ExpandablePanel](ExpandablePanel.md) whose header carries an on/off `CheckBox` for a feature.
The settings of that feature go into the section as children.

Header layout: `[x] Title ........ [Restart required] [arrow]`. Clicking the check box toggles the feature; clicking
anywhere else on the header expands or collapses the child settings.

**When to use:**
- Settings screens where a feature can be switched off and has its own options
- Features that need a server restart after a change
- Features that can only be changed on some servers (for example only on the master)

## Constructor

```java
// Collapsed section
FeatureToggleSection(String title)

// Section with an explicit initial expansion state
FeatureToggleSection(String title, boolean initiallyExpanded)
```

## Key Methods

### Feature State
```java
void setFeatureEnabled(boolean enabled)            // Set the check box; fires the toggle listener on change
boolean isFeatureEnabled()                         // Current check box state
void setOnFeatureToggled(Consumer<Boolean> listener) // Receives the new state, or null to clear
```

### Restart Marker
```java
void setRestartRequired(boolean restartRequired)   // Show or hide "Restart required" in the header
boolean isRestartRequired()
void setRestartMarkerText(Component text, Component tooltip) // Change the marker text and tooltip; null keeps the current one
static Label createRestartIndicator()              // The same marker for a child row, hidden by default
```

### Locking
```java
void setLocked(boolean locked, Component reason)   // Make check box and children read-only; reason is the header tooltip
boolean isLocked()
```

## Behaviour

- Children added with `addChild` are only interactive while the feature is enabled and the section is not locked.
  Otherwise they stay visible but greyed out (see [non-interactive elements](../GuiElement.md#non-interactive-elements)).
- `setLocked(true, reason)` makes the check box and the children read-only, greys out the title and shows `reason` as the
  header tooltip. `FeatureToggleSection.MASTER_ONLY_REASON` is a ready-made reason for master-only features on slave servers.
- The default restart tooltip says the value differs from the one active since server start. For a setting that is only read
  at startup, call `setRestartRequired(true)` once and pass your own wording with `setRestartMarkerText`.
- While expanded, the whole section (header and children) gets a darker background so you can see which rows belong to it.
  Change it with `setBackgroundColor(int)` or turn it off with `setEnableBackground(false)`. Nested expanded sections stack a little darker.

### Constants
```java
FeatureToggleSection.RESTART_INDICATOR_COLOR   // 0xFFFFAA30 - restart marker text color
FeatureToggleSection.LOCKED_TITLE_COLOR        // 0xFF999999 - title color while locked
FeatureToggleSection.EXPANDED_BACKGROUND_COLOR // 0x99484848 - background while expanded
FeatureToggleSection.MASTER_ONLY_REASON        // translatable lock reason for master-only features
```

The marker, tooltips and lock reason are translated (`en_us`, `zh_cn`).

## Code Example

```java
// settings, activeSinceStart and serverIsSlave are your own values, e.g. synced from the server by your own packet.
// Do not call MultiServerManager here: it is server-side and this code runs on the client.
VerticalListView list = new VerticalListView();

FeatureToggleSection interest = new FeatureToggleSection("Interest");
interest.setFeatureEnabled(settings.interestEnabled.get());
interest.setOnFeatureToggled(enabled -> {
    settings.interestEnabled.set(enabled);
    interest.setRestartRequired(enabled != activeSinceStart);
});

TextBox rate = new TextBox();
rate.setText(settings.interestRate.get(), 2);
interest.addChild(rate);

Label rateRestart = FeatureToggleSection.createRestartIndicator();
interest.addChild(rateRestart);

if (serverIsSlave)
    interest.setLocked(true, FeatureToggleSection.MASTER_ONLY_REASON);

list.addChild(interest);
```

Place the section in a container that reflows, such as a `VerticalListView`, so siblings move when it expands.

## Technical Notes

- Extends `ExpandablePanel` and uses its protected `getHeaderButton()`, `getTitleLabel()` and `getContentArea()`
- The `gui/` package is client-only

## See Also

- [ExpandablePanel](ExpandablePanel.md)
- [CheckBox](CheckBox.md)
- [GuiElement](../GuiElement.md)
