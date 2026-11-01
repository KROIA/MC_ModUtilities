# Modal Confirmation Dialog

A confirm-or-cancel modal dialog overlaid on top of a base page.

## What this shows

- Building a "modal" feel by stacking two `Frame` overlays (a translucent backdrop and a centred dialog panel) above the rest of the screen.
- Toggling visibility with `setEnabled(true/false)` instead of adding / removing elements at runtime.
- Disabling the trigger button while the dialog is open so it cannot be re-opened.
- Wiring two buttons (OK / Cancel) to separate `Runnable` callbacks.

## How to run

```
/modutilities openExample dialog
```

## What you see

- A title "Modal Dialog Demo".
- A status line that initially reads "No action taken yet."
- A wide button labelled "Delete world (just kidding)".
- Clicking the button dims the screen with a translucent dark backdrop and shows a centred panel "Confirm" / "Are you really sure you want to do that?" with OK and Cancel buttons.
- Picking either button hides the dialog, re-enables the trigger button, and updates the status line ("You clicked OK." / "You clicked Cancel.").

## Code walkthrough

The dialog itself is a plain `Frame` with three children: a title `Label`, a body `Label` and two `Button`s. It is created up-front and added to the screen, but starts disabled:

```java
dialog = new Frame();
addElement(dialog);
dialog.setEnabled(false);
```

A second `Frame` named `backdrop` is sized to cover the entire screen and uses a semi-transparent black background (`0xAA000000`). The GUI library renders elements in the order they were added, so the dialog (added after the backdrop) appears on top.

Showing and hiding the dialog is just a matter of toggling `setEnabled` on both frames and on the trigger button:

```java
private void showDialog() {
    backdrop.setEnabled(true);
    dialog.setEnabled(true);
    triggerButton.setClickable(false);
}
```

OK and Cancel each invoke a small handler that updates the status label and hides the dialog again.

## Key takeaways

- For modal-style dialogs, prefer a permanent backdrop element you can enable/disable rather than rebuilding the screen.
- Disabling a button via `setClickable(false)` keeps it visible but unresponsive - useful for "freezing" the underlying page while the dialog is up.
- Element order in `addElement(...)` calls determines render order; later elements draw on top.
