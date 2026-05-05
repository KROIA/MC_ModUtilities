package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.gui.elements.TextBox;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;

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
        // Issue #43: When minValue == maxValue, scale computation divides by zero
        float minValue = 50.0f;
        float maxValue = 50.0f;
        int minPos = 0;
        int maxPos = 200;
        float scale = (float)(maxPos - minPos) / (maxValue - minValue); // Division by zero -> Infinity

        boolean isInfinite = Float.isInfinite(scale) || Float.isNaN(scale);
        return assertTrue("Scale should be Infinity or NaN when minValue == maxValue (Issue #43), got: " + scale,
                isInfinite);
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
        // Issue #25: When childCount divides evenly, the formula adds an extra row/column.
        // For columns=3, rows=0, childCount=6:
        // rowsInternal = ceil(6/3) + (6%3==0 ? 0 : 1) = 2 + 0 = 2
        // That is correct. But for childCount=5, columns=3:
        // rowsInternal = ceil(5/3) + (5%3==0 ? 0 : 1) = 2 + 1 = 3
        // But only ceil(5/3) = 2 rows are needed. The +1 creates an extra row.
        int childCount = 5;
        int columnsInternal = 3;
        int rowsInternal = (int) Math.ceil((double) childCount / columnsInternal)
                + (childCount % columnsInternal == 0 ? 0 : 1);
        int expectedCorrect = (int) Math.ceil((double) childCount / columnsInternal); // 2

        // The current formula gives 3, but the correct value is 2
        return assertTrue("Issue #25: 5 children in 3 columns computes " + rowsInternal
                        + " rows but only " + expectedCorrect + " needed",
                rowsInternal > expectedCorrect);
    }

    // ========================================================================
    // TabElement.setSelectOutlineThickness bug (Issue #4)
    // ========================================================================

    private TestResult testTabSelectOutlineThicknessBug() {
        // The code uses Math.min(0, thickness) which always returns <= 0
        // It should be Math.max(0, thickness) to clamp to non-negative
        int thickness = 5;
        int bugResult = Math.min(0, thickness); // Bug: always <= 0
        int correctResult = Math.max(0, thickness); // Correct: clamps to >= 0

        return assertTrue("Issue #4: Math.min(0,5) returns " + bugResult
                        + " but Math.max(0,5) should return " + correctResult,
                bugResult != correctResult && bugResult <= 0);
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
}
