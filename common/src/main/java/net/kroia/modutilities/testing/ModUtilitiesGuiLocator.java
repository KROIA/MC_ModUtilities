package net.kroia.modutilities.testing;

import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.client.GuiContainerScreen;
import net.kroia.modutilities.gui.client.GuiScreen;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.CheckBox;
import net.kroia.modutilities.gui.elements.DropDownMenu;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.MultiLineTextBox;
import net.kroia.modutilities.gui.elements.TextBox;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.kroia.testkit.gui.GuiLocator;
import net.kroia.testkit.gui.GuiNode;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Client only. Shows TestKit's GUI driver the GuiElement tree of {@link GuiScreen}s and {@link GuiContainerScreen}s.
 * <ul>
 *   <li>name = {@link GuiElement#getId()}, type = simple class name (first named superclass for anonymous ones);</li>
 *   <li>text = the element's own text (Label, Button, CheckBox, TextBox, MultiLineTextBox, DropDownMenu label);</li>
 *   <li>bounds = global element position times the Gui's scale, i.e. the screen's GUI-scaled coordinates, which is
 *       what the screen's mouse methods take, so TestKit's clicks land on the element;</li>
 *   <li>textWidth only for Labels (a Button's text lives in its Label child), so layout checks do not flag
 *       TextBox content that legitimately scrolls;</li>
 *   <li>visible = enabled and all ancestors enabled ({@link GuiElement#isVisible()}), and drawn by every ancestor
 *       ({@link GuiElement#isChildRendered}): a list row scrolled fully out of its list's window is not visible,
 *       a partly scrolled-out row still is.</li>
 * </ul>
 */
public final class ModUtilitiesGuiLocator implements GuiLocator {

    /** Registers the locator with TestKit's GUI driver. Call on the client only. */
    public static void register() {
        net.kroia.testkit.gui.Gui.addLocator(new ModUtilitiesGuiLocator());
    }

    @Override
    public List<GuiNode> nodes(Screen screen) {
        Gui gui = screen instanceof GuiScreen s ? s.getGui()
                : screen instanceof GuiContainerScreen<?> c ? c.getGui() : null;
        if (gui == null) return List.of();
        return nodes(null, gui.getElements(), gui.getGuiScale(), true);
    }

    private static List<GuiNode> nodes(GuiElement parent, List<GuiElement> elements, float scale, boolean parentVisible) {
        List<GuiNode> nodes = new ArrayList<>(elements.size());
        for (GuiElement el : elements) {
            boolean visible = parentVisible && el.isVisible() && (parent == null || parent.isChildRendered(el));
            Point p = el.getGlobalPosition();
            int x = Math.round(p.x * scale), y = Math.round(p.y * scale);
            int w = Math.round((p.x + el.getWidth()) * scale) - x, h = Math.round((p.y + el.getHeight()) * scale) - y;
            String text = text(el);
            int textWidth = el instanceof Label l && !text.isEmpty() ? Math.round(el.getTextWidth(l.getDisplayedText()) * scale) : 0;
            nodes.add(new GuiNode(type(el), text, el.getId(), x, y, w, h, textWidth, visible,
                    nodes(el, el.getRenderedChilds(), scale, visible), el));
        }
        return nodes;
    }

    private static String text(GuiElement el) {
        String t = el instanceof Label l ? l.getText()
                : el instanceof Button b ? b.getText()
                : el instanceof CheckBox c ? c.getText()
                : el instanceof TextBox tb ? tb.getText()
                : el instanceof MultiLineTextBox m ? m.getText()
                : el instanceof DropDownMenu d ? d.getLabelText()
                : null;
        return t == null ? "" : t;
    }

    private static String type(GuiElement el) {
        Class<?> c = el.getClass();
        while (c.getSimpleName().isEmpty()) c = c.getSuperclass();
        return c.getSimpleName();
    }
}
