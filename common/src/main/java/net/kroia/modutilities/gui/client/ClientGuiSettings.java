package net.kroia.modutilities.gui.client;

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
 * {@code guiScale}: 0 = follow Minecraft's GUI scale (default), N &gt; 0 = render
 * library screens at N window pixels per GUI unit, independent of Minecraft's GUI
 * scale. Read once when the first library screen opens.
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

    /**
     * Pushes the current Minecraft GUI scale and the library "GUI scale" setting into
     * a screen-hosted {@link Gui}. Called by the hosting screens on construction and
     * on every {@code init()} (resize / MC GUI scale change).
     */
    static void applyTo(Gui target) {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        double mcScale = window.getGuiScale();
        Integer fixed = get().gui.guiScale.get();
        int maxScale = window.calculateScale(0, mc.isEnforceUnicode()); // what MC's "auto" picks for this window
        target.setDisplayScaleFactor(mcScale);
        target.setBaseGuiScale(Gui.fixedBaseGuiScale(fixed == null ? 0 : fixed, maxScale, mcScale));
    }
}
