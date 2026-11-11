package net.kroia.modutilities.gui.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.architectury.platform.Platform;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.setting.ModSettings;
import net.kroia.modutilities.setting.Setting;
import net.kroia.modutilities.setting.SettingsGroup;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-side settings of the ModUtilities GUI library, stored in
 * {@code config/modutilities/ClientConfig.json}:
 * <pre>
 * { "gui": { "guiScale": 0 } }
 * </pre>
 * {@code guiScale}: 0 = auto (default): library screens ignore Minecraft's GUI scale and
 * use the largest scale that fits the window (what Minecraft's own "auto" GUI scale picks),
 * so they keep their size whatever the player's GUI scale is and adapt on window resize;
 * -1 = follow Minecraft's GUI scale; N &gt; 0 = N window pixels per GUI unit, capped to fit
 * the window.
 * <p>
 * In every library screen (no text box focused) Ctrl + '+' / '-' step the scale and
 * Ctrl + '0' switches to auto (pressed again: follow Minecraft). The change applies live
 * and is saved to the file.
 *
 * @apiNote This class is client-only.
 */
public final class ClientGuiSettings extends ModSettings {
    private static final Path PATH = Platform.getConfigFolder().resolve(ModUtilitiesMod.MOD_ID).resolve("ClientConfig.json");

    public static final class GuiGroup extends SettingsGroup {
        public final Setting<Integer> guiScale = registerSetting("guiScale", 0, Integer.class);
        GuiGroup() { super("gui"); }
    }

    public final GuiGroup gui = createGroup(new GuiGroup());

    private static ClientGuiSettings instance;

    private ClientGuiSettings() {
        super("ModUtilities client settings");
        setLogger(ModUtilitiesMod.LOGGER::error, ModUtilitiesMod.LOGGER::error, ModUtilitiesMod.LOGGER::debug);
    }

    public static ClientGuiSettings get() {
        if (instance == null) {
            instance = new ClientGuiSettings();
            if (Files.exists(PATH)) {
                instance.loadSettings(PATH.toString());
            } else {
                try {
                    Files.createDirectories(PATH.getParent());
                    instance.saveSettings(PATH.toString()); // write the defaults so the entry is discoverable
                } catch (Exception e) {
                    ModUtilitiesMod.LOGGER.error("Failed to create {}", PATH, e);
                }
            }
        }
        return instance;
    }

    private static String feedback;
    private static long feedbackUntil;

    /**
     * Pushes the current Minecraft GUI scale and the library "GUI scale" setting into
     * a screen-hosted {@link Gui}. Called by the hosting screens on construction and
     * on every {@code init()} (resize / MC GUI scale change / scale shortcut).
     */
    static void applyTo(Gui target) {
        double mcScale = Minecraft.getInstance().getWindow().getGuiScale();
        target.setDisplayScaleFactor(mcScale);
        target.setBaseGuiScale(Gui.fixedBaseGuiScale(setting(), maxScale(), mcScale));
    }

    private static int setting() {
        Integer value = get().gui.guiScale.get();
        return value == null ? 0 : value;
    }

    /** @return what Minecraft's "auto" GUI scale picks for the current window */
    private static int maxScale() {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        return window.calculateScale(0, mc.isEnforceUnicode());
    }

    /**
     * Handles the GUI scale shortcuts: Ctrl + '+' / '-' (main row, numpad '+' / '-', and the
     * QWERTZ '+' / '-' positions) step the scale, Ctrl + '0' toggles auto / follow Minecraft.
     * Saves the new setting; the caller re-inits its screen so the change shows at once.
     * Callers must skip this while a text box has focus.
     *
     * @return {@code true} if the key was a scale shortcut
     */
    static boolean handleScaleKey(int keyCode) {
        if (!InputCompat.hasControlDown())
            return false;
        int current = setting();
        int max = maxScale();
        int mcScale = (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
        int next;
        switch (keyCode) {
            case InputConstants.KEY_EQUALS, InputConstants.KEY_ADD, InputConstants.KEY_RBRACKET ->
                    next = Gui.stepGuiScaleSetting(current, 1, max, mcScale);
            case InputConstants.KEY_MINUS, InputConstants.KEY_SLASH,
                 //org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT -> // mc<=26.2 (numpad '-': InputConstants has no name for it)
                 net.kroia.modutilities.gui.InputConstants.KEY_KP_SUBTRACT -> // mc>=26.3 (numpad '-', SDL3 scancode)
                    next = Gui.stepGuiScaleSetting(current, -1, max, mcScale);
            case InputConstants.KEY_0, InputConstants.KEY_NUMPAD0 -> next = current == 0 ? -1 : 0;
            default -> { return false; }
        }
        get().gui.guiScale.set(next);
        get().saveSettings(PATH.toString());
        int px = Gui.pixelGuiScale(next, max, mcScale);
        feedback = "GUI scale: " + (next == 0 ? "Auto (" + px + ")" : next < 0 ? "Minecraft (" + px + ")" : String.valueOf(px));
        feedbackUntil = System.currentTimeMillis() + 2000;
        return true;
    }

    /** Draws the feedback of the last scale shortcut for 2 s, top center, in unscaled screen coordinates. */
    static void renderFeedback(ClientGraphics g, int screenWidth) {
        if (feedback == null || System.currentTimeMillis() > feedbackUntil)
            return;
        int w = g.getTextWidth(feedback);
        int x = (screenWidth - w) / 2;
        g.fill(x - 4, 2, x + w + 4, 6 + g.getFontLineHeight(), 0xC0000000);
        g.drawString(Minecraft.getInstance().font, feedback, x, 5, 0xFFFFFFFF);
    }
}
