# Settings / Configuration Screen

A multi-control settings page combining a slider, two checkboxes, a dropdown and an Apply button.

## What this shows

- Combining different input element types: `HorizontalSlider`, `CheckBox`, `DropDownMenu`, `Button`.
- Reading slider values via the `setOnValueChanged(Consumer<Double>)` callback.
- Updating a side label live as the slider moves.
- Populating a `DropDownMenu` with options and reacting to selection through `setOnOptionSelected((index, element) -> ...)`.
- Holding setting state in plain Java fields and writing it out only when the user presses Apply.

## How to run

```
/modutilities openExample settings
```

## What you see

- A "Settings" header at the top.
- A volume slider (0% - 100%) with the percentage shown live to its right.
- An "Enable music" checkbox (default checked).
- A "Fullscreen" checkbox (default unchecked).
- A Difficulty dropdown with `Peaceful / Easy / Normal / Hard` (defaults to Normal).
- An Apply button at the bottom that logs the current values to `latest.log`.

## Code walkthrough

State lives in simple instance fields; the controls just read from / write to them:

```java
private double volume = 0.6;
private boolean musicEnabled = true;
private boolean fullscreenEnabled = false;
private int difficultyIndex = 2;
```

The slider value is on the unit interval `[0, 1]`. The screen converts that to a percentage for display:

```java
volumeSlider.setOnValueChanged(v -> {
    volume = v;
    volumeValueLabel.setText(formatPercent(v));
});
```

A tooltip supplier lets the player see the value while dragging:

```java
volumeSlider.setTooltipSupplier(() -> formatPercent(volume));
```

The dropdown is populated with `addOption(String)` calls and reacts via:

```java
difficultyDropDown = new DropDownMenu(DIFFICULTY_OPTIONS[difficultyIndex], (index, element) -> {
    difficultyIndex = index;
    difficultyDropDown.setLabelText(DIFFICULTY_OPTIONS[index]);
    difficultyDropDown.collapse();
});
```

Pressing Apply just logs the current state - in a real mod you would persist or apply each setting here.

## Layout note

When laying out a `DropDownMenu`, set the *unexpanded* bounds. The expanded list grows downward over neighbouring elements, so make sure there is space below or that adjacent rows are far enough away.

## Key takeaways

- Treat the GUI as a thin layer over plain state fields - this keeps validation and persistence trivial to add later.
- Sliders speak in `[0, 1]`; convert to / from your domain unit at the boundary.
- DropDowns expand downward; budget vertical space accordingly when laying out.
