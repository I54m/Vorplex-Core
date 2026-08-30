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
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.luckperms.api.LuckPerms;
import net.vorplex.core.autoannouncer.AutoAnnouncerScheduler;
import net.vorplex.core.autorestart.AutoRestartConfig;
import net.vorplex.core.autorestart.AutoRestartLogger;
import net.vorplex.core.autorestart.AutoRestartScheduler;
import net.vorplex.core.chat.AdminChatCommand;
import net.vorplex.core.chat.AsyncChatListener;
import net.vorplex.core.chat.StaffChatCommand;
import net.vorplex.core.commands.*;
import net.vorplex.core.database.*;
import net.vorplex.core.listeners.JoinMessageListeners;
import net.vorplex.core.listeners.LeaveMessageListeners;
import net.vorplex.core.listeners.SafeLoginListeners;
import net.vorplex.core.util.ConfigUpdater;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VorplexCore extends JavaPlugin {

    // Misc Plugin variables
    @Getter
    @Setter(AccessLevel.PRIVATE)
    public static VorplexCore instance;
    @Getter
    private String prefix;
    @Getter
    private ExecutorService threadPool;
    @Getter
    private final ZonedDateTime startTime = ZonedDateTime.now();
    @Getter
    private final MiniMessage basicMM = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolvers(
                            StandardTags.color(),
                            StandardTags.gradient(),
                            StandardTags.rainbow(),
                            StandardTags.pride(),
                            StandardTags.reset(),
                            StandardTags.shadowColor(),
                            StandardTags.decorations()
                    )
                    .build())
            .build();


    // Config classes
    @Getter
    public AutoRestartConfig autoRestartConfig;
    @Getter
    public AutoRestartScheduler autoRestartScheduler;

    // Dependency variables
    @Getter
    private boolean placeholderAPI;
    @Getter
    private boolean premiumVanish;
    @Getter
    private LuckPerms luckPermsAPI;

    // Plugin storage Hashmaps
    @Getter
    private final Map<UUID, String> customJoinMessagesCache = new HashMap<>();
    @Getter
    private final Map<UUID, String> customLeaveMessagesCache = new HashMap<>();

    // Database Management variables
    @Getter
    private DatabaseManager databaseManager;
    @Getter
    private StorageProvider storageProvider;

    //Plugin reload command
    public final LiteralCommandNode<CommandSourceStack> RELOAD_COMMAND_NODE = Commands.literal("vorplexcorereload")
            .requires(ctx -> ctx.getSender().isOp())
            .executes((ctx) -> {
                autoRestartScheduler.shutdown();
                AutoAnnouncerScheduler.stop();
                boolean customJoinMessagesPreviousState = getConfig().getBoolean("JoinMessages.CustomJoinMessages.enabled", true);
                boolean customLeaveMessagesPreviousState = getConfig().getBoolean("JoinMessages.CustomLeaveMessages.enabled", true);
                reloadConfig();
                if (this.getConfig().getBoolean("AutoRestart.enabled")) {
                    autoRestartConfig = new AutoRestartConfig();
                    autoRestartScheduler = new AutoRestartScheduler(this);
                    autoRestartScheduler.start(autoRestartConfig);
                }
                if (this.getConfig().getBoolean("AutoAnnouncer.enabled"))
                    AutoAnnouncerScheduler.start();
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    if (!getCustomJoinMessagesCache().isEmpty() && !getConfig().getBoolean("JoinMessages.CustomJoinMessages.enabled", true))
                        getCustomJoinMessagesCache().clear();
                    if (!getCustomLeaveMessagesCache().isEmpty() && !getConfig().getBoolean("JoinMessages.CustomLeaveMessages.enabled", true))
                        getCustomLeaveMessagesCache().clear();

                    //cache online player custom join messages
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!customJoinMessagesPreviousState && getConfig().getBoolean("JoinMessages.CustomJoinMessages.enabled", true))
                            getCustomJoinMessagesCache().put(player.getUniqueId(), getStorageProvider().getJoinMessage(player.getUniqueId()));
                        if (!customLeaveMessagesPreviousState && getConfig().getBoolean("LeaveMessages.CustomLeaveMessages.enabled", true))
                            getCustomLeaveMessagesCache().put(player.getUniqueId(), getStorageProvider().getLeaveMessage(player.getUniqueId()));
                    }
                });
                ctx.getSource().getSender().sendRichMessage(getPrefix() + "<green>Config reloaded!");
                if (databaseManager.isConnected())
                    ctx.getSource().getSender().sendRichMessage(getPrefix() + "<red>Database changes require a reboot!");
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
        if (this.getConfig().getBoolean("MapCommand.enabled")) {
            getComponentLogger().info(Component.text("Enabling Map Command...").color(NamedTextColor.GREEN));
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(MapCommand.COMMAND_NODE));
        }
        if (this.getConfig().getBoolean("WikiCommand.enabled")) {
            getComponentLogger().info(Component.text("Enabling Wiki Command...").color(NamedTextColor.GREEN));
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(WikiCommand.COMMAND_NODE));
        }
        if (this.getConfig().getBoolean("AutoRestart.enabled")) {
            getComponentLogger().info(Component.text("Enabling AutoRestart Module...").color(NamedTextColor.GREEN));
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(AutoRestartCommand.COMMAND_NODE, List.of("restart", "reboot", "autoreboot", "autore")));
            AutoRestartLogger.init();
            AutoRestartLogger.info("Server Starting up...");
            AutoRestartLogger.info("Server was started at: " + getStartTime());
            autoRestartConfig = new AutoRestartConfig();
            autoRestartScheduler = new AutoRestartScheduler(this);
            autoRestartScheduler.start(autoRestartConfig);
        }
        if (getConfig().getBoolean("AutoAnnouncer.enabled")) {
            getComponentLogger().info(Component.text("Enabling Auto Announcer Module...").color(NamedTextColor.GREEN));
            AutoAnnouncerScheduler.start();
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

        if (getConfig().getBoolean("JoinMessages.CustomJoinMessages.enabled")) {
            getComponentLogger().info(Component.text("Enabling Custom Join Messages...").color(NamedTextColor.GREEN));
            databaseRequired = true;
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands ->
                    commands.registrar().register(JoinMessageCommand.COMMAND_NODE, List.of("joinm", "jmessage", "joinmsg", "jmsg"))
            );
        }
        if (getConfig().getBoolean("LeaveMessages.CustomLeaveMessages.enabled")) {
            getComponentLogger().info(Component.text("Enabling Custom Leave Messages...").color(NamedTextColor.GREEN));
            databaseRequired = true;
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands ->
                    commands.registrar().register(LeaveMessageCommand.COMMAND_NODE, List.of("leavem", "lmessage", "leavemsg", "lmsg"))
            );
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
        getComponentLogger().info(Component.text("Plugin loaded in: " + (System.nanoTime() - startTime) / 1000000 + "ms!").color(NamedTextColor.GREEN));
        getComponentLogger().info("───────────────────────────────────────────────────────────");
    }

    @Override
    public void onDisable() {
        if (this.getConfig().getBoolean("AutoRestart.enabled"))
            AutoRestartLogger.info("Server Shutting down...");
        AutoRestartLogger.close();
        if (autoRestartScheduler != null) {
            autoRestartScheduler.shutdown();
        }
        AutoAnnouncerScheduler.stop();
        if (databaseManager.isConnected())
            databaseManager.shutdownConnection();
        threadPool.shutdownNow();
    }
}
