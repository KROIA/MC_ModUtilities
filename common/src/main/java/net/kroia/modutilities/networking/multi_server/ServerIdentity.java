package net.kroia.modutilities.networking.multi_server;

import com.google.gson.*;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.persistence.DataPersistence;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * {@link IServerIdentity} stored in {@code <world>/data/ModUtilities/server_identity.json}.
 * The file is written atomically and only when a field changed. Thread-safe (the master updates
 * {@code known_slaves} from Netty threads).
 */
public final class ServerIdentity implements IServerIdentity {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private record KnownSlave(@Nullable UUID uid, long lastSeenMs) {}

    private final Path file;
    private final UUID serverUid;
    private final long firstStartMs;
    private ServerRole role = ServerRole.STANDALONE;
    /** Last slave_id passed to createSlave. Kept while the server runs as STANDALONE/MASTER, see {@link #slaveId()}. */
    private @Nullable String slaveId;
    private @Nullable UUID masterUid;
    private final Map<String, KnownSlave> knownSlaves = new TreeMap<>();
    /** File content as last read or written; {@link #save()} skips the write when nothing changed. */
    private @Nullable String savedJson;

    private ServerIdentity(Path file, UUID serverUid, long firstStartMs) {
        this.file = file;
        this.serverUid = serverUid;
        this.firstStartMs = firstStartMs;
    }

    /** @return {@code <world>/data/ModUtilities/server_identity.json} of the given server. */
    public static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("ModUtilities").resolve("server_identity.json");
    }

    /**
     * Loads the identity from {@code file}, applies the role and saves it if anything changed.
     * A missing or corrupt file starts a new identity, as does a SLAVE whose slave_id differs from the stored one.
     */
    public static ServerIdentity load(Path file, ServerRole role, @Nullable String slaveId) {
        ServerIdentity id = read(file);
        if (id == null) {
            id = new ServerIdentity(file, UUID.randomUUID(), System.currentTimeMillis());
            info("New server identity " + id.serverUid + " in " + file);
        } else if (role == ServerRole.SLAVE && !Objects.equals(id.slaveId, slaveId)) {
            ServerIdentity old = id;
            id = new ServerIdentity(file, UUID.randomUUID(), System.currentTimeMillis());
            warn("slave_id changed from '" + old.slaveId + "' to '" + slaveId + "': new server_uid " + id.serverUid + " (was " + old.serverUid + ")");
        }
        id.role = role;
        if (role == ServerRole.SLAVE)
            id.slaveId = slaveId;
        id.save();
        return id;
    }

    private static @Nullable ServerIdentity read(Path file) {
        if (!Files.exists(file))
            return null;
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject o = JsonParser.parseString(text).getAsJsonObject();
            ServerIdentity id = new ServerIdentity(file, UUID.fromString(o.get("server_uid").getAsString()), o.get("first_start_ms").getAsLong());
            id.role = ServerRole.valueOf(o.get("role").getAsString());
            id.slaveId = optString(o, "slave_id");
            id.masterUid = optUuid(o, "master_uid");
            if (o.get("known_slaves") instanceof JsonObject known)
                for (Map.Entry<String, JsonElement> e : known.entrySet()) {
                    JsonObject s = e.getValue().getAsJsonObject();
                    id.knownSlaves.put(e.getKey(), new KnownSlave(optUuid(s, "uid"), s.get("last_seen_ms").getAsLong()));
                }
            id.savedJson = text;
            return id;
        } catch (Exception e) {
            error("Corrupt server identity file " + file + ", starting a new identity: " + e);
            return null;
        }
    }

    private static @Nullable String optString(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }
    private static @Nullable UUID optUuid(JsonObject o, String key) {
        String s = optString(o, key);
        return s == null ? null : UUID.fromString(s);
    }

    private void save() {
        JsonObject o = new JsonObject();
        o.addProperty("server_uid", serverUid.toString());
        o.addProperty("role", role.name());
        o.addProperty("slave_id", slaveId);
        o.addProperty("master_uid", masterUid == null ? null : masterUid.toString());
        o.addProperty("first_start_ms", firstStartMs);
        JsonObject known = new JsonObject();
        knownSlaves.forEach((name, s) -> {
            JsonObject e = new JsonObject();
            e.addProperty("uid", s.uid == null ? null : s.uid.toString());
            e.addProperty("last_seen_ms", s.lastSeenMs);
            known.add(name, e);
        });
        o.add("known_slaves", known);
        String json = GSON.toJson(o);
        if (json.equals(savedJson))
            return;
        try {
            Files.createDirectories(file.getParent());
            DataPersistence.writeJsonAtomically(file, json);
            savedJson = json;
        } catch (Exception e) {
            error("Failed to save server identity " + file + ": " + e);
        }
    }

    /**
     * (Master) Whether a handshaking slave reports a uid that already belongs to this master or to another
     * known slave. A reconnect under the same slave_id is not a duplicate. Diagnostics only, no authorization.
     */
    public synchronized boolean isDuplicateUid(String slaveId, @Nullable UUID uid) {
        if (uid == null)
            return false;
        if (uid.equals(serverUid))
            return true;
        for (Map.Entry<String, KnownSlave> e : knownSlaves.entrySet())
            if (!e.getKey().equals(slaveId) && uid.equals(e.getValue().uid))
                return true;
        return false;
    }

    /** (Master) Records a slave that completed the handshake. */
    public synchronized void onSlaveSeen(String slaveId, @Nullable UUID uid) {
        knownSlaves.put(slaveId, new KnownSlave(uid, System.currentTimeMillis()));
        save();
    }

    /** (Slave) Records the master's uid received in the handshake result. */
    public synchronized void setMasterUid(UUID uid) {
        masterUid = uid;
        save();
    }

    @Override public UUID serverUid() { return serverUid; }
    @Override public synchronized ServerRole role() { return role; }
    @Override public synchronized @Nullable String slaveId() { return role == ServerRole.SLAVE ? slaveId : null; }
    @Override public synchronized @Nullable UUID masterUid() { return role == ServerRole.SLAVE ? masterUid : null; }
    @Override public synchronized @Nullable UUID slaveUid(String slaveId) {
        KnownSlave s = knownSlaves.get(slaveId);
        return s == null ? null : s.uid;
    }

    private static void info(String message) {
        ModUtilitiesMod.LOGGER.info("[ServerIdentity]: " + message);
    }
    private static void warn(String message) {
        ModUtilitiesMod.LOGGER.warn("[ServerIdentity]: " + message);
    }
    private static void error(String message) {
        ModUtilitiesMod.LOGGER.error("[ServerIdentity]: " + message);
    }
}
