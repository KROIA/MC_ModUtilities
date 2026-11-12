package net.kroia.modutilities.testing;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.client_server.arrs.AsynchronousRequestResponseSystem;
import net.kroia.modutilities.networking.client_server.arrs.GenericRequest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * <b>Test only</b> (dev builds; release builds do not compile the testing package). Simulates a master that hangs:
 * the slave keeps its master link up, but its ARRS requests to the master are dropped while reported as sent
 * ({@code RequestManager.sendToMaster}), so they fail with {@code RequestOutcomeUnknownException} after their
 * {@link GenericRequest#getTimeoutMs()} instead of the fast "not connected" path. Requests dropped while it was on
 * just time out; turning it off lets the next request through. Sibling of
 * {@code MultiServerManager.setSimulatedMasterDown}. Always switch it off when the test ends.
 */
public final class SimulatedMasterHang {
    private SimulatedMasterHang() {}

    /** The JVM property {@code RequestManager.sendToMaster} reads (spelled out there: release code cannot see this class). */
    public static final String PROPERTY = "modutilities.dev.simulatedMasterHang";

    /** Registered on every dev server (master and slaves): the master answers with the input; 2 s timeout. */
    public static final GenericRequest<String, String> ECHO = new GenericRequest<>() {
        @Override public String getRequestTypeID() { return "modutilities_test_hang_echo"; }
        @Override public long getTimeoutMs() { return 2_000L; }
        @Override public CompletableFuture<String> handleOnMasterServer(String input, String slaveID, @Nullable UUID playerSender) {
            return CompletableFuture.completedFuture(input);
        }
        @Override public void encodeInput(RegistryFriendlyByteBuf buf, String input) { buf.writeUtf(input); }
        @Override public void encodeOutput(RegistryFriendlyByteBuf buf, String output) { buf.writeUtf(output); }
        @Override public String decodeInput(RegistryFriendlyByteBuf buf) { return buf.readUtf(); }
        @Override public String decodeOutput(RegistryFriendlyByteBuf buf) { return buf.readUtf(); }
    };

    /** Called from {@link DevBootstrap#init()}. */
    static void init() {
        AsynchronousRequestResponseSystem.register(ECHO);
    }

    /** @param hang {@code true}: requests from this slave to the master go unanswered; {@code false}: normal */
    public static void set(boolean hang) {
        ModUtilitiesMod.LOGGER.info("[SimulatedMasterHang] " + (hang ? "on" : "off"));
        if (hang) System.setProperty(PROPERTY, "true");
        else System.clearProperty(PROPERTY);
    }

    /** @return whether {@link #set} currently simulates a hanging master */
    public static boolean isOn() {
        return Boolean.getBoolean(PROPERTY);
    }
}
