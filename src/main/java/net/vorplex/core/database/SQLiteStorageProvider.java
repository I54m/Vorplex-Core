package net.vorplex.core.database;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.vorplex.core.VorplexCore;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * SQLite implementation of StorageProvider
 */
public class SQLiteStorageProvider implements StorageProvider {

    private final DatabaseManager databaseManager;
    private final VorplexCore plugin = VorplexCore.getInstance();

    /**
     * Constructor for SQLiteStorageProvider
     *
     * @param databaseManager the DatabaseManager to use to get a connection to the storage
     */
    public SQLiteStorageProvider(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void initialize() throws StorageException {
        if (databaseManager.getStorageType() != StorageType.SQLITE)
            throw new IllegalStateException("Cannot initialize SQLite Storage Provider when storage type is: " + databaseManager.getStorageType());
        try (Connection connection = databaseManager.getConnection()) {
            plugin.getComponentLogger().info(Component.text("Initializing SQLite Storage Provider...").color(NamedTextColor.GREEN));
            if (plugin.getConfig().getBoolean("JoinMessages.CustomJoinMessages.enabled")) {
                String joinMessages = """
                            CREATE TABLE IF NOT EXISTS vorplexcore_joinmessages (
                                UUID TEXT NOT NULL PRIMARY KEY,
                                RawMessage TEXT NOT NULL
                            )
                        """;
                try (PreparedStatement stmt = connection.prepareStatement(joinMessages)) {
                    stmt.executeUpdate();
                }
            }
            if (plugin.getConfig().getBoolean("LeaveMessages.CustomLeaveMessages.enabled")) {
                String leaveMessages = """
                            CREATE TABLE IF NOT EXISTS vorplexcore_leavemessages (
                                UUID TEXT NOT NULL PRIMARY KEY,
                                RawMessage TEXT NOT NULL
                            )
                        """;
                try (PreparedStatement stmt = connection.prepareStatement(leaveMessages)) {
                    stmt.executeUpdate();
                }
            }
            plugin.getComponentLogger().info(Component.text("SQLite Storage Provider initialized!").color(NamedTextColor.GREEN));
        } catch (SQLException e) {
            throw new StorageException("Failed to initialize tables for custom join & leave messages", e);
        }
    }

    @Override
    public @Nullable String getJoinMessage(UUID uuid) throws StorageException {
        String sql = """
                    SELECT RawMessage
                    FROM vorplexcore_joinmessages
                    WHERE UUID = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());

            try (ResultSet results = stmt.executeQuery()) {
                if (results.next()) return results.getString("RawMessage");
            }

        } catch (SQLException e) {
            throw new StorageException("Failed to retrieve join message for UUID " + uuid, e);
        }
        return null;
    }

    @Override
    public @Nullable String getLeaveMessage(UUID uuid) throws StorageException {
        String sql = """
                    SELECT RawMessage
                    FROM vorplexcore_leavemessages
                    WHERE UUID = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());

            try (ResultSet results = stmt.executeQuery()) {
                if (results.next()) return results.getString("RawMessage");
            }

        } catch (SQLException e) {
            throw new StorageException("Failed to retrieve leave message for UUID " + uuid, e);
        }
        return null;
    }

    @Override
    public void setJoinMessage(UUID uuid, String joinMessage) throws StorageException {
        String sql = """
                    INSERT INTO vorplexcore_joinmessages (UUID, RawMessage)
                    VALUES (?, ?)
                    ON CONFLICT(UUID)
                    DO UPDATE SET RawMessage = excluded.RawMessage
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.setString(2, joinMessage);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new StorageException("Failed to set join message for " + uuid, e);
        }
    }

    @Override
    public void setLeaveMessage(UUID uuid, String leaveMessage) throws StorageException {
        String sql = """
                    INSERT INTO vorplexcore_leavemessages (UUID, RawMessage)
                    VALUES (?, ?)
                    ON CONFLICT(UUID)
                    DO UPDATE SET RawMessage = excluded.RawMessage
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.setString(2, leaveMessage);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new StorageException("Failed to set join message for " + uuid, e);
        }
    }

    @Override
    public void deleteJoinMessage(UUID uuid) throws StorageException {
        String sql = """
                    DELETE FROM vorplexcore_joinmessages
                    WHERE UUID = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new StorageException("Failed to delete join message for " + uuid, e);
        }
    }

    @Override
    public void deleteLeaveMessage(UUID uuid) throws StorageException {
        String sql = """
                    DELETE FROM vorplexcore_leavemessages
                    WHERE UUID = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new StorageException("Failed to delete leave message for " + uuid, e);
        }
    }
}
