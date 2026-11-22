package net.kroia.modutilities.gui;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * A short red notice drawn in larger text, centred over the open library screen
 * ({@code GuiScreen} / {@code GuiContainerScreen}) for {@link #DURATION_MS}, word-wrapped to
 * fit the screen width. Use it for messages the player would otherwise miss in the chat
 * while a screen covers it. A newer notice replaces the shown one. Without an open library
 * screen nothing is drawn and the notice simply expires.
 */
public final class ScreenNotice {
    /** How long a notice stays visible. */
    public static final long DURATION_MS = 5000;
    private static final float SCALE = 1.5f;
    private static final int MARGIN = 20;
    private static final int COLOR = 0xFFFF5555; // ChatFormatting.RED

    private static Component message;
    private static long shownUntil;

    private ScreenNotice() {}

    /**
     * Client side: shows {@code message} over the open screen.
     *
     * @param message the text; its own colour is ignored, the notice is always red
     */
    public static void show(Component message) {
        ScreenNotice.message = message;
        shownUntil = System.currentTimeMillis() + DURATION_MS;
    }

    /**
     * Server side: shows {@code message} over {@code player}'s open screen.
     * Translatable components are resolved in the client's language.
     */
    public static void send(ServerPlayer player, Component message) {
        NetworkManager.sendToPlayer(player, new ScreenNoticePacket(message));
    }

    /**
     * Draws the current notice; called by the library screens at the end of their render.
     *
     * @param font the Minecraft {@code Font}
     */
    public static void render(IGraphics g, Object font, int screenWidth, int screenHeight) {
        Component msg = message;
        if (msg == null)
            return;
        if (System.currentTimeMillis() > shownUntil) {
            message = null;
            return;
        }
        String[] lines = GuiElement.wrapText(msg.getString(), (int) ((screenWidth - 2 * MARGIN) / SCALE), g::getTextWidth).split("\n", -1);
        int lineHeight = g.getFontLineHeight() + 1;
        int width = 0;
        for (String line : lines)
            width = Math.max(width, g.getTextWidth(line));
        int height = lines.length * lineHeight;
        g.pushPose();
        g.translate(screenWidth / 2f, screenHeight / 2f, 500); // z above items and tooltips (ignored on 1.21.6+)
        g.scale(SCALE, SCALE, 1);
        g.fill(-width / 2 - 4, -height / 2 - 4, width / 2 + 4, height / 2 + 3, 0xC0000000);
        for (int i = 0; i < lines.length; i++)
            g.drawString(font, lines[i], -g.getTextWidth(lines[i]) / 2, -height / 2 + i * lineHeight, COLOR);
        g.popPose();
    }
}
