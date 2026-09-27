package net.vorplex.core.util.profile;

import java.util.UUID;

/**
 * Represents a player's profile cached by a ProfileCacheProvider
 *
 * @param name the username of the player
 * @param uuid the uuid of the player
 */
public record CachedProfile(String name, UUID uuid) {
}