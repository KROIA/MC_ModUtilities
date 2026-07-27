package net.kroia.modutilities.gui.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.kroia.modutilities.ModUtilitiesMod;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * ModUtilities JEI integration for the generic overlay hide/show API.
 * <p>
 * <b>Placement decision:</b> this plugin lives in {@code common/} with the JEI
 * API as {@code compileOnly}. One shared class keeps the reflection code
 * loader-agnostic and in one place; the mod builds and runs without JEI on the
 * classpath (JEI is not a runtime dependency of any loader module).
 * <p>
 * <b>Discovery is loader-specific:</b> NeoForge JEI scans jars for the
 * {@code @JeiPlugin} annotation, but Fabric JEI discovers plugins ONLY via the
 * {@code jei_mod_plugin} entrypoint (see JEI's {@code FabricPluginFinder}) —
 * this class is therefore also declared as that entrypoint in
 * {@code fabric.mod.json}. Entrypoints are resolved lazily by the loader (only
 * when JEI queries them), so the soft dependency is preserved: without JEI the
 * class is never loaded.
 * <p>
 * <b>Why reflection:</b> JEI 19.x exposes no public API to hide either the
 * {@link mezz.jei.api.runtime.IIngredientListOverlay} or the
 * {@link mezz.jei.api.runtime.IBookmarkOverlay}, or the overlay buttons
 * (config gear, bookmark toggle, lookup-history toggle). The internal
 * {@code mezz.jei.common.config.IClientToggleState} on the overlay's concrete
 * class carries the flag; per-button visibility lives on each wrapper's inner
 * {@code AbstractWidget}. We reach both via reflection and install two
 * consumers into {@link JeiOverlayControl}. Reflection failures degrade to a
 * silent no-op — JEI just stays as it was.
 * <p>
 * <b>Task, 2026-07-26:</b> promoted from BankSystem's
 * {@code net.kroia.banksystem.minecraft.compat.BankSystemJeiPlugin} so any
 * ModUtilities-consuming mod can opt into overlay hide/show via
 * {@code GuiScreen#setHideJeiOverlay} / {@code #setHideJeiButtons}.
 * BankSystem's original plugin — which also registers recipe transfer +
 * exclusion areas — coexists with this one via a distinct {@link #PLUGIN_UID}.
 */
@JeiPlugin
public class ModUtilitiesJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "jei_overlay_control");

    public ModUtilitiesJeiPlugin() {}

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Intentionally empty. This plugin exists only to install the overlay
        // hide/show broker; per-screen exclusion areas are the consuming mod's
        // responsibility (registered from its own @JeiPlugin).
    }

    /**
     * Installs the JEI overlay-hide implementation into
     * {@link JeiOverlayControl}. Reaches JEI's internal
     * {@code mezz.jei.common.config.IClientToggleState} via reflection on the
     * concrete {@link mezz.jei.api.runtime.IIngredientListOverlay} instance
     * returned by {@link IJeiRuntime#getIngredientListOverlay()} — JEI 19.x
     * exposes no public API for this (verified against 19.27.0.336).
     * <p>
     * <b>Two broker entry points, one shared button state:</b>
     * <ul>
     *   <li>{@code setHidden(boolean)} — full hide. Flips
     *       {@code IClientToggleState.toggleOverlayEnabled()} (hides ingredient
     *       list, bookmark list, and — in JEI 19.22+ — the lookup-history
     *       overlay contents; all gated on {@code isOverlayEnabled} inside the
     *       overlays' {@code drawScreen} methods) AND hides EVERY discovered
     *       overlay button.</li>
     *   <li>{@code setButtonsHidden(boolean, String...)} — selective hide.
     *       Hides ONLY the named buttons (by their JEI wrapper field name,
     *       e.g. {@code "bookmarkButton"}, {@code "historyButton"}) and leaves
     *       the overlay-enabled flag alone so the ingredient list stays
     *       visible.</li>
     * </ul>
     * <b>Shared state model.</b> Each discovered button carries a pair of
     * owner flags in {@link HiderState}: {@code fullOwns[i]} (set by
     * {@code setHidden(true)}) and {@code selectiveOwns[i]} (set by
     * {@code setButtonsHidden(true, name)}). A button is hidden whenever
     * EITHER flag is set; its original visibility snapshot is captured on the
     * transition from "neither owner" to "either owner" and restored only when
     * both flags clear. This means:
     * <ul>
     *   <li>If a selective hide is active and a full hide is requested on top,
     *       we do not double-snapshot the shared buttons.</li>
     *   <li>If either session releases while the other is still active, the
     *       shared buttons stay hidden — the restore only fires when the last
     *       owner clears.</li>
     *   <li>The overlay-enabled flag is owned by the full session alone;
     *       selective hides never touch it.</li>
     * </ul>
     * <p>
     * <b>Buttons are discovered</b> by {@link #discoverOverlayButtons(Object, List)}
     * — a name-tolerant sweep of every wrapper on both overlays. In JEI 19.21
     * that yields {@code {configButton, bookmarkButton}}; in JEI 19.27 it
     * also yields the newly-added {@code historyButton}. Future additions are
     * picked up automatically as long as JEI keeps the {@code Wrapper.button}
     * + {@code AbstractWidget.visible} naming.
     * <p>
     * <b>Field name resolution:</b> internal JEI field names ({@code toggleState},
     * wrapper button names like {@code configButton}, and the inner
     * {@code button} field on the wrapper) are stable across loaders because
     * they belong to JEI's own bytecode. The Minecraft
     * {@code AbstractWidget.visible} field, however, is mojmap on NeoForge but
     * intermediary ({@code field_22764}) on Fabric at runtime — we try both
     * names in order via {@link #findFieldByAnyName(Class, String...)}.
     * <p>
     * <b>Failure policy:</b> any {@link Throwable} at install time or per
     * call is caught and the broker either stays at (or reverts to) its no-op
     * consumers. Worst case JEI stays visible, which is harmless.
     */
    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        try {
            Object listOverlay = jeiRuntime.getIngredientListOverlay();
            Object bookmarkOverlay = jeiRuntime.getBookmarkOverlay();
            if (listOverlay == null) {
                logWarn("IJeiRuntime.getIngredientListOverlay() returned null; overlay hider disabled.");
                return;
            }

            // 1) IClientToggleState via IngredientListOverlay#toggleState.
            Object toggleState = readFieldByName(listOverlay, "toggleState");
            if (toggleState == null) {
                logWarn("Could not resolve toggleState on " + listOverlay.getClass().getName()
                        + "; JEI overlay hide-on-open disabled.");
                return;
            }
            Method isOverlayEnabled = toggleState.getClass().getMethod("isOverlayEnabled");
            Method toggleOverlayEnabled = toggleState.getClass().getMethod("toggleOverlayEnabled");

            // 2) Discover every overlay-owned button (config gear, bookmark
            // toggle, lookup-history toggle in 19.22+, anything JEI adds
            // later). Each entry records the wrapper field name so the
            // selective hider can look them up by name.
            List<ButtonRef> buttons = new ArrayList<>();
            discoverOverlayButtons(listOverlay, buttons);
            discoverOverlayButtons(bookmarkOverlay, buttons);

            // Minecraft 'visible' field on AbstractWidget — mojmap on NeoForge,
            // intermediary on Fabric. Look it up once via the runtime class of
            // any button we found; skip button hiding entirely if none.
            Field visibleField = null;
            if (!buttons.isEmpty()) {
                visibleField = findFieldByAnyName(buttons.get(0).innerButton.getClass(),
                        "visible",       // mojmap (NeoForge runtime)
                        "field_22764");  // intermediary (Fabric runtime)
                if (visibleField == null) {
                    logWarn("Could not resolve AbstractWidget#visible field on "
                            + buttons.get(0).innerButton.getClass().getName()
                            + "; JEI overlay buttons will remain visible.");
                }
            }

            final Object toggleStateRef = toggleState;
            final List<ButtonRef> buttonsRef = buttons;
            final Field visibleFieldRef = visibleField;
            final HiderState state = new HiderState(buttons.size());

            // Full-hide consumer (setHidden path): flips overlay + all buttons.
            Consumer<Boolean> fullHider = hidden -> {
                try {
                    if (hidden) {
                        // Idempotent per full session — screen re-init on
                        // resize calls setHidden(true) again; skip the second
                        // time.
                        if (state.overlayFlippedByUs) return;

                        state.savedOverlayEnabled = (Boolean) isOverlayEnabled.invoke(toggleStateRef);
                        for (int i = 0; i < buttonsRef.size(); i++) {
                            if (state.fullOwns[i]) continue;
                            claimButtonHidden(buttonsRef.get(i), state, i, visibleFieldRef);
                            state.fullOwns[i] = true;
                        }
                        if (state.savedOverlayEnabled) {
                            toggleOverlayEnabled.invoke(toggleStateRef);
                        }
                        state.overlayFlippedByUs = true;
                    } else {
                        if (!state.overlayFlippedByUs) return;

                        // Overlay: only re-toggle if it is STILL disabled. If
                        // the user pressed 'O' inside our screen to re-show
                        // JEI, we leave their choice alone.
                        boolean currentlyEnabled = (Boolean) isOverlayEnabled.invoke(toggleStateRef);
                        if (state.savedOverlayEnabled && !currentlyEnabled) {
                            toggleOverlayEnabled.invoke(toggleStateRef);
                        }
                        for (int i = 0; i < buttonsRef.size(); i++) {
                            if (!state.fullOwns[i]) continue;
                            state.fullOwns[i] = false;
                            releaseButtonIfNoOwners(buttonsRef.get(i), state, i, visibleFieldRef);
                        }
                        state.overlayFlippedByUs = false;
                    }
                } catch (Throwable ignored) {
                    // Silent per-call failure — never let a mid-run reflection
                    // error bubble out and crash the calling screen.
                }
            };

            // Selective-hide consumer (setButtonsHidden path): flips only named buttons.
            BiConsumer<Boolean, String[]> selectiveHider = (hide, names) -> {
                try {
                    if (names == null) return;
                    for (String name : names) {
                        int idx = findButtonIndexByName(buttonsRef, name);
                        if (idx < 0) continue; // unknown name — silently ignore per contract
                        if (hide) {
                            if (state.selectiveOwns[idx]) continue; // idempotent per selective session
                            claimButtonHidden(buttonsRef.get(idx), state, idx, visibleFieldRef);
                            state.selectiveOwns[idx] = true;
                        } else {
                            if (!state.selectiveOwns[idx]) continue;
                            state.selectiveOwns[idx] = false;
                            releaseButtonIfNoOwners(buttonsRef.get(idx), state, idx, visibleFieldRef);
                        }
                    }
                } catch (Throwable ignored) {
                    // Silent per-call failure.
                }
            };

            JeiOverlayControl.install(fullHider);
            JeiOverlayControl.installSelective(selectiveHider);
        } catch (Throwable t) {
            JeiOverlayControl.install(null);
            JeiOverlayControl.installSelective(null);
            logWarn("Could not install JEI overlay hider: " + t);
        }
    }

    /**
     * If the given button is currently at its original visibility (no owner
     * has claimed it yet), snapshot its current {@code visible} flag into
     * {@code state.savedVisible[idx]} and set it to {@code false}. If a
     * previous owner already claimed it, this is a silent no-op.
     * <p>
     * Callers are responsible for flipping the appropriate owner flag AFTER
     * calling this method.
     */
    private static void claimButtonHidden(ButtonRef btn, HiderState state, int idx, Field visibleField) {
        if (visibleField == null) return;
        // Already claimed by a prior owner (either full or selective session)?
        if (state.fullOwns[idx] || state.selectiveOwns[idx]) return;
        try {
            state.savedVisible[idx] = visibleField.getBoolean(btn.innerButton);
            visibleField.setBoolean(btn.innerButton, false);
        } catch (Throwable ignored) {
            // Per-button snapshot/hide failure — leave slot null so restore skips it.
            state.savedVisible[idx] = null;
        }
    }

    /**
     * If NEITHER owner (full nor selective) currently holds the given button
     * hidden, restore its saved visibility. Called after clearing an owner
     * flag; must NOT clear the owner flag itself.
     */
    private static void releaseButtonIfNoOwners(ButtonRef btn, HiderState state, int idx, Field visibleField) {
        if (visibleField == null) return;
        if (state.fullOwns[idx] || state.selectiveOwns[idx]) return;
        Boolean saved = state.savedVisible[idx];
        if (saved == null) return; // never snapshotted (or snapshot failed)
        try {
            visibleField.setBoolean(btn.innerButton, saved);
        } catch (Throwable ignored) {
            // Individual restore failure — leave it, continue.
        }
        state.savedVisible[idx] = null;
    }

    /**
     * Linear scan over the discovered button list for the entry whose
     * {@code wrapperName} matches {@code name}. Returns {@code -1} if not
     * found. The list is tiny (2-5 entries in JEI 19.21-19.27), so O(n) is
     * fine and avoids maintaining a parallel map.
     */
    private static int findButtonIndexByName(List<ButtonRef> buttons, String name) {
        if (name == null) return -1;
        for (int i = 0; i < buttons.size(); i++) {
            if (name.equals(buttons.get(i).wrapperName)) return i;
        }
        return -1;
    }

    /**
     * Pair of (JEI wrapper field name, the inner AbstractWidget-derived button
     * object). {@code wrapperName} is the field name on the concrete overlay
     * class (e.g. {@code "configButton"}, {@code "bookmarkButton"},
     * {@code "historyButton"}) and lets the selective-hide path look up a
     * specific button by name.
     */
    private static final class ButtonRef {
        final String wrapperName;
        final Object innerButton;
        ButtonRef(String wrapperName, Object innerButton) {
            this.wrapperName = wrapperName;
            this.innerButton = innerButton;
        }
    }

    /**
     * Shared per-button state for both broker entry points.
     * <p>
     * All parallel arrays are indexed by position in the discovered button
     * list. {@code fullOwns[i]} is set while {@code setHidden(true)} holds
     * button {@code i} hidden; {@code selectiveOwns[i]} is set while a
     * {@code setButtonsHidden(true, name)} session holds it hidden. A button
     * is at hidden state whenever EITHER flag is set; the saved original
     * visibility in {@code savedVisible[i]} is captured on the transition
     * from "neither owner" to "either owner" and restored (then cleared) on
     * the transition back.
     * <p>
     * {@code savedVisible[i]} is boxed so {@code null} encodes "snapshot
     * failed or button not currently claimed by us" — restore skips those
     * slots rather than writing garbage.
     * <p>
     * {@code overlayFlippedByUs} + {@code savedOverlayEnabled} are owned by
     * the full session alone; selective hides never touch the overlay flag.
     */
    private static class HiderState {
        boolean overlayFlippedByUs = false;
        boolean savedOverlayEnabled;
        final Boolean[] savedVisible;
        final boolean[] fullOwns;
        final boolean[] selectiveOwns;

        HiderState(int buttonCount) {
            this.savedVisible = new Boolean[buttonCount];
            this.fullOwns = new boolean[buttonCount];
            this.selectiveOwns = new boolean[buttonCount];
        }
    }

    /**
     * Name-tolerant sweep for overlay-owned buttons. Walks every declared
     * instance field on {@code overlay}'s class hierarchy; for each field
     * value, looks for a nested field named {@code button} whose value carries
     * a boolean {@code visible} (or {@code field_22764}) field. Any match is
     * added to {@code out} as a {@link ButtonRef} pairing the wrapper field
     * name with the inner {@code AbstractWidget}-derived button.
     * <p>
     * This shape matches every JEI overlay button through at least 19.27:
     * both the older {@code GuiIconToggleButton} wrapper and the newer
     * {@code IconButton} wrapper hold their inner Minecraft widget in a field
     * named {@code button}. If JEI ever renames that field the sweep finds
     * nothing on that wrapper and the button stays visible — a soft failure,
     * not a crash.
     * <p>
     * The overlay itself may be {@code null} (e.g. bookmark overlay not
     * available), in which case this is a no-op.
     */
    private static void discoverOverlayButtons(Object overlay, List<ButtonRef> out) {
        if (overlay == null) return;
        Class<?> cls = overlay.getClass();
        while (cls != null && cls != Object.class) {
            for (Field wrapperField : cls.getDeclaredFields()) {
                if (Modifier.isStatic(wrapperField.getModifiers())) continue;
                try {
                    wrapperField.setAccessible(true);
                    Object wrapper = wrapperField.get(overlay);
                    if (wrapper == null) continue;
                    Field innerBtnField = findFieldByName(wrapper.getClass(), "button");
                    if (innerBtnField == null) continue;
                    innerBtnField.setAccessible(true);
                    Object inner = innerBtnField.get(wrapper);
                    if (inner == null) continue;
                    Field visible = findFieldByAnyName(inner.getClass(), "visible", "field_22764");
                    if (visible == null || !visible.getType().equals(boolean.class)) continue;
                    out.add(new ButtonRef(wrapperField.getName(), inner));
                } catch (Throwable ignored) {
                    // This field didn't fit the button shape — move on.
                }
            }
            cls = cls.getSuperclass();
        }
    }

    /**
     * Reads a declared field by name from any level of the class hierarchy of
     * the given instance. Handles {@code setAccessible} and returns
     * {@code null} on any failure.
     */
    private static Object readFieldByName(Object instance, String fieldName) {
        if (instance == null) return null;
        Field f = findFieldByName(instance.getClass(), fieldName);
        if (f == null) return null;
        try {
            f.setAccessible(true);
            return f.get(instance);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Walks the class hierarchy of {@code cls} looking for a declared field
     * with the given name. Returns {@code null} if not found on any level up
     * to (but not including) {@link Object}. Used so this plugin does not
     * hardcode the internal impl class names of JEI's overlays.
     */
    private static Field findFieldByName(Class<?> cls, String name) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // try parent
            }
            c = c.getSuperclass();
        }
        return null;
    }

    /**
     * Convenience wrapper around {@link #findFieldByName(Class, String)} that
     * tries multiple candidate names in order and returns the first one that
     * resolves. Used for the Minecraft {@code AbstractWidget.visible} field,
     * which is {@code visible} in mojmap (NeoForge runtime) and
     * {@code field_22764} in intermediary (Fabric runtime).
     */
    private static Field findFieldByAnyName(Class<?> cls, String... names) {
        for (String name : names) {
            Field f = findFieldByName(cls, name);
            if (f != null) return f;
        }
        return null;
    }

    private static void logWarn(String message) {
        try {
            ModUtilitiesMod.LOGGER.warn("[ModUtilitiesJeiPlugin] " + message);
        } catch (Throwable ignored) {
            // Best-effort logging only.
        }
    }
}
