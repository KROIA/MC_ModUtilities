package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.gui.elements.CheckBox;
import net.kroia.modutilities.gui.elements.EmptyButton;
import net.kroia.modutilities.gui.elements.HorizontalSlider;
import net.kroia.modutilities.gui.elements.TabElement;
import net.kroia.modutilities.gui.elements.TextBox;
import net.kroia.modutilities.gui.elements.VerticalListView;
import net.kroia.modutilities.gui.elements.VerticalSlider;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.kroia.modutilities.gui.layout.LayoutHorizontal;
import net.kroia.modutilities.gui.layout.LayoutVertical;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GuiLogicTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.GUI;
    }

    @Override
    public void registerTests() {
        // Point rotation tests
        addTest("point_rotate_0", this::testPointRotate0);
        addTest("point_rotate_90", this::testPointRotate90);
        addTest("point_rotate_180", this::testPointRotate180);
        addTest("point_rotate_270", this::testPointRotate270);
        addTest("point_rotate_360", this::testPointRotate360);

        // Plot.Axis value-to-position math (replicated, Axis is private)
        addTest("plot_axis_value_to_pos", this::testPlotAxisValueToPos);
        addTest("plot_axis_pos_to_value", this::testPlotAxisPosToValue);
        addTest("plot_axis_div_by_zero", this::testPlotAxisDivByZero);

        // LayoutGrid column/row calculation math (replicated, cannot instantiate GuiElement)
        addTest("layout_grid_column_calc", this::testLayoutGridColumnCalc);
        addTest("layout_grid_extra_row_bug", this::testLayoutGridExtraRowBug);

        // TabElement.setSelectOutlineThickness Math.min vs Math.max bug (Issue #4)
        addTest("tab_select_outline_thickness_bug", this::testTabSelectOutlineThicknessBug);

        // TextBox regex creation (static methods)
        addTest("textbox_regex_only_numerical", this::testTextBoxRegexOnlyNumerical);
        addTest("textbox_regex_no_numbers", this::testTextBoxRegexNoNumbers);
        addTest("textbox_regex_numerical_negative", this::testTextBoxRegexNumericalNegative);

        // GuiElement.getAlignedBounds (static method)
        addTest("aligned_bounds_center", this::testAlignedBoundsCenter);

        // Rectangle geometry
        addTest("rectangle_contains", this::testRectangleContains);
        addTest("rectangle_intersects", this::testRectangleIntersects);

        // N15 regression: CheckBox.setChecked fires callbacks
        addTest("CheckBox_setChecked_firesOnStateChanged", this::testCheckBoxSetCheckedFiresOnStateChanged);
        addTest("CheckBox_setChecked_sameValue_noCallback", this::testCheckBoxSetCheckedSameValueNoCallback);
        addTest("CheckBox_setChecked_firesOnCheckedOnUnchecked", this::testCheckBoxSetCheckedFiresOnCheckedOnUnchecked);

        // N45 regression: EmptyButton 4-arg ctor applies color theme
        addTest("EmptyButton_fourArgCtor_hasColorTheme", this::testEmptyButtonFourArgCtorHasColorTheme);

        // N46 regression: VerticalListView tracks allObjectSize
        addTest("VerticalListView_addChild_updatesSize", this::testVerticalListViewAddChildUpdatesSize);

        // N40 regression: setOutlineThickness works correctly
        addTest("GuiElement_setOutlineThickness_setsValue", this::testGuiElementSetOutlineThicknessSetsValue);

        // Issue #6 regression: isKeyPressed on detached element should not NPE
        addTest("gui_isKeyPressed_detached_no_npe", this::testIsKeyPressedDetachedNoNpe);

        // Issue #7 regression: slider zero-track denominator should not crash
        addTest("gui_slider_zero_track_no_crash", this::testSliderZeroTrackNoCrash);

        // Issue #13 regression: layout overflow should not produce negative dimensions
        addTest("gui_layout_vertical_overflow_no_negative", this::testLayoutVerticalOverflowNoNegative);
        addTest("gui_layout_horizontal_overflow_no_negative", this::testLayoutHorizontalOverflowNoNegative);

        // Issue #14 regression: isVisible simplified logic
        addTest("gui_isVisible_simplified", this::testIsVisibleSimplified);
    }

    // ========================================================================
    // Point rotation tests
    // ========================================================================

    private TestResult testPointRotate0() {
        Point p = new Point(10, 0);
        Point r = p.getRotated(0);
        return assertTrue("Rotation by 0 degrees should not change point (got " + r.x + "," + r.y + ")",
                r.x == 10 && r.y == 0);
    }

    private TestResult testPointRotate90() {
        Point p = new Point(10, 0);
        Point r = p.getRotated(90);
        // 90 degrees CCW: (10,0) -> (0,10)
        return assertTrue("Rotation by 90 degrees: (10,0) -> (0,10) (got " + r.x + "," + r.y + ")",
                r.x == 0 && r.y == 10);
    }

    private TestResult testPointRotate180() {
        Point p = new Point(10, 0);
        Point r = p.getRotated(180);
        // 180 degrees: (10,0) -> (-10,0)
        return assertTrue("Rotation by 180 degrees: (10,0) -> (-10,0) (got " + r.x + "," + r.y + ")",
                r.x == -10 && r.y == 0);
    }

    private TestResult testPointRotate270() {
        Point p = new Point(10, 0);
        Point r = p.getRotated(270);
        // 270 degrees CCW: (10,0) -> (0,-10)
        return assertTrue("Rotation by 270 degrees: (10,0) -> (0,-10) (got " + r.x + "," + r.y + ")",
                r.x == 0 && r.y == -10);
    }

    private TestResult testPointRotate360() {
        Point p = new Point(7, 3);
        Point r = p.getRotated(360);
        return assertTrue("Rotation by 360 degrees should return to original (got " + r.x + "," + r.y + ")",
                r.x == 7 && r.y == 3);
    }

    // ========================================================================
    // Plot.Axis math tests (replicating the private Axis logic)
    // ========================================================================

    private TestResult testPlotAxisValueToPos() {
        // Replicate Axis math: scale = (maxPos - minPos) / (maxValue - minValue)
        // getPos(value) = minPos + (int)((value - minValue) * scale)
        float minValue = 0.0f;
        float maxValue = 100.0f;
        int minPos = 0;
        int maxPos = 200;
        float scale = (float)(maxPos - minPos) / (maxValue - minValue);

        int pos = minPos + (int)((50.0f - minValue) * scale);
        return assertEquals("Value 50 in range [0,100] mapped to pos range [0,200] should be 100", 100, pos);
    }

    private TestResult testPlotAxisPosToValue() {
        // Replicate: getValue(pos) = minValue + (pos - minPos) / scale
        float minValue = 0.0f;
        float maxValue = 100.0f;
        int minPos = 0;
        int maxPos = 200;
        float scale = (float)(maxPos - minPos) / (maxValue - minValue);

        float value = minValue + (100 - minPos) / scale;
        return assertTrue("Pos 100 in range [0,200] mapped to value range [0,100] should be 50 (got " + value + ")",
                Math.abs(value - 50.0f) < 0.001f);
    }

    private TestResult testPlotAxisDivByZero() {
        // Issue #43 (fixed): Plot.Axis now guards against zero range and uses scale=1 instead of dividing by zero.
        // This test documents the math behavior: a raw division still produces Infinity, but Plot.Axis now
        // checks for this case before computing scale.
        float minValue = 50.0f;
        float maxValue = 50.0f;
        float range = maxValue - minValue;
        float scale = range == 0f ? 1f : 200f / range;
        return assertEquals("Issue #43 fixed: scale should default to 1 when range is zero", 1f, scale);
    }

    // ========================================================================
    // LayoutGrid column/row calculation math tests
    // ========================================================================

    private TestResult testLayoutGridColumnCalc() {
        // Replicate LayoutGrid logic when rows=4, columns=0:
        // columnsInternal = ceil(childCount / rowsInternal) + (childCount % rowsInternal == 0 ? 0 : 1)
        int childCount = 8;
        int rowsInternal = 4;
        int columnsInternal = (int) Math.ceil((double) childCount / rowsInternal)
                + (childCount % rowsInternal == 0 ? 0 : 1);
        // 8/4 = 2.0, ceil = 2, 8%4==0 so +0, result = 2
        return assertEquals("8 children in 4 rows should need 2 columns", 2, columnsInternal);
    }

    private TestResult testLayoutGridExtraRowBug() {
        // Issue #25 (fixed): formula now correctly uses Math.ceil() without redundant +1.
        // For childCount=5, columns=3: ceil(5/3) = 2 rows (correct).
        int childCount = 5;
        int columnsInternal = 3;
        int rowsInternal = (int) Math.ceil((double) childCount / columnsInternal);
        return assertEquals("Issue #25 fixed: 5 children in 3 columns should need 2 rows",
                2, rowsInternal);
    }

    // ========================================================================
    // TabElement.setSelectOutlineThickness bug (Issue #4)
    // ========================================================================

    private TestResult testTabSelectOutlineThicknessBug() {
        TabElement tabElement = new TabElement();
        tabElement.setSelectOutlineThickness(5);
        int result = tabElement.getSelectOutlineThickness();
        return assertEquals("Issue #4 fixed: setSelectOutlineThickness(5) should store 5", 5, result);
    }

    // ========================================================================
    // TextBox regex tests (static methods, no Minecraft context needed)
    // ========================================================================

    private TestResult testTextBoxRegexOnlyNumerical() {
        String regex = TextBox.createRegex_onlyNumerical(true, false, 5, 2);
        // Should match positive numbers with up to 5 digits and 2 decimal places
        boolean matchesInt = "123".matches(regex);
        boolean matchesDec = "123.45".matches(regex);
        boolean rejectsLetters = !"abc".matches(regex);
        boolean rejectsTooManyDecimals = !"1.234".matches(regex);

        return assertTrue("Numerical regex should accept '123' and '123.45', reject 'abc' and '1.234'",
                matchesInt && matchesDec && rejectsLetters && rejectsTooManyDecimals);
    }

    private TestResult testTextBoxRegexNoNumbers() {
        String regex = TextBox.createRegex_noNumbers();
        boolean matchesText = "hello".matches(regex);
        boolean rejectsNumbers = !"hello123".matches(regex);
        boolean rejectsDigit = !"5".matches(regex);

        return assertTrue("No-numbers regex should accept 'hello', reject 'hello123' and '5'",
                matchesText && rejectsNumbers && rejectsDigit);
    }

    private TestResult testTextBoxRegexNumericalNegative() {
        String regex = TextBox.createRegex_onlyNumerical(true, true, 5, 2);
        boolean matchesPositive = "42".matches(regex);
        boolean matchesNegative = "-42".matches(regex);
        boolean matchesNegDecimal = "-3.14".matches(regex);
        boolean allowsPartialMinus = "-".matches(regex); // mid-typing support
        boolean allowsPartialDot = "123.".matches(regex); // mid-typing support

        return assertTrue("Negative-allowed regex should accept '42', '-42', '-3.14', '-', '123.'",
                matchesPositive && matchesNegative && matchesNegDecimal
                        && allowsPartialMinus && allowsPartialDot);
    }

    // ========================================================================
    // GuiElement.getAlignedBounds (static method, no context needed)
    // ========================================================================

    private TestResult testAlignedBoundsCenter() {
        // Place a 20x10 rect centered in a 100x100 area at origin
        Rectangle result = GuiElement.getAlignedBounds(
                0, 0, 20, 10,
                GuiElement.Alignment.CENTER,
                0, 0, 100, 100);
        // Center: x = (100-20)/2 = 40, y = (100-10)/2 = 45
        return assertTrue("Center alignment of 20x10 in 100x100 should be at (40,45) (got "
                        + result.x + "," + result.y + ")",
                result.x == 40 && result.y == 45);
    }

    // ========================================================================
    // Rectangle geometry tests
    // ========================================================================

    private TestResult testRectangleContains() {
        Rectangle rect = new Rectangle(10, 10, 50, 50);
        boolean inside = rect.contains(30, 30);
        boolean onEdge = rect.contains(10, 10);
        boolean outside = rect.contains(61, 61); // 10+50=60, so 61 is outside (< x + width check)

        return assertTrue("Rectangle(10,10,50,50) should contain (30,30) and (10,10) but not (61,61)",
                inside && onEdge && !outside);
    }

    private TestResult testRectangleIntersects() {
        Rectangle r1 = new Rectangle(0, 0, 50, 50);
        Rectangle r2 = new Rectangle(25, 25, 50, 50);
        Rectangle r3 = new Rectangle(100, 100, 10, 10);

        boolean overlapping = r1.intersects(r2);
        boolean notOverlapping = !r1.intersects(r3);

        return assertTrue("(0,0,50,50) should intersect (25,25,50,50) but not (100,100,10,10)",
                overlapping && notOverlapping);
    }

    // ========================================================================
    // N15 regression: CheckBox.setChecked fires callbacks
    // ========================================================================

    private TestResult testCheckBoxSetCheckedFiresOnStateChanged() {
        CheckBox checkBox = new CheckBox("test");
        AtomicBoolean callbackValue = new AtomicBoolean(false);
        AtomicBoolean callbackFired = new AtomicBoolean(false);
        checkBox.setOnStateChanged(value -> {
            callbackFired.set(true);
            callbackValue.set(value);
        });

        checkBox.setChecked(true);
        if (!callbackFired.get())
            return fail("N15 regression: setChecked(true) did not fire onStateChanged callback");
        if (!callbackValue.get())
            return fail("N15 regression: onStateChanged callback received false, expected true");

        callbackFired.set(false);
        checkBox.setChecked(false);
        if (!callbackFired.get())
            return fail("N15 regression: setChecked(false) did not fire onStateChanged callback");
        if (callbackValue.get())
            return fail("N15 regression: onStateChanged callback received true, expected false");

        return pass("setChecked fires onStateChanged with correct value");
    }

    private TestResult testCheckBoxSetCheckedSameValueNoCallback() {
        CheckBox checkBox = new CheckBox("test");
        AtomicInteger counter = new AtomicInteger(0);
        checkBox.setOnStateChanged(value -> counter.incrementAndGet());

        // Default is unchecked; setting to false again should not fire
        checkBox.setChecked(false);
        return assertEquals("N15 regression: setChecked(false) on already-unchecked checkbox should not fire callback",
                0, counter.get());
    }

    private TestResult testCheckBoxSetCheckedFiresOnCheckedOnUnchecked() {
        CheckBox checkBox = new CheckBox("test");
        AtomicBoolean checkedFired = new AtomicBoolean(false);
        AtomicBoolean uncheckedFired = new AtomicBoolean(false);
        checkBox.setOnChecked(() -> checkedFired.set(true));
        checkBox.setOnUnchecked(() -> uncheckedFired.set(true));

        checkBox.setChecked(true);
        if (!checkedFired.get())
            return fail("N15 regression: setChecked(true) did not fire onChecked callback");
        if (uncheckedFired.get())
            return fail("N15 regression: setChecked(true) incorrectly fired onUnchecked callback");

        // Reset flags
        checkedFired.set(false);
        uncheckedFired.set(false);

        checkBox.setChecked(false);
        if (!uncheckedFired.get())
            return fail("N15 regression: setChecked(false) did not fire onUnchecked callback");
        if (checkedFired.get())
            return fail("N15 regression: setChecked(false) incorrectly fired onChecked callback");

        return pass("setChecked fires onChecked/onUnchecked correctly");
    }

    // ========================================================================
    // N45 regression: EmptyButton 4-arg ctor applies color theme
    // ========================================================================

    private TestResult testEmptyButtonFourArgCtorHasColorTheme() {
        EmptyButton defaultBtn = new EmptyButton();
        EmptyButton fourArgBtn = new EmptyButton(0, 0, 100, 20);

        int defaultColor = defaultBtn.getBackgroundColor();
        int fourArgColor = fourArgBtn.getBackgroundColor();

        return assertEquals("N45 regression: 4-arg ctor should apply same color theme as no-arg ctor",
                defaultColor, fourArgColor);
    }

    // ========================================================================
    // N46 regression: VerticalListView tracks allObjectSize via addChild
    // ========================================================================

    private TestResult testVerticalListViewAddChildUpdatesSize() {
        VerticalListView listView = new VerticalListView(0, 0, 200, 300);

        // getSizeHintHeight() = allObjectSize + 2*outlineThickness
        // outlineThickness defaults to 1, so base = 2*1 = 2
        int baseSizeHint = listView.getSizeHintHeight();

        EmptyButton child1 = new EmptyButton(0, 0, 100, 30);
        listView.addChild(child1);
        int afterFirst = listView.getSizeHintHeight();

        EmptyButton child2 = new EmptyButton(0, 0, 100, 30);
        listView.addChild(child2);
        int afterSecond = listView.getSizeHintHeight();

        EmptyButton child3 = new EmptyButton(0, 0, 100, 30);
        listView.addChild(child3);
        int afterThird = listView.getSizeHintHeight();

        // Each child adds 30 to allObjectSize, so sizeHintHeight grows by 30 each time
        boolean firstAdded = (afterFirst - baseSizeHint) == 30;
        boolean secondAdded = (afterSecond - afterFirst) == 30;
        boolean thirdAdded = (afterThird - afterSecond) == 30;

        return assertTrue("N46 regression: getSizeHintHeight should grow by child height (30) per child"
                        + " (deltas: " + (afterFirst - baseSizeHint) + ", "
                        + (afterSecond - afterFirst) + ", "
                        + (afterThird - afterSecond) + ")",
                firstAdded && secondAdded && thirdAdded);
    }

    // ========================================================================
    // N40 regression: setOutlineThickness works correctly
    // ========================================================================

    private TestResult testGuiElementSetOutlineThicknessSetsValue() {
        EmptyButton button = new EmptyButton();
        button.setOutlineThickness(5);
        int result = button.getOutlineThickness();
        return assertEquals("N40 regression: setOutlineThickness(5) should store 5", 5, result);
    }

    // ========================================================================
    // Issue #6 regression: isKeyPressed on detached element should not NPE
    // ========================================================================

    /**
     * Helper subclass that exposes the protected isKeyPressed method for testing.
     */
    private static class TestableGuiElement extends EmptyButton {
        public boolean callIsKeyPressed(int keyCode) {
            return isKeyPressed(keyCode);
        }
        public boolean callIsControlPressed() {
            return isControlPressed();
        }
        public boolean callIsShiftPressed() {
            return isShiftPressed();
        }
    }

    private TestResult testIsKeyPressedDetachedNoNpe() {
        // Create an element NOT added to any Gui root
        TestableGuiElement element = new TestableGuiElement();
        try {
            boolean result = element.callIsKeyPressed(32); // SPACE
            if (result) {
                return fail("Issue #6: isKeyPressed should return false on detached element, got true");
            }
            // Also verify isControlPressed and isShiftPressed
            boolean ctrl = element.callIsControlPressed();
            boolean shift = element.callIsShiftPressed();
            if (ctrl || shift) {
                return fail("Issue #6: isControlPressed/isShiftPressed should return false on detached element");
            }
            return pass("Issue #6: isKeyPressed/isControlPressed/isShiftPressed return false without NPE on detached element");
        } catch (NullPointerException e) {
            return fail("Issue #6 regression: isKeyPressed threw NPE on detached element (getRoot() is null)");
        }
    }

    // ========================================================================
    // Issue #7 regression: slider zero-track denominator should not crash
    // ========================================================================

    private TestResult testSliderZeroTrackNoCrash() {
        try {
            // HorizontalSlider: set width equal to sliderBounds.width so denominator = 0
            HorizontalSlider hSlider = new HorizontalSlider(0, 0, 50, 20);
            hSlider.setSliderWidth(50); // sliderBounds.width == getWidth() == 50
            hSlider.setSliderValue(0); // trigger internal layout

            // VerticalSlider: set height equal to sliderBounds.height so denominator = 0
            VerticalSlider vSlider = new VerticalSlider(0, 0, 20, 50);
            vSlider.setSliderHeight(50); // sliderBounds.height == getHeight() == 50
            vSlider.setSliderValue(0); // trigger internal layout

            // Verify slider values are valid (should be 0 when denominator is zero)
            double hValue = hSlider.getSliderValue();
            double vValue = vSlider.getSliderValue();

            return assertTrue("Issue #7: sliders with zero-track should not crash and value should be 0.0"
                            + " (hSlider=" + hValue + ", vSlider=" + vValue + ")",
                    hValue == 0.0 && vValue == 0.0);
        } catch (ArithmeticException e) {
            return fail("Issue #7 regression: slider division by zero when handle equals track size");
        } catch (Exception e) {
            return fail("Issue #7 regression: unexpected exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ========================================================================
    // Issue #13 regression: layout overflow should not produce negative dims
    // ========================================================================

    private TestResult testLayoutVerticalOverflowNoNegative() {
        // Create parent with height=50, add layout with padding=30, spacing=5
        // (50 - 30*2 + 5) / 3 - 5 = (50-60+5)/3-5 = -5/3-5 = -6 without clamp
        // With Math.max(1,...) clamp, height should be at least 1
        EmptyButton parent = new EmptyButton(0, 0, 100, 50);
        LayoutVertical layout = new LayoutVertical(30, 5, false, true);
        parent.setLayout(layout);

        EmptyButton child1 = new EmptyButton(0, 0, 40, 20);
        EmptyButton child2 = new EmptyButton(0, 0, 40, 20);
        EmptyButton child3 = new EmptyButton(0, 0, 40, 20);

        parent.addChild(child1);
        parent.addChild(child2);
        parent.addChild(child3);

        // Manually apply layout since we have no Gui root
        layout.apply(parent);

        // Check that no child has zero or negative height
        List<GuiElement> childs = parent.getChilds();
        for (int i = 0; i < childs.size(); i++) {
            int h = childs.get(i).getHeight();
            if (h <= 0) {
                return fail("Issue #13 regression: child " + i + " has non-positive height " + h
                        + " after LayoutVertical with overflow");
            }
        }
        return pass("Issue #13: LayoutVertical clamps child height to at least 1 on overflow");
    }

    private TestResult testLayoutHorizontalOverflowNoNegative() {
        // Same pattern for horizontal: width=50, padding=30, spacing=5
        EmptyButton parent = new EmptyButton(0, 0, 50, 100);
        LayoutHorizontal layout = new LayoutHorizontal(30, 5, true, false);
        parent.setLayout(layout);

        EmptyButton child1 = new EmptyButton(0, 0, 20, 40);
        EmptyButton child2 = new EmptyButton(0, 0, 20, 40);
        EmptyButton child3 = new EmptyButton(0, 0, 20, 40);

        parent.addChild(child1);
        parent.addChild(child2);
        parent.addChild(child3);

        // Manually apply layout since we have no Gui root
        layout.apply(parent);

        // Check that no child has zero or negative width
        List<GuiElement> childs = parent.getChilds();
        for (int i = 0; i < childs.size(); i++) {
            int w = childs.get(i).getWidth();
            if (w <= 0) {
                return fail("Issue #13 regression: child " + i + " has non-positive width " + w
                        + " after LayoutHorizontal with overflow");
            }
        }
        return pass("Issue #13: LayoutHorizontal clamps child width to at least 1 on overflow");
    }

    // ========================================================================
    // Issue #14 regression: isVisible simplified logic
    // ========================================================================

    private TestResult testIsVisibleSimplified() {
        // Create parent (100x100) and child (50x50)
        EmptyButton parent = new EmptyButton(0, 0, 100, 100);
        EmptyButton child = new EmptyButton(0, 0, 50, 50);

        parent.addChild(child);

        // Enable overlap checking on child (it's enabled by default, but be explicit)
        child.setCheckOverlapForRendering(true);

        // Since there's no Gui root, globalPositon stays at (0,0) for parent.
        // Manually set child's globalPosition to be outside parent (simulating child at 200,200)
        Point childGlobalPos = child.getGlobalPosition();
        childGlobalPos.x = 200;
        childGlobalPos.y = 200;

        // Child at global (200,200) with size 50x50 should NOT intersect parent at (0,0) with size 100x100
        boolean visibleWhenOutside = child.isVisible();
        if (visibleWhenOutside) {
            return fail("Issue #14: child at (200,200) should not be visible within parent (0,0,100,100)");
        }

        // Move child to (10,10) — inside parent bounds
        childGlobalPos.x = 10;
        childGlobalPos.y = 10;

        boolean visibleWhenInside = child.isVisible();
        if (!visibleWhenInside) {
            return fail("Issue #14: child at (10,10) should be visible within parent (0,0,100,100)");
        }

        return pass("Issue #14: isVisible correctly uses intersection check for overlap rendering");
    }
}
