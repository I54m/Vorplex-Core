package net.vorplex.core.database;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface StorageProvider {

    void initialize() throws StorageException;

    @Nullable
    String getJoinMessage(UUID uuid) throws StorageException;

    @Nullable
    String getLeaveMessage(UUID uuid) throws StorageException;

    void setJoinMessage(UUID uuid, String joinMessage) throws StorageException;

    void setLeaveMessage(UUID uuid, String leaveMessage) throws StorageException;

    void deleteJoinMessage(UUID uuid) throws StorageException;

    void deleteLeaveMessage(UUID uuid) throws StorageException;
}
