package net.vorplex.core;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.zaxxer.hikari.HikariDataSource;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.vorplex.core.autorestart.AutoRestartConfig;
import net.vorplex.core.autorestart.AutoRestartScheduler;
import net.vorplex.core.commands.AutoRestartCommand;
import net.vorplex.core.commands.BuyCommand;
import net.vorplex.core.objects.Gift;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
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
    @Setter
    public static VorplexCore instance;
    public AutoRestartConfig autoRestartConfig;
    public static boolean announce;
    private static int previousMessageNumber;
    private final File GiftsStorage = new File(this.getDataFolder(), "GiftsStorage.yml");
    private int cacheTaskid;
    // Legacy Variables - deprecated to be removed
    //TODO Temp prefix until all modules have been converted to minimessage format
    @Deprecated(since = "2.0-SNAPSHOT")
    public String LEGACY_PREFIX;
    public LuckPerms luckPermsAPI = null;

    // Plugin storage Hashmaps
    public Map<String, String> permissionJoinMessages = new HashMap<>();
    public Map<String, String> permissionLeaveMessages = new HashMap<>();
    public Map<UUID, String> customJoinMessages = new HashMap<>();
    public Map<UUID, String> customLeaveMessages = new HashMap<>();
    public Map<UUID, ArrayList<Gift>> gifts = new HashMap<>();
    @Deprecated(since = "2.0-SNAPSHOT")
    public Map<UUID, String> equippedTitles = new HashMap<>();
    private HikariDataSource hikari;
    private static String database;
    private int port;
    public Connection connection;
    @Getter
    private String prefix;
    // SQL Connection variables
    private String host, username;
    @Deprecated(since = "2.0-SNAPSHOT", forRemoval = true)
    public TreeMap<Integer, String> titles = new TreeMap<>();
    @Deprecated(since = "2.0-SNAPSHOT")
    public boolean essentials = false;
    @Deprecated(since = "2.0-SNAPSHOT", forRemoval = true)
    public boolean old = Bukkit.getServer().getVersion().contains("1.7") ||
            Bukkit.getServer().getVersion().contains("1.8") ||
            Bukkit.getServer().getVersion().contains("1.9") ||
            Bukkit.getServer().getVersion().contains("1.10") ||
            Bukkit.getServer().getVersion().contains("1.11") ||
            Bukkit.getServer().getVersion().contains("1.12");

    //Plugin reload command
    public final LiteralCommandNode<CommandSourceStack> RELOAD_COMMAND_NODE = Commands.literal("vorplexcorereload")
            .requires(ctx -> ctx.getSender().isOp())
            .executes((ctx) -> {
                reloadConfig();
                AutoRestartScheduler.stop();
                AutoRestartScheduler.start(new AutoRestartConfig());
//                if (getConfig().getBoolean("JoinMessages.permissionbasedjoinmessages.enabled")) {
//                    permissionJoinMessages.clear();
//                    for (String permission : getConfig().getConfigurationSection("JoinMessages.permissionbasedjoinmessages.messages").getKeys(false)) {
//                        permissionJoinMessages.put(permission, ChatColor.translateAlternateColorCodes('&', getConfig().getString("JoinMessages.permissionbasedjoinmessages.messages." + permission)));
//                    }
//                }
//                if (getConfig().getBoolean("LeaveMessages.permissionbasedleavemessages.enabled")) {
//                    permissionLeaveMessages.clear();
//                    for (String permission : getConfig().getConfigurationSection("LeaveMessages.permissionbasedleavemessages.messages").getKeys(false)) {
//                        permissionLeaveMessages.put(permission, ChatColor.translateAlternateColorCodes('&', getConfig().getString("LeaveMessages.permissionbasedleavemessages.messages." + permission)));
//                    }
//                }
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
        saveDefaultConfig();
        this.getConfig().options().copyDefaults(true);
        prefix = this.getConfig().getString("Plugin-Prefix", "<dark_purple>[<light_purple>Vorplex-Core<dark_purple>] ");
        LEGACY_PREFIX = PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(prefix));
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(this.RELOAD_COMMAND_NODE, List.of("corereload", "vcreload", "vorplexrelaod")));
        //load modules
        if (this.getConfig().getBoolean("buycommand.enabled")) {
            getComponentLogger().info(Component.text("Enabling Buy Command...").color(NamedTextColor.GREEN));
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands ->
                    commands.registrar().register(BuyCommand.COMMAND_NODE));
        }
        if (this.getConfig().getBoolean("AutoRestart.enabled")) {
            getComponentLogger().info(Component.text("Enabling AutoRestart Module...").color(NamedTextColor.GREEN));
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(AutoRestartCommand.COMMAND_NODE, List.of("restart", "reboot", "autoreboot", "autore")));
            autoRestartConfig = new AutoRestartConfig();
            AutoRestartScheduler.start(autoRestartConfig);
        }
