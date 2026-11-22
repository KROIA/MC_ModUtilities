package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.Label;
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
