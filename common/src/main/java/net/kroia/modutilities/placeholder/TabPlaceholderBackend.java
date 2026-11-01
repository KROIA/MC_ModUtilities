package net.kroia.modutilities.placeholder;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.EventBus;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.UtilitiesPlatform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * NEZNAMY TAB bridge. Only class-loaded when the "tab" mod is present (guarded in {@link ModUtilitiesMod#init()}),
 * so a missing TAB never causes NoClassDefFoundError. Identifier format: {@code %<namespace>_<name>%}.
 */
public final class TabPlaceholderBackend {
    private static boolean attached;
    private static Object hookedEventBus;

    private TabPlaceholderBackend() {}

    /** Called on every server start; TAB (re)initialises with the server, so everything is re-registered. */
    public static void onServerStarted() {
        TabAPI tab = TabAPI.getInstance();
        if (tab == null) {
            ModUtilitiesMod.LOGGER.warn("[Placeholder] TAB is loaded but its API is not available; TAB placeholders skipped");
            return;
        }
        if (!attached) {
            attached = true;
            PlaceholderRegistry.addBackend(TabPlaceholderBackend::register); // replays all + forwards late registrations
        } else {
            PlaceholderRegistry.getAll().forEach(TabPlaceholderBackend::registerSafe);
        }
        // "/tab reload" drops API placeholders; re-register them afterwards.
        EventBus bus = tab.getEventBus();
        if (bus != null && bus != hookedEventBus) {
            hookedEventBus = bus;
            bus.register(TabLoadEvent.class, e -> PlaceholderRegistry.getAll().forEach(TabPlaceholderBackend::registerSafe));
        }
        ModUtilitiesMod.LOGGER.info("[Placeholder] Bridged {} placeholder(s) to TAB", PlaceholderRegistry.getAll().size());
    }

    private static void registerSafe(Placeholder p) {
        try {
            register(p);
        } catch (Exception | LinkageError t) {
            ModUtilitiesMod.LOGGER.error("[Placeholder] TAB failed to register \"{}\"", p.id(), t);
        }
    }

    private static void register(Placeholder p) {
        TabAPI tab = TabAPI.getInstance();
        if (tab == null)
            return; // server not running; re-registered on next start
        String identifier = "%" + p.namespace() + "_" + p.name() + "%";
        if (p.isPlayerPlaceholder())
            tab.getPlaceholderManager().registerPlayerPlaceholder(identifier, p.refreshIntervalMs(), tp -> p.evaluate(toServerPlayer(tp)));
        else
            tab.getPlaceholderManager().registerServerPlaceholder(identifier, p.refreshIntervalMs(), () -> p.evaluate(null));
    }

    private static ServerPlayer toServerPlayer(TabPlayer tp) {
        if (tp.getPlayer() instanceof ServerPlayer sp)
            return sp;
        MinecraftServer server = UtilitiesPlatform.getServer();
        return server == null ? null : server.getPlayerList().getPlayer(tp.getUniqueId());
    }
}
