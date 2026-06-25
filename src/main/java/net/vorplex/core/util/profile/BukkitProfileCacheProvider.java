package net.vorplex.core.util.profile;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BukkitProfileCacheProvider implements ProfileCacheProvider {

    /**
     * get the player's uuid or null if the player's uuid is not cached
     * Checks if the player is online
     * Then if they are in the Bukkit Offline Player cache
     * Lastly if they are internally cached from a previous fetch
     *
     * @param playerName the name of the player to get the uuid for
     * @return the uuid of the player or null if not cached
     */
    @Override
    @Nullable
    public UUID getCachedUUID(String playerName) {
        //Check if player is online
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) return player.getUniqueId();
        //Check Bukkit Offline Player Cache
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (offlinePlayer != null) {
            storeProfile(offlinePlayer.getUniqueId(), offlinePlayer.getName() == null ? playerName : offlinePlayer.getName());
            return offlinePlayer.getUniqueId();
        } else {
            //else check internal cache
            CachedProfile profile = getFromCache(playerName);
            return profile != null ? profile.uuid() : null;
        }
    }

    /**
     * get the player's name or null if the player's name is not cached
     * Checks if the player is online
     * Then if they are in the Bukkit Offline Player cache
     * Lastly if they are internally cached from a previous fetch
     *
     * @param uuid the uuid of the player to get the name for
     * @return the name of the player or null if not cached
     */
    @Override
    public @Nullable String getCachedName(UUID uuid) {
        //Check if player is online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) return player.getName();
        //Check Bukkit Offline Player Cache
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.getName() != null) return offlinePlayer.getName();
        else {
            // check internal cache
            CachedProfile profile = getFromCache(uuid);
            return profile != null ? profile.name() : null;
        }
    }

}