//        if (this.getConfig().getBoolean("VorplexServer.enabled")) {
//            this.getServer().getPluginManager().registerEvents(new CommandPreProcess(), this);
//            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
//            getLogger().info("Enabled Server Module");
//        }
//        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
//        if (provider != null)
//            luckPermsAPI = provider.getProvider();
//        else {
//            getLogger().severe("Luckperms not detected! Disabling plugin!");
//            this.setEnabled(false);
//            return;
//        }
//
//        if (getConfig().getBoolean("Announcer.enabled")) {
//            announce = false;
//            getLogger().info("Detected no players online! disabling announcements to save resources!");
//            Bukkit.getPluginManager().registerEvents(new PlayerQuit(), this);
//            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
//                if (announce) {
//                    String message;
//                    int messageNumber;
//                    int maxMesages;
//                    maxMesages = getConfig().getInt("Announcer.NumberOfMessages");
//                    if (getConfig().getBoolean("Announcer.Random"))
//                        messageNumber = (int) ((Math.random() * maxMesages) + 1);
//                    else {
//                        messageNumber = previousMessageNumber + 1;
//                        if (previousMessageNumber >= maxMesages)
//                            previousMessageNumber = 0;
//                        else previousMessageNumber++;
//                    }
//                    message = getConfig().getString("Announcer.Messages." + messageNumber + ".text");
//                    String prefix = getConfig().getString("Announcer.Prefix");
//
//                    ClickEvent.Action clickEventAction = ClickEvent.Action.valueOf(getConfig().getString("Announcer.Messages." + messageNumber + ".clickevent.action"));
//                    String clickEventValue = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Announcer.Messages." + messageNumber + ".clickevent.value"));
//                    HoverEvent.Action hoverEventAction = HoverEvent.Action.valueOf(getConfig().getString("Announcer.Messages." + messageNumber + ".hoverevent.action"));
//                    BaseComponent[] hoverEventValue = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Announcer.Messages." + messageNumber + ".hoverevent.value")));
//                    TextComponent messagetext = new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix) + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', message)));
//                    messagetext.setHoverEvent(new HoverEvent(hoverEventAction, hoverEventValue));
//                    messagetext.setClickEvent(new ClickEvent(clickEventAction, clickEventValue));
//                    for (Player players : Bukkit.getOnlinePlayers()) {
//                        players.spigot().sendMessage(messagetext);
//                    }
//                }
//            }, 720, (getConfig().getInt("Announcer.Interval") * 20));
//            getLogger().info("Enabled Auto Announcer Module");
//        }
//        if (getConfig().getBoolean("Announcer.enabled") ||
//                getConfig().getBoolean("JoinMessages.enabled") ||
//                getConfig().getBoolean("Hub.enabled") ||
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
//        if (getConfig().getBoolean("RankTitle.enabled")) {
//            Bukkit.getPluginCommand("ranktitle").setExecutor(new RankTitleCommand());
//            getLogger().info("Enabled Rank Title Module");
//        }
//        if (getConfig().getBoolean("JoinMessages.enabled")) {
//            getLogger().info("Enabled Join Messages Module");
//            if (getConfig().getBoolean("JoinMessages.permissionbasedjoinmessages.enabled")) {
//                for (String permission : getConfig().getConfigurationSection("JoinMessages.permissionbasedjoinmessages.messages").getKeys(false)) {
//                    permissionJoinMessages.put(permission, ChatColor.translateAlternateColorCodes('&', getConfig().getString("JoinMessages.permissionbasedjoinmessages.messages." + permission)));
//                }
//                getLogger().info("Enabled Permission Based Join Messages");
//            }
//            if (getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
//                setupSQLConnection();
//                Bukkit.getPluginCommand("joinmessage").setExecutor(new JoinMessageCommand());
//                getLogger().info("Enabled Custom Join Messages");
//            }
//            if (Bukkit.getPluginManager().isPluginEnabled("SuperVanish") || Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) {
//                if (getConfig().getBoolean("JoinMessages.SendOnUnVanish"))
//                    Bukkit.getPluginManager().registerEvents(new PlayerShow(), this);
//            }
//        }
//        if (getConfig().getBoolean("LeaveMessages.enabled")) {
//            getLogger().info("Enabled Leave Messages Module");
//            if (getConfig().getBoolean("LeaveMessages.permissionbasedleavemessages.enabled")) {
//                for (String permission : getConfig().getConfigurationSection("LeaveMessages.permissionbasedleavemessages.messages").getKeys(false)) {
//                    permissionLeaveMessages.put(permission, ChatColor.translateAlternateColorCodes('&', getConfig().getString("LeaveMessages.permissionbasedleavemessages.messages." + permission)));
//                }
//                getLogger().info("Enabled Permission Based Leave Messages");
//            }
//            if (getConfig().getBoolean("LeaveMessages.customleavemessages.enabled")) {
//                setupSQLConnection();
//                Bukkit.getPluginCommand("leavemessage").setExecutor(new LeaveMessageCommand());
//                getLogger().info("Enabled Custom Leave Messages");
//            }
//            if (Bukkit.getPluginManager().isPluginEnabled("SuperVanish") || Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) {
//                if (getConfig().getBoolean("LeaveMessages.SendOnVanish"))
//                    Bukkit.getPluginManager().registerEvents(new PlayerHide(), this);
//            }
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
//        Bukkit.getPluginManager().registerEvents(new PlayerDeath(), this);
//        Bukkit.getPluginManager().registerEvents(new PlayerKick(), this);
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
            if (getConfig().getBoolean("Titles.enabled")) {
                String titles = "CREATE TABLE IF NOT EXISTS `" + database + "`.`vorplexcore_titles` ( `ID` SMALLINT NOT NULL AUTO_INCREMENT , `RawTitle` VARCHAR(256) NOT NULL , PRIMARY KEY (`ID`))" +
                        " ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
                String equippedTitles = "CREATE TABLE IF NOT EXISTS `" + database + "`.`vorplexcore_equippedtitles` ( `UUID` VARCHAR(36) NOT NULL , `TitleID` SMALLINT NOT NULL ," +
                        " `RawTitle` VARCHAR(256) NOT NULL , PRIMARY KEY (`UUID`)) ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";
                PreparedStatement stmt1 = connection.prepareStatement(titles);
                PreparedStatement stmt2 = connection.prepareStatement(equippedTitles);
                stmt1.executeUpdate();
                stmt1.close();
                stmt2.executeUpdate();
                stmt2.close();
            }
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
