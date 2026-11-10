package net.kroia.modutilities.gui.elements;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * An {@link ExpandablePanel} whose header carries a feature on/off {@link CheckBox}.
 * <p>
 * Header layout: {@code [x] Title ........ [Restart required] [arrow]}. Clicking the
 * check box toggles the feature, clicking anywhere else on the header expands or
 * collapses the child settings (the enclosing list reflows, see
 * {@link ExpandablePanel}).
 * <ul>
 *   <li>Children added via {@link #addChild} are only interactive while the feature
 *       is enabled; otherwise they stay visible but greyed out
 *       ({@link #setInteractive(boolean)}).</li>
 *   <li>{@link #setRestartRequired(boolean)} shows a "Restart required" marker in the
 *       header ({@link #setRestartMarkerText} changes its wording). Use
 *       {@link #createRestartIndicator()} for the same marker on child rows.</li>
 *   <li>{@link #setLocked(boolean, Component)} makes the check box and the children
 *       read-only and shows the reason as header tooltip (e.g. on slave servers).</li>
 * </ul>
 *
 * @apiNote The {@code gui/} package is client-only.
 */
public class FeatureToggleSection extends ExpandablePanel {

    /** Text color of the restart marker. */
    public static final int RESTART_INDICATOR_COLOR = 0xFFFFAA30;
    /** Title color while the section is locked. */
    public static final int LOCKED_TITLE_COLOR = 0xFF999999;

    private static final Component RESTART_REQUIRED = Component.translatable("gui.modutilities.feature_section.restart_required");
    private static final Component RESTART_REQUIRED_TOOLTIP = Component.translatable("gui.modutilities.feature_section.restart_required.tooltip");
    private static final Component EXPAND_TOOLTIP = Component.translatable("gui.modutilities.feature_section.expand");
    private static final Component COLLAPSE_TOOLTIP = Component.translatable("gui.modutilities.feature_section.collapse");
    /** Default lock reason for master-only features on slave servers. */
    public static final Component MASTER_ONLY_REASON = Component.translatable("gui.modutilities.feature_section.master_only");

    private final CheckBox checkBox;
    private final Label restartIndicator;
    private final int titleColor;
    private boolean locked = false;
    private Component lockReason = null;
    private Consumer<Boolean> onFeatureToggled = null;

    public FeatureToggleSection(String title) {
        this(title, false);
    }

    public FeatureToggleSection(String title, boolean initiallyExpanded) {
        super(title, initiallyExpanded);
        titleColor = getHeaderTextColor();

        checkBox = new CheckBox("", checked -> {
            updateInteractivity();
            if (onFeatureToggled != null)
                onFeatureToggled.accept(checked);
        });
        checkBox.setEnableBackground(false);
        checkBox.setEnableOutline(false);
        getHeaderButton().addChild(checkBox);

        restartIndicator = createRestartIndicator();
        getHeaderButton().addChild(restartIndicator);

        getHeaderButton().setHoverTooltipSupplier(() -> {
            if (locked && lockReason != null)
                return lockReason.getString();
            return (isExpanded() ? COLLAPSE_TOOLTIP : EXPAND_TOOLTIP).getString();
        });
        updateInteractivity();
    }

    /**
     * Creates a hidden "Restart required" marker label (warning color, explanatory
     * hover tooltip). Show it with {@code setEnabled(true)} when the edited value
     * differs from the value active since server start.
     */
    public static Label createRestartIndicator() {
        Label label = new Label(RESTART_REQUIRED.getString());
        label.setAlignment(Alignment.RIGHT);
        label.setTextColor(RESTART_INDICATOR_COLOR);
        label.setHoverTooltipSupplier(RESTART_REQUIRED_TOOLTIP::getString);
        label.setHoverTooltipMousePositionAlignment(Alignment.TOP_RIGHT);
        label.setEnabled(false);
        return label;
    }

    /** Sets the feature check box. Fires the {@link #setOnFeatureToggled toggle listener} on change (like {@link CheckBox#setChecked}). */
    public void setFeatureEnabled(boolean enabled) {
        checkBox.setChecked(enabled);
    }

    public boolean isFeatureEnabled() {
        return checkBox.isChecked();
    }

    /** @param onFeatureToggled receives the new check box state, or {@code null} to clear */
    public void setOnFeatureToggled(Consumer<Boolean> onFeatureToggled) {
        this.onFeatureToggled = onFeatureToggled;
    }

    /** Shows/hides the "Restart required" marker in the header. */
    public void setRestartRequired(boolean restartRequired) {
        if (restartIndicator.isEnabled() == restartRequired)
            return;
        restartIndicator.setEnabled(restartRequired);
        layoutChangedInternal();
    }

    public boolean isRestartRequired() {
        return restartIndicator.isEnabled();
    }

    /**
     * Replaces the restart marker's text and hover tooltip. The default tooltip says the
     * value differs from the one active since server start; for a static marker (setting
     * only read at startup) call {@code setRestartRequired(true)} and pass your own wording.
     *
     * @param text    marker text, or {@code null} to keep the current one
     * @param tooltip hover tooltip, or {@code null} to keep the current one
     */
    public void setRestartMarkerText(Component text, Component tooltip) {
        if (text != null)
            restartIndicator.setText(text.getString());
        if (tooltip != null)
            restartIndicator.setHoverTooltipSupplier(tooltip::getString);
        layoutChangedInternal();
    }

    /**
     * Locks the section: check box and children become read-only (children stay
     * visible when expanded) and {@code reason} is shown as header tooltip.
     *
     * @param reason why the section is locked, e.g. {@link #MASTER_ONLY_REASON}; may be {@code null}
     */
    public void setLocked(boolean locked, Component reason) {
        this.locked = locked;
        this.lockReason = reason;
        checkBox.setCheckable(!locked);
        setHeaderTextColor(locked ? LOCKED_TITLE_COLOR : titleColor);
        updateInteractivity();
    }

    public boolean isLocked() {
        return locked;
    }

    private void updateInteractivity() {
        getContentArea().setInteractive(checkBox.isChecked() && !locked);
    }

    @Override
    protected void layoutChanged() {
        super.layoutChanged();
        int h = getHeaderHeight();
        int arrowX = getWidth() - h;
        int restartW = restartIndicator.isEnabled() ? getTextWidth(restartIndicator.getText()) + 6 : 0;
        checkBox.setBounds(0, 0, h, h);
        restartIndicator.setBounds(arrowX - restartW, 0, restartW, h);
        getTitleLabel().setBounds(h, 0, Math.max(0, arrowX - restartW - h), h);
    }
}
