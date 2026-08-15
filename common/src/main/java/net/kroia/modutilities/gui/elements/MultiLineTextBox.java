package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.ColorUtilities;
import net.kroia.modutilities.gui.InputConstants;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A multi-line text input widget with word-wrapping, selection, clipboard and
 * a vertical scrollbar.
 * <p>
 * Unlike {@link TextBox} the Enter key inserts a newline character instead of
 * committing / clearing focus. Content is stored internally as a list of
 * source lines separated by {@code \n} characters; visual line breaks are
 * computed from the widget's inner width using the current font.
 * <p>
 * Supported keyboard shortcuts while focused:
 * <ul>
 *     <li><b>Enter</b> - insert {@code \n} at cursor</li>
 *     <li><b>Backspace / Delete</b> - delete previous/next char (merges lines)</li>
 *     <li><b>Left / Right / Up / Down</b> - move cursor</li>
 *     <li><b>Home / End</b> - jump to line start / end</li>
 *     <li><b>Ctrl+Home / Ctrl+End</b> - jump to document start / end</li>
 *     <li><b>Ctrl+A</b> - select all</li>
 *     <li><b>Shift+Arrows / Home / End</b> - extend selection</li>
 *     <li><b>Ctrl+C / Ctrl+X / Ctrl+V</b> - copy / cut / paste</li>
 * </ul>
 */
public class MultiLineTextBox extends GuiElement {

    private static final int PADDING = 4;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int BLINK_TICKS = 45;

    private final List<String> lines = new ArrayList<>();
    private int cursorLine = 0;
    private int cursorCol = 0;
    private int desiredCol = 0;

    private int selAnchorLine = -1;
    private int selAnchorCol = -1;

    private String placeholder = "";
    private int placeholderColor = 0xFF888888;
    private int maxLength = 0;
    private int maxCharsPerLine = 0;
    private int maxRows = 0;
    private boolean editable = true;

    private int cursorColor = 0xFFFFFFFF;
    private int selectionColor = 0x66336699;
    private int scrollbarColor = 0xFF666666;
    private int scrollbarTrackColor = 0x44000000;
    private int backgroundColor;
    private int hoverBackgroundColor;
    private int focusedBackgroundColor;

    private int scrollY = 0;
    private boolean scrollbarDragging = false;
    private double scrollbarDragOffset = 0;

    private boolean selecting = false;

    private int cursorBlinkCounter = 0;
    private boolean cursorVisible = true;

    private Consumer<String> textChangedFromUser = null;

    private final List<VisualLine> visualLines = new ArrayList<>();

    private static final class VisualLine {
        final int sourceLine;
        final int startCol;
        final int endCol;
        final String text;

        VisualLine(int sourceLine, int startCol, int endCol, String text) {
            this.sourceLine = sourceLine;
            this.startCol = startCol;
            this.endCol = endCol;
            this.text = text;
        }
    }

    /**
     * Creates a multi-line text box at the given position and size.
     * @param x the x-coordinate (relative to the parent)
     * @param y the y-coordinate (relative to the parent)
     * @param width the width in pixels
     * @param height the height in pixels
     */
    public MultiLineTextBox(int x, int y, int width, int height) {
        super(x, y, width, height);
        lines.add("");
        backgroundColor = ColorUtilities.setBrightness(DEFAULT_BACKGROUND_COLOR, 0.8f);
        hoverBackgroundColor = ColorUtilities.setBrightness(backgroundColor, 0.8f);
        focusedBackgroundColor = ColorUtilities.setBrightness(backgroundColor, 0.6f);
        setOutlineColor(ColorUtilities.setBrightness(backgroundColor, 0.4f));
    }

