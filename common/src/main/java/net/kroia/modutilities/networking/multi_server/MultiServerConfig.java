package net.kroia.modutilities.networking.multi_server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kroia.modutilities.ModUtilitiesMod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simple JSON config loaded from {@code config/MultiServerConfig.json}.
 *
 * Example master config (MultiServerConfig.json on the master server):
 * <pre>
 * {
 *   "enable": true,
 *   "isMaster": true,
 *   "masterTcpPort": 25575,
 *   "sharedSecret": "change-me-please"
 * }
 * </pre>
 *
 * Example slave config (MultiServerConfig.json on slave_a / slave_b):
 * <pre>
 * {
 *   "enable": true,
 *   "isMaster": false,
 *   "slaveID": "slave_a",
 *   "masterHost": "127.0.0.1",
 *   "masterTcpPort": 25575,
 *   "sharedSecret": "change-me-please"
 * }
 * </pre>
 */
public class MultiServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "MultiServerConfig.json");

    private static volatile MultiServerConfig instance = null;

    // ── Fields (mapped from JSON) ────────────────────────────────────────────

    /** If true, the usage of Server <--> Server communication is enabled. */
    public boolean enable = false;

    /** If true, this server acts as the master server and listens for slave connections. */
    public boolean isMaster = false;

    /** [Master only] Port the master TCP listener binds to. */
    public int masterTcpPort = 25575;

    /** [Master only] Secret token that slave servers must send in their handshake. */
    public String sharedSecret = "change-me-please";

    /** [Slave only] Unique ID for this slave server, e.g. "slave_a". */
    public String slaveID = "slave_a";

    /** [Slave only] Hostname or IP of the master server. */
    public String masterHost = "127.0.0.1";

    // ── Load / Save ──────────────────────────────────────────────────────────

    /**
     * Returns the singleton config instance, loading it from disk on first access.
     * If no config file exists yet, a default one is created and persisted.
     *
     * @return The shared {@link MultiServerConfig} instance.
     *
     * @apiNote
     * Subsequent calls reuse the same instance; the file is not re-read after the first
     * call. Mutating the returned object's public fields will not automatically be
     * persisted back to disk.
     */
    public static MultiServerConfig get() {
        if (instance == null) {
            synchronized (MultiServerConfig.class) {
                if (instance == null) {
                    instance = load();
                }
            }
        }
        return instance;
    }

    private static MultiServerConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            MultiServerConfig defaults = new MultiServerConfig();
            defaults.save();
            info("Created default config at "+ CONFIG_PATH);
            return defaults;
        }
        try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
            MultiServerConfig cfg = GSON.fromJson(reader, MultiServerConfig.class);
            info("Config loaded — isMaster="+cfg.isMaster+", slaveID="+cfg.slaveID);
            return cfg;
        } catch (IOException e) {
            error("Failed to read config, using defaults", e);
            return new MultiServerConfig();
        }
    }

    private void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            error("Failed to save default config", e);
        }
    }


    protected static void info(String message) {
        ModUtilitiesMod.LOGGER.info("[MultiServerConfig]"+message);
    }
    protected static void error(String message) {
        ModUtilitiesMod.LOGGER.error("[MultiServerConfig]"+message);
    }
    protected static void error(String message, Throwable throwable) {
        ModUtilitiesMod.LOGGER.error("[MultiServerConfig]"+message, throwable);
    }
    protected static void warn(String message) {
        ModUtilitiesMod.LOGGER.warn("[MultiServerConfig]"+message);
    }
    protected static void debug(String message) {
        ModUtilitiesMod.LOGGER.debug("[MultiServerConfig]"+message);
    }
}
