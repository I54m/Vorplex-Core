package net.vorplex.core;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.zaxxer.hikari.HikariDataSource;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.vorplex.core.autoannouncer.AutoAnnouncerScheduler;
import net.vorplex.core.autopickup.AutoPickupConfig;
import net.vorplex.core.autorestart.AutoRestartConfig;
import net.vorplex.core.autorestart.AutoRestartScheduler;
import net.vorplex.core.chat.AdminChatCommand;
import net.vorplex.core.chat.AsyncChatListener;
import net.vorplex.core.chat.StaffChatCommand;
import net.vorplex.core.commands.*;
import net.vorplex.core.listeners.AutoItemPickupListeners;
import net.vorplex.core.listeners.JoinMessageListeners;
import net.vorplex.core.listeners.LeaveMessageListeners;
import net.vorplex.core.listeners.SafeLoginListeners;
import net.vorplex.core.objects.Gift;
import net.vorplex.core.util.ConfigUpdater;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class VorplexCore extends JavaPlugin {

    // Misc Plugin variables
    @Getter
    @Setter(AccessLevel.PRIVATE)
    public static VorplexCore instance;
    @Getter
    private String prefix;
    private final File GiftsStorage = new File(this.getDataFolder(), "GiftsStorage.yml");

    // Config classes
    @Getter
    public AutoPickupConfig autoPickupConfig;
    @Getter
    public AutoRestartConfig autoRestartConfig;

    // Dependency variables
    @Getter
    private boolean placeholderAPI;
    //TODO change to using a getter and deprecate use of public variable
    public LuckPerms luckPermsAPI;

    // Plugin storage Hashmaps
    public Map<UUID, String> customJoinMessages = new HashMap<>();
    public Map<UUID, String> customLeaveMessages = new HashMap<>();
    public Map<UUID, ArrayList<Gift>> gifts = new HashMap<>();

    // SQL Connection variables - to be moved to storage class later
    private HikariDataSource hikari;
    private String host, username;
    private static String database;
    private int port;
    public Connection connection;
    private int cacheTaskid;

    // Legacy Variables - deprecated to be removed
    //TODO Temp prefix until all modules have been converted to minimessage format
    @Deprecated(since = "2.0-SNAPSHOT", forRemoval = true)
    public String LEGACY_PREFIX;
    @Deprecated(since = "2.0-SNAPSHOT", forRemoval = true)
    public boolean essentials = false;

    //Plugin reload command
    public final LiteralCommandNode<CommandSourceStack> RELOAD_COMMAND_NODE = Commands.literal("vorplexcorereload")
            .requires(ctx -> ctx.getSender().isOp())
            .executes((ctx) -> {
                AutoRestartScheduler.stop();
                AutoAnnouncerScheduler.stop();
                reloadConfig();
                if (this.getConfig().getBoolean("AutoRestart.enabled"))
                    AutoRestartScheduler.start(new AutoRestartConfig());
                if (this.getConfig().getBoolean("AutoAnnouncer.enabled"))
                    AutoAnnouncerScheduler.start();
                if (getConfig().getBoolean("AutoPickup.enabled"))
                    autoPickupConfig = new AutoPickupConfig();
//                if (getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
//                    cacheJoinMessages();
//                }
//                if (getConfig().getBoolean("LeaveMessages.customLeavemessages.enabled")) {
//                    cacheLeaveMessages();
//                }
                ctx.getSource().getSender().sendRichMessage(getPrefix() + "<green>Config reloaded!");
                return Command.SINGLE_SUCCESS;
            }).build();

    @Override
    public void onEnable() {
        long startTime = System.nanoTime();
        getComponentLogger().info("");
        getComponentLogger().info(Component.text("██╗   ██╗ ██████╗ ██████╗ ██████╗ ██╗     ███████╗██╗  ██╗").color(NamedTextColor.LIGHT_PURPLE));
        getComponentLogger().info(Component.text("██║   ██║██╔═══██╗██╔══██╗██╔══██╗██║     ██╔════╝╚██╗██╔╝").color(NamedTextColor.LIGHT_PURPLE));
        getComponentLogger().info(Component.text("██║   ██║██║   ██║██████╔╝██████╔╝██║     █████╗   ╚███╔╝").color(NamedTextColor.LIGHT_PURPLE));
        getComponentLogger().info(Component.text("╚██╗ ██╔╝██║   ██║██╔══██╗██╔═══╝ ██║     ██╔══╝   ██╔██╗").color(NamedTextColor.LIGHT_PURPLE));
        getComponentLogger().info(Component.text(" ╚████╔╝ ╚██████╔╝██║  ██║██║     ███████╗███████╗██╔╝ ██╗").color(NamedTextColor.LIGHT_PURPLE));
        getComponentLogger().info(Component.text("  ╚═══╝   ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚══════╝╚══════╝╚═╝  ╚═╝").color(NamedTextColor.LIGHT_PURPLE));
        getComponentLogger().info(Component.text("                  __   __   __   ___").color(NamedTextColor.DARK_PURPLE));
        getComponentLogger().info(Component.text("                 /  ` /  \\ |__) |__").color(NamedTextColor.DARK_PURPLE));
        getComponentLogger().info(Component.text("                 \\__, \\__/ |  \\ |___").color(NamedTextColor.DARK_PURPLE));
        getComponentLogger().info("───────────────────────────────────────────────────────────");
        getComponentLogger().info(Component.text("Developed by I54m").color(NamedTextColor.RED));
        getComponentLogger().info(Component.text("v" + getPluginMeta().getVersion() + " Running on " + getServer().getVersion()).color(NamedTextColor.RED));
        getComponentLogger().info("───────────────────────────────────────────────────────────");
        setInstance(this);
        ConfigUpdater.checkAndUpdate();
        prefix = this.getConfig().getString("Plugin-Prefix", "<dark_purple>[<light_purple>Vorplex-Core<dark_purple>] ");
        LEGACY_PREFIX = PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(prefix));
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(this.RELOAD_COMMAND_NODE, List.of("corereload", "vcreload", "vorplexreload")));
        //register luckperms api
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPermsAPI = provider.getProvider();
            getComponentLogger().info(Component.text("LuckPerms Detected!").color(NamedTextColor.GREEN));
        } else throw new IllegalStateException("LuckPerms not detected!");
        //register placeholder api
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderAPI = true;
            getComponentLogger().info(Component.text("PlaceholderAPI Detected!").color(NamedTextColor.GREEN));
        } else {
            placeholderAPI = false;
            getComponentLogger().info(Component.text("PlaceholderAPI NOT Detected!").color(NamedTextColor.RED));
            getComponentLogger().info(Component.text("Placeholder support will not be enabled!").color(NamedTextColor.RED));
        }
        //load modules
        if (this.getConfig().getBoolean("BuyCommand.enabled")) {
            getComponentLogger().info(Component.text("Enabling Buy Command...").color(NamedTextColor.GREEN));
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(BuyCommand.COMMAND_NODE));
        }
        if (this.getConfig().getBoolean("AutoRestart.enabled")) {
            getComponentLogger().info(Component.text("Enabling AutoRestart Module...").color(NamedTextColor.GREEN));
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(AutoRestartCommand.COMMAND_NODE, List.of("restart", "reboot", "autoreboot", "autore")));
            autoRestartConfig = new AutoRestartConfig();
            AutoRestartScheduler.start(autoRestartConfig);
        }
        if (getConfig().getBoolean("AutoAnnouncer.enabled")) {
            getComponentLogger().info(Component.text("Enabling Auto Announcer Module...").color(NamedTextColor.GREEN));
            AutoAnnouncerScheduler.start();
        }
        if (getConfig().getBoolean("AutoItemPickup.enabled")) {
            getComponentLogger().info(Component.text("Enabling Auto Item Pickup Module...").color(NamedTextColor.GREEN));
            autoPickupConfig = new AutoPickupConfig();
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(ToggleAutoPickupCommand.COMMAND_NODE, List.of("tapu")));
            getServer().getPluginManager().registerEvents(new AutoItemPickupListeners(), this);
        }
        if (getConfig().getBoolean("Chats.Staff.enabled") || getConfig().getBoolean("Chats.Admin.enabled")) {
            getServer().getPluginManager().registerEvents(new AsyncChatListener(), this);
            if (getConfig().getBoolean("Chats.Staff.enabled")) {
                getComponentLogger().info(Component.text("Enabling Staff Chat...").color(NamedTextColor.GREEN));
                this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(StaffChatCommand.COMMAND_NODE, List.of("sc")));
            }
            if (getConfig().getBoolean("Chats.Admin.enabled")) {
                getComponentLogger().info(Component.text("Enabling Admin Chat...").color(NamedTextColor.GREEN));
                this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(AdminChatCommand.COMMAND_NODE, List.of("ac")));
            }
        }
        if (getConfig().getBoolean("SafeLogin.enabled")) {
            getComponentLogger().info(Component.text("Enabling Safe Login Module...").color(NamedTextColor.GREEN));
            getServer().getPluginManager().registerEvents(new SafeLoginListeners(), this);
        }
        if (getConfig().getBoolean("RankTitle.enabled")) {
            getComponentLogger().info(Component.text("Enabling Rank Title Module...").color(NamedTextColor.GREEN));
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                commands.registrar().register(RankTitleCommand.COMMAND_NODE, List.of("titlerank", "tr", "rt"));
                commands.registrar().register(RealRankCommand.COMMAND_NODE, List.of("rankreal", "truerank"));
            });
        }
        if (getConfig().getBoolean("JoinMessages.PermissionJoinMessages.enabled") || getConfig().getBoolean("JoinMessages.CustomJoinMessages.enabled")) {
            getComponentLogger().info(Component.text("Enabling Join Messages...").color(NamedTextColor.GREEN));
            getServer().getPluginManager().registerEvents(new JoinMessageListeners(), this);
        }
        if (getConfig().getBoolean("LeaveMessages.PermissionLeaveMessages.enabled") || getConfig().getBoolean("LeaveMessages.CustomLeaveMessages.enabled")) {
            getComponentLogger().info(Component.text("Enabling Leave Messages...").color(NamedTextColor.GREEN));
            getServer().getPluginManager().registerEvents(new LeaveMessageListeners(), this);
        }


