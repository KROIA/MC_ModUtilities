# Form with Validation

A simple registration form built from `Label` + `TextBox` rows with live validation feedback and a Submit button.

## What this shows

- Building a labelled form by laying out `Label` and `TextBox` pairs in `updateLayout(Gui)`.
- Constraining a numeric field with `TextBox.createRegex_onlyNumerical(...)`.
- Reacting to user input via `TextBox.setOnTextChanged(...)` to provide live, non-blocking validation.
- Recolouring a status `Label` to communicate validation state (green / orange / red).
- Triggering final validation from a `Button`'s falling edge callback.

## How to run

```
/modutilities openExample form
```

The command requires permission level 2 (operator).

## What you see

- A title "User Registration" centred at the top.
- Three labelled fields: Name, Age (numbers only, max 3 digits), Email.
- A status line under the fields. While you type, it updates to either "Looks good - press Submit to confirm." (green) or a specific error message (red).
- A Submit button. If validation passes the status line turns cyan and the entry is logged via `ModUtilitiesMod.LOGGER.info(...)`.

## Code walkthrough

The screen extends `GuiScreen` and is annotated `@Environment(EnvType.CLIENT)` because all GUI rendering happens on the client.

Each input is created in the constructor and added with `addElement(...)`:

```java
nameField = new TextBox();
nameField.setMaxChars(32);
nameField.setOnTextChanged(s -> validateLive());

ageField = new TextBox();
ageField.setMatchRegex(TextBox.createRegex_onlyNumerical(true, false, 3, 0));
ageField.setOnTextChanged(s -> validateLive());
```

`validateLive()` reruns `validate()` and updates `statusLabel` colour based on whether the form is currently valid. `validate()` returns `null` on success or an error message describing the first problem.

The Submit button triggers a final check:

```java
submitButton = new Button("Submit", this::onSubmit);
```

`updateLayout(Gui)` positions every element relative to a `(x, y)` origin computed from `getWidth() / getHeight()`, so the form re-centres when the player resizes the window.

## Key takeaways

- Validation logic is independent of layout - it just reads `getText()` / `getInt()` from the fields.
- Use `TextBox.createRegex_onlyNumerical(...)` to constrain input *as the user types* rather than after-the-fact.
- A single shared `validate()` method keeps the live preview and the final submit consistent.
