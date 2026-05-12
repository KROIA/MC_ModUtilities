package net.kroia.modutilities.gui.display;

import net.kroia.modutilities.gui.Gui;

@FunctionalInterface
public interface ContentBuilder {
    void build(Gui gui, int width, int height);
}
