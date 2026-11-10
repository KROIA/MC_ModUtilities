package net.kroia.modutilities.testing.tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kroia.modutilities.JsonUtilities;
import net.kroia.modutilities.discord.DiscordWebhooks;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;

import javax.net.ssl.SSLSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * DiscordWebhooks against a temporary config folder and a fake HTTP function (no network). The module's test
 * hooks are package-private, so they are reached reflectively; teardown restores them.
 */
public class DiscordTests extends TestSuite {
    private static final String MOD = "mu_discord_test";
    private static final String OTHER_MOD = "mu_discord_test_other";
    private static final String HOOK_A = "https://discord.com/api/webhooks/1/token-a";
    private static final String HOOK_B = "https://canary.discord.com/api/webhooks/2/token-b";

    private record Call(URI uri, JsonObject body, long nanos) {}

    private final List<Call> calls = Collections.synchronizedList(new ArrayList<>());
    /** Status codes to answer with, in order; 204 once empty. */
    private final List<Integer> answers = Collections.synchronizedList(new ArrayList<>());
    private volatile CountDownLatch gate;
    private Path dir;
    private Object originalHttp;

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.DISCORD;
    }

    @Override
    public void registerTests() {
        addTest("config_generated_and_merged", this::testConfigMerged);
        addTest("render_and_omit_empty_field", this::testRender);
        addTest("min_amount_and_multi_webhook", this::testMinAmountAndRouting);
        addTest("batch_split_10_embeds", this::testBatchTen);
        addTest("batch_split_6000_chars", this::testBatchChars);
        addTest("rate_limit_retry_after", this::testRetryAfter);
        addTest("status_overview_names_only", this::testStatus);
    }

    @Override
    public void setup() {
        try {
            dir = Files.createTempDirectory("mu_discord_test");
            Files.writeString(dir.resolve("webhooks.json"), """
                    {"webhooks": {"a": "%s", "b": "%s", "bad": "http://example.com/api/webhooks/3/x"}}
                    """.formatted(HOOK_A, HOOK_B));
            Files.writeString(dir.resolve(MOD + ".json"), """
                    {"types": {
                      "render": {"enabled": true, "webhooks": ["a"], "title": "Deposit {player}", "color": 123,
                                 "fields": [{"name": "Player", "value": "{player}", "inline": true},
                                            {"name": "Note", "value": "{note}"}]},
                      "multi": {"enabled": true, "webhooks": ["a", "b", "bad"], "minAmount": 100, "title": "Multi"},
                      "big": {"enabled": true, "webhooks": ["b"], "title": "big",
                              "fields": [{"name": "A", "value": "{x}"}, {"name": "B", "value": "{y}"}]},
                      "off": {"enabled": false, "webhooks": ["a"]}
                    }}
                    """);
            originalHttp = field("http").get(null);
            field("configDir").set(null, dir);
            BiFunction<URI, String, CompletableFuture<HttpResponse<String>>> fake = this::fakeHttp;
            field("http").set(null, fake);
            DiscordWebhooks.register(MOD, List.of("render", "multi", "big", "off", "newtype"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void teardown() {
        try {
            Method shutdown = DiscordWebhooks.class.getDeclaredMethod("shutdown");
            shutdown.setAccessible(true);
            shutdown.invoke(null);
            ((Map<?, ?>) field("REGISTERED").get(null)).remove(MOD);
            ((Map<?, ?>) field("REGISTERED").get(null)).remove(OTHER_MOD);
            field("configDir").set(null, null);
            field("http").set(null, originalHttp);
            DiscordWebhooks.reload(); // restores the routes of real registered mods, if any
            if (dir != null) {
                try (Stream<Path> files = Files.walk(dir)) {
                    files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------------------------------------

    private TestResult testConfigMerged() {
        try {
            JsonObject types = JsonUtilities.fromString(Files.readString(dir.resolve(MOD + ".json"))).getAsJsonObject()
                    .getAsJsonObject("types");
            JsonObject added = types.getAsJsonObject("newtype");
            if (added == null || added.get("enabled").getAsBoolean() || !added.getAsJsonArray("webhooks").isEmpty())
                return fail("new type not added disabled/unrouted: " + added);
            if (!"Deposit {player}".equals(types.getAsJsonObject("render").get("title").getAsString()))
                return fail("user override was not kept");
            if (!types.getAsJsonObject("render").has("minAmount"))
                return fail("missing default key not added");
            return assertTrue("active", DiscordWebhooks.isActive());
        } catch (Exception e) {
            return fail(e.toString());
        }
    }

    private TestResult testRender() {
        reset();
        DiscordWebhooks.post(MOD, "render", Map.of("player", "player_a"));
        DiscordWebhooks.post(MOD, "off", Map.of("player", "player_a"));
        DiscordWebhooks.post(MOD, "unknown", Map.of());
        if (!waitCalls(1))
            return fail("no request sent");
        pump(200); // a disabled/unknown type would show up as a second call
        if (calls.size() != 1)
            return fail("expected 1 request, got " + calls.size());
        JsonObject body = calls.get(0).body;
        if (!body.getAsJsonObject("allowed_mentions").getAsJsonArray("parse").isEmpty())
            return fail("allowed_mentions.parse must be empty");
        JsonObject embed = body.getAsJsonArray("embeds").get(0).getAsJsonObject();
        if (!"Deposit player_a".equals(embed.get("title").getAsString()) || embed.get("color").getAsInt() != 123)
            return fail("title/color wrong: " + embed);
        JsonArray fields = embed.getAsJsonArray("fields");
        if (fields.size() != 1 || !"player_a".equals(fields.get(0).getAsJsonObject().get("value").getAsString()))
            return fail("empty field not omitted: " + fields);
        return assertEquals("uri", URI.create(HOOK_A), calls.get(0).uri);
    }

    private TestResult testMinAmountAndRouting() {
        reset();
        DiscordWebhooks.post(MOD, "multi", Map.of("amount", "-50"));
        DiscordWebhooks.post(MOD, "multi", Map.of("amount", "-150"));
        if (!waitCalls(2))
            return fail("expected 2 requests, got " + calls.size());
        pump(200);
        Set<URI> uris = new HashSet<>();
        int embeds = 0;
        for (Call c : calls) {
            uris.add(c.uri);
            embeds += c.body.getAsJsonArray("embeds").size();
        }
        if (calls.size() != 2 || embeds != 2)
            return fail("below-minimum event was sent or routing wrong: " + calls.size() + " requests, " + embeds + " embeds");
        return assertEquals("routed to both valid webhooks", Set.of(URI.create(HOOK_A), URI.create(HOOK_B)), uris);
    }

    private TestResult testBatchTen() {
        return assertEquals("embeds per message", List.of(1, 10, 10, 5), gatedBatch("render", 26, Map.of("player", "p")));
    }

    private TestResult testBatchChars() {
        // each embed: 3 + 1 + 1000 + 1 + 1000 = 2005 chars -> 2 fit in 6000
        Map<String, String> values = Map.of("x", "x".repeat(1000), "y", "y".repeat(1000));
        return assertEquals("embeds per message", List.of(1, 2, 2, 1), gatedBatch("big", 6, values));
    }

    private TestResult testRetryAfter() {
        reset();
        answers.add(429);
        DiscordWebhooks.post(MOD, "render", Map.of("player", "p"));
        if (!waitCalls(2))
            return fail("429 was not retried");
        long waitedMillis = (calls.get(1).nanos - calls.get(0).nanos) / 1_000_000L;
        if (!calls.get(0).body.equals(calls.get(1).body))
            return fail("retry sent a different body");
        if (waitedMillis < 300)
            return fail("retry_after 300 ms not honored, waited " + waitedMillis + " ms");
        return pass("waited " + waitedMillis + " ms");
    }

    @SuppressWarnings("unchecked")
    private TestResult testStatus() {
        try {
            DiscordWebhooks.register(OTHER_MOD, List.of("othertype"));
            Method status = DiscordWebhooks.class.getDeclaredMethod("status");
            status.setAccessible(true);
            List<String> lines = (List<String>) status.invoke(null);
            for (String line : lines) {
                if (line.contains("http") || line.contains("token") || line.contains("/api/webhooks"))
                    return fail("status shows a webhook URL: " + line);
            }
            List<String> expected = List.of(
                    "  webhook 'bad': invalid",
                    "  mod '" + MOD + "': 3 of 5 types enabled",
                    "    render: enabled -> a",
                    "    multi: enabled, min 100 -> a, b, bad",
                    "    big: enabled -> b",
                    "    off: disabled -> a",
                    "    newtype: disabled",
                    "  mod '" + OTHER_MOD + "': 0 of 1 types enabled",
                    "    othertype: disabled");
            for (String line : expected) {
                if (!lines.contains(line))
                    return fail("missing '" + line + "' in " + lines);
            }
            return assertTrue("type lines follow their mod header",
                    lines.indexOf("    newtype: disabled") + 1 == lines.indexOf("  mod '" + OTHER_MOD + "': 0 of 1 types enabled"));
        } catch (Exception e) {
            return fail(e.toString());
        }
    }

    // ---------------------------------------------------------------------------------------------

    /** Holds the sender in the first request while the rest is queued, so they go out as one drained batch. */
    private List<Integer> gatedBatch(String type, int count, Map<String, String> values) {
        reset();
        CountDownLatch g = new CountDownLatch(1);
        gate = g;
        DiscordWebhooks.post(MOD, type, values);
        waitCalls(1);
        for (int i = 1; i < count; i++)
            DiscordWebhooks.post(MOD, type, values);
        g.countDown();
        pumpUntil(() -> embedCount() >= count, 5000);
        List<Integer> sizes = new ArrayList<>();
        synchronized (calls) {
            for (Call c : calls)
                sizes.add(c.body.getAsJsonArray("embeds").size());
        }
        return sizes;
    }

    private int embedCount() {
        synchronized (calls) {
            return calls.stream().mapToInt(c -> c.body.getAsJsonArray("embeds").size()).sum();
        }
    }

    private void reset() {
        calls.clear();
        answers.clear();
        gate = null;
    }

    private boolean waitCalls(int n) {
        return pumpUntil(() -> calls.size() >= n, 5000);
    }

    private CompletableFuture<HttpResponse<String>> fakeHttp(URI uri, String body) {
        calls.add(new Call(uri, JsonUtilities.fromString(body).getAsJsonObject(), System.nanoTime()));
        CountDownLatch g = gate;
        if (g != null) {
            gate = null;
            try {
                g.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        int status = answers.isEmpty() ? 204 : answers.remove(0);
        String responseBody = status == 429 ? "{\"retry_after\": 0.3}" : "";
        return CompletableFuture.completedFuture(new FakeResponse(status, responseBody, uri));
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field f = DiscordWebhooks.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private record FakeResponse(int statusCode, String body, URI uri) implements HttpResponse<String> {
        @Override public HttpRequest request() { return HttpRequest.newBuilder(uri).build(); }
        @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (a, b) -> true); }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
