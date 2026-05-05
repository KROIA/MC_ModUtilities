# TextBox

## Overview

The `TextBox` is a text input field that allows users to enter and edit text. It supports input validation through regular expressions, text selection, clipboard operations, and provides visual feedback for focus and hover states. TextBoxes are essential for any interface requiring user text input.

**When to use:**
- User input forms (name, search, configuration values)
- Number inputs with validation
- Command inputs and console interfaces
- Filter and search fields

## Constructor

```java
// Default textbox (100px wide)
TextBox()

// Positioned textbox with specific width
TextBox(int x, int y, int width)
```

**Parameters:**
- `x`, `y` - Position coordinates
- `width` - Width of the text box (height is fixed at Label.DEFAULT_HEIGHT = 15px)

## Key Methods

### Text Management
```java
void setText(String text)           // Set the text content
String getText()                    // Get the current text
void setText(double value)          // Set text from double value
void setText(int value)             // Set text from int value
void setText(long value)            // Set text from long value
double getDouble()                  // Parse text as double
int getInt()                        // Parse text as int
long getLong()                      // Parse text as long
```

### Input Validation
```java
void setMatchRegex(String matchRegex)    // Set regex pattern for allowed input
String getMatchRegex()                   // Get current regex pattern
void setMaxChars(int maxChars)           // Set maximum character limit
int getMaxChars()                        // Get maximum character limit
```

### Regex Helpers
```java
// Create regex for numeric input
static String createRegex_onlyNumerical(
    boolean allowPositive, 
    boolean allowNegative, 
    int maxDigits, 
    int maxDecimalDigits
)

// Create regex that excludes numbers
static String createRegex_noNumbers()
```

### Customization
```java
void setAlignment(Alignment alignment)              // Set text alignment
void setCursorColor(int color)                      // Set cursor color
void setHoverBackgroundColor(int color)             // Set hover background color
void setFocusedBackgroundColor(int color)           // Set focused background color
void setBackgroundColor(int color)                  // Set idle background color
void setTextColor(int color)                        // Set text color
void setTextFontScale(float scale)                  // Set font scale
```

### Event Callback
```java
void setOnTextChanged(Consumer<String> callback)    // Called when text changes from user input
```

## Input Validation

The TextBox uses regular expressions to validate user input in real-time. Only characters matching the regex pattern are accepted.

### Default Pattern
```java
".*"  // Accepts all characters
```

### Common Patterns

**Numeric Only (Positive Integers)**
```java
textBox.setMatchRegex("^\\d*$");
```

**Decimal Numbers**
```java
String regex = TextBox.createRegex_onlyNumerical(
    true,   // allow positive
    true,   // allow negative
    10,     // max digits before decimal
    2       // max digits after decimal
);
textBox.setMatchRegex(regex);
```

**Letters Only**
```java
textBox.setMatchRegex("^[a-zA-Z]*$");
```

**Alphanumeric**
```java
textBox.setMatchRegex("^[a-zA-Z0-9]*$");
```

**Email Format**
```java
textBox.setMatchRegex("^[a-zA-Z0-9._%+-]*@?[a-zA-Z0-9.-]*\\.?[a-zA-Z]*$");
```

## Keyboard Shortcuts

The TextBox supports standard text editing shortcuts:

- **Ctrl+A** - Select all text
- **Ctrl+C** - Copy selected text to clipboard
- **Ctrl+X** - Cut selected text to clipboard
- **Ctrl+V** - Paste text from clipboard
- **Ctrl+Backspace** - Delete previous word
- **Ctrl+Delete** - Delete next word
- **Ctrl+Left/Right** - Jump to previous/next word
- **Shift+Left/Right** - Select text character by character
- **Shift+Ctrl+Left/Right** - Select text word by word
- **Backspace** - Delete previous character
- **Delete** - Delete next character
- **Enter/Escape** - Remove focus from textbox

## Styling

### Default Colors
```java
background = setBrightness(DEFAULT_BACKGROUND_COLOR, 0.8f)
hoverBackground = setBrightness(background, 0.8f)
focusedBackground = setBrightness(background, 0.6f)
outline = setBrightness(background, 0.4f)
cursorColor = 0xFF222222
selectionColor = 0xAA222222
```

### Customization Example
```java
TextBox textBox = new TextBox(0, 0, 150);
textBox.setBackgroundColor(0xFF333333);
textBox.setHoverBackgroundColor(0xFF444444);
textBox.setFocusedBackgroundColor(0xFF555555);
textBox.setCursorColor(0xFFFFFFFF);
textBox.setTextColor(0xFFFFFFFF);
```

