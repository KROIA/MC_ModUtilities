package net.kroia.modutilities.testing.tests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.modutilities.networking.NetworkPacketManager;
import net.kroia.modutilities.networking.client_server.arrs.GenericRequest;
import net.kroia.modutilities.networking.client_server.arrs.GenericRequestPacket;
import net.kroia.modutilities.networking.client_server.arrs.RequestManager;
import net.kroia.modutilities.networking.client_server.arrs.RequestNotSentException;
import net.kroia.modutilities.networking.client_server.arrs.RequestOutcomeUnknownException;
import net.kroia.modutilities.networking.client_server.arrs.RequestRegistry;
import net.kroia.modutilities.networking.client_server.streaming.GenericStream;
import net.kroia.modutilities.networking.client_server.streaming.StreamRegistry;
import net.kroia.modutilities.networking.multi_server.MultiServerConfig;
import net.kroia.modutilities.networking.multi_server.ServerIdentity;
import net.kroia.modutilities.networking.multi_server.ServerRole;
import net.kroia.modutilities.networking.multi_server.codec.PayloadDecoder;
import net.kroia.modutilities.networking.multi_server.codec.PayloadEncoder;
import net.kroia.modutilities.networking.multi_server.master.MasterPacketHandler;
import net.kroia.modutilities.networking.multi_server.master.MasterTCPServer;
import net.kroia.modutilities.networking.multi_server.payload.HandshakePayload;
import net.kroia.modutilities.networking.multi_server.payload.HandshakeResultPayload;
import net.kroia.modutilities.networking.multi_server.payload.PacketIds;
import net.kroia.modutilities.networking.multi_server.slave.SlaveServerClient;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class NetworkingTests extends TestSuite {

    private RequestRegistry requestRegistry;
    private StreamRegistry streamRegistry;

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.NETWORKING;
    }

    @Override
    public void setup() {
        requestRegistry = new RequestRegistry();
        streamRegistry = new StreamRegistry();
    }

    @Override
    public void teardown() {
        requestRegistry.clear();
        streamRegistry.clear();
    }

    @Override
    public void registerTests() {
        // RequestRegistry tests
        addTest("request_registry_register", this::testRequestRegistryRegister);
        addTest("request_registry_retrieve", this::testRequestRegistryRetrieve);
        addTest("request_registry_duplicate", this::testRequestRegistryDuplicate);
        addTest("request_registry_unregister", this::testRequestRegistryUnregister);
        addTest("request_registry_unknown_id", this::testRequestRegistryUnknownId);
        addTest("request_registry_clear", this::testRequestRegistryClear);

        // StreamRegistry tests
        addTest("stream_registry_register", this::testStreamRegistryRegister);
        addTest("stream_registry_retrieve", this::testStreamRegistryRetrieve);
        addTest("stream_registry_duplicate", this::testStreamRegistryDuplicate);
        addTest("stream_registry_unregister", this::testStreamRegistryUnregister);
        addTest("stream_registry_unknown_id", this::testStreamRegistryUnknownId);
        addTest("stream_registry_clear", this::testStreamRegistryClear);

        // MultiServerConfig tests
        addTest("multi_server_config_defaults", this::testMultiServerConfigDefaults);

        // Thread safety tests
        addTest("request_registry_concurrent", this::testRequestRegistryConcurrent);

        // Regression tests
        addTest("MultiServerConfig_concurrentGet_sameInstance", this::testMultiServerConfigConcurrentGet);
        addTest("GenericStream_copyFrom_copiesAllFields", this::testGenericStreamCopyFromCopiesAllFields);

        // Codec roundtrip tests
        addTest("codec_enum_roundtrip", this::testCodecEnumRoundtrip);
        addTest("codec_enum_invalid_ordinal", this::testCodecEnumInvalidOrdinal);
        addTest("codec_objectArray_roundtrip", this::testCodecObjectArrayRoundtrip);
        addTest("codec_objectArray_negative_length", this::testCodecObjectArrayNegativeLength);
        addTest("codec_objectArray_invalid_count", this::testCodecObjectArrayInvalidCount);
        addTest("codec_objectArray_index_out_of_bounds", this::testCodecObjectArrayIndexOutOfBounds);
        addTest("codec_floatArray_roundtrip", this::testCodecFloatArrayRoundtrip);
        addTest("codec_list_roundtrip", this::testCodecListRoundtrip);
        addTest("codec_map_roundtrip", this::testCodecMapRoundtrip);
        addTest("codec_set_roundtrip", this::testCodecSetRoundtrip);
        addTest("codec_nullable_present", this::testCodecNullablePresent);
        addTest("codec_nullable_absent", this::testCodecNullableAbsent);
        addTest("codec_jsonElement_roundtrip", this::testCodecJsonElementRoundtrip);
        addTest("codec_friendlyByteBuf_roundtrip", this::testCodecFriendlyByteBufRoundtrip);

        // Multi-server handshake tests
        addTest("handshake_codec_roundtrip", this::testHandshakeCodecRoundtrip);
        addTest("handshake_mc_version_mismatch_rejected", this::testHandshakeMcVersionMismatchRejected);
        addTest("handshake_missing_uid_decodes_null", this::testHandshakeMissingUidDecodesNull);

        // M1 server identity
        addTest("identity_file_roundtrip", this::testIdentityFileRoundtrip);
        addTest("identity_corrupt_file_recovers", this::testIdentityCorruptFileRecovers);
        addTest("identity_slave_id_change_regenerates_uid", this::testIdentitySlaveIdChangeRegeneratesUid);
        addTest("identity_duplicate_uid_check", this::testIdentityDuplicateUidCheck);

        // M2 request failures
        addTest("request_timeout_outcome_unknown", this::testRequestTimeoutOutcomeUnknown);
        addTest("request_timeout_override", this::testRequestTimeoutOverride);
        addTest("request_not_sent_plain_failure", this::testRequestNotSentPlainFailure);
        addTest("request_failures_off_by_default", this::testRequestFailuresOffByDefault);
        addTest("request_disconnect_sweep", this::testRequestDisconnectSweep);
    }

    // ========================================================================
    // RequestRegistry tests
    // ========================================================================

    private TestResult testRequestRegistryRegister() {
        requestRegistry.clear();
        GenericRequest<String, String> request = createDummyRequest("test_register");
        GenericRequest<String, String> result = requestRegistry.register(request);
        return assertNotNull("register() should return the request on success", result);
    }

    private TestResult testRequestRegistryRetrieve() {
        requestRegistry.clear();
        GenericRequest<String, String> request = createDummyRequest("test_retrieve");
        requestRegistry.register(request);
        GenericRequest<?, ?> retrieved = requestRegistry.getRegisteredRequest("test_retrieve");
        return assertEquals("Retrieved request should match registered one", request, retrieved);
    }

    private TestResult testRequestRegistryDuplicate() {
        requestRegistry.clear();
        GenericRequest<String, String> first = createDummyRequest("dup_id");
        GenericRequest<String, String> second = createDummyRequest("dup_id");
        requestRegistry.register(first);
        GenericRequest<String, String> result = requestRegistry.register(second);
        return assertNull("Duplicate registration should return null", result);
    }

    private TestResult testRequestRegistryUnregister() {
        requestRegistry.clear();
        GenericRequest<String, String> request = createDummyRequest("test_unreg");
        requestRegistry.register(request);
        requestRegistry.unregister(request);
        GenericRequest<?, ?> retrieved = requestRegistry.getRegisteredRequest("test_unreg");
        return assertNull("Unregistered request should not be found", retrieved);
    }

    private TestResult testRequestRegistryUnknownId() {
        requestRegistry.clear();
        GenericRequest<?, ?> result = requestRegistry.getRegisteredRequest("nonexistent_id");
        return assertNull("Unknown ID should return null", result);
    }

    private TestResult testRequestRegistryClear() {
        requestRegistry.clear();
        requestRegistry.register(createDummyRequest("clear_a"));
        requestRegistry.register(createDummyRequest("clear_b"));
        requestRegistry.register(createDummyRequest("clear_c"));
        requestRegistry.clear();
        return assertTrue("Registry should be empty after clear()",
                requestRegistry.getRegistry().isEmpty());
    }

    // ========================================================================
    // StreamRegistry tests
    // ========================================================================

    private TestResult testStreamRegistryRegister() {
        streamRegistry.clear();
        GenericStream<String, String> stream = createDummyStream("test_register");
        GenericStream<String, String> result = streamRegistry.register(stream);
        return assertNotNull("register() should return the stream on success", result);
    }

    private TestResult testStreamRegistryRetrieve() {
        streamRegistry.clear();
        GenericStream<String, String> stream = createDummyStream("test_retrieve");
        streamRegistry.register(stream);
        GenericStream<?, ?> retrieved = streamRegistry.getRegisteredStream("test_retrieve");
        return assertEquals("Retrieved stream should match registered one", stream, retrieved);
    }

    private TestResult testStreamRegistryDuplicate() {
        streamRegistry.clear();
        GenericStream<String, String> first = createDummyStream("dup_id");
        GenericStream<String, String> second = createDummyStream("dup_id");
        streamRegistry.register(first);
        GenericStream<String, String> result = streamRegistry.register(second);
        return assertNull("Duplicate registration should return null", result);
    }

    private TestResult testStreamRegistryUnregister() {
        streamRegistry.clear();
        GenericStream<String, String> stream = createDummyStream("test_unreg");
        streamRegistry.register(stream);
        streamRegistry.unregister(stream);
        GenericStream<?, ?> retrieved = streamRegistry.getRegisteredStream("test_unreg");
        return assertNull("Unregistered stream should not be found", retrieved);
    }

    private TestResult testStreamRegistryUnknownId() {
        streamRegistry.clear();
        GenericStream<?, ?> result = streamRegistry.getRegisteredStream("nonexistent_id");
        return assertNull("Unknown ID should return null", result);
    }

    private TestResult testStreamRegistryClear() {
        streamRegistry.clear();
        streamRegistry.register(createDummyStream("clear_a"));
        streamRegistry.register(createDummyStream("clear_b"));
        streamRegistry.register(createDummyStream("clear_c"));
        streamRegistry.clear();
        return assertTrue("Registry should be empty after clear()",
                streamRegistry.getRegistry().isEmpty());
    }

    // ========================================================================
    // MultiServerConfig tests
    // ========================================================================

    private TestResult testMultiServerConfigDefaults() {
        MultiServerConfig config = new MultiServerConfig();
        if (config.enable) {
            return fail("Default 'enable' should be false");
        }
        if (config.isMaster) {
            return fail("Default 'isMaster' should be false");
        }
        if (config.masterTcpPort != 25575) {
            return fail("Default 'masterTcpPort' should be 25575, got " + config.masterTcpPort);
        }
        if (!"change-me-please".equals(config.sharedSecret)) {
            return fail("Default 'sharedSecret' should be 'change-me-please'");
        }
        if (!"slave_a".equals(config.slaveID)) {
            return fail("Default 'slaveID' should be 'slave_a'");
        }
        if (!"127.0.0.1".equals(config.masterHost)) {
            return fail("Default 'masterHost' should be '127.0.0.1'");
        }
        return pass("All MultiServerConfig default values are correct");
    }

    // ========================================================================
    // Thread safety tests
    // ========================================================================

    private TestResult testRequestRegistryConcurrent() {
        RequestRegistry concurrentRegistry = new RequestRegistry();
        int threadCount = 8;
        int requestsPerThread = 50;
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadIdx = t;
            Thread thread = new Thread(() -> {
                try {
                    for (int i = 0; i < requestsPerThread; i++) {
                        String id = "thread_" + threadIdx + "_req_" + i;
                        concurrentRegistry.register(createDummyRequest(id));
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            });
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                concurrentRegistry.clear();
                return fail("Thread interrupted during concurrent test");
            }
        }

        int expectedTotal = threadCount * requestsPerThread;
        int actualSize = concurrentRegistry.getRegistry().size();
        concurrentRegistry.clear();

        if (!errors.isEmpty()) {
            return fail("Concurrent registration threw " + errors.size()
                    + " exception(s): " + errors.get(0).getClass().getSimpleName());
        }

        return assertEquals("All concurrent registrations should be present",
                expectedTotal, actualSize);
    }

    // ========================================================================
    // Regression tests
    // ========================================================================

    /**
     * N7 regression: Verifies MultiServerConfig.get() double-checked locking
     * returns the same singleton instance when called from 8+ threads concurrently.
     */
    private TestResult testMultiServerConfigConcurrentGet() {
        int threadCount = 8;
        List<MultiServerConfig> results = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            Thread thread = new Thread(() -> {
                try {
                    MultiServerConfig config = MultiServerConfig.get();
                    results.add(config);
                } catch (Throwable e) {
                    errors.add(e);
                }
            });
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                return fail("Thread interrupted during MultiServerConfig concurrent get test");
            }
        }

        if (!errors.isEmpty()) {
            return fail("Concurrent get() threw " + errors.size()
                    + " exception(s): " + errors.get(0).getClass().getSimpleName()
                    + " - " + errors.get(0).getMessage());
        }

        if (results.size() != threadCount) {
            return fail("Expected " + threadCount + " results but got " + results.size());
        }

        // Verify all returned instances are the same object (identity check)
        MultiServerConfig first = results.get(0);
        for (int i = 1; i < results.size(); i++) {
            if (first != results.get(i)) {
                return fail("Thread " + i + " returned a different MultiServerConfig instance (identity check failed)");
            }
        }

        return pass("All " + threadCount + " threads received the same MultiServerConfig singleton instance");
    }

    /**
     * N23 regression: Verifies GenericStream.copyFrom copies all internal fields.
     * Tests requestorPlayerUUID, StreamID, and manager fields.
     * Note: contextData cannot be set without a RegistryFriendlyByteBuf, so it is
     * tested only as a null-to-null copy. A full integration test would be needed
     * to verify contextData copying with an actual buffer.
     */
    private TestResult testGenericStreamCopyFromCopiesAllFields() {
        GenericStream<String, String> source = createDummyStream("copy_source");
        GenericStream<String, String> target = createDummyStream("copy_target");

        // Set fields on the source
        UUID testPlayerUUID = UUID.randomUUID();
        UUID testStreamID = UUID.randomUUID();
        source.setRequestorPlayerUUID(testPlayerUUID);
        source.setStreamID(testStreamID);

        // Perform copy
        target.copyFrom(source);

        // Verify requestorPlayerUUID was copied
        if (target.getRequestorPlayerUUID() == null) {
            return fail("copyFrom did not copy requestorPlayerUUID (was null)");
        }
        if (!testPlayerUUID.equals(target.getRequestorPlayerUUID())) {
            return fail("copyFrom copied wrong requestorPlayerUUID: expected "
                    + testPlayerUUID + ", got " + target.getRequestorPlayerUUID());
        }

        // Verify StreamID was copied
        if (target.getStreamID() == null) {
            return fail("copyFrom did not copy StreamID (was null)");
        }
        if (!testStreamID.equals(target.getStreamID())) {
            return fail("copyFrom copied wrong StreamID: expected "
                    + testStreamID + ", got " + target.getStreamID());
        }

        // Verify contextData was copied (both null in this case since we can't set it
        // without a RegistryFriendlyByteBuf, but ensures the field assignment path works)
        if (target.getContextData() != null) {
            return fail("copyFrom should have copied null contextData, got " + target.getContextData());
        }

        // Verify manager was copied (both null since no StreamManager is available in tests)
        if (target.getManager() != null) {
            return fail("copyFrom should have copied null manager, got " + target.getManager());
        }

        return pass("copyFrom correctly copies all fields (requestorPlayerUUID, StreamID, contextData, manager)");
    }

    // ========================================================================
    // Codec roundtrip tests
    // ========================================================================

    /** Test enum used exclusively by codec tests. */
    private enum TestEnum { ALPHA, BETA, GAMMA }

    /**
     * Creates a fresh {@link RegistryFriendlyByteBuf} backed by an unpooled heap buffer
     * with {@link RegistryAccess#EMPTY}. Suitable for codecs that do not perform
     * registry look-ups (all codecs in {@link ExtraCodecUtils}).
     */
    private RegistryFriendlyByteBuf createBuf() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    private TestResult testCodecEnumRoundtrip() {
        StreamCodec<RegistryFriendlyByteBuf, TestEnum> codec = ExtraCodecUtils.enumStreamCodec(TestEnum.class);
        RegistryFriendlyByteBuf buf = createBuf();
        try {
            for (TestEnum value : TestEnum.values()) {
                codec.encode(buf, value);
            }
            for (TestEnum expected : TestEnum.values()) {
                TestEnum decoded = codec.decode(buf);
                if (decoded != expected) {
                    return fail("Enum roundtrip failed: expected " + expected + ", got " + decoded);
                }
            }
            return pass("All enum values survive encode/decode roundtrip");
        } finally {
            buf.release();
        }
    }

    private TestResult testCodecEnumInvalidOrdinal() {
        StreamCodec<RegistryFriendlyByteBuf, TestEnum> codec = ExtraCodecUtils.enumStreamCodec(TestEnum.class);

        // Test negative ordinal
        RegistryFriendlyByteBuf buf1 = createBuf();
        try {
            buf1.writeVarInt(-1);
            try {
                codec.decode(buf1);
                return fail("Negative ordinal should throw DecoderException");
            } catch (DecoderException e) {
                // expected
            }
        } finally {
            buf1.release();
        }

        // Test ordinal >= constants.length
        RegistryFriendlyByteBuf buf2 = createBuf();
        try {
            buf2.writeVarInt(TestEnum.values().length); // one past the end
            try {
                codec.decode(buf2);
                return fail("Out-of-bounds ordinal should throw DecoderException");
            } catch (DecoderException e) {
                // expected
            }
        } finally {
            buf2.release();
        }

        return pass("Invalid enum ordinals correctly throw DecoderException");
    }

    // ========================================================================
    // Multi-server handshake tests
    // ========================================================================

    private TestResult testHandshakeCodecRoundtrip() {
        HandshakePayload sent = new HandshakePayload("server_a", "secret", "1.21.1", UUID.randomUUID());
        EmbeddedChannel encoder = new EmbeddedChannel(new PayloadEncoder());
        encoder.writeOutbound(sent);
        ByteBuf encoded = encoder.readOutbound();
        EmbeddedChannel decoder = new EmbeddedChannel(new PayloadDecoder());
        decoder.writeInbound(encoded);
        Object decoded = decoder.readInbound();
        if (!sent.equals(decoded))
            return fail("Handshake roundtrip failed: expected " + sent + ", got " + decoded);

        // Older slaves send only serverId and token: must decode with an empty mcVersion, not throw.
        ByteBuf legacy = Unpooled.buffer();
        legacy.writeByte(PacketIds.HANDSHAKE);
        for (String str : new String[]{"server_a", "secret"}) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            legacy.writeShort(bytes.length);
            legacy.writeBytes(bytes);
        }
        decoder.writeInbound(legacy);
        return assertEquals("Legacy handshake should decode with empty mcVersion",
                new HandshakePayload("server_a", "secret", "", null), decoder.readInbound());
    }

    private TestResult testHandshakeMcVersionMismatchRejected() {
        MasterTCPServer master = new MasterTCPServer(null, "secret", 0, () -> {}, e -> {}, id -> {}, id -> {});
        EmbeddedChannel channel = new EmbeddedChannel(new MasterPacketHandler(null, master));
        channel.writeInbound(new HandshakePayload("server_a", "secret", "0.0.0-other", null));
        Object response = channel.readOutbound();
        if (!(response instanceof HandshakeResultPayload hr)
                || hr.result() != SlaveServerClient.ConnectionEstablishState.MC_VERSION_MISMATCH)
            return fail("Expected MC_VERSION_MISMATCH, got " + response);
        if (channel.isOpen())
            return fail("Channel should be closed after a version mismatch");
        return pass("Mismatched Minecraft version is rejected and the channel closed");
    }

    private TestResult testHandshakeMissingUidDecodesNull() {
        EmbeddedChannel encoder = new EmbeddedChannel(new PayloadEncoder());
        EmbeddedChannel decoder = new EmbeddedChannel(new PayloadDecoder());
        UUID masterUid = UUID.randomUUID();
        // Old slave (no uid) and old master (no masterUid): the trailing field is omitted and decodes as null.
        List<Object> payloads = List.of(
                new HandshakePayload("server_a", "secret", "1.21.1", null),
                new HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState.SUCCESS),
                new HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState.SUCCESS, masterUid));
        for (Object sent : payloads) {
            encoder.writeOutbound(sent);
            decoder.writeInbound((ByteBuf) encoder.readOutbound());
            Object decoded = decoder.readInbound();
            if (!sent.equals(decoded))
                return fail("Roundtrip failed: expected " + sent + ", got " + decoded);
        }

        // Fields appended by a newer peer are skipped, not decoded as the next packet.
        ByteBuf future = Unpooled.buffer();
        future.writeByte(PacketIds.HANDSHAKE_RESULT);
        future.writeInt(SlaveServerClient.ConnectionEstablishState.SUCCESS.ordinal());
        UUIDUtil.STREAM_CODEC.encode(future, masterUid);
        future.writeLong(0L); // unknown future field
        decoder.writeInbound(future);
        if (!(decoder.readInbound() instanceof HandshakeResultPayload hr) || !masterUid.equals(hr.masterUid()))
            return fail("Payload with an unknown trailing field did not decode");
        return assertNull("Unknown trailing bytes must not decode as another packet", decoder.readInbound());
    }

    // ========================================================================
    // Server identity tests
    // ========================================================================

    /** Runs {@code test} with an identity file path in a fresh temp directory, deleted afterwards. */
    private TestResult withIdentityFile(java.util.function.Function<Path, TestResult> test) {
        Path dir;
        try { dir = Files.createTempDirectory("modutil_test_identity"); }
        catch (IOException e) { return fail("Failed to create temp directory: " + e.getMessage()); }
        try {
            return test.apply(dir.resolve("data").resolve("ModUtilities").resolve("server_identity.json"));
        } finally {
            try (var walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            } catch (IOException ignored) {}
        }
    }

    private TestResult testIdentityFileRoundtrip() {
        return withIdentityFile(file -> {
            ServerIdentity master = ServerIdentity.load(file, ServerRole.MASTER, null);
            if (!Files.exists(file))
                return fail("Identity file was not written");
            UUID slaveUid = UUID.randomUUID();
            master.onSlaveSeen("slave_a", slaveUid);
            master.onSlaveSeen("slave_old", null);

            ServerIdentity loaded = ServerIdentity.load(file, ServerRole.MASTER, null);
            if (!master.serverUid().equals(loaded.serverUid()))
                return fail("server_uid changed on reload");
            if (loaded.role() != ServerRole.MASTER || loaded.slaveId() != null || loaded.masterUid() != null)
                return fail("Unexpected master fields: " + loaded.role() + ", " + loaded.slaveId() + ", " + loaded.masterUid());
            if (!slaveUid.equals(loaded.slaveUid("slave_a")))
                return fail("known_slaves uid lost: " + loaded.slaveUid("slave_a"));
            if (loaded.slaveUid("slave_old") != null || loaded.slaveUid("never_seen") != null)
                return fail("Slave without uid / unknown slave should have a null uid");

            Path slaveFile = file.resolveSibling("slave_identity.json");
            ServerIdentity slave = ServerIdentity.load(slaveFile, ServerRole.SLAVE, "slave_a");
            slave.setMasterUid(master.serverUid());
            ServerIdentity slaveLoaded = ServerIdentity.load(slaveFile, ServerRole.SLAVE, "slave_a");
            if (!slave.serverUid().equals(slaveLoaded.serverUid()) || !"slave_a".equals(slaveLoaded.slaveId()))
                return fail("Slave identity changed on reload");
            return assertEquals("master_uid persists on the slave", master.serverUid(), slaveLoaded.masterUid());
        });
    }

    private TestResult testIdentityCorruptFileRecovers() {
        return withIdentityFile(file -> {
            for (String corrupt : new String[]{"{ not json", "{\"server_uid\":\"not-a-uuid\"}", ""}) {
                try {
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, corrupt);
                } catch (IOException e) {
                    return fail("Failed to write corrupt file: " + e.getMessage());
                }
                ServerIdentity recovered = ServerIdentity.load(file, ServerRole.STANDALONE, null);
                UUID reloaded = ServerIdentity.load(file, ServerRole.STANDALONE, null).serverUid();
                if (!recovered.serverUid().equals(reloaded))
                    return fail("Identity recovered from '" + corrupt + "' was not persisted");
            }
            return pass("Corrupt identity files are replaced by a new, persisted identity");
        });
    }

    private TestResult testIdentitySlaveIdChangeRegeneratesUid() {
        return withIdentityFile(file -> {
            UUID uid = ServerIdentity.load(file, ServerRole.SLAVE, "slave_a").serverUid();
            if (!uid.equals(ServerIdentity.load(file, ServerRole.SLAVE, "slave_a").serverUid()))
                return fail("Same slave_id must keep the uid");
            // A STANDALONE resolve (e.g. first access before createSlave) keeps the uid and the stored slave_id.
            ServerIdentity standalone = ServerIdentity.load(file, ServerRole.STANDALONE, null);
            if (!uid.equals(standalone.serverUid()) || standalone.slaveId() != null)
                return fail("STANDALONE must keep the uid and report no slave_id");
            if (!uid.equals(ServerIdentity.load(file, ServerRole.SLAVE, "slave_a").serverUid()))
                return fail("slave_a after a STANDALONE start must keep the uid");
            UUID renamed = ServerIdentity.load(file, ServerRole.SLAVE, "slave_b").serverUid();
            if (uid.equals(renamed))
                return fail("A changed slave_id must regenerate the uid");
            return assertEquals("The new uid is persisted", renamed, ServerIdentity.load(file, ServerRole.SLAVE, "slave_b").serverUid());
        });
    }

    private TestResult testIdentityDuplicateUidCheck() {
        return withIdentityFile(file -> {
            ServerIdentity master = ServerIdentity.load(file, ServerRole.MASTER, null);
            UUID uidA = UUID.randomUUID();
            master.onSlaveSeen("slave_a", uidA);
            if (!master.isDuplicateUid("slave_b", uidA))
                return fail("Another slave reporting slave_a's uid must be a duplicate");
            if (master.isDuplicateUid("slave_a", uidA))
                return fail("A reconnect of slave_a must not be a duplicate");
            if (!master.isDuplicateUid("slave_b", master.serverUid()))
                return fail("A slave reporting the master's uid must be a duplicate");
            if (master.isDuplicateUid("slave_b", UUID.randomUUID()))
                return fail("A new uid must not be a duplicate");
            if (master.isDuplicateUid("slave_b", null))
                return fail("A slave without uid (older ModUtilities) must not be a duplicate");
            // known_slaves is persisted, so the check also covers slaves that are offline.
            return assertTrue("Duplicate check survives a reload",
                    ServerIdentity.load(file, ServerRole.MASTER, null).isDuplicateUid("slave_b", uidA));
        });
    }

    private TestResult testCodecObjectArrayRoundtrip() {
        StreamCodec<RegistryFriendlyByteBuf, String> stringCodec = ByteBufCodecs.STRING_UTF8.cast();
        StreamCodec<RegistryFriendlyByteBuf, String[]> codec =
                ExtraCodecUtils.objectArrayStreamCodec(stringCodec, String[]::new);

        // Create a sparse array: indices 0, 2 are set; index 1 is null
        String[] original = new String[]{"hello", null, "world"};
        RegistryFriendlyByteBuf buf = createBuf();
        try {
            codec.encode(buf, original);
            String[] decoded = codec.decode(buf);

            if (decoded.length != original.length) {
                return fail("Array length mismatch: expected " + original.length + ", got " + decoded.length);
            }
            if (!"hello".equals(decoded[0])) {
                return fail("Index 0: expected 'hello', got '" + decoded[0] + "'");
            }
            if (decoded[1] != null) {
                return fail("Index 1: expected null, got '" + decoded[1] + "'");
            }
            if (!"world".equals(decoded[2])) {
                return fail("Index 2: expected 'world', got '" + decoded[2] + "'");
            }
            return pass("Sparse object array roundtrip preserves values and nulls");
        } finally {
            buf.release();
        }
    }

    private TestResult testCodecObjectArrayNegativeLength() {
        StreamCodec<RegistryFriendlyByteBuf, String> stringCodec = ByteBufCodecs.STRING_UTF8.cast();
        StreamCodec<RegistryFriendlyByteBuf, String[]> codec =
                ExtraCodecUtils.objectArrayStreamCodec(stringCodec, String[]::new);

        RegistryFriendlyByteBuf buf = createBuf();
        try {
            buf.writeVarInt(-1); // negative array length
            buf.writeVarInt(0);  // element count
            try {
                codec.decode(buf);
                return fail("Negative array length should throw DecoderException");
            } catch (DecoderException e) {
                return pass("Negative array length correctly throws DecoderException");
            }
        } finally {
            buf.release();
        }
    }

    private TestResult testCodecObjectArrayInvalidCount() {
        StreamCodec<RegistryFriendlyByteBuf, String> stringCodec = ByteBufCodecs.STRING_UTF8.cast();
        StreamCodec<RegistryFriendlyByteBuf, String[]> codec =
                ExtraCodecUtils.objectArrayStreamCodec(stringCodec, String[]::new);

        RegistryFriendlyByteBuf buf = createBuf();
        try {
            buf.writeVarInt(3);  // array length = 3
            buf.writeVarInt(5);  // element count = 5 (> length, invalid)
            try {
                codec.decode(buf);
                return fail("Element count > array length should throw DecoderException");
            } catch (DecoderException e) {
                return pass("Invalid element count correctly throws DecoderException");
            }
        } finally {
            buf.release();
        }
    }

    private TestResult testCodecObjectArrayIndexOutOfBounds() {
        StreamCodec<RegistryFriendlyByteBuf, String> stringCodec = ByteBufCodecs.STRING_UTF8.cast();
        StreamCodec<RegistryFriendlyByteBuf, String[]> codec =
                ExtraCodecUtils.objectArrayStreamCodec(stringCodec, String[]::new);

        RegistryFriendlyByteBuf buf = createBuf();
        try {
            buf.writeVarInt(2);  // array length = 2
            buf.writeVarInt(1);  // element count = 1
            buf.writeVarInt(5);  // index = 5 (out of bounds for length 2)
            ByteBufCodecs.STRING_UTF8.encode(buf, "bad");
            try {
                codec.decode(buf);
                return fail("Out-of-bounds index should throw DecoderException");
            } catch (DecoderException e) {
                return pass("Out-of-bounds array index correctly throws DecoderException");
            }
        } finally {
            buf.release();
        }
    }

    private TestResult testCodecFloatArrayRoundtrip() {
        float[] original = {1.0f, -3.14f, 0.0f, Float.MAX_VALUE, Float.MIN_VALUE};
        RegistryFriendlyByteBuf buf = createBuf();
        try {
            ExtraCodecUtils.FLOAT_ARRAY_CODEC.encode(buf, original);
            float[] decoded = ExtraCodecUtils.FLOAT_ARRAY_CODEC.decode(buf);

            if (decoded.length != original.length) {
                return fail("Float array length mismatch: expected " + original.length + ", got " + decoded.length);
            }
            for (int i = 0; i < original.length; i++) {
                if (Float.compare(original[i], decoded[i]) != 0) {
                    return fail("Float array index " + i + ": expected " + original[i] + ", got " + decoded[i]);
                }
            }
            return pass("Float array roundtrip preserves all values including extremes");
        } finally {
            buf.release();
        }
    }

    private TestResult testCodecListRoundtrip() {
        StreamCodec<RegistryFriendlyByteBuf, List<String>> codec =
                ExtraCodecUtils.listStreamCodec(ByteBufCodecs.STRING_UTF8.cast());

        List<String> original = List.of("aaa", "bbb", "ccc");
        RegistryFriendlyByteBuf buf = createBuf();
        try {
            codec.encode(buf, new ArrayList<>(original));
            List<String> decoded = codec.decode(buf);

            if (!original.equals(decoded)) {
                return fail("List roundtrip failed: expected " + original + ", got " + decoded);
            }
            return pass("List codec roundtrip preserves elements and order");
        } finally {
            buf.release();
        }
    }

    private TestResult testCodecMapRoundtrip() {
        StreamCodec<RegistryFriendlyByteBuf, HashMap<String, Integer>> codec =
                ExtraCodecUtils.mapStreamCodec(
                        ByteBufCodecs.STRING_UTF8.cast(),
                        ByteBufCodecs.VAR_INT.cast(),
                        HashMap::new);

        HashMap<String, Integer> original = new HashMap<>();
        original.put("gold", 100);
        original.put("silver", 50);
        original.put("bronze", 10);

        RegistryFriendlyByteBuf buf = createBuf();
        try {
            codec.encode(buf, original);
            HashMap<String, Integer> decoded = codec.decode(buf);

            if (!original.equals(decoded)) {
                return fail("Map roundtrip failed: expected " + original + ", got " + decoded);
            }
            return pass("Map codec roundtrip preserves all key-value pairs");
        } finally {
            buf.release();
        }
    }

    private TestResult testCodecSetRoundtrip() {
        StreamCodec<RegistryFriendlyByteBuf, HashSet<String>> codec =
                ExtraCodecUtils.setStreamCodec(ByteBufCodecs.STRING_UTF8.cast(), HashSet::new);

        HashSet<String> original = new HashSet<>(Set.of("x", "y", "z"));

        RegistryFriendlyByteBuf buf = createBuf();
        try {
            codec.encode(buf, original);
            HashSet<String> decoded = codec.decode(buf);

            if (!original.equals(decoded)) {
                return fail("Set roundtrip failed: expected " + original + ", got " + decoded);
            }
            return pass("Set codec roundtrip preserves all elements");
        } finally {
            buf.release();
        }
    }

    private TestResult testCodecNullablePresent() {
        StreamCodec<RegistryFriendlyByteBuf, String> innerCodec = ByteBufCodecs.STRING_UTF8.cast();
        StreamCodec<RegistryFriendlyByteBuf, String> codec = ExtraCodecUtils.nullable(innerCodec);

        RegistryFriendlyByteBuf buf = createBuf();
        try {
            codec.encode(buf, "present");
            String decoded = codec.decode(buf);
            if (!"present".equals(decoded)) {
                return fail("Nullable codec with present value: expected 'present', got '" + decoded + "'");
            }
            return pass("Nullable codec correctly encodes/decodes a non-null value");
        } finally {
            buf.release();
        }
    }

    private TestResult testCodecNullableAbsent() {
        StreamCodec<RegistryFriendlyByteBuf, String> innerCodec = ByteBufCodecs.STRING_UTF8.cast();
        StreamCodec<RegistryFriendlyByteBuf, String> codec = ExtraCodecUtils.nullable(innerCodec);

        RegistryFriendlyByteBuf buf = createBuf();
        try {
            codec.encode(buf, null);
            String decoded = codec.decode(buf);
            if (decoded != null) {
                return fail("Nullable codec with null value: expected null, got '" + decoded + "'");
            }
            return pass("Nullable codec correctly encodes/decodes a null value");
        } finally {
            buf.release();
        }
    }

    private TestResult testCodecJsonElementRoundtrip() {
        JsonObject original = new JsonObject();
        original.addProperty("name", "test");
        original.addProperty("value", 42);
        original.addProperty("flag", true);

        RegistryFriendlyByteBuf buf = createBuf();
        try {
            ExtraCodecUtils.JSON_ELEMENT_CODEC.encode(buf, original);
            JsonElement decoded = ExtraCodecUtils.JSON_ELEMENT_CODEC.decode(buf);

            if (!original.equals(decoded)) {
                return fail("JsonElement roundtrip failed: expected " + original + ", got " + decoded);
            }
            return pass("JsonElement codec roundtrip preserves structure and values");
        } finally {
            buf.release();
        }
    }

    private TestResult testCodecFriendlyByteBufRoundtrip() {
        byte[] testData = {0x01, 0x02, 0x03, (byte) 0xFF, 0x00};
        FriendlyByteBuf innerBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(testData));

        RegistryFriendlyByteBuf buf = createBuf();
        try {
            ExtraCodecUtils.FRIENDLY_BYTE_BUF_CODEC.encode(buf, innerBuf);
            FriendlyByteBuf decoded = ExtraCodecUtils.FRIENDLY_BYTE_BUF_CODEC.decode(buf);

            if (decoded.readableBytes() != testData.length) {
                int got = decoded.readableBytes();
                decoded.release();
                return fail("FriendlyByteBuf length mismatch: expected " + testData.length + ", got " + got);
            }
            for (int i = 0; i < testData.length; i++) {
                byte actual = decoded.readByte();
                if (actual != testData[i]) {
                    decoded.release();
                    return fail("FriendlyByteBuf byte " + i + ": expected " + testData[i] + ", got " + actual);
                }
            }
            decoded.release();
            return pass("FriendlyByteBuf codec roundtrip preserves all bytes");
        } finally {
            innerBuf.release();
            buf.release();
        }
    }

    // ========================================================================
    // M2 request failures (timeouts, refused sends, disconnect sweep)
    // ========================================================================

    /** Runs {@code body} with the failure switch on and a manager whose master/slave sends return {@code sendResult}. */
    private TestResult withFailures(boolean enabled, boolean sendResult, java.util.function.Function<RequestManager, TestResult> body) {
        boolean old = RequestManager.ENABLE_REQUEST_FAILURES;
        RequestManager.ENABLE_REQUEST_FAILURES = enabled;
        try {
            NetworkPacketManager npm = new NetworkPacketManager("modutilities_test") {
                @Override public void setupClientReceiverPackets() {}
                @Override public void setupServerReceiverPackets() {}
                @Override public void setupServerServerPackets() {}
            };
            return body.apply(new RequestManager(npm) {
                @Override protected boolean sendToMaster(GenericRequestPacket packet) { return sendResult; }
                @Override protected boolean sendToSlave(String slaveID, GenericRequestPacket packet) { return sendResult; }
            });
        } finally {
            RequestManager.ENABLE_REQUEST_FAILURES = old;
        }
    }

    /** @return the failure cause of a completed future, or null if it is pending or succeeded */
    private static Throwable causeOf(CompletableFuture<?> f) {
        if (!f.isCompletedExceptionally()) return null;
        try {
            f.join();
            return null;
        } catch (java.util.concurrent.CompletionException | java.util.concurrent.CancellationException e) {
            return e.getCause() != null ? e.getCause() : e;
        }
    }

    private TestResult testRequestTimeoutOutcomeUnknown() {
        return withFailures(true, true, m -> {
            CompletableFuture<String> f = m.sendRequestToMaster(createDummyRequest("m2_timeout", 1), "x");
            if (f.isDone() && !f.isCompletedExceptionally()) return skip("no server buffer (server not running)");
            pump(20);
            int failed = m.cleanupExpiredRequests(false);
            if (failed != 1) return fail("expected 1 timed-out request, got " + failed);
            Throwable cause = causeOf(f);
            return assertTrue("timeout must fail with RequestOutcomeUnknownException, got " + cause,
                    cause instanceof RequestOutcomeUnknownException);
        });
    }

    private TestResult testRequestTimeoutOverride() {
        return withFailures(true, true, m -> {
            CompletableFuture<String> shortF = m.sendRequestToSlave(createDummyRequest("m2_short", 1), "s", "x");
            CompletableFuture<String> defaultF = m.sendRequestToSlave(createDummyRequest("m2_default"), "s", "x");
            if (shortF.isDone() && !shortF.isCompletedExceptionally()) return skip("no server buffer (server not running)");
            pump(20);
            m.cleanupExpiredRequests(false);
            if (!(causeOf(shortF) instanceof RequestOutcomeUnknownException))
                return fail("getTimeoutMs() = 1 ms request should have timed out");
            return assertFalse("default 30 s request must still be pending", defaultF.isDone());
        });
    }

    private TestResult testRequestNotSentPlainFailure() {
        return withFailures(true, false, m -> {
            CompletableFuture<String> toMaster = m.sendRequestToMaster(createDummyRequest("m2_not_sent"), "x");
            if (toMaster.isDone() && !toMaster.isCompletedExceptionally()) return skip("no server buffer (server not running)");
            CompletableFuture<String> toSlave = m.sendRequestToSlave(createDummyRequest("m2_not_sent"), "s", "x");
            for (CompletableFuture<String> f : List.of(toMaster, toSlave)) {
                Throwable cause = causeOf(f);
                if (cause == null) return fail("refused send must fail at once");
                if (!(cause instanceof RequestNotSentException))
                    return fail("refused send must fail with RequestNotSentException, got " + cause);
            }
            // holders removed: nothing left for the sweep
            return assertEquals("holders must be removed", 0, m.failRequestsOnConnectionLost("s", Long.MAX_VALUE)
                    + m.failRequestsOnConnectionLost(null, Long.MAX_VALUE));
        });
    }

    private TestResult testRequestFailuresOffByDefault() {
        return withFailures(false, false, m -> {
            CompletableFuture<String> f = m.sendRequestToMaster(createDummyRequest("m2_off"), "x");
            if (f.isDone() && !f.isCompletedExceptionally()) return skip("no server buffer (server not running)");
            return assertFalse("switch off: refused send stays pending (old behaviour)", f.isDone());
        });
    }

    private TestResult testRequestDisconnectSweep() {
        return withFailures(true, true, m -> {
            CompletableFuture<String> toA = m.sendRequestToSlave(createDummyRequest("m2_sweep"), "a", "x");
            if (toA.isDone() && !toA.isCompletedExceptionally()) return skip("no server buffer (server not running)");
            CompletableFuture<String> toB = m.sendRequestToSlave(createDummyRequest("m2_sweep"), "b", "x");
            CompletableFuture<String> toMaster = m.sendRequestToMaster(createDummyRequest("m2_sweep"), "x");
            long now = System.currentTimeMillis();
            // Requests created after the disconnect are kept.
            if (m.failRequestsOnConnectionLost("a", now - 60_000) != 0)
                return fail("requests newer than the disconnect must be kept");
            m.failRequestsOnConnectionLost("a", now);
            if (!(causeOf(toA) instanceof RequestOutcomeUnknownException))
                return fail("slave 'a' disconnect must fail its request with RequestOutcomeUnknownException");
            if (toB.isDone() || toMaster.isDone())
                return fail("slave 'a' disconnect must not touch requests to 'b' or to the master");
            m.failRequestsOnConnectionLost(null, now);
            if (!(causeOf(toMaster) instanceof RequestOutcomeUnknownException))
                return fail("master disconnect must fail requests to the master");
            return assertFalse("master disconnect must not touch master-to-slave requests", toB.isDone());
        });
    }

    // ========================================================================
    // Dummy implementations for testing
    // ========================================================================

    private GenericRequest<String, String> createDummyRequest(String typeId) {
        return createDummyRequest(typeId, RequestManager.DEFAULT_TIMEOUT_MS);
    }

    private GenericRequest<String, String> createDummyRequest(String typeId, long timeoutMs) {
        return new GenericRequest<>() {
            @Override
            public String getRequestTypeID() {
                return typeId;
            }

            @Override
            public long getTimeoutMs() {
                return timeoutMs;
            }

            @Override
            public void encodeInput(RegistryFriendlyByteBuf buf, String input) {
                // Stub — not needed for registry tests
            }

            @Override
            public void encodeOutput(RegistryFriendlyByteBuf buf, String output) {
                // Stub — not needed for registry tests
            }

            @Override
            public String decodeInput(RegistryFriendlyByteBuf buf) {
                return null; // Stub
            }

            @Override
            public String decodeOutput(RegistryFriendlyByteBuf buf) {
                return null; // Stub
            }
        };
    }

    private GenericStream<String, String> createDummyStream(String typeId) {
        return new GenericStream<>() {
            @Override
            public GenericStream<String, String> copy() {
                return createDummyStream(typeId);
            }

            @Override
            public String getStreamTypeID() {
                return typeId;
            }

            @Override
            public void encodeContextData(RegistryFriendlyByteBuf buffer, String context) {
                // Stub — not needed for registry tests
            }

            @Override
            public String decodeContextData(RegistryFriendlyByteBuf buffer) {
                return null; // Stub
            }

            @Override
            public void encodeData(RegistryFriendlyByteBuf buffer, String data) {
                // Stub — not needed for registry tests
            }

            @Override
            public String decodeData(RegistryFriendlyByteBuf buffer) {
                return null; // Stub
            }
        };
    }
}
