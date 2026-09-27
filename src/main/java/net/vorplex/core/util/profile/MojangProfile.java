package net.vorplex.core.util.profile;

/**
 * Represents a player's profile when fetched from the mojang api
 *
 * @param name the username of the player
 * @param id   the mojang provided uuid of the player
 */
public record MojangProfile(String name, String id) {
}
