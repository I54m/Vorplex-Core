package net.vorplex.core.util.profile;

import net.vorplex.core.util.UUIDFetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player profile cache provider used by NameFetcher and UUIDFetcher.
 * Must be implemented by any cache providers
 */
public interface ProfileCacheProvider {

    /**
     * The cache of player profiles
     */
    Map<UUID, CachedProfile> CACHED_PROFILES = new ConcurrentHashMap<>();

    @Nullable
    UUID getCachedUUID(String playerName);

    @Nullable
    String getCachedName(UUID uuid);

    /**
     * Default implementation of storing a cached profile
     *
     * @param profile the cached profile to store
     */
    default void storeProfile(@NotNull CachedProfile profile) {
        CACHED_PROFILES.put(profile.uuid(), profile);
    }

    /**
     * Default implementation of storing a mojang profile
     * @param profile the mojang profile to store
     */
    default void storeProfile(@NotNull MojangProfile profile) {
        storeProfile(new CachedProfile(profile.name(), UUIDFetcher.formatUUID(profile.id())));
    }

    /**
     * Default implementation of storing a player's raw info
     * @param uuid the uuid of the player to store
     * @param name the name of the player to store
     */
    default void storeProfile(@NotNull UUID uuid, @NotNull String name) {
        storeProfile(new CachedProfile(name, uuid));
    }

    /**
     * Default implementation of getting a player from the cache via uuid
     * @param uuid the uuid of the player whose profile is being fetched
     * @return the CachedProfile of the player
     */
    @Nullable
    default CachedProfile getFromCache(UUID uuid) {
        return CACHED_PROFILES.get(uuid);
    }

    /**
     * Default implementation of getting a player from the cache via name
     * @param name the name of the player whose profile is being fetched
     * @return the CachedProfile of the player
     */
    @Nullable
    default CachedProfile getFromCache(String name) {
        return CACHED_PROFILES.values().stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .orElse(null);
    }
}
