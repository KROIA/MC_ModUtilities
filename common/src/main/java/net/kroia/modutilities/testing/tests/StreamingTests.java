package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.networking.NetworkPacketManager;
import net.kroia.modutilities.networking.client_server.streaming.GenericStream;
import net.kroia.modutilities.networking.client_server.streaming.StreamManager;
import net.kroia.modutilities.networking.client_server.streaming.streamholder.ClientReceiverStreamHolder;
import net.kroia.modutilities.networking.client_server.streaming.streamholder.ServerReceiverStreamHolder;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamingTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.STREAMING;
    }

    @Override
    public void registerTests() {
        // GenericStream field tests
        addTest("GenericStream_setStreamID_getStreamID", this::testGenericStreamSetGetStreamID);
        addTest("GenericStream_setRequestorPlayerUUID_getRequestorPlayerUUID", this::testGenericStreamSetGetRequestorPlayerUUID);
        addTest("GenericStream_sendPacket_throwsWithoutManager", this::testGenericStreamSendPacketThrowsWithoutManager);
        addTest("GenericStream_stopStream_throwsWithoutManager", this::testGenericStreamStopStreamThrowsWithoutManager);
        addTest("GenericStream_setManager_getManager", this::testGenericStreamSetGetManager);

        // ClientReceiverStreamHolder tests
        addTest("ClientReceiverStreamHolder_onStreamStopped_callsHandler", this::testClientReceiverOnStreamStoppedCallsHandler);
        addTest("ClientReceiverStreamHolder_onStreamStopped_idempotent", this::testClientReceiverOnStreamStoppedIdempotent);
        addTest("ClientReceiverStreamHolder_onStreamStopped_nullHandlerSafe", this::testClientReceiverOnStreamStoppedNullHandler);

        // ServerReceiverStreamHolder tests
        addTest("ServerReceiverStreamHolder_onStreamStopped_callsHandler", this::testServerReceiverOnStreamStoppedCallsHandler);
        addTest("ServerReceiverStreamHolder_onStreamStopped_idempotent", this::testServerReceiverOnStreamStoppedIdempotent);
        addTest("ServerReceiverStreamHolder_onStreamStopped_nullHandlerSafe", this::testServerReceiverOnStreamStoppedNullHandler);
    }

    // ========================================================================
    // GenericStream field tests
    // ========================================================================

    private TestResult testGenericStreamSetGetStreamID() {
        GenericStream<String, String> stream = createDummyStream("field_test");
        UUID id = UUID.randomUUID();
        stream.setStreamID(id);
        return assertEquals("setStreamID/getStreamID should round-trip", id, stream.getStreamID());
    }

    private TestResult testGenericStreamSetGetRequestorPlayerUUID() {
        GenericStream<String, String> stream = createDummyStream("field_test");
        UUID playerUUID = UUID.randomUUID();
        stream.setRequestorPlayerUUID(playerUUID);
        return assertEquals("setRequestorPlayerUUID/getRequestorPlayerUUID should round-trip",
                playerUUID, stream.getRequestorPlayerUUID());
    }

    private TestResult testGenericStreamSendPacketThrowsWithoutManager() {
        TestableStream stream = createTestableStream("no_manager_send");
        return assertThrows("sendPacket() should throw IllegalStateException when manager is null",
                IllegalStateException.class, stream::callSendPacket);
    }

    private TestResult testGenericStreamStopStreamThrowsWithoutManager() {
        TestableStream stream = createTestableStream("no_manager_stop");
        return assertThrows("stopStream() should throw IllegalStateException when manager is null",
                IllegalStateException.class, stream::callStopStream);
    }

    private TestResult testGenericStreamSetGetManager() {
        GenericStream<String, String> stream = createDummyStream("manager_test");
        StreamManager manager = new StreamManager(createStubNetworkManager());
        stream.setManager(manager);
        if (stream.getManager() != manager) {
            return fail("setManager/getManager should return the same instance");
        }
        return pass("setManager/getManager round-trips correctly");
    }

    // ========================================================================
    // ClientReceiverStreamHolder tests (N4/N5 regression coverage)
    // ========================================================================

    /**
     * Verifies that onStreamStopped invokes the streamStoppedHandler callback exactly once.
     */
    private TestResult testClientReceiverOnStreamStoppedCallsHandler() {
        AtomicInteger callCount = new AtomicInteger(0);
        GenericStream<String, String> stream = createDummyStream("client_stop");
        UUID streamID = UUID.randomUUID();
        // sendEcho=false avoids the networkManager.sendToServer() call
        ClientReceiverStreamHolder<String, String> holder = new ClientReceiverStreamHolder<>(
                createStubNetworkManager(), stream, null, callCount::incrementAndGet, streamID);

        holder.onStreamStopped(false);

        return assertEquals("streamStoppedHandler should be called once", 1, callCount.get());
    }

    /**
     * Verifies that calling onStreamStopped twice only invokes the handler once
     * (isStopped guard).
     */
    private TestResult testClientReceiverOnStreamStoppedIdempotent() {
        AtomicInteger callCount = new AtomicInteger(0);
        GenericStream<String, String> stream = createDummyStream("client_idempotent");
        UUID streamID = UUID.randomUUID();
        ClientReceiverStreamHolder<String, String> holder = new ClientReceiverStreamHolder<>(
                createStubNetworkManager(), stream, null, callCount::incrementAndGet, streamID);

        holder.onStreamStopped(false);
        holder.onStreamStopped(false); // second call should be a no-op

        return assertEquals("streamStoppedHandler should be called exactly once despite two stop calls",
                1, callCount.get());
    }

    /**
     * Verifies that onStreamStopped does not throw when streamStoppedHandler is null.
     * (N4 regression: null handler must not cause NPE.)
     */
    private TestResult testClientReceiverOnStreamStoppedNullHandler() {
        GenericStream<String, String> stream = createDummyStream("client_null_handler");
        UUID streamID = UUID.randomUUID();
        ClientReceiverStreamHolder<String, String> holder = new ClientReceiverStreamHolder<>(
                createStubNetworkManager(), stream, null, null, streamID);

        try {
            holder.onStreamStopped(false);
        } catch (Exception e) {
            return fail("onStreamStopped with null handler should not throw, but threw: "
                    + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return pass("onStreamStopped with null streamStoppedHandler completes without error");
    }

    // ========================================================================
    // ServerReceiverStreamHolder tests (N5 regression coverage)
    // ========================================================================

    /**
     * Verifies that onStreamStopped invokes the streamStoppedHandler callback.
     * The networkManager.sendToClient() path is safe because
     * ServerPlayerUtilities.getOnlinePlayer() returns null in a test environment.
     */
    private TestResult testServerReceiverOnStreamStoppedCallsHandler() {
        AtomicInteger callCount = new AtomicInteger(0);
        GenericStream<String, String> stream = createDummyStream("server_stop");
        UUID streamID = UUID.randomUUID();
        UUID playerUUID = UUID.randomUUID();
        ServerReceiverStreamHolder<String, String> holder = new ServerReceiverStreamHolder<>(
                createStubNetworkManager(), stream, null, callCount::incrementAndGet,
                streamID, playerUUID);

        holder.onStreamStopped();

        return assertEquals("streamStoppedHandler should be called once", 1, callCount.get());
    }

    /**
     * Verifies that calling onStreamStopped twice only invokes the handler once
     * (isStopped guard).
     */
    private TestResult testServerReceiverOnStreamStoppedIdempotent() {
        AtomicInteger callCount = new AtomicInteger(0);
        GenericStream<String, String> stream = createDummyStream("server_idempotent");
        UUID streamID = UUID.randomUUID();
        UUID playerUUID = UUID.randomUUID();
        ServerReceiverStreamHolder<String, String> holder = new ServerReceiverStreamHolder<>(
                createStubNetworkManager(), stream, null, callCount::incrementAndGet,
                streamID, playerUUID);

        holder.onStreamStopped();
        holder.onStreamStopped(); // second call should be a no-op

        return assertEquals("streamStoppedHandler should be called exactly once despite two stop calls",
                1, callCount.get());
    }

    /**
     * Verifies that onStreamStopped does not throw when streamStoppedHandler is null.
     * (N5 regression: null handler must not cause NPE.)
     */
    private TestResult testServerReceiverOnStreamStoppedNullHandler() {
        GenericStream<String, String> stream = createDummyStream("server_null_handler");
        UUID streamID = UUID.randomUUID();
        UUID playerUUID = UUID.randomUUID();
        ServerReceiverStreamHolder<String, String> holder = new ServerReceiverStreamHolder<>(
                createStubNetworkManager(), stream, null, null,
                streamID, playerUUID);

        try {
            holder.onStreamStopped();
        } catch (NullPointerException e) {
            return fail("onStreamStopped with null handler should not throw NPE");
        } catch (Exception e) {
            // ServerPlayerUtilities.getOnlinePlayer() may throw in test env -- that is fine,
            // the important thing is the handler path did not NPE.
            // But actually it returns null gracefully, so this shouldn't happen either.
            return fail("onStreamStopped with null handler threw unexpected: "
                    + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return pass("onStreamStopped with null streamStoppedHandler completes without error");
    }

    // ========================================================================
    // Dummy / stub helpers
    // ========================================================================

    /**
     * A GenericStream subclass that exposes the protected sendPacket() and
     * stopStream() methods for testing.
     */
    private static class TestableStream extends GenericStream<String, String> {
        private final String typeId;

        TestableStream(String typeId) {
            this.typeId = typeId;
        }

        /** Public bridge to the protected sendPacket(). */
        public void callSendPacket() {
            sendPacket();
        }

        /** Public bridge to the protected stopStream(). */
        public void callStopStream() {
            stopStream();
        }

        @Override
        public GenericStream<String, String> copy() {
            return new TestableStream(typeId);
        }

        @Override
        public String getStreamTypeID() {
            return typeId;
        }

        @Override
        public void encodeContextData(RegistryFriendlyByteBuf buffer, String context) { }

        @Override
        public String decodeContextData(RegistryFriendlyByteBuf buffer) {
            return null;
        }

        @Override
        public void encodeData(RegistryFriendlyByteBuf buffer, String data) { }

        @Override
        public String decodeData(RegistryFriendlyByteBuf buffer) {
            return null;
        }
    }

    private TestableStream createTestableStream(String typeId) {
        return new TestableStream(typeId);
    }

    private GenericStream<String, String> createDummyStream(String typeId) {
        return createTestableStream(typeId);
    }

    /**
     * Creates a minimal NetworkPacketManager stub that does not require
     * a live Architectury networking layer. The send methods are overridden
     * to be no-ops so stream holders can be tested in isolation.
     */
    private NetworkPacketManager createStubNetworkManager() {
        return new NetworkPacketManager("test_mod", "test_channel") {
            @Override
            public void setupClientReceiverPackets() { }

            @Override
            public void setupServerReceiverPackets() { }

            @Override
            public void setupServerServerPackets() { }

            @Override
            public void sendToServer(net.kroia.modutilities.networking.client_server.NetworkPacket packet) {
                // No-op for testing
            }

            @Override
            public void sendToClient(net.minecraft.server.level.ServerPlayer receiver,
                                     net.kroia.modutilities.networking.client_server.NetworkPacket packet) {
                // No-op for testing
            }
        };
    }
}