## Code Examples

### Basic Text Input
```java
TextBox nameInput = new TextBox(10, 10, 200);
nameInput.setText("Enter your name");
nameInput.setOnTextChanged(name -> {
    System.out.println("Name: " + name);
});
gui.addChild(nameInput);
```

### Number Input with Validation
```java
TextBox priceInput = new TextBox(10, 40, 150);
// Allow positive decimals only, max 2 decimal places
String regex = TextBox.createRegex_onlyNumerical(true, false, 10, 2);
priceInput.setMatchRegex(regex);
priceInput.setText(19.99);

priceInput.setOnTextChanged(text -> {
    double price = priceInput.getDouble();
    updatePriceDisplay(price);
});
```

### Search Box
```java
TextBox searchBox = new TextBox(10, 10, 250);
searchBox.setAlignment(Alignment.LEFT);
searchBox.setOnTextChanged(searchText -> {
    filterResults(searchText.toLowerCase());
});

// Add visual indicator
searchBox.setFocusedBackgroundColor(0xFF2244AA);
searchBox.setTextColor(0xFFFFFFFF);
```

### Integer Range Input
```java
TextBox levelInput = new TextBox(10, 70, 100);
levelInput.setMatchRegex("^\\d{0,3}$");  // Max 3 digits
levelInput.setText(1);

levelInput.setOnTextChanged(text -> {
    int level = levelInput.getInt();
    if (level > 100) {
        levelInput.setText(100);
    } else if (level < 1 && !text.isEmpty()) {
        levelInput.setText(1);
    }
});
```

### Password Field (Display Masking)
```java
TextBox passwordBox = new TextBox(10, 100, 200);
StringBuilder actualPassword = new StringBuilder();

passwordBox.setOnTextChanged(displayText -> {
    int cursorPos = displayText.length();
    actualPassword.setLength(0);
    actualPassword.append(/* actual password logic */);
    
    // Mask with asterisks
    String masked = "*".repeat(displayText.length());
    passwordBox.setText(masked);
});
```

## Common Patterns

### Form Validation
```java
TextBox emailBox = new TextBox(10, 10, 300);
Label validationLabel = new Label("");

emailBox.setOnTextChanged(email -> {
    if (isValidEmail(email)) {
        validationLabel.setText("Valid email");
        validationLabel.setTextColor(0xFF44AA44);
        emailBox.setOutlineColor(0xFF44AA44);
    } else {
        validationLabel.setText("Invalid email format");
        validationLabel.setTextColor(0xFFAA4444);
        emailBox.setOutlineColor(0xFFAA4444);
    }
});
```

### Character Counter
```java
TextBox commentBox = new TextBox(10, 10, 400);
Label counterLabel = new Label("0 / 200");
commentBox.setMaxChars(200);

commentBox.setOnTextChanged(text -> {
    counterLabel.setText(text.length() + " / 200");
});
```

### Linked Input Fields
```java
TextBox widthBox = new TextBox(10, 10, 80);
TextBox heightBox = new TextBox(100, 10, 80);
CheckBox aspectRatio = new CheckBox("Lock Aspect Ratio");

widthBox.setMatchRegex("^\\d*$");
heightBox.setMatchRegex("^\\d*$");

widthBox.setOnTextChanged(text -> {
    if (aspectRatio.isChecked()) {
        int width = widthBox.getInt();
        heightBox.setText(width * 9 / 16);  // 16:9 ratio
    }
});
```

## Best Practices

1. **Input Validation**: Always set appropriate regex patterns to guide user input
2. **Feedback**: Use `setOnTextChanged` to provide immediate validation feedback
3. **Max Length**: Set reasonable `maxChars` limits to prevent excessive input
4. **Placeholder Text**: Consider using initial text as a placeholder (clear on first focus)
5. **Alignment**: Use LEFT alignment for most text, RIGHT for numbers/prices
6. **Error Indication**: Change outline or background color to indicate invalid input
7. **Type Conversion**: Use `getInt()`, `getDouble()`, `getLong()` for numeric inputs with error handling
8. **Focus Management**: Call `removeFocus()` when the user presses Enter to confirm input

## Technical Notes

- The cursor blinks with a 40-tick interval (approximately 2 seconds per cycle)
- Text selection supports mouse drag and shift+arrow keys
- The textbox automatically handles text overflow (no scrolling - consider limiting width)
- Clipboard operations use the Minecraft clipboard API
- The textbox extends `GuiElement` and supports all standard GUI features
- Font scale affects the label but the textbox height remains fixed
