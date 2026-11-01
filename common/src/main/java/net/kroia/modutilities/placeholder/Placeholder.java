package net.kroia.modutilities.placeholder;

import net.kroia.modutilities.ModUtilitiesMod;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A registered placeholder, as handed to backends. Exactly one of {@link #player()} / {@link #server()} is non-null.
 * Backends should call {@link #evaluate(ServerPlayer)}, which never throws.
 */
public final class Placeholder {
    private final String namespace;
    private final String name;
    private final int refreshIntervalMs;
    private final Function<ServerPlayer, String> player;
    private final Supplier<String> server;
    private volatile boolean errorLogged;

    Placeholder(String namespace, String name, int refreshIntervalMs, Function<ServerPlayer, String> player, Supplier<String> server) {
        this.namespace = namespace;
        this.name = name;
        this.refreshIntervalMs = refreshIntervalMs;
        this.player = player;
        this.server = server;
    }

    public String namespace() { return namespace; }
    public String name() { return name; }
    public int refreshIntervalMs() { return refreshIntervalMs; }
    public Function<ServerPlayer, String> player() { return player; }
    public Supplier<String> server() { return server; }
    public boolean isPlayerPlaceholder() { return player != null; }

    /** {@code <namespace>:<name>} */
    public String id() { return namespace + ":" + name; }

    /**
     * Evaluates the placeholder. Exceptions are caught and logged once; null and failures yield {@code ""}.
     *
     * @param target the player for player placeholders (null yields {@code ""}); ignored for server placeholders
     */
    public String evaluate(ServerPlayer target) {
        try {
            String value;
            if (player != null)
                value = target == null ? null : player.apply(target);
            else
                value = server.get();
            return value == null ? "" : value;
        } catch (Exception | LinkageError t) {
            if (!errorLogged) {
                errorLogged = true;
                ModUtilitiesMod.LOGGER.error("[Placeholder] \"{}\" threw; returning empty string (further errors suppressed)", id(), t);
            }
            return "";
        }
    }
}
