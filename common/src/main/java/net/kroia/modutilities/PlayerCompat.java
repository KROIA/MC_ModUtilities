package net.kroia.modutilities;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

/**
 * Version-agnostic player profile accessors, so dependent mods need no Minecraft-version toggles
 * of their own. Minecraft 1.21.9 ships authlib 7, where {@link GameProfile} became a record
 * ({@code getName()}/{@code getId()} were replaced by {@code name()}/{@code id()}).
 */
public final class PlayerCompat {
    private PlayerCompat() {}

    /** @return the player name stored in the profile */
    public static String profileName(GameProfile profile) {
        //return profile.getName(); // mc<=1.21.8
        return profile.name(); // mc>=1.21.9
    }

    /** @return the player UUID stored in the profile */
    public static UUID profileId(GameProfile profile) {
        //return profile.getId(); // mc<=1.21.8
        return profile.id(); // mc>=1.21.9
    }
}
