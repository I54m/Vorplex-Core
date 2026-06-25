package net.vorplex.core.util.profile;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TestProfileCacheProvider implements ProfileCacheProvider {

    @Override
    public @Nullable UUID getCachedUUID(String playerName) {
        return null;
    }

    @Override
    public @Nullable String getCachedName(UUID uuid) {
        return null;
    }
}
