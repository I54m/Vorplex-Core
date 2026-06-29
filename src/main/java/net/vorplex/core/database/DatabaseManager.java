package net.vorplex.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.vorplex.core.VorplexCore;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private final VorplexCore plugin = VorplexCore.getInstance();
    @Getter
    private final StorageType storageType;
    private HikariDataSource hikari;

    public DatabaseManager(StorageType storageType) {
        this.storageType = storageType;
    }

    public void initializeConnection() {
        if (hikari == null) {
            plugin.getComponentLogger().info(Component.text("Establishing Database connection...").color(NamedTextColor.GREEN));

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setPoolName("Vorplex-Core");

            switch (storageType) {
                case MYSQL -> {
                    String host = plugin.getConfig().getString("Database.MYSQL.host");
                    String database = plugin.getConfig().getString("Database.MYSQL.database");
                    String username = plugin.getConfig().getString("Database.MYSQL.username");
                    String password = plugin.getConfig().getString("Database.MYSQL.password");
                    int port = plugin.getConfig().getInt("Database.MYSQL.port");
                    String extraArguments = plugin.getConfig().getString("Database.MYSQL.extraArguments");


                    hikariConfig.addDataSourceProperty("serverName", host);
                    hikariConfig.addDataSourceProperty("port", port);
                    hikariConfig.addDataSourceProperty("cachePrepStmts", true);
                    hikariConfig.addDataSourceProperty("prepStmtCacheSize", 100);
                    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
                    hikariConfig.addDataSourceProperty("useServerPrepStmts", true);
                    hikariConfig.setPassword(password);
                    hikariConfig.setUsername(username);
                    hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?" + extraArguments);
                    hikariConfig.setMaximumPoolSize(10);
                    hikariConfig.setMinimumIdle(10);
                }
                case SQLITE -> {
                    String filename = plugin.getConfig().getString("Database.SQLite.filename", "vorplexcore.db");
                    String filePath = new File(plugin.getDataFolder(), filename).getPath();

                    hikariConfig.setJdbcUrl("jdbc:sqlite:" + filePath);

                    hikariConfig.addDataSourceProperty("foreign_keys", "true");
                    hikariConfig.setMaximumPoolSize(1);
                    hikariConfig.setMinimumIdle(1);
                }
            }
            hikari = new HikariDataSource(hikariConfig);
        } else
            throw new IllegalStateException("Cannot Initialize Connection whilst a connection is already established!");
    }

    public Connection getConnection() throws SQLException {
        return hikari.getConnection();
    }

    public boolean isConnected() {
        return hikari != null && hikari.isRunning() && !hikari.isClosed();
    }

    public void shutdownConnection() {
        try {
            if (hikari != null && !hikari.isClosed()) {
                plugin.getComponentLogger().info(Component.text("Shutting Down Database connection...").color(NamedTextColor.GREEN));
                hikari.close();
                hikari = null;
                plugin.getComponentLogger().info(Component.text("Shut Down Database connection!").color(NamedTextColor.GREEN));
            } else throw new IllegalStateException("Cannot Shutdown connection when no connection is established!");
        } catch (Exception e) {
            plugin.getComponentLogger().error("Could not Close Storage!");
            plugin.getComponentLogger().error("Error Message: {}", e.getMessage());
        }
    }
}
