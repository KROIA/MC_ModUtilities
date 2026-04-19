package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.ColorUtilities;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.awt.event.InputEvent;
import java.util.function.Consumer;

public class TextBox extends GuiElement {

    String text = "";

    //boolean allowAllChars = true;
    //boolean allowNumbers = true;
    //boolean allowDecimal = true;
    //boolean allowNegativeNumbers = true;
    //boolean allowLetters = true;
    private String matchRegex;

    private final Label textLabel;
    private int maxChars = 20;
    //private int maxDecimalChar = 20;
    private int cursorColor = 0xFF222222;
    private int selectionColor = 0xAA222222;
    private int backgroundColor;
    private int hoverBackgroundColor;
    private int focusedBackgroundColor;
    private int currentCursorPos = 0;
    private int cursorBlinkCounter = 0;
    private boolean cursorVisible = false;

    private int labelPadding = 2;

    private int selectionCursonIdxStart = -1;
    private int selectionCursonIdxEnd = -1;

    Consumer<String> textChangedFromUser = null;
    public TextBox(int x, int y, int width) {
        super(x, y, width, Label.DEFAULT_HEIGHT);
        matchRegex = ".*";

        backgroundColor = ColorUtilities.setBrightness(DEFAULT_BACKGROUND_COLOR, 0.8f);
        hoverBackgroundColor = ColorUtilities.setBrightness(backgroundColor, 0.8f);
        focusedBackgroundColor = ColorUtilities.setBrightness(backgroundColor, 0.6f);
        setOutlineColor(ColorUtilities.setBrightness(backgroundColor, 0.4f));

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

    //public void setAllowNumbers(boolean allowNumbers, boolean allowDecimal) {
    //    this.allowNumbers = allowNumbers;
    //    this.allowDecimal = allowDecimal;
    //}

    public void setMatchRegex(String matchRegex) {
        this.matchRegex = matchRegex;
    }
    public String getMatchRegex() {
        return matchRegex;
    }
    public static String createRegex_onlyNumerical(boolean allowPositive, boolean allowNegative, int maxDigits, int maxDecimalDigits)
    {
        // Build the sign part
        String sign;
        if (allowPositive && allowNegative) {
            sign = "-?";
        } else if (allowNegative) {
            sign = "-";
        } else {
            sign = "";
        }

        // Build the digits part — use {0,} instead of {1,} to allow mid-typing e.g. "-"
        String digits = maxDigits > 0 ? "\\d{0," + maxDigits + "}" : "\\d*";

        // Build the decimal part — digits after "." are optional to allow mid-typing e.g. "123."
        String decimal = maxDecimalDigits > 0 ? "(\\.\\d{0," + maxDecimalDigits + "})?" : "";

        // Combine into final regex
        return "^" + sign + digits + decimal + "$";
    }
    public static String createRegex_noNumbers()
    {
        return "^[^\\d]+$";
    }


    public void setAlignment(Alignment alignment) {
        this.textLabel.setAlignment(alignment);
    }
    /*public boolean isAllowingNumbers() {
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
    public void setAllowAllChars(boolean allowAllChars) {
        this.allowAllChars = allowAllChars;
    }
    public boolean isAllowAllChars() {
        return allowAllChars;
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
    }*/
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
        currentCursorPos = Math.min(text.length(), currentCursorPos);
        updateTextLabel();
    }
    public void setText(double value) {
       // setAllowNumbers(true, true);
        setText(String.valueOf(value));
    }
    public void setText(int value) {
       // setAllowNumbers(true, false);
        setText(String.valueOf(value));
    }
    public void setText(long value) {
        //setAllowNumbers(true, false);
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

        if(selectionCursonIdxStart > -1 && selectionCursonIdxEnd > -1)
        {
            int startX = getCursorXPos(selectionCursonIdxStart);
            int endX = getCursorXPos(selectionCursonIdxEnd);
            drawRect(startX, textLabel.getTop(), endX-startX, textLabel.getHeight(), selectionColor);
        }
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

                int cursorX = getCursorXPos(currentCursorPos);
                drawRect(cursorX+1, 3,1, getHeight()-6, cursorColor);
                drawRect(cursorX, 2,3, 1, cursorColor);
                drawRect(cursorX, getHeight()-4,3, 1, cursorColor);
            }
        }
    }
    private int getCursorXPos(int cursorPos)
    {
        int cursorX;
        if(cursorPos >= text.length())
            cursorX = textLabel.getTextWidth(text);
        else
            cursorX = textLabel.getTextWidth(text.substring(0, cursorPos));
        if(textLabel.getAlignment() == Alignment.RIGHT)
        {
            int textWidth = textLabel.getTextWidth(text);
            cursorX = textLabel.getWidth() - textWidth + cursorX;
        }
        else
            cursorX += textLabel.getX();
        return cursorX;
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
        double mouseX = getMouseX();
        int cursorPos = 0;
        if(textLabel.getAlignment() == Alignment.RIGHT)
        {
            int textWidth = textLabel.getTextWidth(text);
            mouseX -= textLabel.getWidth()-textWidth;
        }
        else
            mouseX -= textLabel.getX();

        for (int i = 0; i < text.length(); i++) {
            String subString = text.substring(0, i);
            int textWidth = textLabel.getTextWidth(subString);
            if (textWidth >= mouseX) {
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
        else
        {
            selectionCursonIdxStart = -1;
            selectionCursonIdxEnd = -1;
        }
    }

    @Override
    protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(!isFocused())
            return false;

        boolean isShiftDown = isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT);
        boolean isControlDown = isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL);



        switch(keyCode)
        {
            case GLFW.GLFW_KEY_A:
            {
                if(isControlDown) {
                    // select all
                    selectionCursonIdxStart = 0;
                    selectionCursonIdxEnd = text.length();
                    return true;
                }
                return false;
            }
            case GLFW.GLFW_KEY_C:
            {
                if(isControlDown && selectionCursonIdxStart != -1 && selectionCursonIdxEnd != -1)
                {
                    String subString = text.substring(selectionCursonIdxStart, selectionCursonIdxEnd);
                    // put substring into clipboard
                    Minecraft.getInstance().keyboardHandler.setClipboard(subString);
                    return true;
                }
                return false;
            }
            case GLFW.GLFW_KEY_X:
            {
                if(isControlDown && selectionCursonIdxStart != -1 && selectionCursonIdxEnd != -1)
                {
                    String subString = text.substring(selectionCursonIdxStart, selectionCursonIdxEnd);
                    text = text.substring(0, selectionCursonIdxStart) +  text.substring(selectionCursonIdxEnd);
                    // put substring into clipboard
                    Minecraft.getInstance().keyboardHandler.setClipboard(subString);
                    selectionCursonIdxStart = -1;
                    selectionCursonIdxEnd = -1;
                    updateTextLabel();
                    emitTextChanged();
                    return true;
                }
                return false;
            }
            case GLFW.GLFW_KEY_V:
            {
                if(isControlDown)
                {
                    String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                    String newText = text;
                    int currentCursorPosTmp = currentCursorPos;
                    if(selectionCursonIdxStart != -1 && selectionCursonIdxEnd != -1)
                    {
                        newText = text.substring(0, selectionCursonIdxStart) + text.substring(selectionCursonIdxEnd);
                        currentCursorPosTmp =  selectionCursonIdxStart;
                    }
                    String textToCursorTmp = newText.substring(0, currentCursorPosTmp);
                    String textAfterCursorTmp = newText.substring(currentCursorPosTmp);
                    newText = textToCursorTmp + clipboard + textAfterCursorTmp;
                    if(!strIsAllowed(newText))
                        return false;

                    boolean hasChanged = false;
                    if(selectionCursonIdxStart != -1 && selectionCursonIdxEnd != -1)
                    {
                        // overwrite the current selection
                        // remove selected text section
                        text = text.substring(0, selectionCursonIdxStart) + text.substring(selectionCursonIdxEnd);
                        currentCursorPos = selectionCursonIdxStart;
                        selectionCursonIdxStart = -1;
                        selectionCursonIdxEnd = -1;
                        hasChanged = true;
                    }


                    // Insert text
                    if(!clipboard.isEmpty()) {
                        String textToCursor = text.substring(0, currentCursorPos);
                        String textAfterCursor = text.substring(currentCursorPos);
                        text = textToCursor + clipboard + textAfterCursor;
                        hasChanged = true;
                    }
                    if(hasChanged) {
                        selectionCursonIdxStart = -1;
                        selectionCursonIdxEnd = -1;
                        updateTextLabel();
                        emitTextChanged();
                        return true;
                    }
                }
                return false;
            }
            case GLFW.GLFW_KEY_BACKSPACE:
            {
                // Check if CTRL is pressed, if so, remove last word
                if(isControlDown)
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
                else if(selectionCursonIdxStart != -1 && selectionCursonIdxEnd != -1)
                {
                    // remove selected text section
                    text = text.substring(0, selectionCursonIdxStart) + text.substring(selectionCursonIdxEnd);
                    currentCursorPos = selectionCursonIdxStart;
                    selectionCursonIdxStart = -1;
                    selectionCursonIdxEnd = -1;
                    updateTextLabel();
                    emitTextChanged();
                    return true;
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
                if(isControlDown)
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
                else if(selectionCursonIdxStart != -1 && selectionCursonIdxEnd != -1)
                {
                    // remove selected text section
                    String newText = text.substring(0, selectionCursonIdxStart) + text.substring(selectionCursonIdxEnd);
                    text = newText;
                    currentCursorPos = selectionCursonIdxStart;
                    selectionCursonIdxStart = -1;
                    selectionCursonIdxEnd = -1;
                    updateTextLabel();
                    emitTextChanged();
                    return true;
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
                    if(isControlDown && !isShiftDown)
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
                        selectionCursonIdxStart = -1;
                        selectionCursonIdxEnd = -1;
                    }
                    else if(isShiftDown && !isControlDown)
                    {
                        if(selectionCursonIdxEnd == -1)
                        {
                            selectionCursonIdxEnd =  currentCursorPos;
                        }
                        currentCursorPos--;
                        selectionCursonIdxStart = currentCursorPos;
                    }
                    else if(isShiftDown)
                    {
                        if(selectionCursonIdxEnd == -1)
                        {
                            selectionCursonIdxEnd =  currentCursorPos;
                        }
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
                        selectionCursonIdxStart = currentCursorPos;
                    }
                    else {
                        selectionCursonIdxStart = -1;
                        selectionCursonIdxEnd = -1;
                        currentCursorPos--;
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT:
            {
                if(currentCursorPos < text.length())
                {
                    if(isControlDown && !isShiftDown)
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
                        selectionCursonIdxStart = -1;
                        selectionCursonIdxEnd = -1;
                    }
                    else if(isShiftDown && !isControlDown)
                    {
                        if(selectionCursonIdxStart == -1)
                        {
                            selectionCursonIdxStart =  currentCursorPos;
                        }
                        currentCursorPos++;
                        selectionCursonIdxEnd = Math.min(currentCursorPos, text.length());
                    }
                    else if(isShiftDown)
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
                        if(selectionCursonIdxStart == -1)
                        {
                            selectionCursonIdxStart =  currentCursorPos;
                        }
                        if(nextSpace != -1)
                        {
                            currentCursorPos += nextSpace;
                        }
                        else
                        {
                            currentCursorPos = text.length();
                        }
                        selectionCursonIdxEnd = Math.min(currentCursorPos, text.length());
                    }
                    else {
                        selectionCursonIdxStart = -1;
                        selectionCursonIdxEnd = -1;
                        currentCursorPos++;
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
            case GLFW.GLFW_KEY_ESCAPE:
            {
                selectionCursonIdxEnd = -1;
                selectionCursonIdxStart = -1;
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
            if(selectionCursonIdxStart != -1 && selectionCursonIdxEnd != -1)
            {
                // remove selected text section
                text = text.substring(0, selectionCursonIdxStart) + text.substring(selectionCursonIdxEnd);
                currentCursorPos = selectionCursonIdxStart;
                selectionCursonIdxStart = -1;
                selectionCursonIdxEnd = -1;
                //updateTextLabel();
                //emitTextChanged();
            }

            //if(text.length() >= maxChars)
            //    return false;
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

    private boolean strIsAllowed(String str)
    {
        if(str.length() > maxChars)
            return false;
        return str.matches(matchRegex);
    }
    private boolean canConsume(char codePoint)
    {
        String newText = text.substring(0, currentCursorPos) + codePoint + text.substring(currentCursorPos);
        return strIsAllowed(newText);

       /* if(text.length() >= maxChars)
            return false;
        if(allowAllChars)
            return true;
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
            //String leftPart = newStr;
            String rightPart = "";
            if(decimalIndex != -1)
            {
                //leftPart = newStr.substring(0, decimalIndex);
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
        return false; // Disallow all other characters*/
    }
}
