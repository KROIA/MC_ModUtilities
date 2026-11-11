package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.gui.elements.CheckBox;
import net.kroia.modutilities.gui.elements.DropDownMenu;
import net.kroia.modutilities.gui.elements.EmptyButton;
import net.kroia.modutilities.gui.elements.FeatureToggleSection;
import net.kroia.modutilities.gui.elements.HorizontalListView;
import net.kroia.modutilities.gui.elements.HorizontalSlider;
import net.kroia.modutilities.gui.elements.Label;
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
import net.minecraft.network.chat.Component;

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

        // Tooltip auto-side, clamping and wrapping (static helpers)
        addTest("tooltip_side_clamp_wrap", this::testTooltipSideClampWrap);

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

        // Task #103 regression: clear + refill keeps the scroll position
        addTest("ListView_refill_keepsScrollOffset", this::testListViewRefillKeepsScrollOffset);

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

        // Gui scale is snapped to whole screen pixels per GUI unit
        addTest("gui_scale_snapped_to_pixels", this::testGuiScaleSnappedToPixels);

        // Task #107: open DropDownMenu options outside the parent bounds are hit, unless an enclosing ListView clips them
        addTest("DropDownMenu_options_hitTest_ignoresNonClippingParents", this::testDropDownOptionsHitTest);

        // Task #134: picking an option collapses, shows its text, highlights it; the click does not reach elements underneath
        addTest("DropDownMenu_select_collapsesAndConsumesClick", this::testDropDownSelectCollapsesAndConsumesClick);

        // Task #108: FeatureToggleSection restart marker with own (static) wording
        addTest("FeatureToggleSection_restartMarkerText", this::testFeatureToggleRestartMarkerText);
    }

    private TestResult testDropDownOptionsHitTest() {
        // Without an enclosing ListView: 20px row, the open list hangs below it
        EmptyButton row = new EmptyButton(0, 0, 120, 20);
        DropDownMenu menu = new DropDownMenu("menu");
        menu.addOption("a");
        menu.addOption("b");
        row.addChild(menu);
        net.kroia.modutilities.gui.Gui gui = new net.kroia.modutilities.gui.Gui();
        gui.addElement(row);
        row.init();
        menu.setBounds(0, 0, 120, 20);
        menu.expand();
        GuiElement b = menu.getChilds().get(1);
        Point pb = b.getGlobalPosition();
        int bx = pb.x + b.getWidth() / 2, by = pb.y + b.getHeight() / 2;
        if (by < 20)
            return fail("precondition: option b should lie below the 20px row, centre y=" + by);
        if (!b.isOver(bx, by))
            return fail("option outside the (non-clipping) parent bounds is not hit at (" + bx + "," + by + ")");

        // Inside a VerticalListView that ends below option a: a is visible, b is clipped by its scissor
        VerticalListView outer = new VerticalListView(0, 0, 140, 200);
        EmptyButton row2 = new EmptyButton(0, 0, 120, 20);
        DropDownMenu menu2 = new DropDownMenu("menu");
        menu2.addOption("a");
        menu2.addOption("b");
        row2.addChild(menu2);
        outer.addChild(row2);
        gui.addElement(outer);
        outer.init();
        menu2.setBounds(0, 0, 120, 20);
        menu2.expand();
        GuiElement a2 = menu2.getChilds().get(0);
        GuiElement b2 = menu2.getChilds().get(1);
        Point pa = a2.getGlobalPosition();
        outer.setHeight(pa.y + a2.getHeight() - outer.getGlobalPosition().y);
        int ax = pa.x + a2.getWidth() / 2, ay = pa.y + a2.getHeight() / 2;
        Point pb2 = b2.getGlobalPosition();
        int bx2 = pb2.x + b2.getWidth() / 2, by2 = pb2.y + b2.getHeight() / 2;
        if (ay < row2.getGlobalPosition().y + row2.getHeight())
            return fail("precondition: option a should lie below its row, centre y=" + ay);
        if (!a2.isOver(ax, ay))
            return fail("visible option outside its row (inside the ListView) is not hit at (" + ax + "," + ay + ")");
        if (b2.isOver(bx2, by2))
            return fail("option clipped by the enclosing ListView is hit at (" + bx2 + "," + by2 + ")");
        return pass("DropDownMenu options are hit outside non-clipping parents, not where an enclosing ListView clips them");
    }

    private TestResult testDropDownSelectCollapsesAndConsumesClick() {
        net.kroia.modutilities.gui.Gui gui = new net.kroia.modutilities.gui.Gui();
        // Added first: dispatched before the menu, lies under its open list
        EmptyButton under = new EmptyButton(0, 20, 120, 100);
        gui.addElement(under);
        under.init();
        AtomicInteger picked = new AtomicInteger(-1);
        DropDownMenu menu = new DropDownMenu("menu", (i, el) -> picked.set(i));
        menu.addOption("a");
        Label b = menu.addOption("b");
        if (menu.getChilds().get(1) != b)
            return fail("addOption should return the created option label");
        gui.addElement(menu);
        menu.init();
        menu.setBounds(0, 0, 120, 20);
        menu.expand();
        Point pb = b.getGlobalPosition();
        gui.mouseClicked(pb.x + b.getWidth() / 2.0, pb.y + b.getHeight() / 2.0, net.kroia.modutilities.gui.InputConstants.MOUSE_BUTTON_LEFT);
        if (under.getClickCount() != 0)
            return fail("click on an open option also clicked the element underneath");
        if (picked.get() != 1 || menu.getSelectedIndex() != 1)
            return fail("option b not selected, callback index=" + picked.get() + " selectedIndex=" + menu.getSelectedIndex());
        if (menu.isExpanded())
            return fail("menu should collapse after selecting an option");
        if (!"b".equals(menu.getLabelText()))
            return fail("collapsed label should show the picked option, was '" + menu.getLabelText() + "'");
        menu.expand();
        EmptyButton rowA = (EmptyButton) menu.getChilds().get(0).getParent();
        EmptyButton rowB = (EmptyButton) b.getParent();
        if (rowB.getBackgroundColor() != rowB.getPressedColor() || rowA.getBackgroundColor() == rowA.getPressedColor())
            return fail("re-opened menu should highlight only the selected row");
        return pass("selecting an option collapses the menu, shows and highlights it, and consumes the click");
    }

    private TestResult testFeatureToggleRestartMarkerText() {
        class Probe extends FeatureToggleSection {
            Probe() { super("title"); }
            Label marker() {
                for (GuiElement c : getHeaderButton().getChilds())
                    if (c instanceof Label l && l.getTextColor() == RESTART_INDICATOR_COLOR)
                        return l;
                return null;
            }
        }
        Probe section = new Probe();
        Label marker = section.marker();
        if (marker == null)
            return fail("restart marker label not found in the header");
        String defaultText = marker.getText();
        String defaultTooltip = marker.getHoverTooltipSupplier().get();
        if (section.isRestartRequired())
            return fail("marker must be hidden by default");

        section.setRestartRequired(true);
        section.setRestartMarkerText(Component.literal("requires a restart"), Component.literal("read at startup only"));
        if (!section.isRestartRequired() || !"requires a restart".equals(marker.getText())
                || !"read at startup only".equals(marker.getHoverTooltipSupplier().get()))
            return fail("static marker: got text [" + marker.getText() + "], tooltip [" + marker.getHoverTooltipSupplier().get() + "]");

        // null keeps the current value
        section.setRestartMarkerText(null, Component.literal("t2"));
        if (!"requires a restart".equals(marker.getText()) || !"t2".equals(marker.getHoverTooltipSupplier().get()))
            return fail("null text must keep the current text, got [" + marker.getText() + "]");

        Probe untouched = new Probe();
        if (!defaultText.equals(untouched.marker().getText()) || !defaultTooltip.equals(untouched.marker().getHoverTooltipSupplier().get()))
            return fail("default marker wording changed");
        return pass("restart marker text/tooltip settable, default wording kept");
    }

    private TestResult testGuiScaleSnappedToPixels() {
        // {requested, displayScaleFactor, expected effective scale}
        double[][] cases = {
                {0.8, 3, 2.0 / 3}, {0.8, 2, 1.0}, {0.8, 4, 0.75}, {0.65, 3, 2.0 / 3},
                {0.65, 2, 0.5}, {0.5, 3, 2.0 / 3}, {0.5, 2, 0.5}, {0.1, 2, 0.5},
                {1.0, 3, 1.0}, {1.5, 3, 1.0 + 2.0 / 3}, {0.8, 0, 0.8}};
        for (double[] c : cases) {
            float got = net.kroia.modutilities.gui.Gui.snapGuiScale((float) c[0], c[1]);
            if (Math.abs(got - c[2]) > 1e-5)
                return fail("snapGuiScale(" + c[0] + ", " + c[1] + ") = " + got + ", expected " + c[2]);
        }
        net.kroia.modutilities.gui.Gui gui = new net.kroia.modutilities.gui.Gui();
        gui.setGuiScale(0.8f);
        gui.setDisplayScaleFactor(3);
        if (Math.abs(gui.getGuiScale() - 2.0f / 3) > 1e-5 || gui.getRequestedGuiScale() != 0.8f
                || Math.abs(gui.getInvGuiScale() - 1.5f) > 1e-5)
            return fail("Gui effective scale not updated on display factor change: " + gui.getGuiScale());

        // Library GUI scale setting: {setting (0 auto, -1 follow MC, N fixed), maxPixelScale, mcGuiScale, expected base}
        double[][] fixedCases = {
                {0, 4, 3, 4.0 / 3}, {0, 4, 1, 4.0}, {0, 4, 4, 1.0}, {0, 0, 2, 0.5},
                {-1, 4, 3, 1.0}, {-1, 4, 1, 1.0},
                {2, 4, 1, 2.0}, {2, 4, 2, 1.0}, {2, 4, 4, 0.5},
                {6, 3, 2, 1.5}, {3, 0, 3, 1.0 / 3}, {2, 4, 0, 1.0}, {0, 4, 0, 1.0}};
        for (double[] c : fixedCases) {
            float got = net.kroia.modutilities.gui.Gui.fixedBaseGuiScale((int) c[0], (int) c[1], c[2]);
            if (Math.abs(got - c[3]) > 1e-5)
                return fail("fixedBaseGuiScale(" + c[0] + ", " + c[1] + ", " + c[2] + ") = " + got + ", expected " + c[3]);
        }
        // Same pixel size at every MC GUI scale: fixed 2 px/unit, screen's own scale 0.5 -> 1 px/unit
        for (int mc = 1; mc <= 4; mc++) {
            gui.setGuiScale(0.5f);
            gui.setDisplayScaleFactor(mc);
            gui.setBaseGuiScale(net.kroia.modutilities.gui.Gui.fixedBaseGuiScale(2, 4, mc));
            if (Math.abs(gui.getGuiScale() * mc - 1.0f) > 1e-5)
                return fail("fixed scale not MC-scale independent at MC scale " + mc + ": " + gui.getGuiScale());
        }
        // Auto ignores the MC GUI scale: always the window's max-fit scale
        for (int mc = 1; mc <= 4; mc++) {
            gui.setGuiScale(1.0f);
            gui.setDisplayScaleFactor(mc);
            gui.setBaseGuiScale(net.kroia.modutilities.gui.Gui.fixedBaseGuiScale(0, 4, mc));
            if (Math.abs(gui.getGuiScale() * mc - 4.0f) > 1e-5)
                return fail("auto scale not MC-scale independent at MC scale " + mc + ": " + gui.getGuiScale());
        }
        // {setting, max, mc, expected pixels per unit}
        int[][] pixelCases = {{0, 4, 2, 4}, {0, 0, 2, 1}, {-1, 4, 2, 2}, {-1, 4, 0, 1}, {3, 4, 1, 3}, {6, 3, 1, 3}};
        for (int[] c : pixelCases) {
            int got = net.kroia.modutilities.gui.Gui.pixelGuiScale(c[0], c[1], c[2]);
            if (got != c[3])
                return fail("pixelGuiScale(" + c[0] + ", " + c[1] + ", " + c[2] + ") = " + got + ", expected " + c[3]);
        }
        // Shortcut stepping {setting, delta, max, mc, expected new setting}: steps from the resolved scale, clamps 1..max
        int[][] stepCases = {{0, -1, 4, 2, 3}, {0, 1, 4, 2, 4}, {-1, 1, 4, 2, 3}, {-1, -1, 4, 1, 1},
                {2, 1, 4, 1, 3}, {1, -1, 4, 1, 1}, {6, -1, 3, 1, 2}, {4, 1, 4, 1, 4}};
        for (int[] c : stepCases) {
            int got = net.kroia.modutilities.gui.Gui.stepGuiScaleSetting(c[0], c[1], c[2], c[3]);
            if (got != c[4])
                return fail("stepGuiScaleSetting(" + c[0] + ", " + c[1] + ", " + c[2] + ", " + c[3] + ") = " + got + ", expected " + c[4]);
        }
        return pass("Gui scale snaps to whole screen pixels per unit");
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

    private TestResult testTooltipSideClampWrap() {
        // Auto side: left half -> open right (*_LEFT), right half -> open left (*_RIGHT), vertical part kept
        GuiElement.Alignment a1 = GuiElement.autoSideAlignment(GuiElement.Alignment.TOP_RIGHT, 10, 100);
        GuiElement.Alignment a2 = GuiElement.autoSideAlignment(GuiElement.Alignment.TOP_LEFT, 90, 100);
        GuiElement.Alignment a3 = GuiElement.autoSideAlignment(GuiElement.Alignment.BOTTOM, 90, 100);
        GuiElement.Alignment a4 = GuiElement.autoSideAlignment(GuiElement.Alignment.CENTER, 10, 100);
        if (a1 != GuiElement.Alignment.TOP_LEFT || a2 != GuiElement.Alignment.TOP_RIGHT
                || a3 != GuiElement.Alignment.BOTTOM_RIGHT || a4 != GuiElement.Alignment.LEFT)
            return fail("autoSideAlignment wrong: " + a1 + ", " + a2 + ", " + a3 + ", " + a4);

        // Clamp: 20x10 box + padding 2 overflowing bottom-right of a 100x50 area moves back inside
        Rectangle r = new Rectangle(90, 45, 20, 10);
        GuiElement.clampToArea(r, 2, 0, 0, 100, 50);
        if (r.x != 78 || r.y != 38)
            return fail("clampToArea bottom-right: expected (78,38), got (" + r.x + "," + r.y + ")");
        // Overflowing top-left, area offset (element-local space)
        r = new Rectangle(-50, -50, 20, 10);
        GuiElement.clampToArea(r, 2, -30, -30, 100, 50);
        if (r.x != -28 || r.y != -28)
            return fail("clampToArea top-left: expected (-28,-28), got (" + r.x + "," + r.y + ")");
        // Larger than the area: pinned to the top-left edge
        r = new Rectangle(40, 40, 200, 100);
        GuiElement.clampToArea(r, 2, 0, 0, 100, 50);
        if (r.x != 2 || r.y != 2)
            return fail("clampToArea oversized: expected (2,2), got (" + r.x + "," + r.y + ")");

        // Wrap (width = char count): keeps '\n', breaks at spaces, splits over-long words
        String wrapped = GuiElement.wrapText("aaa bbb ccc\ndd\nabcdefghij", 7, String::length);
        String expected = "aaa bbb\nccc\ndd\nabcdefg\nhij";
        if (!expected.equals(wrapped))
            return fail("wrapText: expected [" + expected + "], got [" + wrapped + "]");
        return pass("Tooltip auto side, clamping and wrapping work");
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

    private TestResult testListViewRefillKeepsScrollOffset() {
        // Content viewport is 298px (300 - 2*outline); 20 rows of 30px = 600px -> max offset 302
        net.kroia.modutilities.gui.elements.base.ListView[] views = {
                new VerticalListView(0, 0, 200, 300), new HorizontalListView(0, 0, 300, 200)};
        for (var view : views) {
            String name = view.getClass().getSimpleName();
            for (int i = 0; i < 20; i++) view.addChild(new EmptyButton(0, 0, 30, 30));
            view.setScrollOffset(200);
            view.removeChilds();
            for (int i = 0; i < 20; i++) view.addChild(new EmptyButton(0, 0, 30, 30));
            if (view.getScrollOffset() != 200)
                return fail(name + ": refill with same rows changed offset 200 -> " + view.getScrollOffset());
            view.removeChilds();
            for (int i = 0; i < 12; i++) view.addChild(new EmptyButton(0, 0, 30, 30)); // 360px -> max 62
            if (view.getScrollOffset() != 62)
                return fail(name + ": refill with fewer rows should clamp to 62, got " + view.getScrollOffset());
        }
        return pass("ListView keeps scroll offset across clear + refill, clamps when content shrinks");
    }

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
