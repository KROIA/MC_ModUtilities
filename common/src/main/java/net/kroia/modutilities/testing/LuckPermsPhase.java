package net.kroia.modutilities.testing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kroia.modutilities.ModChecker;
import net.kroia.modutilities.permission.LuckPermsBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * "LuckPerms removed from a save" test scenario (tools/autotest {@code --luckperms removed}):
 * <ol>
 *   <li><b>Phase 1</b>, world W WITH LuckPerms: tests {@link #record} what must survive, e.g. "player X has the admin
 *       flag". The values go to {@code <world>/testing/luckperms_phase1.json}.</li>
 *   <li><b>Phase 2</b>, the same world W WITHOUT LuckPerms ({@link #isPhase2}): tests read the values back with
 *       {@link #expected} / {@link #expectations} and check the mod still works and the state is intact.</li>
 * </ol>
 * File layout: {@code {"mods": {"<modId>": {"<key>": "<value>", ...}, ...}}}. Keys and values are plain strings,
 * namespaced per mod id; a key recorded again overwrites the old value.
 */
public final class LuckPermsPhase {
    private LuckPermsPhase() {}

    /** Location of the phase file, relative to the world folder. */
    public static final String FILE = "testing/luckperms_phase1.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** @return {@code <world>/testing/luckperms_phase1.json} of this server's world */
    public static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(FILE).normalize();
    }

    /** @return true if the world has a phase file (it ran with LuckPerms before) */
    public static boolean phase1Exists(MinecraftServer server) {
        return Files.isRegularFile(file(server));
    }

    /** @return true if this run is phase 2: the world has a phase file and LuckPerms is not installed */
    public static boolean isPhase2(MinecraftServer server) {
        return !ModChecker.isModLoaded("luckperms") && phase1Exists(server);
    }

    /**
     * Phase 1: stores {@code key=value} for {@code modId} in the world's phase file.
     * Only writes while LuckPerms is available, so runs without it never create a phase file.
     *
     * @return true if written; false without LuckPerms
     * @throws UncheckedIOException if the file cannot be written
     */
    public static synchronized boolean record(MinecraftServer server, String modId, String key, String value) {
        if (!LuckPermsBridge.isAvailable())
            return false;
        JsonObject root = read(server);
        JsonObject mods = root.has("mods") ? root.getAsJsonObject("mods") : new JsonObject();
        JsonObject mod = mods.has(modId) ? mods.getAsJsonObject(modId) : new JsonObject();
        mod.addProperty(key, value);
        mods.add(modId, mod);
        root.add("mods", mods);
        Path path = file(server);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return true;
    }

    /** @return the value {@code modId} recorded for {@code key} in phase 1, or null */
    public static @Nullable String expected(MinecraftServer server, String modId, String key) {
        return expectations(server, modId).get(key);
    }

    /** @return everything {@code modId} recorded in phase 1 (empty if nothing / no phase file) */
    public static Map<String, String> expectations(MinecraftServer server, String modId) {
        Map<String, String> out = new LinkedHashMap<>();
        JsonObject root = read(server);
        if (root.has("mods") && root.getAsJsonObject("mods").has(modId)) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("mods").getAsJsonObject(modId).entrySet())
                out.put(e.getKey(), e.getValue().getAsString());
        }
        return out;
    }

    private static JsonObject read(MinecraftServer server) {
        Path path = file(server);
        if (!Files.isRegularFile(path))
            return new JsonObject();
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
