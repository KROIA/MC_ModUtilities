package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.networking.client_server.arrs.GenericRequest;
import net.kroia.modutilities.networking.client_server.arrs.RequestRegistry;
import net.kroia.modutilities.networking.client_server.streaming.GenericStream;
import net.kroia.modutilities.networking.client_server.streaming.StreamRegistry;
import net.kroia.modutilities.networking.multi_server.MultiServerConfig;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    // Dummy implementations for testing
    // ========================================================================

    private GenericRequest<String, String> createDummyRequest(String typeId) {
        return new GenericRequest<>() {
            @Override
            public String getRequestTypeID() {
                return typeId;
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
