package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class TextBox extends GuiElement {

    String text = "";

    boolean allowNumbers = true;
    boolean allowDecimal = true;
    boolean allowNegativeNumbers = true;
    boolean allowLetters = true;

    private final Label textLabel;
    private int maxChars = 20;
    private int maxDecimalChar = 20;
    private int cursorColor = 0xFF222222;
    private int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private int hoverBackgroundColor = DEFAULT_HOVER_BACKGROUND_COLOR;
    private int focusedBackgroundColor = DEFAULT_FOCUSED_BACKGROUND_COLOR;
    private int currentCursorPos = 0;
    private int cursorBlinkCounter = 0;
    private boolean cursorVisible = false;

    private int labelPadding = 2;

    Consumer<String> textChangedFromUser = null;
    public TextBox(int x, int y, int width) {
        super(x, y, width, Label.DEFAULT_HEIGHT);
        textLabel = new Label("");
        textLabel.setBounds(labelPadding, 0, width-2*labelPadding, Label.DEFAULT_HEIGHT);
        textLabel.setAlignment(Alignment.LEFT);

        addChild(textLabel);
        textLabel.setText(text);
        addChild(textLabel);
    }
    public TextBox() {
        this(0,0,100);
    }

    public void setAllowNumbers(boolean allowNumbers, boolean allowDecimal) {
        this.allowNumbers = allowNumbers;
        this.allowDecimal = allowDecimal;
    }

    public boolean isAllowingNumbers() {
        return allowNumbers;
    }
    public boolean isAllowingDecimal() {
        return allowDecimal;
    }
    public void setAllowNegativeNumbers(boolean allowNegativeNumbers) {
        this.allowNegativeNumbers = allowNegativeNumbers;
    }
    public boolean isAllowingNegativeNumbers() {
        return allowNegativeNumbers;
    }
    public void setMaxDecimalChar(int maxDecimalChar) {
        this.maxDecimalChar = maxDecimalChar;
    }
    public int getMaxDecimalChar() {
        return maxDecimalChar;
    }
    public void setAllowLetters(boolean allowLetters) {
        this.allowLetters = allowLetters;
    }
    public boolean isAllowingLetters() {
        return allowLetters;
    }
    public void setCursorColor(int cursorColor) {
        this.cursorColor = cursorColor;
    }
    public int getCursorColor() {
        return cursorColor;
    }
    @Override
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    @Override
    public int getBackgroundColor() {
        return this.backgroundColor;
    }
    public void setHoverBackgroundColor(int hoverBackgroundColor) {
        this.hoverBackgroundColor = hoverBackgroundColor;
    }
    public int getHoverBackgroundColor() {
        return hoverBackgroundColor;
    }
    public void setFocusedBackgroundColor(int focusedBackgroundColor) {
        this.focusedBackgroundColor = focusedBackgroundColor;
    }
    public int getFocusedBackgroundColor() {
        return focusedBackgroundColor;
    }

    public void setOnTextChanged(Consumer<String> textChangedFromUser) {
        this.textChangedFromUser = textChangedFromUser;
    }

    @Override
    public void setTextColor(int color) {
        textLabel.setTextColor(color);
    }
    @Override
    public int getTextColor() {
        return textLabel.getTextColor();
    }
    @Override
    public void setTextFontScale(float scale) {
        textLabel.setTextFontScale(scale);
    }
    @Override
    public float getTextFontScale() {
        return textLabel.getTextFontScale();
    }

    public String getText() {
        return text;
    }
    public double getDouble() {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    public int getInt() {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    public long getLong() {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setText(String text) {
        this.text = text;
        currentCursorPos = text.length();
        updateTextLabel();
    }
    public void setText(double value) {
        setAllowNumbers(true, true);
        setText(String.valueOf(value));
    }
    public void setText(int value) {
        setAllowNumbers(true, false);
        setText(String.valueOf(value));
    }
    public void setText(long value) {
        setAllowNumbers(true, false);
        setText(String.valueOf(value));
    }
    public void setMaxChars(int maxChars) {
        this.maxChars = maxChars;
    }
    public int getMaxChars() {
        return maxChars;
    }

    @Override
    protected void renderBackground()
    {
        super.setBackgroundColor(isFocused()?focusedBackgroundColor:(isMouseOver()?hoverBackgroundColor:backgroundColor));
        super.renderBackground();
    }
    @Override
    protected void render() {

        // Draw cursor
        if(isFocused())
        {
            cursorBlinkCounter++;
            if(cursorBlinkCounter > 40)
            {
                cursorBlinkCounter = 0;
                cursorVisible = !cursorVisible;
            }
            if(cursorVisible) {

                int cursorX = textLabel.getTextWidth(text.substring(0, currentCursorPos)) + textLabel.getX();
                drawRect(cursorX+1, 3,1, getHeight()-6, cursorColor);
                drawRect(cursorX, 2,3, 1, cursorColor);
                drawRect(cursorX, getHeight()-4,3, 1, cursorColor);
            }
        }
    }

    @Override
    protected void layoutChanged() {
        textLabel.setBounds(labelPadding, labelPadding, getWidth()-2*labelPadding, getHeight()-2*labelPadding);
    }

    @Override
    public boolean mouseClickedOverElement(int button)
    {
        setFocused();
        // Get cursor position
        double mouseX = getMouseX()-textLabel.getX();
        int cursorPos = 0;
        for (int i = 0; i < text.length(); i++) {
            String subString = text.substring(0, i);
            int textWidth = textLabel.getTextWidth(subString);
            if(textWidth >= mouseX)
            {
                break;
            }
            cursorPos++;
        }
        currentCursorPos = cursorPos;
        return true;
    }

    @Override
    public void focusGained() {
    }
    @Override
    public void focusLost() {
        cursorVisible = false;
    }

    @Override
    protected void mouseClicked(int button) {
        if(!isMouseOver())
        {
            removeFocus();
        }
    }

    @Override
    protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(!isFocused())
            return false;

        switch(keyCode)
        {
            case GLFW.GLFW_KEY_BACKSPACE:
            {
                // Check if CTRL is pressed, if so, remove last word
                if((modifiers & GLFW.GLFW_MOD_CONTROL) == GLFW.GLFW_MOD_CONTROL)
                {
                    String textToCursor = text.substring(0, currentCursorPos);
                    String textAfterCursor = text.substring(currentCursorPos);
                    int lastSpace = textToCursor.length()-1;
                    // Find first char that is not a space
                    while(lastSpace > 0 && textToCursor.charAt(lastSpace) == ' ')
                    {
                        lastSpace--;
                    }
                    // Find next space
                    lastSpace = textToCursor.lastIndexOf(' ', lastSpace);
                    if(lastSpace != -1)
                    {
                        textToCursor = textToCursor.substring(0, lastSpace+1);
                        currentCursorPos = textToCursor.length();
                        text = textToCursor + textAfterCursor;
                        updateTextLabel();
                        emitTextChanged();
                        return true;
                    }
                    else
                    {
                        textToCursor = "";
                        text = textToCursor + textAfterCursor;
                        currentCursorPos = 0;
                        updateTextLabel();
                        emitTextChanged();
                        return true;
                    }
                }
                else
                {
                    String textToCursor = text.substring(0, currentCursorPos);
                    String textAfterCursor = text.substring(currentCursorPos);
                    if(!textToCursor.isEmpty())
                    {
                        textToCursor = textToCursor.substring(0, textToCursor.length() - 1);
                        currentCursorPos = textToCursor.length();
                        text = textToCursor + textAfterCursor;
                        updateTextLabel();
                        emitTextChanged();
                        return true;
                    }
                }
                return false;
            }
            case GLFW.GLFW_KEY_DELETE:
            {
                // Check if CTRL is pressed, if so, remove next word
                if((modifiers & GLFW.GLFW_MOD_CONTROL) == GLFW.GLFW_MOD_CONTROL)
                {
                    String textToCursor = text.substring(0, currentCursorPos);
                    String textAfterCursor = text.substring(currentCursorPos);
                    int nextSpace = 0;
                    // Find first char that is not a space
                    while(nextSpace < textAfterCursor.length() && textAfterCursor.charAt(nextSpace) == ' ')
                    {
                        nextSpace++;
                    }
                    // Find next space
                    nextSpace = textAfterCursor.indexOf(' ', nextSpace);

                    if(nextSpace != -1)
                    {
                        textAfterCursor = textAfterCursor.substring(nextSpace);
                        text = textToCursor + textAfterCursor;
                        updateTextLabel();
                        emitTextChanged();
                        return true;
                    }
                    else
                    {
                        textAfterCursor = "";
                        text = textToCursor + textAfterCursor;
                        updateTextLabel();
                        emitTextChanged();
                        return true;
                    }
                }
                else
                {
                    String textToCursor = text.substring(0, currentCursorPos);
                    String textAfterCursor = text.substring(currentCursorPos);
                    if(!textAfterCursor.isEmpty())
                    {
                        textAfterCursor = textAfterCursor.substring(1);
                        text = textToCursor + textAfterCursor;
                        updateTextLabel();
                        emitTextChanged();
                        return true;
                    }
                }
                return false;
            }
            case GLFW.GLFW_KEY_LEFT:
            {
                if(currentCursorPos > 0)
                {
                    if((modifiers & GLFW.GLFW_MOD_CONTROL) == GLFW.GLFW_MOD_CONTROL)
                    {
                        String textToCursor = text.substring(0, currentCursorPos);
                        int lastSpace = textToCursor.length()-1;
                        // Find first char that is not a space
                        while(lastSpace > 0 && textToCursor.charAt(lastSpace) == ' ')
                        {
                            lastSpace--;
                        }
                        // Find next space
                        lastSpace = textToCursor.lastIndexOf(' ', lastSpace);


                        if(lastSpace != -1)
                        {
                            currentCursorPos = lastSpace+1;
                        }
                        else
                        {
                            currentCursorPos = 0;
                        }
                    }
                    else
                        currentCursorPos--;
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT:
            {
                if(currentCursorPos < text.length())
                {
                    if((modifiers & GLFW.GLFW_MOD_CONTROL) == GLFW.GLFW_MOD_CONTROL)
                    {
                        String textAfterCursor = text.substring(currentCursorPos);
                        int nextSpace = 0;
                        // Find first char that is not a space
                        while(nextSpace < textAfterCursor.length() && textAfterCursor.charAt(nextSpace) == ' ')
                        {
                            nextSpace++;
                        }
                        // Find next space
                        nextSpace = textAfterCursor.indexOf(' ', nextSpace);

                        if(nextSpace != -1)
                        {
                            currentCursorPos += nextSpace;
                        }
                        else
                        {
                            currentCursorPos = text.length();
                        }
                    }
                    else
                        currentCursorPos++;
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
            case GLFW.GLFW_KEY_ESCAPE:
            {
                removeFocus();
                return true;
            }
        }

        return true;
    }

    @Override
    protected boolean charTyped(char codePoint, int modifiers) {
        if(!isFocused())
            return false;

        if(canConsume(codePoint))
        {
            if(text.length() >= maxChars)
                return false;
            // Insert character at cursor position
            text = text.substring(0, currentCursorPos) + codePoint + text.substring(currentCursorPos);
            currentCursorPos++;
            updateTextLabel();
            emitTextChanged();
            return true;
        }
        return false;
    }
    private void updateTextLabel()
    {
        textLabel.setText(text);
    }
    private void emitTextChanged()
    {
        if(textChangedFromUser != null)
            textChangedFromUser.accept(getText());
    }
    private boolean canConsume(char codePoint)
    {
        if(text.length() >= maxChars)
            return false;
        if(allowLetters && Character.isLetter(codePoint)) {
            return true; // Allow letters
        }

        if(allowNumbers)
        {
            if(!allowDecimal && codePoint == '.')
                return false; // Disallow decimal point if not allowed
            if((!allowNegativeNumbers || currentCursorPos > 0) && codePoint == '-')
                return false; // Disallow negative sign if not allowed
            //boolean isDigit = Character.isDigit(codePoint);
            String newStr = text.substring(0, currentCursorPos) + codePoint + text.substring(currentCursorPos);
            int decimalIndex = newStr.indexOf('.');
            String leftPart = newStr;
            String rightPart = "";
            if(decimalIndex != -1)
            {
                leftPart = newStr.substring(0, decimalIndex);
                rightPart = newStr.substring(decimalIndex + 1);
            }

            // Check if the right part has more than maxDecimalChar digits
            if(rightPart.length() > maxDecimalChar)
                return false; // Disallow more than maxDecimalChar digits in the right part

            boolean isDigit = Character.isDigit(codePoint);
            if(isDigit)
            {
                if(rightPart.indexOf('.') != -1)
                    return false; // Disallow multiple decimal points

                return true; // Allow digits
            }

            else if(allowDecimal && codePoint == '.')
            {
                // Allow decimal point only if it is at the position of the cursor
                return text.indexOf('.') == -1;
            }
            else if(allowNegativeNumbers && codePoint == '-' && currentCursorPos == 0)
            {
                return text.indexOf('-') == -1; // Allow negative sign only at the beginning and only once
            }
        }
        return false; // Disallow all other characters
    }
}
