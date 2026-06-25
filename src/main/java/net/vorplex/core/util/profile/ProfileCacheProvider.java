package net.vorplex.core.util.profile;

import net.vorplex.core.util.UUIDFetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public interface ProfileCacheProvider {

    Map<UUID, CachedProfile> CACHED_PROFILES = new ConcurrentHashMap<>();

    @Nullable
    UUID getCachedUUID(String playerName);

    @Nullable
    String getCachedName(UUID uuid);

    default void storeProfile(@NotNull CachedProfile profile) {
        CACHED_PROFILES.put(profile.uuid(), profile);
    }

    default void storeProfile(@NotNull MojangProfile profile) {
        storeProfile(new CachedProfile(profile.name(), UUIDFetcher.formatUUID(profile.id())));
    }

    default void storeProfile(@NotNull UUID uuid, @NotNull String name) {
        storeProfile(new CachedProfile(name, uuid));
    }

    @Nullable
    default CachedProfile getFromCache(UUID uuid) {
        return CACHED_PROFILES.get(uuid);
    }

    @Nullable
    default CachedProfile getFromCache(String name) {
        return CACHED_PROFILES.values().stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .orElse(null);
    }
}
