package net.vorplex.core.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BukkitUUIDCacheProvider implements UUIDCacheProvider {

    /**
     * get the Bukkit cache of a player's uuid or null if the player's uuid is not cached
     *
     * @param playerName the name of the player to get the uuid for
     * @return the uuid of the player if in the cache
     */
    @Override
    @Nullable
    public UUID getCachedUUID(String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        return offlinePlayer == null ? null : offlinePlayer.getUniqueId();
    }
}