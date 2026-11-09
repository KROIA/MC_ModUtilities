package net.kroia.modutilities.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;
import net.kroia.modutilities.JsonUtilities;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.PermissionCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional Discord webhook output for server events (e.g. bank transactions, market trades).
 *
 * <h2>Inert until configured</h2>
 * Nothing happens (no thread, no file access) until a mod calls {@link #register(String, List)}. Registering
 * creates {@code config/modutilities/discord/webhooks.json} and {@code config/modutilities/discord/<modId>.json}
 * if missing; every message type starts disabled. The sender thread is only started once the server admin
 * enables a type and routes it to a valid webhook.
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li>{@code webhooks.json}: {@code {"webhooks": {"<name>": "<url>"}}}. Only {@code https} URLs on
 *       {@code discord.com}, {@code discordapp.com}, {@code ptb.discord.com} or {@code canary.discord.com} with a
 *       path below {@code /api/webhooks/} are accepted; other entries are skipped with a warning naming the
 *       webhook name. Several mods and types may share one webhook name.</li>
 *   <li>{@code <modId>.json}: {@code {"language": "en_us", "types": {"<type>": {"enabled": false,
 *       "webhooks": ["<name>", ...], "minAmount": 0}}}}. A type may additionally override {@code title},
 *       {@code color} and {@code fields} of its template. New types are added to the file, user edits are kept.</li>
 * </ul>
 *
 * <h2>Templates</h2>
 * Each mod ships {@code assets/<modId>/discord/<lang>.json} (fallback {@code en_us}, then the type key as title):
 * <pre>{@code
 * { "deposit": { "title": "Deposit", "color": 3447003,
 *                "fields": [ {"name": "Player", "value": "{player}", "inline": true} ] } }
 * }</pre>
 * {@code {key}} placeholders are replaced from the posted values; a field whose value renders empty is omitted.
 *
 * <h2>Security</h2>
 * Webhook URLs contain a secret token. They are never logged, never sent to clients and never shown by the
 * {@code /modutilities discord status} command; only webhook names appear in output.
 *
 * <h2>Multi-server</h2>
 * This class does not know about master/slave servers. Callers must only post from the multi-server MASTER
 * (or a standalone server), otherwise every event is reported once per server.
 */
public final class DiscordWebhooks {
    private DiscordWebhooks() {}

    public static final String PERMISSION = "modutilities.command.discord";

    static final int MAX_QUEUE = 1000;
    static final int MAX_EMBEDS_PER_MESSAGE = 10;
    static final int MAX_MESSAGE_CHARS = 6000;
    private static final int MAX_TITLE = 256;
    private static final int MAX_FIELD_NAME = 256;
    private static final int MAX_FIELD_VALUE = 1024;
    private static final int MAX_FIELDS = 25;
    private static final int MAX_SERVER_ERROR_RETRIES = 3;
    private static final int MAX_RATE_LIMIT_RETRIES = 10;
    private static final long FLUSH_TIMEOUT_MILLIS = 5000;
    private static final long DROP_WARN_INTERVAL_MILLIS = 30_000;
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final Set<String> HOSTS = Set.of("discord.com", "discordapp.com", "ptb.discord.com", "canary.discord.com");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9_.-]+)}");

    /** Config folder; package-private so the dev test can point it at a temporary folder. Null = default. */
    static Path configDir;
    /**
     * Sends one JSON body to one webhook URL. Package-private so the dev test can replace it with a fake;
     * the default uses a lazily created {@link HttpClient}.
     */
    static volatile BiFunction<URI, String, CompletableFuture<HttpResponse<String>>> http = DiscordWebhooks::sendHttp;

    private record Field(String name, String value, boolean inline) {}
    private record Route(String title, Integer color, List<Field> fields, List<String> webhooks, double minAmount) {}
    private record Item(String webhook, URI uri, JsonObject embed, int chars) {}

    /** modId -> registered types, in registration order. Kept across server restarts (mods re-register anyway). */
    private static final Map<String, List<String>> REGISTERED = new LinkedHashMap<>();
    /** modId -> type -> route; only enabled types with at least one valid webhook. */
    private static Map<String, Map<String, Route>> routes = Map.of();
    private static Map<String, URI> webhooks = Map.of();
    private static Set<String> invalidWebhooks = Set.of();
    private static volatile boolean active;

    private static final ArrayBlockingQueue<Item> QUEUE = new ArrayBlockingQueue<>(MAX_QUEUE);
    private static final AtomicLong DROPPED = new AtomicLong();
    private static long lastDropWarn;
    /** The running sender; set to null on shutdown, which tells that thread to drain the queue and exit. */
    private static volatile Thread sender;
    /** A sender that did not finish draining within the flush timeout; it stops at the next interrupt. */
    private static volatile Thread abandoned;
    private static final Object CLIENT_LOCK = new Object();
    private static HttpClient client;

    // ---------------------------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------------------------

    /**
     * Declares a mod's message types and loads (or creates) its config. Call on the server, e.g. from
     * {@code LifecycleEvent.SERVER_STARTED}; calling again replaces the type list and re-reads all config files.
     *
     * @param modId the calling mod's id; also names its config file and template folder
     * @param types every message type the mod may {@link #post}
     */
    public static synchronized void register(String modId, List<String> types) {
        REGISTERED.put(modId, List.copyOf(types));
        reload();
    }

    /**
     * Queues one embed for every webhook the type is routed to. A cheap no-op if the mod is not registered or the
     * type is disabled or unrouted. Never throws and never blocks on the network.
     * <p>
     * Only post from the multi-server MASTER (or a standalone server); this class does not check that.
     *
     * @param values placeholder values for the type's template; if {@code "amount"} is numeric, its absolute
     *               value must be at least the type's {@code minAmount} or the event is skipped
     */
    public static synchronized void post(String modId, String type, Map<String, String> values) {
        Map<String, Route> modRoutes = routes.get(modId);
        Route route = modRoutes == null ? null : modRoutes.get(type);
        if (route == null)
            return;
        try {
            if (route.minAmount > 0 && Math.abs(parseAmount(values.get("amount"))) < route.minAmount)
                return;
            JsonObject embed = render(route, type, values);
            int chars = embedChars(embed);
            for (String name : route.webhooks) {
                Item item = new Item(name, webhooks.get(name), embed, chars);
                while (!QUEUE.offer(item)) {
                    if (QUEUE.poll() != null)
                        noteDropped();
                }
            }
        } catch (RuntimeException e) {
            ModUtilitiesMod.LOGGER.warn("[Discord] Could not build message '{}' of mod '{}'", type, modId, e);
        }
    }

    /** Re-reads {@code webhooks.json} and the config of every registered mod; starts the sender if needed. */
    public static synchronized void reload() {
        if (REGISTERED.isEmpty())
            return;
        loadWebhooks();
        Map<String, Map<String, Route>> newRoutes = new HashMap<>();
        for (Map.Entry<String, List<String>> e : REGISTERED.entrySet()) {
            Map<String, Route> modRoutes = loadMod(e.getKey(), e.getValue());
            if (!modRoutes.isEmpty())
                newRoutes.put(e.getKey(), modRoutes);
        }
        routes = newRoutes;
        active = !newRoutes.isEmpty();
        if (active && sender == null) {
            sender = new Thread(DiscordWebhooks::runSender, "ModUtilities-Discord");
            sender.setDaemon(true);
            sender.start();
        }
    }

    /** @return true if at least one message type is enabled and routed to a valid webhook */
    public static boolean isActive() {
        return active;
    }

    /** Wires the lifecycle hook and the {@code /modutilities discord} command. Called once by ModUtilities. */
    public static void init() {
        PermissionCompat.registerNode(PERMISSION, 3);
        LifecycleEvent.SERVER_STOPPING.register(server -> shutdown());
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("modutilities").then(Commands.literal("discord")
                        .requires(src -> PermissionCompat.hasPermission(src, PERMISSION, 3))
                        .then(Commands.literal("reload").executes(ctx -> {
                            reload();
                            reply(ctx, "Discord config reloaded, " + (active ? "active" : "inactive"));
                            return 1;
                        }))
                        .then(Commands.literal("status").executes(ctx -> {
                            status().forEach(line -> reply(ctx, line));
                            return 1;
                        })))));
    }

    // ---------------------------------------------------------------------------------------------
    // Lifecycle / status
    // ---------------------------------------------------------------------------------------------

    /** Stops routing, flushes the queue for up to 5 s and stops the sender. Registrations are kept. */
    private static synchronized void shutdown() {
        routes = Map.of();
        active = false;
        Thread t = sender;
        sender = null;
        if (t != null) {
            t.interrupt(); // wakes an idle take(); the thread sees sender != itself, drains the queue and exits
            try {
                t.join(FLUSH_TIMEOUT_MILLIS);
                if (t.isAlive()) {
                    abandoned = t; // aborts a pending request or wait at the next interrupt
                    t.interrupt();
                    t.join(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (!QUEUE.isEmpty())
            ModUtilitiesMod.LOGGER.warn("[Discord] {} message(s) not delivered before shutdown", QUEUE.size());
        QUEUE.clear();
        synchronized (CLIENT_LOCK) {
            if (client != null) {
                client.shutdownNow();
                client = null;
            }
        }
    }

    private static synchronized List<String> status() {
        List<String> lines = new ArrayList<>();
        lines.add("Discord: " + (active ? "active" : "inactive") + ", queue " + QUEUE.size() + ", dropped " + DROPPED.get());
        Set<String> names = new TreeSet<>(webhooks.keySet());
        names.addAll(invalidWebhooks);
        for (String name : names)
            lines.add("  webhook '" + name + "': " + (webhooks.containsKey(name) ? "valid" : "invalid"));
        for (Map.Entry<String, List<String>> e : REGISTERED.entrySet()) {
            int enabled = routes.getOrDefault(e.getKey(), Map.of()).size();
            lines.add("  mod '" + e.getKey() + "': " + enabled + " of " + e.getValue().size() + " types enabled");
        }
        return lines;
    }

    private static void reply(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    private static void noteDropped() {
        DROPPED.incrementAndGet();
        long now = System.currentTimeMillis();
        if (now - lastDropWarn >= DROP_WARN_INTERVAL_MILLIS) {
            lastDropWarn = now;
            ModUtilitiesMod.LOGGER.warn("[Discord] Queue full, dropping oldest messages ({} dropped so far)", DROPPED.get());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Config
    // ---------------------------------------------------------------------------------------------

    private static Path dir() {
        return configDir != null ? configDir : Platform.getConfigFolder().resolve(ModUtilitiesMod.MOD_ID).resolve("discord");
    }

    private static void loadWebhooks() {
        Path file = dir().resolve("webhooks.json");
        Map<String, URI> valid = new LinkedHashMap<>();
        Set<String> invalid = new TreeSet<>();
        if (!Files.exists(file)) {
            JsonObject root = new JsonObject();
            root.addProperty("_comment", "Discord webhooks by name: \"<name>\": \"https://discord.com/api/webhooks/<id>/<token>\". "
                    + "Message types route to these names in <modId>.json. Webhook URLs are secrets: do not share this file.");
            root.add("webhooks", new JsonObject());
            write(file, root);
        } else {
            JsonObject root = read(file);
            JsonElement map = root == null ? null : root.get("webhooks");
            if (map != null && map.isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : map.getAsJsonObject().entrySet()) {
                    URI uri = validUrl(e.getValue());
                    if (uri != null) {
                        valid.put(e.getKey(), uri);
                    } else {
                        invalid.add(e.getKey());
                        ModUtilitiesMod.LOGGER.warn("[Discord] Webhook '{}' is not a valid Discord webhook URL and is ignored", e.getKey());
                    }
                }
            }
        }
        webhooks = valid;
        invalidWebhooks = invalid;
    }

    static URI validUrl(JsonElement value) {
        try {
            URI uri = new URI(value.getAsString().trim());
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && HOSTS.contains(uri.getHost().toLowerCase())
                    && uri.getPath() != null && uri.getPath().startsWith("/api/webhooks/"))
                return uri;
        } catch (Exception ignored) {
            // not a string or not a URI; the caller warns with the name only
        }
        return null;
    }

    /** Loads/creates/merges {@code <modId>.json}; @return routes of enabled types with valid webhooks. */
    private static Map<String, Route> loadMod(String modId, List<String> types) {
        Path file = dir().resolve(modId + ".json");
        boolean exists = Files.exists(file);
        JsonObject root = exists ? read(file) : new JsonObject();
        boolean writable = root != null; // never overwrite a file the admin broke; use defaults in memory
        if (root == null)
            root = new JsonObject();
        boolean changed = !exists;

        if (!root.has("language")) {
            root.addProperty("language", DEFAULT_LANGUAGE);
            changed = true;
        }
        if (!(root.get("types") instanceof JsonObject)) {
            root.add("types", new JsonObject());
            changed = true;
        }
        JsonObject typesObj = root.getAsJsonObject("types");
        for (String type : types) {
            if (!(typesObj.get(type) instanceof JsonObject)) {
                typesObj.add(type, new JsonObject());
                changed = true;
            }
            JsonObject t = typesObj.getAsJsonObject(type);
            if (!t.has("enabled")) { t.addProperty("enabled", false); changed = true; }
            if (!t.has("webhooks")) { t.add("webhooks", new JsonArray()); changed = true; }
            if (!t.has("minAmount")) { t.addProperty("minAmount", 0); changed = true; }
        }
        if (changed && writable)
            write(file, root);

        String language = DEFAULT_LANGUAGE;
        if (root.get("language") != null && root.get("language").isJsonPrimitive()
                && root.get("language").getAsString().matches("[a-z]{2,3}_[a-z]{2,3}"))
            language = root.get("language").getAsString();
        JsonObject fallback = loadTemplates(modId, DEFAULT_LANGUAGE);
        JsonObject selected = language.equals(DEFAULT_LANGUAGE) ? fallback : loadTemplates(modId, language);

        Map<String, Route> result = new HashMap<>();
        for (String type : types) {
            try {
                JsonObject t = typesObj.getAsJsonObject(type);
                if (!t.get("enabled").getAsBoolean())
                    continue;
                List<String> names = new ArrayList<>();
                for (JsonElement n : t.getAsJsonArray("webhooks")) {
                    String name = n.getAsString();
                    if (webhooks.containsKey(name)) {
                        if (!names.contains(name))
                            names.add(name);
                    } else {
                        ModUtilitiesMod.LOGGER.warn("[Discord] Type '{}' of mod '{}' routes to unknown or invalid webhook '{}'", type, modId, name);
                    }
                }
                if (names.isEmpty())
                    continue;
                JsonObject template = selected.get(type) instanceof JsonObject o ? o
                        : fallback.get(type) instanceof JsonObject o2 ? o2 : new JsonObject();
                String title = string(t, "title", string(template, "title", type));
                JsonElement color = t.has("color") ? t.get("color") : template.get("color");
                JsonElement fields = t.has("fields") ? t.get("fields") : template.get("fields");
                result.put(type, new Route(title, color == null ? null : color.getAsInt(), parseFields(fields),
                        List.copyOf(names), t.get("minAmount").getAsDouble()));
            } catch (RuntimeException e) {
                ModUtilitiesMod.LOGGER.warn("[Discord] Type '{}' in {}.json is malformed and stays disabled", type, modId);
            }
        }
        return result;
    }

    private static JsonObject loadTemplates(String modId, String language) {
        Optional<Path> path = Platform.getOptionalMod(modId)
                .flatMap(mod -> findResource(mod, "assets", modId, "discord", language + ".json"));
        if (path.isEmpty())
            return new JsonObject();
        JsonObject templates = read(path.get());
        return templates == null ? new JsonObject() : templates;
    }

    private static Optional<Path> findResource(Mod mod, String... path) {
        try {
            return mod.findResource(path);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static List<Field> parseFields(JsonElement fields) {
        List<Field> result = new ArrayList<>();
        if (fields == null || !fields.isJsonArray())
            return result;
        for (JsonElement e : fields.getAsJsonArray()) {
            if (e instanceof JsonObject f)
                result.add(new Field(string(f, "name", ""), string(f, "value", ""),
                        f.has("inline") && f.get("inline").getAsBoolean()));
        }
        return result;
    }

    private static String string(JsonObject o, String key, String def) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    private static JsonObject read(Path file) {
        try {
            JsonElement e = JsonUtilities.fromString(Files.readString(file, StandardCharsets.UTF_8));
            if (e instanceof JsonObject o)
                return o;
        } catch (Exception ignored) {
            // the exception text may quote file content, so it is not logged
        }
        ModUtilitiesMod.LOGGER.warn("[Discord] {} is not a valid JSON object; it is ignored and left unchanged", file.getFileName());
        return null;
    }

    private static void write(Path file, JsonObject root) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, JsonUtilities.toPrettyString(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ModUtilitiesMod.LOGGER.warn("[Discord] Could not write {}", file.getFileName(), e);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------------------------------

    private static double parseAmount(String amount) {
        if (amount == null)
            return Double.POSITIVE_INFINITY;
        try {
            return Double.parseDouble(amount.trim());
        } catch (NumberFormatException e) {
            return Double.POSITIVE_INFINITY; // not numeric: no filtering
        }
    }

    private static String fill(String template, Map<String, String> values) {
        Matcher m = PLACEHOLDER.matcher(template);
        return m.replaceAll(r -> Matcher.quoteReplacement(values.getOrDefault(r.group(1), "")));
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** Builds the embed within Discord's per-embed limits (title, field sizes, 25 fields, 6000 chars total). */
    private static JsonObject render(Route route, String type, Map<String, String> values) {
        String title = cut(fill(route.title, values), MAX_TITLE);
        if (title.isBlank())
            title = cut(type, MAX_TITLE);
        int chars = title.length();
        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        if (route.color != null)
            embed.addProperty("color", route.color);
        JsonArray fields = new JsonArray();
        for (Field f : route.fields) {
            String value = fill(f.value, values);
            if (value.isBlank())
                continue;
            String name = cut(f.name.isBlank() ? "​" : fill(f.name, values), MAX_FIELD_NAME);
            value = cut(value, MAX_FIELD_VALUE);
            if (fields.size() >= MAX_FIELDS || chars + name.length() + value.length() > MAX_MESSAGE_CHARS)
                break;
            chars += name.length() + value.length();
            JsonObject field = new JsonObject();
            field.addProperty("name", name);
            field.addProperty("value", value);
            field.addProperty("inline", f.inline);
            fields.add(field);
        }
        embed.add("fields", fields);
        return embed;
    }

    private static int embedChars(JsonObject embed) {
        int chars = embed.get("title").getAsString().length();
        for (JsonElement f : embed.getAsJsonArray("fields"))
            chars += f.getAsJsonObject().get("name").getAsString().length() + f.getAsJsonObject().get("value").getAsString().length();
        return chars;
    }

    // ---------------------------------------------------------------------------------------------
    // Sender thread
    // ---------------------------------------------------------------------------------------------

    private static boolean aborted() {
        return abandoned == Thread.currentThread();
    }

    private static void runSender() {
        Map<String, Long> blockedUntil = new HashMap<>(); // webhook name -> System.nanoTime() before which not to send
        List<Item> batch = new ArrayList<>();
        while (true) {
            batch.clear();
            if (sender != Thread.currentThread()) {
                // shutdown: deliver what is left, then exit
                if (QUEUE.drainTo(batch) == 0)
                    return;
            } else {
                try {
                    batch.add(QUEUE.take());
                    QUEUE.drainTo(batch);
                } catch (InterruptedException e) {
                    continue;
                }
            }
            Map<String, List<Item>> byWebhook = new LinkedHashMap<>();
            for (Item item : batch)
                byWebhook.computeIfAbsent(item.webhook, k -> new ArrayList<>()).add(item);
            for (List<Item> items : byWebhook.values()) {
                int start = 0;
                while (start < items.size()) {
                    int end = start;
                    int chars = 0;
                    while (end < items.size() && end - start < MAX_EMBEDS_PER_MESSAGE
                            && (end == start || chars + items.get(end).chars <= MAX_MESSAGE_CHARS)) {
                        chars += items.get(end).chars;
                        end++;
                    }
                    if (!deliver(items.subList(start, end), blockedUntil))
                        return;
                    start = end;
                }
            }
        }
    }

    /** @return false if the thread was told to stop while delivering */
    private static boolean deliver(List<Item> items, Map<String, Long> blockedUntil) {
        Item first = items.get(0);
        JsonObject payload = new JsonObject();
        JsonObject mentions = new JsonObject();
        mentions.add("parse", new JsonArray());
        payload.add("allowed_mentions", mentions);
        JsonArray embeds = new JsonArray();
        items.forEach(i -> embeds.add(i.embed));
        payload.add("embeds", embeds);
        String body = JsonUtilities.toString(payload);

        int serverErrors = 0;
        int rateLimits = 0;
        while (true) {
            Long until = blockedUntil.get(first.webhook);
            if (until != null && !sleep((until - System.nanoTime()) / 1_000_000L))
                return false;
            HttpResponse<String> response = null;
            try {
                CompletableFuture<HttpResponse<String>> future = http.apply(first.uri, body);
                while (response == null) {
                    try {
                        response = future.get();
                    } catch (InterruptedException e) {
                        if (aborted()) {
                            future.cancel(true);
                            return false;
                        } // otherwise: shutdown is flushing; keep waiting for this request
                    }
                }
            } catch (ExecutionException | RuntimeException e) {
                response = null; // connection error / timeout: retried like a 5xx (the exception may name the URL)
            }
            if (response != null) {
                String remaining = response.headers().firstValue("X-RateLimit-Remaining").orElse(null);
                String resetAfter = response.headers().firstValue("X-RateLimit-Reset-After").orElse(null);
                if ("0".equals(remaining) && resetAfter != null)
                    blockedUntil.put(first.webhook, System.nanoTime() + seconds(resetAfter, 1) * 1_000_000L);
                else
                    blockedUntil.remove(first.webhook);
                int code = response.statusCode();
                if (code >= 200 && code < 300)
                    return true;
                if (code == 429) {
                    if (++rateLimits > MAX_RATE_LIMIT_RETRIES) {
                        ModUtilitiesMod.LOGGER.warn("[Discord] Webhook '{}' stayed rate limited; {} embed(s) dropped", first.webhook, items.size());
                        return true;
                    }
                    if (!sleep(retryAfterMillis(response)))
                        return false;
                    continue;
                }
                if (code < 500) {
                    ModUtilitiesMod.LOGGER.warn("[Discord] Webhook '{}' rejected a message with HTTP {}; {} embed(s) dropped", first.webhook, code, items.size());
                    return true;
                }
            }
            if (++serverErrors > MAX_SERVER_ERROR_RETRIES) {
                ModUtilitiesMod.LOGGER.warn("[Discord] Webhook '{}' failed {} times; {} embed(s) dropped", first.webhook, serverErrors, items.size());
                return true;
            }
            if (!sleep(1000L << (serverErrors - 1)))
                return false;
        }
    }

    private static long retryAfterMillis(HttpResponse<String> response) {
        try {
            JsonElement e = JsonUtilities.fromString(response.body());
            if (e instanceof JsonObject o && o.has("retry_after"))
                return (long) Math.ceil(o.get("retry_after").getAsDouble() * 1000);
        } catch (RuntimeException ignored) {
            // fall back to the header
        }
        return seconds(response.headers().firstValue("Retry-After").orElse("1"), 1);
    }

    private static long seconds(String value, double def) {
        double s;
        try {
            s = Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            s = def;
        }
        return (long) Math.ceil(Math.max(0, s) * 1000);
    }

    /** Sleeps; an interrupt during the flush phase is ignored unless the flush timed out. @return false to stop */
    private static boolean sleep(long millis) {
        long end = System.nanoTime() + Math.max(0, millis) * 1_000_000L;
        while (true) {
            long leftNanos = end - System.nanoTime();
            if (leftNanos <= 0)
                return true;
            try {
                Thread.sleep(leftNanos / 1_000_000L, (int) (leftNanos % 1_000_000L));
            } catch (InterruptedException e) {
                if (aborted())
                    return false;
            }
        }
    }

    private static CompletableFuture<HttpResponse<String>> sendHttp(URI uri, String body) {
        HttpClient c;
        synchronized (CLIENT_LOCK) {
            if (client == null)
                client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            c = client;
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return c.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
