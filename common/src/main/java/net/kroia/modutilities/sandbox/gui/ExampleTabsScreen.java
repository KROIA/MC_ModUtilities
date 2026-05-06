package net.kroia.modutilities.sandbox.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiScreen;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.Frame;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.TabElement;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.network.chat.Component;

/**
 * Usecase example: a tabbed interface with three different content tabs.
 *
 * Demonstrates:
 *  - {@link TabElement} hosting multiple {@link Frame} content panes
 *  - Adding tabs by string name with {@link TabElement#addTab(String, GuiElement)}
 *  - Each tab's content laid out independently inside its own Frame
 *
 * Open via: /modutilities openExample tabs
 */
@Environment(EnvType.CLIENT)
public class ExampleTabsScreen extends GuiScreen {

    private final TabElement tabElement;
    private final Frame infoTab;
    private final Frame controlsTab;
    private final Frame aboutTab;

    private final Label infoTitle;
    private final Label infoBody;

    private final Label controlsTitle;
    private final Button controlsButton;
    private final Label controlsCounterLabel;
    private int counter = 0;

    private final Label aboutTitle;
    private final Label aboutBody;

    public ExampleTabsScreen() {
        super(Component.literal("Tabs Example"));

        tabElement = new TabElement();
        addElement(tabElement);

        // Tab 1: information
        infoTab = new Frame();
        infoTitle = new Label("Information");
        infoTitle.setAlignment(GuiElement.Alignment.CENTER);
        infoTitle.setTextFontScale(1.4f);
        infoBody = new Label("This is the Information tab. Tabs let you split a screen into separately-rendered sections.");
        infoBody.setAlignment(GuiElement.Alignment.CENTER);
        infoTab.addChild(infoTitle);
        infoTab.addChild(infoBody);

        // Tab 2: interactive controls
        controlsTab = new Frame();
        controlsTitle = new Label("Controls");
        controlsTitle.setAlignment(GuiElement.Alignment.CENTER);
        controlsTitle.setTextFontScale(1.4f);
        controlsCounterLabel = new Label("Clicked: 0 times");
        controlsCounterLabel.setAlignment(GuiElement.Alignment.CENTER);
        controlsButton = new Button("Click me", () -> {
            counter++;
            controlsCounterLabel.setText("Clicked: " + counter + " times");
        });
        controlsTab.addChild(controlsTitle);
        controlsTab.addChild(controlsCounterLabel);
        controlsTab.addChild(controlsButton);

        // Tab 3: about
        aboutTab = new Frame();
        aboutTitle = new Label("About");
        aboutTitle.setAlignment(GuiElement.Alignment.CENTER);
        aboutTitle.setTextFontScale(1.4f);
        aboutBody = new Label("Built with the MC_ModUtilities GUI library.");
        aboutBody.setAlignment(GuiElement.Alignment.CENTER);
        aboutTab.addChild(aboutTitle);
        aboutTab.addChild(aboutBody);

        tabElement.addTab("Info", infoTab);
        tabElement.addTab("Controls", controlsTab);
        tabElement.addTab("About", aboutTab);
    }

    public static void open() {
        GuiScreen.setScreen(new ExampleTabsScreen());
    }

    @Override
    protected void updateLayout(Gui gui) {
        int margin = 20;
        tabElement.setBounds(margin, margin, getWidth() - margin * 2, getHeight() - margin * 2);

        // Info tab
        int iw = infoTab.getWidth();
        infoTitle.setBounds(0, 16, iw, 22);
        infoBody.setBounds(20, infoTitle.getBottom() + 12, iw - 40, 30);

        // Controls tab
        int cw = controlsTab.getWidth();
        controlsTitle.setBounds(0, 16, cw, 22);
        controlsCounterLabel.setBounds(0, controlsTitle.getBottom() + 12, cw, 18);
        controlsButton.setBounds(cw / 2 - 60, controlsCounterLabel.getBottom() + 12, 120, 22);

        // About tab
        int aw = aboutTab.getWidth();
        aboutTitle.setBounds(0, 16, aw, 22);
        aboutBody.setBounds(20, aboutTitle.getBottom() + 12, aw - 40, 18);
    }
}
