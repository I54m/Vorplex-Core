package net.vorplex.core.database;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * StorageProvider Interface used by different storage implementations
 */
public interface StorageProvider {

    /**
     * Initializes the storage once there is a database connection
     * Creates required tables and other startup checks
     *
     * @throws StorageException if the initialization could not complete
     */
    void initialize() throws StorageException;

    /**
     * Gets the join message of a player
     * @param uuid the uuid of the player to get a join message for
     * @return The raw join message or null if none was found
     * @throws StorageException if there was an unrecoverable exception encountered
     */
    @Nullable
    String getJoinMessage(UUID uuid) throws StorageException;

    /**
     * Gets the leave message of a player
     * @param uuid the uuid of the player to get a leave message for
     * @return The raw leave message or null if none was found
     * @throws StorageException if there was an unrecoverable exception encountered
     */
    @Nullable
    String getLeaveMessage(UUID uuid) throws StorageException;

    /**
     * Sets the join message of a player
     * @param uuid the uuid of the player to set a join message for
     * @param joinMessage the raw join message to set for the player
     * @throws StorageException if there was an unrecoverable exception encountered
     */
    void setJoinMessage(UUID uuid, String joinMessage) throws StorageException;

    /**
     * Sets the leave message of a player
     * @param uuid the uuid of the player to set a leave message for
     * @param leaveMessage the raw leave message to set for the player
     * @throws StorageException if there was an unrecoverable exception encountered
     */
    void setLeaveMessage(UUID uuid, String leaveMessage) throws StorageException;

    /**
     * Deletes or clears a player's join message
     * @param uuid the uuid of the player to delete the join message
     * @throws StorageException if there was an unrecoverable exception encountered
     */
    void deleteJoinMessage(UUID uuid) throws StorageException;

    /**
     * Deletes or clears a player's leave message
     * @param uuid the uuid of the player to delete the leave message
     * @throws StorageException if there was an unrecoverable exception encountered
     */
    void deleteLeaveMessage(UUID uuid) throws StorageException;
}
