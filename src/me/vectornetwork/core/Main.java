package me.vectornetwork.core;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import com.zaxxer.hikari.HikariDataSource;
import me.vectornetwork.core.autorestart.Config;
import me.vectornetwork.core.autorestart.Scheduler;
import me.vectornetwork.core.commands.*;
import me.vectornetwork.core.listeners.*;
import me.vectornetwork.core.objects.Gift;
import me.vectornetwork.core.util.BookUtils;
import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Main extends JavaPlugin {

    public String prefix = null;

    public static String PREFIX = null;
    public static String PREFIX_NO_COLOR = null;

    public static Main instance;
    public static boolean announce;
    private static int previousMessageNumber;
    private File GiftsStorage = new File(this.getDataFolder(), "GiftsStorage.yml");

    private int cacheTaskid;

    public Map<UUID, String> equippedTitles = new HashMap<>();
    public TreeMap<Integer, String> titles = new TreeMap<>();
    public Map<String, String> permissionJoinMessages = new HashMap<>();
    public Map<String, String> permissionLeaveMessages = new HashMap<>();
    public Map<UUID, String> customJoinMessages = new HashMap<>();
    public Map<UUID, String> customLeaveMessages = new HashMap<>();
    public Map<UUID, ArrayList<Gift>> gifts = new HashMap<>();

    private HikariDataSource hikari;
    private String host, username, password, extraArguments;
    private static String database;
    private int port;
    public Connection connection;

    private void setInstance(Main instance) {
        Main.instance = instance;
    }

    public static Main getInstance() {
        return instance;
    }

    public Config config = null;
    public LuckPerms luckPermsAPI = null;

    public boolean essentials = false;
    public boolean old = Bukkit.getServer().getVersion().contains("1.7") ||
            Bukkit.getServer().getVersion().contains("1.8") ||
            Bukkit.getServer().getVersion().contains("1.9") ||
            Bukkit.getServer().getVersion().contains("1.10") ||
            Bukkit.getServer().getVersion().contains("1.11") ||
            Bukkit.getServer().getVersion().contains("1.12");


    @Override
    public void onEnable() {
        setInstance(this);
        saveDefaultConfig();
        this.getConfig().options().copyDefaults(true);
        prefix = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("Prefixes.plugin", "&5[&d&lVector-Core&5] "));
        PREFIX = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("Prefixes.autorestart-module", "&5[&dVector-Restart&5] "));
        PREFIX_NO_COLOR = ChatColor.stripColor(PREFIX);
        this.getCommand("vectorcorereload").setExecutor(this);
        //load modules
        if (this.getConfig().getBoolean("VoteBookGUI.enabled")) {
            this.getCommand("vote").setExecutor(new VoteGUICommand());
            new BookUtils();
            getLogger().info("Enabled Vote Book Module");
        }
        if (this.getConfig().getBoolean("VoteRewards.enabled")) {
            this.getServer().getPluginManager().registerEvents(new PlayerVote(), this);
            getLogger().info("Enabled Vote Rewards Module");
        }
        if (this.getConfig().getBoolean("AutoRestart.enabled")) {
            getCommand("autorestart").setExecutor(new AutoRe());
            config = new Config();
            Scheduler.start(config);
            getLogger().info("Enabled Auto Restart Module");
        }
        if (this.getConfig().getBoolean("VectorServer.enabled")) {
            this.getServer().getPluginManager().registerEvents(new CommandPreProcess(), this);
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            getLogger().info("Enabled Server Module");
        }
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null)
            luckPermsAPI = provider.getProvider();
        else {
            getLogger().severe("Luckperms not detected! Disabling plugin!");
            this.setEnabled(false);
            return;
        }

        if (getConfig().getBoolean("Announcer.enabled")) {
            announce = false;
            getLogger().info("Detected no players online! disabling announcements to save resources!");
            Bukkit.getPluginManager().registerEvents(new PlayerQuit(), this);
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
                if (announce) {
                    String message;
                    int messageNumber;
                    int maxMesages;
                    maxMesages = getConfig().getInt("Announcer.NumberOfMessages");
                    if (getConfig().getBoolean("Announcer.Random"))
                        messageNumber = (int) ((Math.random() * maxMesages) + 1);
                    else {
                        messageNumber = previousMessageNumber + 1;
                        if (previousMessageNumber >= maxMesages)
                            previousMessageNumber = 0;
                        else previousMessageNumber++;
                    }
                    message = getConfig().getString("Announcer.Messages." + messageNumber + ".text");
                    String prefix = getConfig().getString("Announcer.Prefix");

                    ClickEvent.Action clickEventAction = ClickEvent.Action.valueOf(getConfig().getString("Announcer.Messages." + messageNumber + ".clickevent.action"));
                    String clickEventValue = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Announcer.Messages." + messageNumber + ".clickevent.value"));
                    HoverEvent.Action hoverEventAction = HoverEvent.Action.valueOf(getConfig().getString("Announcer.Messages." + messageNumber + ".hoverevent.action"));
                    BaseComponent[] hoverEventValue = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Announcer.Messages." + messageNumber + ".hoverevent.value")));
                    TextComponent messagetext = new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix) + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', message)));
                    messagetext.setHoverEvent(new HoverEvent(hoverEventAction, hoverEventValue));
                    messagetext.setClickEvent(new ClickEvent(clickEventAction, clickEventValue));
                    for (Player players : Bukkit.getOnlinePlayers()) {
                        players.spigot().sendMessage(messagetext);
                    }
                }
            }, 720, (getConfig().getInt("Announcer.Interval") * 20));
            getLogger().info("Enabled Auto Announcer Module");
        }
        if (getConfig().getBoolean("Announcer.enabled") || getConfig().getBoolean("JoinMessages.enabled") || getConfig().getBoolean("Hub.enabled"))
            Bukkit.getPluginManager().registerEvents(new PlayerJoin(), this);

        if (getConfig().getBoolean("Hub.enabled")) {
            if (Bukkit.getPluginManager().isPluginEnabled("EssentialsSpawn")) {
                essentials = true;
                getLogger().info("Essentials spawn detected, using as adapter for spawn teleporting!");
            }
            getLogger().info("Enabled Hub Module");
        }
        if (getConfig().getBoolean("RankTitle.enabled")) {
            Bukkit.getPluginCommand("ranktitle").setExecutor(new RankTitleCommand());
            getLogger().info("Enabled Rank Title Module");
        }
        if (getConfig().getBoolean("Titles.enabled")) {
            if (Bukkit.getPluginManager().isPluginEnabled("MVdWPlaceholderAPI")) {
                setupSQLConnection();
                Bukkit.getPluginCommand("title").setExecutor(new TitleCommand());
                PlaceholderAPI.registerPlaceholder(this, "vector_titles", (event) -> {
                    if (event.isOnline() && event.getPlayer() != null) {
                        Player player = event.getPlayer();
                        UUID uuid = player.getUniqueId();
                        if (equippedTitles.containsKey(uuid) && equippedTitles.get(uuid) != null) {
                            if (equippedTitles.get(uuid).isEmpty()) return "";
                            else
                                return ChatColor.DARK_GRAY + "[" + ChatColor.translateAlternateColorCodes('&', equippedTitles.get(uuid)) + ChatColor.DARK_GRAY + "]" + ChatColor.RESET + " ";
                        }
                    }
                    return "";
                });
                titles.put(0, "");
                getLogger().info("Enabled Title Module");
            } else {
                getLogger().warning("MVdWPlaceholderAPI not detected, title module will not be enabled!");
            }
        }
        if (getConfig().getBoolean("JoinMessages.enabled")) {
            getLogger().info("Enabled Join Messages Module");
            if (getConfig().getBoolean("JoinMessages.permissionbasedjoinmessages.enabled")) {
                for (String permission : getConfig().getConfigurationSection("JoinMessages.permissionbasedjoinmessages.messages").getKeys(false)) {
                    permissionJoinMessages.put(permission, ChatColor.translateAlternateColorCodes('&', getConfig().getString("JoinMessages.permissionbasedjoinmessages.messages." + permission)));
                }
                getLogger().info("Enabled Permission Based Join Messages");
            }
            if (getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
                setupSQLConnection();
                Bukkit.getPluginCommand("joinmessage").setExecutor(new JoinMessageCommand());
                getLogger().info("Enabled Custom Join Messages");
            }
            if (Bukkit.getPluginManager().isPluginEnabled("SuperVanish") || Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) {
                if (getConfig().getBoolean("JoinMessages.SendOnUnVanish"))
                    Bukkit.getPluginManager().registerEvents(new PlayerShow(), this);
            }
        }
        if (getConfig().getBoolean("LeaveMessages.enabled")) {
            getLogger().info("Enabled Leave Messages Module");
            if (getConfig().getBoolean("LeaveMessages.permissionbasedleavemessages.enabled")) {
                for (String permission : getConfig().getConfigurationSection("LeaveMessages.permissionbasedleavemessages.messages").getKeys(false)) {
                    permissionLeaveMessages.put(permission, ChatColor.translateAlternateColorCodes('&', getConfig().getString("LeaveMessages.permissionbasedleavemessages.messages." + permission)));
                }
                getLogger().info("Enabled Permission Based Leave Messages");
            }
            if (getConfig().getBoolean("LeaveMessages.customleavemessages.enabled")) {
                setupSQLConnection();
                Bukkit.getPluginCommand("leavemessage").setExecutor(new LeaveMessageCommand());
                getLogger().info("Enabled Custom Leave Messages");
            }
            if (Bukkit.getPluginManager().isPluginEnabled("SuperVanish") || Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) {
                if (getConfig().getBoolean("LeaveMessages.SendOnVanish"))
                    Bukkit.getPluginManager().registerEvents(new PlayerHide(), this);
            }
        }
        if (getConfig().getBoolean("Gifts.enabled")) {
            try {
                if (GiftsStorage.exists())
                    loadGifts();
                Bukkit.getPluginCommand("gift").setExecutor(new GiftCommand());
                Bukkit.getPluginCommand("gifts").setExecutor(new GiftsCommand());
                getLogger().info("Enabled Gifts Module");
            } catch (Exception e) {
                e.printStackTrace();
                getLogger().info("ERROR: Could not enable Gifts Module, GiftsStorage.yml could not be loaded!!");
            }
        }
        Bukkit.getPluginManager().registerEvents(new PlayerDeath(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerKick(), this);
        if (hikari != null || connection != null) {
            setupmysql();
            startCaching();
        }
    }

    @Override
    public void onDisable() {
        Scheduler.stop();
        try {
            if (hikari != null && !hikari.isClosed()) {
                getLogger().info("Closing Storage....");
                Bukkit.getScheduler().cancelTask(cacheTaskid);
                hikari.close();
                connection = null;
                hikari = null;
                getLogger().info("Storage Closed");
            }
            if (getConfig().getBoolean("Gifts.enabled")) {
                saveGifts();
            }
        } catch (Exception e) {
            getLogger().severe("Could not Close Storage!");
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("vectorcorereload") || label.equalsIgnoreCase("vectorreload") ||
                label.equalsIgnoreCase("corereload") || label.equalsIgnoreCase("vcreload")) {
            if (sender.isOp()) {
                reloadConfig();
                Scheduler.stop();
                Scheduler.start(new Config());
                if (getConfig().getBoolean("JoinMessages.permissionbasedjoinmessages.enabled")) {
                    permissionJoinMessages.clear();
                    for (String permission : getConfig().getConfigurationSection("JoinMessages.permissionbasedjoinmessages.messages").getKeys(false)) {
                        permissionJoinMessages.put(permission, ChatColor.translateAlternateColorCodes('&', getConfig().getString("JoinMessages.permissionbasedjoinmessages.messages." + permission)));
                    }
                }
                if (getConfig().getBoolean("LeaveMessages.permissionbasedleavemessages.enabled")) {
                    permissionLeaveMessages.clear();
                    for (String permission : getConfig().getConfigurationSection("LeaveMessages.permissionbasedleavemessages.messages").getKeys(false)) {
                        permissionLeaveMessages.put(permission, ChatColor.translateAlternateColorCodes('&', getConfig().getString("LeaveMessages.permissionbasedleavemessages.messages." + permission)));
                    }
                }
                if (getConfig().getBoolean("Titles.enabled")) {
                    cacheTitles();
                }
                if (getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
                    cacheJoinMessages();
                }
                if (getConfig().getBoolean("LeaveMessages.customLeavemessages.enabled")) {
                    cacheLeaveMessages();
                }
                sender.sendMessage(prefix + ChatColor.GREEN + "Config reloaded!");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission for this command!");
            }
            return false;
        }
        return false;
    }

    private void setupSQLConnection() {
        if (hikari == null || connection == null) {
            getLogger().info("Establishing MYSQL connection...");
            host = getConfig().getString("MySQL.host");
            database = getConfig().getString("MySQL.database");
            username = getConfig().getString("MySQL.username");
            password = getConfig().getString("MySQL.password");
            port = getConfig().getInt("MySQL.port");
            extraArguments = getConfig().getString("MySQL.extraArguments");
            hikari = new HikariDataSource();
            hikari.addDataSourceProperty("serverName", host);
            hikari.addDataSourceProperty("port", port);
            hikari.addDataSourceProperty("cachePrepStmts", true);
            hikari.addDataSourceProperty("prepStmtCacheSize", 100);
            hikari.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
            hikari.addDataSourceProperty("useServerPrepStmts", true);
            hikari.setPassword(password);
            hikari.setUsername(username);
            hikari.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.extraArguments);
            hikari.setPoolName("Vector-Core");
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
            if (getConfig().getBoolean("Titles.enabled")) {
                String titles = "CREATE TABLE IF NOT EXISTS `" + database + "`.`vectorcore_titles` ( `ID` SMALLINT NOT NULL AUTO_INCREMENT , `RawTitle` VARCHAR(256) NOT NULL , PRIMARY KEY (`ID`))" +
                        " ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
                String equippedTitles = "CREATE TABLE IF NOT EXISTS `" + database + "`.`vectorcore_equippedtitles` ( `UUID` VARCHAR(36) NOT NULL , `TitleID` SMALLINT NOT NULL ," +
                        " `RawTitle` VARCHAR(256) NOT NULL , PRIMARY KEY (`UUID`)) ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
                PreparedStatement stmt1 = connection.prepareStatement(titles);
                PreparedStatement stmt2 = connection.prepareStatement(equippedTitles);
                stmt1.executeUpdate();
                stmt1.close();
                stmt2.executeUpdate();
                stmt2.close();
            }
            if (getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
                String joinMessages = "CREATE TABLE IF NOT EXISTS `" + database + "`.`vectorcore_joinmessages` ( `UUID` VARCHAR(36) NOT NULL ," +
                        "`RawMessage` VARCHAR(512) NOT NULL , PRIMARY KEY (`UUID`)) ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
                PreparedStatement stmt1 = connection.prepareStatement(joinMessages);
                stmt1.executeUpdate();
                stmt1.close();
            }
            if (getConfig().getBoolean("LeaveMessages.customleavemessages.enabled")) {
                String joinMessages = "CREATE TABLE IF NOT EXISTS `" + database + "`.`vectorcore_leavemessages` ( `UUID` VARCHAR(36) NOT NULL ," +
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
            if (getConfig().getBoolean("Titles.enabled")) {
                cacheTitles();
            }
            if (getConfig().getBoolean("JoinMessages.customjoinmessages")) {
                cacheJoinMessages();
            }
            if (getConfig().getBoolean("LeaveMessages.customLeavemessages.enabled")) {
                cacheLeaveMessages();
            }
        }, 200, 100);
        if (getConfig().getBoolean("Titles.enabled")) {
            cacheTitles();
        }
        if (getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
            cacheJoinMessages();
        }
        if (getConfig().getBoolean("LeaveMessages.customLeavemessages.enabled")) {
            cacheLeaveMessages();
        }
    }

    private void cacheTitles() {
        titles.clear();
        titles.put(0, "");
        equippedTitles.clear();
        try {
            String sqltitles = "SELECT * FROM `vectorcore_titles`;";
            PreparedStatement stmttitles = connection.prepareStatement(sqltitles);
            ResultSet resultstitles = stmttitles.executeQuery();
            while (resultstitles.next()) {
                String rawTitle = resultstitles.getString("RawTitle");
                if (rawTitle.contains("%sinquo%"))
                    rawTitle = rawTitle.replace("%sinquo%", "'");
                if (rawTitle.contains("%dubquo%"))
                    rawTitle = rawTitle.replace("%dubquo%", "\"");
                if (rawTitle.contains("%bcktck%"))
                    rawTitle = rawTitle.replace("%bcktck%", "`");
                titles.put(resultstitles.getInt("ID"), rawTitle);
            }
            resultstitles.close();
            stmttitles.close();
            String sqlequippedtitles = "SELECT * FROM `vectorcore_equippedtitles`;";
            PreparedStatement stmtequippedtitles = connection.prepareStatement(sqlequippedtitles);
            ResultSet resultsequippedtitles = stmtequippedtitles.executeQuery();
            while (resultsequippedtitles.next()) {
                String rawTitle = resultsequippedtitles.getString("RawTitle");
                if (rawTitle.contains("%sinquo%"))
                    rawTitle = rawTitle.replace("%sinquo%", "'");
                if (rawTitle.contains("%dubquo%"))
                    rawTitle = rawTitle.replace("%dubquo%", "\"");
                if (rawTitle.contains("%bcktck%"))
                    rawTitle = rawTitle.replace("%bcktck%", "`");
                equippedTitles.put(UUID.fromString(resultsequippedtitles.getString("UUID")), rawTitle);
            }
            resultsequippedtitles.close();
            stmtequippedtitles.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            getLogger().warning("Unable to cache titles from mysql database, title placeholders may no longer work!!");
        }
    }

    private void cacheJoinMessages() {
        customJoinMessages.clear();
        try {
            String sql = "SELECT * FROM `vectorcore_joinmessages`;";
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
            String sql = "SELECT * FROM `vectorcore_leavemessages`;";
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
