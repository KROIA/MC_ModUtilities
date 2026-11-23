package net.kroia.modutilities.placeholder;

import net.kroia.modutilities.ModUtilitiesMod;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Loader- and placeholder-mod-neutral registry for text placeholders.
 * <p>
 * Dependent mods declare placeholders here (usually during mod init). ModUtilities bridges every
 * registration to each installed placeholder system (NEZNAMY TAB, Patbox Text Placeholder API, ...)
 * as soon as that system becomes available; registrations made after a backend is active are
 * forwarded immediately. Dependent mods need no compile dependency on any placeholder mod.
 * <p>
 * The identifier format is chosen per backend: TAB uses {@code %<namespace>_<name>%},
 * Placeholder API uses {@code <namespace>:<name>}.
 * <p>
 * <b>Thread safety:</b> backends may evaluate placeholders off the server thread (TAB does).
 * Placeholder functions must therefore be thread-safe, ideally returning cached values that the
 * server thread updates. An exception thrown by a function is caught, logged once, and replaced
 * by an empty string.
 */
public final class PlaceholderRegistry {
    private static final Pattern ID_PART = Pattern.compile("[a-z0-9_]+");

    private static final Map<String, Placeholder> PLACEHOLDERS = new LinkedHashMap<>();
    private static final List<Consumer<Placeholder>> BACKENDS = new ArrayList<>();

    private PlaceholderRegistry() {}

    /**
     * Registers a per-player placeholder.
     *
     * @param namespace         usually the mod ID, {@code [a-z0-9_]+}
     * @param name              placeholder name, {@code [a-z0-9_]+}
     * @param refreshIntervalMs how often the backend should re-evaluate the value (> 0); ignored by backends without refresh support
     * @param function          value provider; must be thread-safe (see class Javadoc)
     */
    public static void registerPlayer(String namespace, String name, int refreshIntervalMs, Function<ServerPlayer, String> function) {
        add(new Placeholder(namespace, name, refreshIntervalMs, function, null));
    }

    /**
     * Registers a server-wide placeholder (same value for every player).
     *
     * @param namespace         usually the mod ID, {@code [a-z0-9_]+}
     * @param name              placeholder name, {@code [a-z0-9_]+}
     * @param refreshIntervalMs how often the backend should re-evaluate the value (> 0); ignored by backends without refresh support
     * @param supplier          value provider; must be thread-safe (see class Javadoc)
     */
    public static void registerServer(String namespace, String name, int refreshIntervalMs, Supplier<String> supplier) {
        add(new Placeholder(namespace, name, refreshIntervalMs, null, supplier));
    }

    private static synchronized void add(Placeholder p) {
        if (p.namespace() == null || p.name() == null
                || !ID_PART.matcher(p.namespace()).matches() || !ID_PART.matcher(p.name()).matches()) {
            ModUtilitiesMod.LOGGER.warn("[Placeholder] Rejected \"{}:{}\": namespace and name must match [a-z0-9_]+", p.namespace(), p.name());
            return;
        }
        if (p.refreshIntervalMs() <= 0) {
            ModUtilitiesMod.LOGGER.warn("[Placeholder] Rejected \"{}\": refresh interval must be > 0, got {}", p.id(), p.refreshIntervalMs());
            return;
        }
        if ((p.player() == null) == (p.server() == null)) {
            ModUtilitiesMod.LOGGER.warn("[Placeholder] Rejected \"{}\": function is null", p.id());
            return;
        }
        if (PLACEHOLDERS.putIfAbsent(p.id(), p) != null) {
            ModUtilitiesMod.LOGGER.warn("[Placeholder] Duplicate \"{}\" ignored", p.id());
            return;
        }
        for (Consumer<Placeholder> backend : BACKENDS)
            forward(backend, p);
    }

    /**
     * Internal: attaches a backend. Replays every existing registration into it and forwards all later ones.
     */
    public static synchronized void addBackend(Consumer<Placeholder> backend) {
        BACKENDS.add(backend);
        for (Placeholder p : PLACEHOLDERS.values())
            forward(backend, p);
    }

    /** Internal: snapshot of all registrations, for backends that must re-register after a reload. */
    public static synchronized List<Placeholder> getAll() {
        return new ArrayList<>(PLACEHOLDERS.values());
    }

    private static void forward(Consumer<Placeholder> backend, Placeholder p) {
        try {
            backend.accept(p);
        } catch (Throwable t) {
            ModUtilitiesMod.LOGGER.error("[Placeholder] Backend failed to register \"{}\"", p.id(), t);
        }
    }
}
