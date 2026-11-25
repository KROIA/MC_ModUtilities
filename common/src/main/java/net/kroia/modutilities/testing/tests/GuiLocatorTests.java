package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.VerticalListView;
import net.kroia.modutilities.gui.layout.LayoutVertical;
import net.kroia.modutilities.gui.client.GuiScreen;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.kroia.testkit.TestCategory;
import net.kroia.testkit.TestResult;
import net.kroia.testkit.TestSuite;
import net.kroia.testkit.gui.Gui;
import net.kroia.testkit.gui.GuiNode;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicInteger;

/** Client GUI test: TestKit's driver sees and clicks GuiElements through {@code ModUtilitiesGuiLocator}. */
public class GuiLocatorTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.GUI;
    }

    @Override
    public Side side() {
        return Side.CLIENT;
    }

    @Override
    public void registerTests() {
        addTest("locator_find_and_click_button", this::testFindAndClickButton);
        addTest("locator_list_row_scrolled_out_hidden", this::testListRowScrolledOutHidden);
    }

    @Override
    public void teardown() {
        Gui.close();
    }

    private TestResult testFindAndClickButton() {
        LocatorScreen screen = new LocatorScreen();
        Gui.open(screen);

        GuiNode label = Gui.find("Locator test");
        if (!"Label".equals(label.type())) return fail("label type: " + label);
        GuiNode button = Gui.find("mu_locator_button");
        if (!"Button".equals(button.type()) || !"Click me".equals(button.text()))
            return fail("button node: " + button);
        if (button.width() <= 0 || button.height() <= 0) return fail("button has no size: " + button);

        Gui.click(button);
        int clicks = Gui.onClient(screen.clicks::get);
        if (clicks != 1) return assertEquals("button clicks after one click", 1, clicks);

        var problems = Gui.layoutProblems();
        return assertTrue("layout problems: " + problems, problems.isEmpty());
    }

    /** MU-170: a list row outside the list's scroll window is not visible, so tests cannot click it. */
    private TestResult testListRowScrolledOutHidden() {
        Gui.open(new ListScreen());
        GuiNode first = Gui.find("mu_locator_row_0");
        GuiNode last = Gui.find("mu_locator_row_" + (ListScreen.ROWS - 1));
        if (!first.visible()) return fail("first row must be visible: " + first);
        return assertFalse("last row is scrolled out of the list, must not be visible: " + last, last.visible());
    }

    private static final class ListScreen extends GuiScreen {
        static final int ROWS = 20;
        private final VerticalListView list = new VerticalListView();

        ListScreen() {
            super(Component.literal("Locator list test"));
            list.setLayout(new LayoutVertical(0, 0, false, false));
            for (int i = 0; i < ROWS; i++) {
                Label row = new Label("Row " + i);
                row.setId("mu_locator_row_" + i);
                row.setBounds(0, 0, 80, 20);
                list.addChild(row);
            }
            addElement(list);
        }

        @Override
        protected void updateLayout(net.kroia.modutilities.gui.Gui gui) {
            list.setBounds(10, 10, 100, 60); // 3 of 20 rows fit
        }
    }

    private static final class LocatorScreen extends GuiScreen {
        final AtomicInteger clicks = new AtomicInteger();
        private final Label label = new Label("Locator test");
        private final Button button = new Button("Click me", clicks::incrementAndGet);

        LocatorScreen() {
            super(Component.literal("Locator test"));
            button.setId("mu_locator_button");
            addElement(label);
            addElement(button);
        }

        @Override
        protected void updateLayout(net.kroia.modutilities.gui.Gui gui) {
            label.setBounds(10, 10, 100, 20);
            button.setBounds(10, 40, 100, 20);
        }
    }
}