    /**
     * Creates a multi-line text box with default position (0,0) and size 150x80.
     */
    public MultiLineTextBox() {
        this(0, 0, 150, 80);
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /** @return the full text content with {@code \n} separators. */
    public String getText() {
        return String.join("\n", lines);
    }

    /**
     * Replaces the text content. Cursor is clamped to document end and any
     * active selection is cleared. Does not fire the text-changed callback.
     * @param text the new text content
     */
    public void setText(String text) {
        lines.clear();
        if (text == null || text.isEmpty()) {
            lines.add("");
        } else {
            for (String s : text.split("\n", -1)) {
                lines.add(s);
            }
        }
        cursorLine = lines.size() - 1;
        cursorCol = lines.get(cursorLine).length();
        desiredCol = cursorCol;
        clearSelection();
        rebuildWrap();
        markDirty();
    }

    /**
     * Sets the placeholder shown in grey when the box is empty and unfocused.
     * @param placeholder the placeholder text (may be {@code null})
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
    }

    /**
     * Sets the maximum number of characters. Newlines count as one character.
     * @param maxLength the character cap, or {@code 0} for unlimited
     */
    public void setMaxLength(int maxLength) {
        this.maxLength = Math.max(0, maxLength);
    }

    public int getMaxLength() { return maxLength; }

    /**
     * Sets the maximum number of characters allowed in a single source line.
     * Newlines are not counted. {@code 0} disables the per-line cap.
     */
    public void setMaxCharsPerLine(int maxCharsPerLine) {
        this.maxCharsPerLine = Math.max(0, maxCharsPerLine);
    }

    public int getMaxCharsPerLine() { return maxCharsPerLine; }

    /**
     * Sets the maximum number of source lines (rows). {@code 0} disables the cap.
     * Combined with {@link #setMaxCharsPerLine(int)} this yields an effective cap of
     * {@code maxCharsPerLine * maxRows} content characters.
     */
    public void setMaxRows(int maxRows) {
        this.maxRows = Math.max(0, maxRows);
    }

    public int getMaxRows() { return maxRows; }

    /**
     * Toggles read-only mode. When {@code false} selection and copy still work
     * but no edits are permitted.
     * @param editable {@code true} to allow edits
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /** @return {@code true} if the box currently accepts edits. */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Sets a callback invoked when the user changes the text. Programmatic
     * mutations via {@link #setText(String)} do <i>not</i> trigger this.
     * @param cb the callback receiving the new text, or {@code null} to clear
     */
    public void setOnTextChanged(Consumer<String> cb) {
        this.textChangedFromUser = cb;
    }

    public void setSelectionColor(int c) { this.selectionColor = c; }
    public void setCursorColor(int c) { this.cursorColor = c; }
    public void setPlaceholderColor(int c) { this.placeholderColor = c; }
    @Override public void setBackgroundColor(int c) { this.backgroundColor = c; }
    @Override public int getBackgroundColor() { return this.backgroundColor; }
    public void setHoverBackgroundColor(int c) { this.hoverBackgroundColor = c; }
    public void setFocusedBackgroundColor(int c) { this.focusedBackgroundColor = c; }

    // ---------------------------------------------------------------------
    // Serialization
    // ---------------------------------------------------------------------

    @Override
    public SyncCategory getSyncCategory() { return SyncCategory.INPUT; }

    @Override
    public List<GuiElement> getSerializableChildren() { return List.of(); }

    @Override
    public CompoundTag serializeState() {
        CompoundTag tag = super.serializeState();
        tag.putString("text", getText());
        return tag;
    }

    @Override
    public void deserializeState(CompoundTag tag) {
        super.deserializeState(tag);
        if (tag.contains("text"))
            setText(tag.getString("text"));
    }

    // ---------------------------------------------------------------------
    // Layout / wrapping
    // ---------------------------------------------------------------------

    @Override
    protected void layoutChanged() {
        rebuildWrap();
    }

    private int innerWidth() {
        int w = getWidth() - 2 * PADDING;
        if (needsScrollbar())
            w -= SCROLLBAR_WIDTH + 2;
        return Math.max(1, w);
    }

    private int innerWidthForWrap() {
        // Reserve scrollbar width preemptively when overflowing to keep wrap stable.
        int w = getWidth() - 2 * PADDING - SCROLLBAR_WIDTH - 2;
        return Math.max(1, w);
    }

    private int innerHeight() {
        return Math.max(1, getHeight() - 2 * PADDING);
    }

    private boolean needsScrollbar() {
        return contentHeight() > innerHeight();
    }

    private int contentHeight() {
        return visualLines.size() * getTextHeight();
    }

    private int maxScrollY() {
        return Math.max(0, contentHeight() - innerHeight());
    }

    private void rebuildWrap() {
        visualLines.clear();
        int wrapWidth = innerWidthForWrap();
        for (int li = 0; li < lines.size(); li++) {
            wrapLine(li, lines.get(li), wrapWidth);
        }
        if (visualLines.isEmpty()) {
            visualLines.add(new VisualLine(0, 0, 0, ""));
        }
        clampScroll();
    }

    private void wrapLine(int sourceLine, String text, int wrapWidth) {
        if (text.isEmpty()) {
            visualLines.add(new VisualLine(sourceLine, 0, 0, ""));
            return;
        }
        int start = 0;
        int len = text.length();
        while (start < len) {
            int end = findWrapEnd(text, start, wrapWidth);
            if (end <= start) end = start + 1;
            visualLines.add(new VisualLine(sourceLine, start, end, text.substring(start, end)));
            start = end;
        }
    }

    private int findWrapEnd(String text, int start, int wrapWidth) {
        int len = text.length();
        int lastSpaceEnd = -1;
        int i = start;
        int currentWidth = 0;
        while (i < len) {
            char c = text.charAt(i);
            int cw = getTextWidth(String.valueOf(c));
            if (currentWidth + cw > wrapWidth && i > start) {
                if (lastSpaceEnd > start)
                    return lastSpaceEnd;
                return i;
            }
            currentWidth += cw;
            i++;
            if (c == ' ') lastSpaceEnd = i;
        }
        return len;
    }

    private void clampScroll() {
        int max = maxScrollY();
        if (scrollY > max) scrollY = max;
        if (scrollY < 0) scrollY = 0;
    }

    // ---------------------------------------------------------------------
    // Cursor / selection helpers
    // ---------------------------------------------------------------------

    private void clearSelection() {
        selAnchorLine = -1;
        selAnchorCol = -1;
    }

    private boolean hasSelection() {
        if (selAnchorLine < 0) return false;
        return !(selAnchorLine == cursorLine && selAnchorCol == cursorCol);
    }

    private void startSelectionIfNone() {
        if (selAnchorLine < 0) {
            selAnchorLine = cursorLine;
            selAnchorCol = cursorCol;
        }
    }

    private int[] selectionOrdered() {
        if (!hasSelection()) return null;
        int aL = selAnchorLine, aC = selAnchorCol, bL = cursorLine, bC = cursorCol;
        if (aL > bL || (aL == bL && aC > bC)) {
            return new int[] { bL, bC, aL, aC };
        }
        return new int[] { aL, aC, bL, bC };
    }

    private String extractSelection() {
        int[] s = selectionOrdered();
        if (s == null) return "";
        int aL = s[0], aC = s[1], bL = s[2], bC = s[3];
        if (aL == bL) return lines.get(aL).substring(aC, bC);
        StringBuilder sb = new StringBuilder();
        sb.append(lines.get(aL).substring(aC)).append('\n');
        for (int i = aL + 1; i < bL; i++) sb.append(lines.get(i)).append('\n');
        sb.append(lines.get(bL), 0, bC);
        return sb.toString();
    }

    private void deleteSelection() {
        int[] s = selectionOrdered();
        if (s == null) return;
        int aL = s[0], aC = s[1], bL = s[2], bC = s[3];
        String head = lines.get(aL).substring(0, aC);
        String tail = lines.get(bL).substring(bC);
        // Remove lines between
        for (int i = bL; i > aL; i--) lines.remove(i);
        lines.set(aL, head + tail);
        cursorLine = aL;
        cursorCol = aC;
        desiredCol = cursorCol;
        clearSelection();
    }

    private int totalChars() {
        int n = 0;
        for (String s : lines) n += s.length();
        return n + Math.max(0, lines.size() - 1); // newlines
    }

    private int selectionCharCount() {
        return extractSelection().length();
    }

    private boolean canInsert(int count) {
        if (maxLength <= 0) return true;
        int cur = totalChars() - selectionCharCount();
        return cur + count <= maxLength;
    }

    private int effectiveCursorLineLength() {
        if (!hasSelection()) return lines.get(cursorLine).length();
        int aL = selAnchorLine, aC = selAnchorCol, bL = cursorLine, bC = cursorCol;
        if (aL > bL || (aL == bL && aC > bC)) { int tL=aL,tC=aC; aL=bL; aC=bC; bL=tL; bC=tC; }
        return aC + (lines.get(bL).length() - bC);
    }

    private int effectiveRowCount() {
        if (!hasSelection()) return lines.size();
        int aL = Math.min(selAnchorLine, cursorLine);
        int bL = Math.max(selAnchorLine, cursorLine);
        return lines.size() - (bL - aL);
    }

    private boolean canInsertCharOnCurrentLine() {
        return maxCharsPerLine <= 0 || effectiveCursorLineLength() + 1 <= maxCharsPerLine;
    }

    private boolean canAddRow() {
        return maxRows <= 0 || effectiveRowCount() + 1 <= maxRows;
    }

    private String clipForPaste(String clip) {
        if (maxCharsPerLine <= 0 && maxRows <= 0) return clip;
        String[] parts = clip.split("\n", -1);
        int allowedParts = parts.length;
        if (maxRows > 0) {
            int rowsBudget = Math.max(0, maxRows - effectiveRowCount());
            allowedParts = Math.min(allowedParts, rowsBudget + 1);
            if (allowedParts <= 0) return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allowedParts; i++) {
            String p = parts[i];
            if (maxCharsPerLine > 0 && p.length() > maxCharsPerLine)
                p = p.substring(0, maxCharsPerLine);
            if (i > 0) sb.append('\n');
            sb.append(p);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // Insertion helpers
    // ---------------------------------------------------------------------

    private void insertChar(char c) {
        if (hasSelection()) deleteSelection();
        String line = lines.get(cursorLine);
        lines.set(cursorLine, line.substring(0, cursorCol) + c + line.substring(cursorCol));
        cursorCol++;
        desiredCol = cursorCol;
    }

    private void insertNewline() {
        if (hasSelection()) deleteSelection();
        String line = lines.get(cursorLine);
        String head = line.substring(0, cursorCol);
        String tail = line.substring(cursorCol);
        lines.set(cursorLine, head);
        lines.add(cursorLine + 1, tail);
        cursorLine++;
        cursorCol = 0;
        desiredCol = 0;
    }

    private void insertString(String text) {
        if (text.isEmpty()) return;
        if (hasSelection()) deleteSelection();
        String[] parts = text.split("\n", -1);
        if (parts.length == 1) {
            String line = lines.get(cursorLine);
            lines.set(cursorLine, line.substring(0, cursorCol) + parts[0] + line.substring(cursorCol));
            cursorCol += parts[0].length();
        } else {
            String line = lines.get(cursorLine);
            String head = line.substring(0, cursorCol);
            String tail = line.substring(cursorCol);
            lines.set(cursorLine, head + parts[0]);
            for (int i = 1; i < parts.length - 1; i++) {
                lines.add(cursorLine + i, parts[i]);
            }
            String last = parts[parts.length - 1];
            lines.add(cursorLine + parts.length - 1, last + tail);
            cursorLine += parts.length - 1;
            cursorCol = last.length();
        }
        desiredCol = cursorCol;
    }

    // ---------------------------------------------------------------------
    // Visual/source mapping
    // ---------------------------------------------------------------------

    private int findVisualLineIndex(int srcLine, int srcCol) {
        int best = 0;
        for (int i = 0; i < visualLines.size(); i++) {
            VisualLine vl = visualLines.get(i);
            if (vl.sourceLine != srcLine) continue;
            best = i;
            if (srcCol >= vl.startCol && srcCol <= vl.endCol) {
                // Prefer beginning of next visual line only if we're not at last vline of source.
                return i;
            }
            if (srcCol < vl.startCol) return i;
        }
        return best;
    }

    private int cursorXInVisualLine(VisualLine vl, int srcCol) {
        int col = Math.max(vl.startCol, Math.min(vl.endCol, srcCol));
        return getTextWidth(vl.text.substring(0, col - vl.startCol));
    }

    // ---------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------

    @Override
    protected void renderBackground() {
        int bg = isFocused() ? focusedBackgroundColor : (isMouseOver() ? hoverBackgroundColor : backgroundColor);
        super.setBackgroundColor(bg);
        super.renderBackground();
    }

    @Override
    protected void render() {
        int lineH = getTextHeight();
        int contentX = PADDING;
        int contentY = PADDING;
        int viewW = innerWidth();
        int viewH = innerHeight();

        enableScissor(contentX, contentY, viewW, viewH);

        // Placeholder
        boolean isEmpty = lines.size() == 1 && lines.get(0).isEmpty();
        if (isEmpty && !isFocused() && !placeholder.isEmpty()) {
            drawText(placeholder, contentX, contentY, placeholderColor);
        }

        // Selection
        int[] sel = selectionOrdered();
        if (sel != null) {
            drawSelectionHighlights(contentX, contentY, sel, lineH);
        }

        // Text
        for (int i = 0; i < visualLines.size(); i++) {
            VisualLine vl = visualLines.get(i);
            int y = contentY + i * lineH - scrollY;
            if (y + lineH < contentY || y > contentY + viewH) continue;
            if (!vl.text.isEmpty())
                drawText(vl.text, contentX, y, getTextColor());
        }

        // Cursor
        if (isFocused()) {
            cursorBlinkCounter++;
            if (cursorBlinkCounter >= BLINK_TICKS) {
                cursorBlinkCounter = 0;
                cursorVisible = !cursorVisible;
            }
            if (cursorVisible) {
                int vIdx = findVisualLineIndex(cursorLine, cursorCol);
                VisualLine vl = visualLines.get(vIdx);
                int cx = contentX + cursorXInVisualLine(vl, cursorCol);
                int cy = contentY + vIdx * lineH - scrollY;
                drawRect(cx, cy, 1, lineH, cursorColor);
            }
        }

        disableScissor();

        // Scrollbar
        if (needsScrollbar()) {
            drawScrollbar();
        }
    }

    private void drawSelectionHighlights(int contentX, int contentY, int[] sel, int lineH) {
        int aL = sel[0], aC = sel[1], bL = sel[2], bC = sel[3];
        int viewH = innerHeight();
        for (int i = 0; i < visualLines.size(); i++) {
            VisualLine vl = visualLines.get(i);
            if (vl.sourceLine < aL || vl.sourceLine > bL) continue;

            int lineStart, lineEnd;
            if (vl.sourceLine == aL && vl.sourceLine == bL) {
                lineStart = Math.max(vl.startCol, aC);
                lineEnd = Math.min(vl.endCol, bC);
            } else if (vl.sourceLine == aL) {
                lineStart = Math.max(vl.startCol, aC);
                lineEnd = vl.endCol;
            } else if (vl.sourceLine == bL) {
                lineStart = vl.startCol;
                lineEnd = Math.min(vl.endCol, bC);
            } else {
                lineStart = vl.startCol;
                lineEnd = vl.endCol;
            }
            if (lineEnd < lineStart) continue;

            int x1 = contentX + cursorXInVisualLine(vl, lineStart);
            int x2 = contentX + cursorXInVisualLine(vl, lineEnd);
            int w = Math.max(1, x2 - x1);
            // Extend a hair for line-spanning selections to hint the newline.
            boolean includesNewline = (vl.sourceLine < bL) && (lineEnd == vl.endCol) &&
                    (visualLines.indexOf(vl) == lastVisualIndexOf(vl.sourceLine));
            if (includesNewline) w += 3;
            int y = contentY + i * lineH - scrollY;
            if (y + lineH < contentY || y > contentY + viewH) continue;
            drawRect(x1, y, w, lineH, selectionColor);
        }
    }

    private int lastVisualIndexOf(int sourceLine) {
        int last = -1;
        for (int i = 0; i < visualLines.size(); i++) {
            if (visualLines.get(i).sourceLine == sourceLine) last = i;
        }
        return last;
    }

    private void drawScrollbar() {
        int trackX = getWidth() - PADDING - SCROLLBAR_WIDTH;
        int trackY = PADDING;
        int trackH = innerHeight();
        drawRect(trackX, trackY, SCROLLBAR_WIDTH, trackH, scrollbarTrackColor);
        int contentH = contentHeight();
        int thumbH = Math.max(10, (int) ((long) trackH * trackH / contentH));
        int maxScroll = maxScrollY();
        int thumbY = trackY + (maxScroll == 0 ? 0 : (int) ((long) (trackH - thumbH) * scrollY / maxScroll));
        drawRect(trackX, thumbY, SCROLLBAR_WIDTH, thumbH, scrollbarColor);
    }

    // ---------------------------------------------------------------------
    // Mouse input
    // ---------------------------------------------------------------------

    @Override
    protected void mouseClicked(int button) {
        if (!isMouseOver()) {
            removeFocus();
            selecting = false;
            scrollbarDragging = false;
        }
    }

    @Override
    public boolean mouseClickedOverElement(int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false;
        setFocused();
        int mx = getMouseX();
        int my = getMouseY();

        // Scrollbar hit?
        if (needsScrollbar()) {
            int trackX = getWidth() - PADDING - SCROLLBAR_WIDTH;
            if (mx >= trackX && mx < trackX + SCROLLBAR_WIDTH) {
                scrollbarDragging = true;
                int trackH = innerHeight();
                int contentH = contentHeight();
                int thumbH = Math.max(10, (int) ((long) trackH * trackH / contentH));
                int maxScroll = maxScrollY();
                int thumbY = PADDING + (maxScroll == 0 ? 0 : (int) ((long) (trackH - thumbH) * scrollY / maxScroll));
                if (my >= thumbY && my < thumbY + thumbH) {
                    scrollbarDragOffset = my - thumbY;
                } else {
                    scrollbarDragOffset = thumbH / 2.0;
                }
                updateScrollbarDrag(my);
                return true;
            }
        }

        placeCursorAtMouse(mx, my, false);
        selecting = true;
        cursorVisible = true;
        cursorBlinkCounter = 0;
        return true;
    }

    @Override
    public boolean mouseDragged(int button, double deltaX, double deltaY) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (scrollbarDragging) {
            updateScrollbarDrag(getMouseY());
            return true;
        }
        if (selecting) {
            placeCursorAtMouse(getMouseX(), getMouseY(), true);
            return true;
        }
        return false;
    }

    @Override
    protected void mouseReleased(int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            selecting = false;
            scrollbarDragging = false;
        }
    }

    @Override
    protected boolean mouseScrolledOverElement(double delta) {
        if (!needsScrollbar()) return false;
        scrollY -= (int) (delta * getTextHeight() * 3);
        clampScroll();
        return true;
    }

    private void updateScrollbarDrag(int mouseY) {
        int trackH = innerHeight();
        int contentH = contentHeight();
        int thumbH = Math.max(10, (int) ((long) trackH * trackH / contentH));
        int maxScroll = maxScrollY();
        int trackRange = trackH - thumbH;
        if (trackRange <= 0) { scrollY = 0; return; }
        double thumbTop = mouseY - PADDING - scrollbarDragOffset;
        thumbTop = Math.max(0, Math.min(trackRange, thumbTop));
        scrollY = (int) (thumbTop * maxScroll / trackRange);
        clampScroll();
    }

    private void placeCursorAtMouse(int mx, int my, boolean extendSelection) {
        int lineH = getTextHeight();
        int localY = my - PADDING + scrollY;
        int vIdx = localY / lineH;
        if (vIdx < 0) vIdx = 0;
        if (vIdx >= visualLines.size()) vIdx = visualLines.size() - 1;
        VisualLine vl = visualLines.get(vIdx);
        int relX = mx - PADDING;

        int col = vl.startCol;
        int lastW = 0;
        for (int i = 0; i <= vl.text.length(); i++) {
            int w = getTextWidth(vl.text.substring(0, i));
            if (w >= relX) {
                // Pick closer of (i-1, i)
                col = vl.startCol + (Math.abs(w - relX) < Math.abs(lastW - relX) ? i : Math.max(0, i - 1));
                break;
            }
            lastW = w;
            col = vl.startCol + i;
        }

        if (extendSelection) {
            startSelectionIfNone();
        } else {
            clearSelection();
        }
        cursorLine = vl.sourceLine;
        cursorCol = col;
        desiredCol = cursorCol;
        ensureCursorVisible();
    }

    private void ensureCursorVisible() {
        int lineH = getTextHeight();
        int vIdx = findVisualLineIndex(cursorLine, cursorCol);
        int cy = vIdx * lineH;
        if (cy < scrollY) scrollY = cy;
        else if (cy + lineH > scrollY + innerHeight()) scrollY = cy + lineH - innerHeight();
        clampScroll();
    }

    // ---------------------------------------------------------------------
    // Keyboard input
    // ---------------------------------------------------------------------

    @Override
    protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;

        boolean ctrl = isControlPressed();
        boolean shift = isShiftPressed();

        cursorVisible = true;
        cursorBlinkCounter = 0;

        switch (keyCode) {
            case InputConstants.KEY_ESCAPE:
                removeFocus();
                return true;
            case InputConstants.KEY_A:
                if (ctrl) {
                    selAnchorLine = 0;
                    selAnchorCol = 0;
                    cursorLine = lines.size() - 1;
                    cursorCol = lines.get(cursorLine).length();
                    desiredCol = cursorCol;
                    return true;
                }
                return false;
            case InputConstants.KEY_C:
                if (ctrl) {
                    if (hasSelection() && getRoot() != null)
                        getRoot().getInputProvider().setClipboard(extractSelection());
                    return true;
                }
                return false;
            case InputConstants.KEY_X:
                if (ctrl) {
                    if (hasSelection() && getRoot() != null) {
                        getRoot().getInputProvider().setClipboard(extractSelection());
                        if (editable) {
                            deleteSelection();
                            afterMutation();
                        }
                    }
                    return true;
                }
                return false;
            case InputConstants.KEY_V:
                if (ctrl) {
                    if (!editable) return true;
                    String clip = getRoot() != null ? getRoot().getInputProvider().getClipboard() : "";
                    if (clip == null || clip.isEmpty()) return true;
                    clip = clip.replace("\r\n", "\n").replace('\r', '\n');
                    int addCount = clip.length() - selectionCharCount();
                    if (maxLength > 0 && totalChars() - selectionCharCount() + clip.length() > maxLength) {
                        int allowed = maxLength - (totalChars() - selectionCharCount());
                        if (allowed <= 0) return true;
                        clip = clip.substring(0, allowed);
                    }
                    clip = clipForPaste(clip);
                    if (clip.isEmpty()) return true;
                    insertString(clip);
                    afterMutation();
                    return true;
                }
                return false;
            case InputConstants.KEY_BACKSPACE:
                if (!editable) return true;
                if (hasSelection()) {
                    deleteSelection();
                    afterMutation();
                } else if (cursorCol > 0) {
                    String line = lines.get(cursorLine);
                    lines.set(cursorLine, line.substring(0, cursorCol - 1) + line.substring(cursorCol));
                    cursorCol--;
                    desiredCol = cursorCol;
                    afterMutation();
                } else if (cursorLine > 0) {
                    String prev = lines.get(cursorLine - 1);
                    String cur = lines.get(cursorLine);
                    int newCol = prev.length();
                    lines.set(cursorLine - 1, prev + cur);
                    lines.remove(cursorLine);
                    cursorLine--;
                    cursorCol = newCol;
                    desiredCol = cursorCol;
                    afterMutation();
                }
                return true;
            case InputConstants.KEY_DELETE:
                if (!editable) return true;
                if (hasSelection()) {
                    deleteSelection();
                    afterMutation();
                } else {
                    String line = lines.get(cursorLine);
                    if (cursorCol < line.length()) {
                        lines.set(cursorLine, line.substring(0, cursorCol) + line.substring(cursorCol + 1));
                        afterMutation();
                    } else if (cursorLine < lines.size() - 1) {
                        lines.set(cursorLine, line + lines.get(cursorLine + 1));
                        lines.remove(cursorLine + 1);
                        afterMutation();
                    }
                }
                return true;
            case InputConstants.KEY_ENTER:
            case InputConstants.KEY_KP_ENTER:
                if (!editable) return true;
                if (!canInsert(1)) return true;
                if (!canAddRow()) return true;
                insertNewline();
                afterMutation();
                return true;
            case InputConstants.KEY_LEFT:
                moveCursorHorizontal(-1, shift);
                return true;
            case InputConstants.KEY_RIGHT:
                moveCursorHorizontal(1, shift);
                return true;
            case InputConstants.KEY_UP:
                moveCursorVertical(-1, shift);
                return true;
            case InputConstants.KEY_DOWN:
                moveCursorVertical(1, shift);
                return true;
            case InputConstants.KEY_HOME:
                if (shift) startSelectionIfNone(); else clearSelection();
                if (ctrl) { cursorLine = 0; cursorCol = 0; }
                else { cursorCol = 0; }
                desiredCol = cursorCol;
                ensureCursorVisible();
                return true;
            case InputConstants.KEY_END:
                if (shift) startSelectionIfNone(); else clearSelection();
                if (ctrl) { cursorLine = lines.size() - 1; cursorCol = lines.get(cursorLine).length(); }
                else { cursorCol = lines.get(cursorLine).length(); }
                desiredCol = cursorCol;
                ensureCursorVisible();
                return true;
        }
        return false;
    }

    private void moveCursorHorizontal(int dir, boolean shift) {
        if (shift) startSelectionIfNone(); else clearSelection();
        if (dir < 0) {
            if (cursorCol > 0) cursorCol--;
            else if (cursorLine > 0) {
                cursorLine--;
                cursorCol = lines.get(cursorLine).length();
            }
        } else {
            if (cursorCol < lines.get(cursorLine).length()) cursorCol++;
            else if (cursorLine < lines.size() - 1) {
                cursorLine++;
                cursorCol = 0;
            }
        }
        desiredCol = cursorCol;
        ensureCursorVisible();
    }

    private void moveCursorVertical(int dir, boolean shift) {
        if (shift) startSelectionIfNone(); else clearSelection();
        int target = cursorLine + dir;
        if (target < 0 || target >= lines.size()) {
            if (dir < 0) { cursorCol = 0; }
            else { cursorCol = lines.get(cursorLine).length(); }
            desiredCol = cursorCol;
        } else {
            cursorLine = target;
            cursorCol = Math.min(desiredCol, lines.get(cursorLine).length());
        }
        ensureCursorVisible();
    }

    @Override
    protected boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused() || !editable) return false;
        if (codePoint < 32 || codePoint == 127) return false;
        if (!canInsert(1)) return true;
        if (!canInsertCharOnCurrentLine()) return true;
        insertChar(codePoint);
        afterMutation();
        return true;
    }

    private void afterMutation() {
        rebuildWrap();
        ensureCursorVisible();
        markDirty();
        if (textChangedFromUser != null)
            textChangedFromUser.accept(getText());
    }

    @Override
    public void focusLost() {
        cursorVisible = false;
        selecting = false;
        scrollbarDragging = false;
    }
}
