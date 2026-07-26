package net.kroia.modutilities.gui.jei;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Loader- and JEI-neutral broker for toggling JEI's ingredient list and
 * bookmark overlays (including their overlay buttons) from screen lifecycle
 * hooks, without leaking any {@code mezz.jei.*} classes into the screens
 * themselves.
 * <p>
 * <b>Why this indirection:</b> {@link net.kroia.modutilities.gui.client.GuiScreen}
 * and {@link net.kroia.modutilities.gui.client.GuiContainerScreen} are loaded
 * whether or not JEI is on the classpath. If the base classes referenced
 * {@link ModUtilitiesJeiPlugin} directly, a JEI-less run would fail to link
 * them the first time they are touched. {@code ModUtilitiesJeiPlugin} imports
 * {@code mezz.jei.*} and is discovered lazily by JEI (Fabric entrypoint,
 * NeoForge annotation scan) — without JEI it is never class-loaded. This
 * broker has no JEI imports, so the base classes (and consuming mods) can call
 * {@link #setHidden(boolean)} and {@link #setButtonsHidden(boolean, String...)}
 * unconditionally. When JEI is absent the installed slots stay at their no-op
 * defaults and every call is a silent skip.
 * <p>
 * <b>Two entry points, two use-cases:</b>
 * <ul>
 *   <li>{@link #setHidden(boolean)} — full hide: flips the overlay-enabled
 *       flag AND hides every discovered overlay button. Use for screens that
 *       want JEI to disappear entirely while open.</li>
 *   <li>{@link #setButtonsHidden(boolean, String...)} — targeted hide: hides
 *       only the named overlay buttons (by JEI's wrapper field name, e.g.
 *       {@code "bookmarkButton"}, {@code "historyButton"}) without touching
 *       the overlay-enabled flag. Use when the ingredient list itself should
 *       stay visible. Unknown names silently ignored.</li>
 * </ul>
 * The two entry points share the same per-button state inside
 * {@link ModUtilitiesJeiPlugin} — a per-button pair of owner flags (one per
 * caller category) drives the actual widget-level hide/restore, so a button
 * requested hidden by both callers only becomes visible again when both
 * release it.
 * <p>
 * <b>Threading invariant:</b> {@link #install(Consumer)},
 * {@link #installSelective(BiConsumer)} (called from
 * {@link ModUtilitiesJeiPlugin#onRuntimeAvailable}) and every {@code setHidden}
 * / {@code setButtonsHidden} call (from screen {@code init} / {@code onClose}
 * / {@code removed}) run on the Minecraft client render thread. The
 * installed-slot fields are therefore intentionally not {@code volatile}. If
 * any caller ever moves off that thread, they must be marked volatile.
 * <p>
 * <b>Failure policy:</b> every entry point swallows every {@code Throwable}
 * from the installed slot. Failing to toggle JEI must never crash the calling
 * screen — the worst-case fallback is that JEI stays as it was, which is
 * harmless.
 */
public final class JeiOverlayControl {

    // Same-thread only (see class javadoc "Threading invariant").
    private static Consumer<Boolean> hider = h -> {};
    private static BiConsumer<Boolean, String[]> selectiveHider = (h, names) -> {};

    private JeiOverlayControl() {}

    /**
     * Installs the full-hide toggle implementation used by
     * {@link #setHidden(boolean)}. Called once by
     * {@link ModUtilitiesJeiPlugin#onRuntimeAvailable} when JEI is present and
     * reflection into JEI's internal {@code IClientToggleState} succeeded.
     * Passing {@code null} reverts to the no-op default (used when reflection
     * fails).
     */
    public static void install(Consumer<Boolean> impl) {
        hider = impl != null ? impl : h -> {};
    }

    /**
     * Installs the selective button-hide implementation used by
     * {@link #setButtonsHidden(boolean, String...)}. Same install-once
     * semantics as {@link #install(Consumer)}; {@code null} reverts to
     * the no-op default.
     */
    public static void installSelective(BiConsumer<Boolean, String[]> impl) {
        selectiveHider = impl != null ? impl : (h, names) -> {};
    }

    /**
     * Request JEI's ingredient list + bookmark overlays hidden ({@code true})
     * or restored ({@code false}). Hides EVERY discovered overlay button and
     * flips the overlay-enabled flag. Safe to call whether or not JEI is
     * loaded. Idempotency (multiple {@code setHidden(true)} in a row from
     * screen resize) and "did the user hide JEI themselves?" bookkeeping live
     * inside the installed hider.
     */
    public static void setHidden(boolean hidden) {
        try {
            hider.accept(hidden);
        } catch (Throwable ignored) {
            // Fail-safe: never let overlay toggling break the calling screen.
        }
    }

    /**
     * Request only the named overlay buttons hidden ({@code true}) or
     * restored ({@code false}) — leaves the overlay-enabled flag alone and
     * does not touch any other button. {@code names} use JEI's wrapper
     * field names as discovered by
     * {@code ModUtilitiesJeiPlugin#discoverOverlayButtons} (typically
     * {@code "configButton"}, {@code "bookmarkButton"}, {@code "historyButton"}
     * on JEI 19.27); unknown names are silently ignored.
     * <p>
     * Interacts cleanly with {@link #setHidden(boolean)} — if a full hide is
     * already active, this call is a no-op for buttons already hidden by it
     * (no double-snapshot); on restore, each button only re-appears when
     * BOTH the full and the selective session have released it.
     */
    public static void setButtonsHidden(boolean hide, String... names) {
        try {
            selectiveHider.accept(hide, names != null ? names : new String[0]);
        } catch (Throwable ignored) {
            // Fail-safe: never let overlay toggling break the calling screen.
        }
    }
}
