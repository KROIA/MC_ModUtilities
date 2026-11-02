package net.kroia.modutilities.gui.client;

import net.kroia.modutilities.ModUtilitiesMod;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Fires the container-foreground render hook that vanilla
 * {@link AbstractContainerScreen} rendering normally triggers.
 * {@link GuiContainerScreen} skips the vanilla container render path (its
 * {@link ContainerView} draws slots and the carried item itself), but JEI 23+
 * (mc 1.21.7+) draws its ingredient list only from that hook.
 * <p>
 * Resolved reflectively so common code needs neither NeoForge nor JEI-Fabric
 * on the classpath:
 * <ul>
 *     <li>NeoForge: posts the container foreground event on {@code NeoForge.EVENT_BUS}
 *         (every listener sees it, exactly as with a vanilla container screen).</li>
 *     <li>Fabric/Quilt: invokes JEI's {@code JeiScreenEvents.DRAW_FOREGROUND}, which
 *         JEI otherwise fires from its mixin into the vanilla container render.</li>
 * </ul>
 * Any lookup or invocation failure disables the hook (logged once).
 *
 * @apiNote This class is client-only.
 */
final class ContainerForegroundHook {

    private static final String NEOFORGE_EVENT = "net.neoforged.neoforge.client.event.ContainerScreenEvent$Render$Foreground"; // mc<=1.21.11
    //private static final String NEOFORGE_EVENT = "net.neoforged.neoforge.client.event.ScreenEvent$Render$Foreground"; // mc>=26.1

    private static boolean resolved;
    private static boolean disabled;
    private static Object neoBus;
    private static Method neoPost;
    private static Constructor<?> neoEventCtor;
    private static Object jeiEvent;
    private static Method jeiInvoker;
    private static Method jeiDraw;

    private ContainerForegroundHook() {}

    static void fire(AbstractContainerScreen<?> screen, Object graphics, int mouseX, int mouseY, float partialTick) {
        if (disabled)
            return;
        Object[] neoArgs = {screen, graphics, mouseX, mouseY}; // mc<=1.21.11
        //Object[] neoArgs = {screen, graphics, mouseX, mouseY, partialTick}; // mc>=26.1
        try {
            if (!resolved)
                resolve(neoArgs.length);
            if (neoEventCtor != null)
                neoPost.invoke(neoBus, neoEventCtor.newInstance(neoArgs));
            else if (jeiDraw != null)
                // invoker() is re-read every frame: Fabric swaps it when listeners register
                jeiDraw.invoke(jeiInvoker.invoke(jeiEvent), screen, graphics, mouseX, mouseY);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            disabled = true;
            ModUtilitiesMod.LOGGER.warn("Container foreground hook failed; JEI's ingredient list may not draw on ModUtilities container screens", e);
        }
    }

    private static void resolve(int neoArgCount) throws ReflectiveOperationException {
        resolved = true;
        Class<?> neoEvent = findClass(NEOFORGE_EVENT);
        if (neoEvent != null) {
            for (Constructor<?> c : neoEvent.getConstructors())
                if (c.getParameterCount() == neoArgCount)
                    neoEventCtor = c;
            if (neoEventCtor == null)
                throw new NoSuchMethodException(NEOFORGE_EVENT + " constructor with " + neoArgCount + " parameters");
            neoBus = Class.forName("net.neoforged.neoforge.common.NeoForge").getField("EVENT_BUS").get(null);
            neoPost = Class.forName("net.neoforged.bus.api.IEventBus").getMethod("post", Class.forName("net.neoforged.bus.api.Event"));
            return;
        }
        Class<?> jeiEvents = findClass("mezz.jei.fabric.events.JeiScreenEvents");
        if (jeiEvents == null)
            return; // no JEI on Fabric/Quilt: nothing to fire
        jeiEvent = jeiEvents.getField("DRAW_FOREGROUND").get(null);
        jeiInvoker = Class.forName("net.fabricmc.fabric.api.event.Event").getMethod("invoker");
        for (Method m : Class.forName("mezz.jei.fabric.events.JeiScreenEvents$DrawForeground").getMethods())
            if (m.getName().equals("drawForeground"))
                jeiDraw = m;
        if (jeiDraw == null)
            throw new NoSuchMethodException("JeiScreenEvents$DrawForeground.drawForeground");
    }

    private static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
