package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.gui.client.GuiScreen;
import net.kroia.modutilities.gui.elements.DropDownMenu;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.kroia.testkit.TestCategory;
import net.kroia.testkit.TestResult;
import net.kroia.testkit.TestSuite;
import net.kroia.testkit.gui.Gui;
import net.kroia.testkit.gui.GuiNode;
import net.minecraft.network.chat.Component;

/** Client GUI tests of single ModUtilities widgets, driven through TestKit. */
public class GuiWidgetTests extends TestSuite {

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
        addTest("dropdown_label_click_toggles", this::testDropDownLabelClickToggles);
        addTest("label_ellipsis_cuts_overflow", this::testLabelEllipsisCutsOverflow);
    }

    @Override
    public void teardown() {
        Gui.close();
    }

    /** MU-176: a click on the drop-down's label (not the arrow) opens the menu, a second one closes it. */
    private TestResult testDropDownLabelClickToggles() {
        DropDownScreen screen = new DropDownScreen();
        Gui.open(screen);
        GuiNode label = Gui.find(n -> "Label".equals(n.type()) && "Pick one".equals(n.text()), "drop-down label 'Pick one'");
        Gui.click(label);
        if (!Gui.onClient(screen.menu::isExpanded)) return fail("click on the label must expand the menu");
        Gui.click(label);
        return assertFalse("second click on the label must collapse the menu", Gui.onClient(screen.menu::isExpanded));
    }

    /** MU-177: with ellipsis on, overflowing text is cut to fit and ends with "..."; off, the full text is drawn. */
    private TestResult testLabelEllipsisCutsOverflow() {
        String full = "A label text that is much too long for its width";
        return Gui.onClient(() -> {
            Label label = new Label(full);
            label.setSize(60, Label.DEFAULT_HEIGHT);
            if (!full.equals(label.getDisplayedText()))
                return fail("ellipsis off: displayed text must be the full text, got '" + label.getDisplayedText() + "'");
            label.setEllipsisEnabled(true);
            String shown = label.getDisplayedText();
            int inner = label.getWidth() - 2 * label.getPadding();
            if (!shown.endsWith("...")) return fail("ellipsis on: displayed text must end with '...', got '" + shown + "'");
            if (label.getTextWidth(shown) > inner)
                return fail("ellipsis on: '" + shown + "' is " + label.getTextWidth(shown) + " px, inner width " + inner);
            return assertEquals("getText() must stay the full text", full, label.getText());
        });
    }

    private static final class DropDownScreen extends GuiScreen {
        final DropDownMenu menu = new DropDownMenu("Pick one");

        DropDownScreen() {
            super(Component.literal("Drop-down test"));
            menu.addOption("a");
            menu.addOption("b");
            addElement(menu);
        }

        @Override
        protected void updateLayout(net.kroia.modutilities.gui.Gui gui) {
            menu.setBounds(10, 10, 100, 20);
        }
    }
}
