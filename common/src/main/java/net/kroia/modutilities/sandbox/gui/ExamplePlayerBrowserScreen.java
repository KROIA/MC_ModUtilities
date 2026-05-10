package net.kroia.modutilities.sandbox.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiScreen;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.EmptyButton;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.VerticalListView;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.layout.LayoutVertical;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Usecase example: a list of online players with details and a refresh action.
 *
 * Demonstrates:
 *  - Reading {@link PlayerInfo} from the client connection
 *  - Filling a {@link VerticalListView} with custom row buttons
 *  - Selecting a row and showing its details in a side panel
 *  - A refresh button that rebuilds the list at runtime
 *
 * Open via: /modutilities openExample playerBrowser
 */
@Environment(EnvType.CLIENT)
public class ExamplePlayerBrowserScreen extends GuiScreen {

    private final Label title;
    private final VerticalListView listView;
    private final Label detailsTitle;
    private final Label detailsName;
    private final Label detailsLatency;
    private final Label detailsGameMode;
    private final Button refreshButton;

    private PlayerInfo selected = null;

    public ExamplePlayerBrowserScreen() {
        super(Component.literal("Player Browser Example"));

        title = new Label("Online Players");
        title.setAlignment(GuiElement.Alignment.CENTER);
        title.setTextFontScale(1.4f);
        addElement(title);

        listView = new VerticalListView();
        LayoutVertical layout = new LayoutVertical();
        layout.stretchX = true;
        layout.spacing = 2;
        listView.setLayout(layout);
        addElement(listView);

        detailsTitle = new Label("Details");
        detailsTitle.setAlignment(GuiElement.Alignment.CENTER);
        detailsTitle.setTextFontScale(1.2f);
        addElement(detailsTitle);

        detailsName = new Label("Name: -");
        detailsLatency = new Label("Ping: -");
        detailsGameMode = new Label("Game mode: -");
        addElement(detailsName);
        addElement(detailsLatency);
        addElement(detailsGameMode);

        refreshButton = new Button("Refresh", this::rebuildList);
        addElement(refreshButton);

        rebuildList();
    }

    public static void open() {
        GuiScreen.setScreen(new ExamplePlayerBrowserScreen());
    }

    private void rebuildList() {
        listView.removeChilds();

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            Label noConnection = new Label("Not connected to a server.");
            noConnection.setAlignment(GuiElement.Alignment.CENTER);
            noConnection.setHeight(20);
            listView.addChild(noConnection);
            return;
        }

        List<PlayerInfo> players = new ArrayList<>(connection.getOnlinePlayers());
        players.sort(Comparator.comparing(p -> p.getProfile().getName().toLowerCase()));

        for (PlayerInfo info : players) {
            listView.addChild(createPlayerRow(info));
        }

        if (players.isEmpty()) {
            Label empty = new Label("No players online.");
            empty.setAlignment(GuiElement.Alignment.CENTER);
            empty.setHeight(20);
            listView.addChild(empty);
        }
    }

    private GuiElement createPlayerRow(PlayerInfo info) {
        EmptyButton row = new EmptyButton();
        row.setHeight(18);
        row.setOnFallingEdge(() -> select(info));

        Label nameLabel = new Label(info.getProfile().getName());
        nameLabel.setAlignment(GuiElement.Alignment.LEFT);
        nameLabel.setPadding(4);
        row.addChild(nameLabel);

        Label pingLabel = new Label(info.getLatency() + " ms");
        pingLabel.setAlignment(GuiElement.Alignment.RIGHT);
        pingLabel.setPadding(4);
        row.addChild(pingLabel);

        // Sub-children inside an EmptyButton don't auto-resize, so size them
        // when the row is laid out via a tiny inline layout.
        row.setLayout(new net.kroia.modutilities.gui.layout.Layout() {
            @Override
            public void apply(GuiElement element) {
                int w = element.getWidth();
                int h = element.getHeight();
                nameLabel.setBounds(0, 0, w * 2 / 3, h);
                pingLabel.setBounds(w * 2 / 3, 0, w / 3, h);
            }
        });
        return row;
    }

    private void select(PlayerInfo info) {
        selected = info;
        if (info == null) {
            detailsName.setText("Name: -");
            detailsLatency.setText("Ping: -");
            detailsGameMode.setText("Game mode: -");
            return;
        }
        detailsName.setText("Name: " + info.getProfile().getName());
        detailsLatency.setText("Ping: " + info.getLatency() + " ms");
        detailsGameMode.setText("Game mode: " + info.getGameMode().getName());
    }

    @Override
    protected void updateLayout(Gui gui) {
        int w = getWidth();
        int h = getHeight();
        int margin = 12;

        title.setBounds(0, margin, w, 20);

        int contentTop = title.getBottom() + 8;
        int contentBottom = h - margin - 30;
        int sidebarWidth = 180;

        int listWidth = w - sidebarWidth - margin * 3;
        listView.setBounds(margin, contentTop, listWidth, contentBottom - contentTop);

        int sx = listView.getRight() + margin;
        int sy = contentTop;
        detailsTitle.setBounds(sx, sy, sidebarWidth, 18);
        sy += 22;
        detailsName.setBounds(sx, sy, sidebarWidth, 14);
        sy += 16;
        detailsLatency.setBounds(sx, sy, sidebarWidth, 14);
        sy += 16;
        detailsGameMode.setBounds(sx, sy, sidebarWidth, 14);

        refreshButton.setBounds(w / 2 - 60, contentBottom + 4, 120, 22);
    }
}
