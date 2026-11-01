package net.kroia.modutilities.sandbox.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.client.GuiScreen;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.TabElement;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class ExampleTabsScreen extends GuiScreen {

    private final TabElement tabElement;

    public ExampleTabsScreen() {
        super(Component.literal("Tabs Example"));

        tabElement = new TabElement();
        addElement(tabElement);

        tabElement.addTab("Info", new InfoTab());
        tabElement.addTab("Controls", new ControlsTab());
        tabElement.addTab("About", new AboutTab());
    }

    public static void open() {
        GuiScreen.setScreen(new ExampleTabsScreen());
    }

    @Override
    protected void updateLayout(Gui gui) {
        int margin = 20;
        tabElement.setBounds(margin, margin, getWidth() - margin * 2, getHeight() - margin * 2);
    }

    private static class InfoTab extends GuiElement {
        private final Label title;
        private final Label body;

        InfoTab() {
            title = new Label("Information");
            title.setAlignment(Alignment.CENTER);
            title.setTextFontScale(1.4f);
            addChild(title);

            body = new Label("This is the Information tab. Tabs let you split\na screen into separately-rendered sections.");
            body.setAlignment(Alignment.CENTER);
            addChild(body);
        }

        @Override
        protected void layoutChanged() {
            int w = getWidth();
            title.setBounds(0, 16, w, 22);
            body.setBounds(20, title.getBottom() + 12, w - 40, 30);
        }

        @Override
        protected void render() {}
    }

    private static class ControlsTab extends GuiElement {
        private final Label title;
        private final Label counterLabel;
        private final Button button;
        private int counter = 0;

        ControlsTab() {
            title = new Label("Controls");
            title.setAlignment(Alignment.CENTER);
            title.setTextFontScale(1.4f);
            addChild(title);

            counterLabel = new Label("Clicked: 0 times");
            counterLabel.setAlignment(Alignment.CENTER);
            addChild(counterLabel);

            button = new Button("Click me", () -> {
                counter++;
                counterLabel.setText("Clicked: " + counter + " times");
            });
            addChild(button);
        }

        @Override
        protected void layoutChanged() {
            int w = getWidth();
            title.setBounds(0, 16, w, 22);
            counterLabel.setBounds(0, title.getBottom() + 12, w, 18);
            button.setBounds(w / 2 - 60, counterLabel.getBottom() + 12, 120, 22);
        }

        @Override
        protected void render() {}
    }

    private static class AboutTab extends GuiElement {
        private final Label title;
        private final Label body;

        AboutTab() {
            title = new Label("About");
            title.setAlignment(Alignment.CENTER);
            title.setTextFontScale(1.4f);
            addChild(title);

            body = new Label("Built with the MC_ModUtilities GUI library.");
            body.setAlignment(Alignment.CENTER);
            addChild(body);
        }

        @Override
        protected void layoutChanged() {
            int w = getWidth();
            title.setBounds(0, 16, w, 22);
            body.setBounds(20, title.getBottom() + 12, w - 40, 18);
        }

        @Override
        protected void render() {}
    }
}
