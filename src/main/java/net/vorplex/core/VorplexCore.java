package net.vorplex.core;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
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
import net.vorplex.core.database.*;
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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VorplexCore extends JavaPlugin {

    // Misc Plugin variables
    @Getter
    @Setter(AccessLevel.PRIVATE)
    public static VorplexCore instance;
    @Getter
    private String prefix;
    private final File GiftsStorage = new File(this.getDataFolder(), "GiftsStorage.yml");
    @Getter
    private ExecutorService threadPool;

    // Config classes
    @Getter
    public AutoPickupConfig autoPickupConfig;
    @Getter
    public AutoRestartConfig autoRestartConfig;

    // Dependency variables
    @Getter
    private boolean placeholderAPI;
    @Getter
    private boolean premiumVanish;
    @Getter
    private LuckPerms luckPermsAPI;

    // Plugin storage Hashmaps
    public Map<UUID, String> customJoinMessages = new HashMap<>();
    public Map<UUID, String> customLeaveMessages = new HashMap<>();
    public Map<UUID, ArrayList<Gift>> gifts = new HashMap<>();

    // Database Management variables
    @Getter
    private DatabaseManager databaseManager;
    @Getter
    private StorageProvider storageProvider;

    // Legacy Variables - deprecated to be removed
    //TODO Temp prefix until all modules have been converted to minimessage format
    @Deprecated(since = "2.0-SNAPSHOT", forRemoval = true)
    public String LEGACY_PREFIX;
    @Deprecated(since = "2.0-SNAPSHOT", forRemoval = true)
    public boolean essentials = false;
    @Deprecated(since = "2.0-SNAPSHOT-1.4.2", forRemoval = true)
    public Connection connection;

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
        threadPool = Executors.newFixedThreadPool(4,
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("VorplexCore-Async");
                    return thread;
                });
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
        //register premiumVanish
        if (Bukkit.getPluginManager().isPluginEnabled("SuperVanish") || Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) {
            premiumVanish = true;
            getComponentLogger().info(Component.text("PremiumVanish Detected!").color(NamedTextColor.GREEN));
        } else {
            premiumVanish = false;
            getComponentLogger().info(Component.text("PremiumVanish NOT Detected!").color(NamedTextColor.RED));
        }
        //load modules
        boolean databaseRequired = false;
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

        if (getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
            getComponentLogger().info(Component.text("Enabling Custom Join Messages...").color(NamedTextColor.GREEN));
            databaseRequired = true;
//            Bukkit.getPluginCommand("joinmessage").setExecutor(new JoinMessageCommand());
        }
        if (getConfig().getBoolean("LeaveMessages.customleavemessages.enabled")) {
            getComponentLogger().info(Component.text("Enabling Custom Leave Messages...").color(NamedTextColor.GREEN));
            databaseRequired = true;
//            Bukkit.getPluginCommand("leavemessage").setExecutor(new LeaveMessageCommand());
        }


        if (databaseRequired) {
            StorageType storageType = StorageType.valueOf(getConfig().getString("Database.Type", "SQLITE").toUpperCase());
            databaseManager = new DatabaseManager(storageType);
            switch (storageType) {
                case SQLITE -> storageProvider = new SQLiteStorageProvider(databaseManager);
                case MYSQL -> storageProvider = new SQLStorageProvider(databaseManager);
            }
            databaseManager.initializeConnection();
            storageProvider.initialize();
        }

//        if (getConfig().getBoolean("Hub.enabled")) {
//            Bukkit.getPluginManager().registerEvents(new PlayerJoin(), this);
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
        getComponentLogger().info(Component.text("Plugin loaded in: " + (System.nanoTime() - startTime) / 1000000 + "ms!").color(NamedTextColor.GREEN));
        getComponentLogger().info("───────────────────────────────────────────────────────────");
    }

    @Override
    public void onDisable() {
        AutoRestartScheduler.stop();
        AutoAnnouncerScheduler.stop();
        if (databaseManager.isConnected())
            databaseManager.shutdownConnection();
        threadPool.shutdownNow();
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
