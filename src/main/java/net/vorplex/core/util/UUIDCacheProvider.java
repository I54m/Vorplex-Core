package net.vorplex.core.util;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface UUIDCacheProvider {
    @Nullable
    UUID getCachedUUID(String playerName);
}