//        if (getConfig().getBoolean("Hub.enabled") ||
//                (getConfig().getBoolean("ViaVersion.enable-legacy-warning-on-join") && Bukkit.getPluginManager().isPluginEnabled("ViaVersion")))
//            Bukkit.getPluginManager().registerEvents(new PlayerJoin(), this);
//
//        if (getConfig().getBoolean("Hub.enabled")) {
//            if (Bukkit.getPluginManager().isPluginEnabled("EssentialsSpawn")) {
//                essentials = true;
//                getLogger().info("Essentials spawn detected, using as adapter for spawn teleporting!");
//            }
//            if (getConfig().getBoolean("Hub.oxygen-helmet-enabled")) {
//                ItemMeta im = PlayerJoin.oxygenHelmet.getItemMeta();
//                im.setDisplayName(ChatColor.WHITE + "Oxygen Helmet");
//                im.setLore(Arrays.asList(ChatColor.WHITE + "It's probably best to keep this on..."));
//                PlayerJoin.oxygenHelmet.setItemMeta(im);
//                Bukkit.getPluginManager().registerEvents(new InventoryClick(), this);
//            }
//            getLogger().info("Enabled Hub Module");
//        }
//        if (getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
//            setupSQLConnection();
//            Bukkit.getPluginCommand("joinmessage").setExecutor(new JoinMessageCommand());
//            getLogger().info("Enabled Custom Join Messages");
//        }
//        if (getConfig().getBoolean("LeaveMessages.customleavemessages.enabled")) {
//            setupSQLConnection();
//            Bukkit.getPluginCommand("leavemessage").setExecutor(new LeaveMessageCommand());
//            getLogger().info("Enabled Custom Leave Messages");
//        }
//        if (getConfig().getBoolean("Gifts.enabled")) {
//            try {
//                if (GiftsStorage.exists())
//                    loadGifts();
//                Bukkit.getPluginCommand("gift").setExecutor(new GiftCommand());
//                Bukkit.getPluginCommand("gifts").setExecutor(new GiftsCommand());
//                getLogger().info("Enabled Gifts Module");
//            } catch (Exception e) {
//                e.printStackTrace();
//                getLogger().info("ERROR: Could not enable Gifts Module, GiftsStorage.yml could not be loaded!!");
//            }
//        }
//        if (hikari != null || connection != null) {
//            setupmysql();
//            startCaching();
//        }
        getComponentLogger().info(Component.text("Plugin loaded in: " + (System.nanoTime() - startTime) / 1000000 + "ms!").color(NamedTextColor.GREEN));
        getComponentLogger().info("───────────────────────────────────────────────────────────");
    }

    @Override
    public void onDisable() {
        AutoRestartScheduler.stop();
        AutoAnnouncerScheduler.stop();
//        try {
//            if (hikari != null && !hikari.isClosed()) {
//                getLogger().info("Closing Storage....");
//                Bukkit.getScheduler().cancelTask(cacheTaskid);
//                hikari.close();
//                connection = null;
//                hikari = null;
//                getLogger().info("Storage Closed");
//            }
//            if (getConfig().getBoolean("Gifts.enabled")) {
//                saveGifts();
//            }
//        } catch (Exception e) {
//            getLogger().severe("Could not Close Storage!");
//            e.printStackTrace();
//        }
    }


    private void setupSQLConnection() {
        if (hikari == null || connection == null) {
            getLogger().info("Establishing MYSQL connection...");
            host = getConfig().getString("MySQL.host");
            database = getConfig().getString("MySQL.database");
            username = getConfig().getString("MySQL.username");
            String password = getConfig().getString("MySQL.password");
            port = getConfig().getInt("MySQL.port");
            String extraArguments = getConfig().getString("MySQL.extraArguments");
            hikari = new HikariDataSource();
            hikari.addDataSourceProperty("serverName", host);
            hikari.addDataSourceProperty("port", port);
            hikari.addDataSourceProperty("cachePrepStmts", true);
            hikari.addDataSourceProperty("prepStmtCacheSize", 100);
            hikari.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
            hikari.addDataSourceProperty("useServerPrepStmts", true);
            hikari.setPassword(password);
            hikari.setUsername(username);
            hikari.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + extraArguments);
            hikari.setPoolName("Vorplex-Core");
            hikari.setMaximumPoolSize(10);
            hikari.setMinimumIdle(10);
            try {
                openConnection();
            } catch (SQLException e) {
                getLogger().severe("MYSQL Connection failed!!! (SQLException)");
            }
        }
    }

    private void openConnection() throws SQLException {
        if (connection != null && !connection.isClosed() || hikari == null)
            return;
        connection = hikari.getConnection();
        getLogger().info("MYSQL Connected to server: " + host + ":" + port + " with user: " + username + "!");
    }

    private void setupmysql() {
        try {
            getLogger().info("Setting up MYSQL...");
            String createdb = "CREATE DATABASE IF NOT EXISTS " + database;
            PreparedStatement stmt = connection.prepareStatement(createdb);
            stmt.executeUpdate();
            stmt.close();
            getLogger().info(database + " Database Created!");
            if (getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
                String joinMessages = "CREATE TABLE IF NOT EXISTS `" + database + "`.`vorplexcore_joinmessages` ( `UUID` VARCHAR(36) NOT NULL ," +
                        "`RawMessage` VARCHAR(512) NOT NULL , PRIMARY KEY (`UUID`)) ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
                PreparedStatement stmt1 = connection.prepareStatement(joinMessages);
                stmt1.executeUpdate();
                stmt1.close();
            }
            if (getConfig().getBoolean("LeaveMessages.customleavemessages.enabled")) {
                String joinMessages = "CREATE TABLE IF NOT EXISTS `" + database + "`.`vorplexcore_leavemessages` ( `UUID` VARCHAR(36) NOT NULL ," +
                        "`RawMessage` VARCHAR(512) NOT NULL , PRIMARY KEY (`UUID`)) ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
                PreparedStatement stmt1 = connection.prepareStatement(joinMessages);
                stmt1.executeUpdate();
                stmt1.close();
            }
            getLogger().info("Tables Created!");
            String usedb = "USE " + database;
            PreparedStatement stmt3 = connection.prepareStatement(usedb);
            stmt3.executeUpdate();
            stmt3.close();
            getLogger().info("Database Set to: " + database);
            getLogger().info("MYSQL setup!");
            getLogger().info("");
            getLogger().info("SQL Connection is now online!");
            getLogger().info("");
        } catch (SQLException e) {
            getLogger().severe("Could not Setup MYSQL!!");
            e.printStackTrace();
        }
    }

    private void startCaching() {
        cacheTaskid = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (getConfig().getBoolean("JoinMessages.customjoinmessages")) {
                cacheJoinMessages();
            }
            if (getConfig().getBoolean("LeaveMessages.customLeavemessages.enabled")) {
                cacheLeaveMessages();
            }
        }, 200, 100);
        if (getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
            cacheJoinMessages();
        }
        if (getConfig().getBoolean("LeaveMessages.customLeavemessages.enabled")) {
            cacheLeaveMessages();
        }
    }

    private void cacheJoinMessages() {
        customJoinMessages.clear();
        try {
            String sql = "SELECT * FROM `vorplexcore_joinmessages`;";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet results = stmt.executeQuery();
            while (results.next()) {
                String joinMessageRaw = results.getString("RawMessage");
                if (joinMessageRaw.contains("%sinquo%"))
                    joinMessageRaw = joinMessageRaw.replace("%sinquo%", "'");
                if (joinMessageRaw.contains("%dubquo%"))
                    joinMessageRaw = joinMessageRaw.replace("%dubquo%", "\"");
                if (joinMessageRaw.contains("%bcktck%"))
                    joinMessageRaw = joinMessageRaw.replace("%bcktck%", "`");
                customJoinMessages.put(UUID.fromString(results.getString("UUID")), joinMessageRaw);
            }
            results.close();
            stmt.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            getLogger().warning("Unable to cache join messages from mysql database, custom join messages may no longer work!!");
        }
    }

    private void cacheLeaveMessages() {
        customLeaveMessages.clear();
        try {
            String sql = "SELECT * FROM `vorplexcore_leavemessages`;";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet results = stmt.executeQuery();
            while (results.next()) {
                String leaveMessageRaw = results.getString("RawMessage");
                if (leaveMessageRaw.contains("%sinquo%"))
                    leaveMessageRaw = leaveMessageRaw.replace("%sinquo%", "'");
                if (leaveMessageRaw.contains("%dubquo%"))
                    leaveMessageRaw = leaveMessageRaw.replace("%dubquo%", "\"");
                if (leaveMessageRaw.contains("%bcktck%"))
                    leaveMessageRaw = leaveMessageRaw.replace("%bcktck%", "`");
                customLeaveMessages.put(UUID.fromString(results.getString("UUID")), leaveMessageRaw);
            }
            results.close();
            stmt.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            getLogger().warning("Unable to cache leave messages from mysql database, custom leave messages may no longer work!!");
        }
    }

    private void saveGifts() {
        YamlConfiguration config = new YamlConfiguration();
        try {
            if (!GiftsStorage.exists())
                //noinspection ResultOfMethodCallIgnored
                GiftsStorage.createNewFile();
            config.load(GiftsStorage);
            for (UUID keys : gifts.keySet()) {
                for (Gift gift : gifts.get(keys)) {
                    config.set("gifts." + keys.toString() + ".giftno" + gifts.get(keys).indexOf(gift) + ".sender", gift.getSender().toString());
                    config.set("gifts." + keys.toString() + ".giftno" + gifts.get(keys).indexOf(gift) + ".item", gift.getItem());
                }
            }
            config.save(GiftsStorage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadGifts() {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(GiftsStorage);
            ConfigurationSection section = config.getConfigurationSection("gifts");
            if (section != null) {
                for (final String keys : section.getKeys(false)) {
                    ArrayList<Gift> gifts = new ArrayList<>();
                    ConfigurationSection section2 = config.getConfigurationSection("gifts." + keys);
                    if (section2 == null)
                        throw new NullPointerException("Could not load gifts from file: Missing gifts configuration section!");
                    for (String giftno : section2.getKeys(false)) {
                        UUID sender = UUID.fromString(config.getString("gifts." + keys + "." + giftno + ".sender"));
                        ItemStack item = config.getItemStack("gifts." + keys + "." + giftno + ".item");
                        gifts.add(new Gift(item, sender));
                    }
                    this.gifts.put(UUID.fromString(keys), gifts);
                }
                config.set("gifts", null);
                config.save(GiftsStorage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
